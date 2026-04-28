/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.live;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.livebus.LiveDataBus;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.message.LiveStreamingEndMessageContent;
import cn.wildfirechat.message.LiveStreamingStartMessageContent;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.model.ChatRoomMembersInfo;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GetChatRoomMembersInfoCallback;
import cn.wildfirechat.remote.OnReceiveMessageListener;

/**
 * 直播流媒体 SDK 唯一入口
 *
 * <p>面向开发者提供直播全链路封装。上层 UI 只需调用此类方法，
 * 无需关心 WebRTC 信令、聊天室保活、消息编解码等底层细节。</p>
 *
 * <h2>主播流程</h2>
 * <ol>
 *   <li>用 {@link #generateCallId()} / {@link #generatePin()} 生成直播参数</li>
 *   <li>调用 AVEngineKit.startConference() 开启 WebRTC 会议</li>
 *   <li>会话 Connected 后调用 {@link #onHostSessionConnected} — SDK 接管聊天室生命周期</li>
 *   <li>调用 {@link #sendLiveStartNotification} 通知机器人推流和会话成员</li>
 *   <li>调用 {@link #getViewers} 获取聊天室实时观众列表（用于连麦邀请）</li>
 *   <li>结束时调用 {@link #sendLiveEndNotification} + {@link #onSessionEnded}</li>
 * </ol>
 *
 * <h2>观众流程</h2>
 * <ol>
 *   <li>调用 {@link #getHlsUrl} 获取播放地址</li>
 *   <li>订阅 {@link #EVENT_CO_STREAM_INVITE} 监听主播邀请</li>
 *   <li>调用 {@link #requestCoStream} 发起连麦申请</li>
 *   <li>调用 {@link #joinConferenceForCoStream} 加入 WebRTC 连麦</li>
 * </ol>
 *
 * <h2>LiveDataBus 事件</h2>
 * <ul>
 *   <li>{@link #EVENT_CO_STREAM_REQUEST}  主播收到观众连麦请求，value = {@link LiveCoStreamContent}</li>
 *   <li>{@link #EVENT_CO_STREAM_INVITE}   观众收到主播连麦邀请，value = {@link LiveCoStreamContent}</li>
 *   <li>{@link #EVENT_CO_STREAM_ACCEPTED} 连麦被对端接受，value = {@link LiveCoStreamContent}</li>
 *   <li>{@link #EVENT_CO_STREAM_REJECTED} 连麦被对端拒绝，value = {@link LiveCoStreamContent}</li>
 * </ul>
 */
public class LiveStreamingKit implements OnReceiveMessageListener {

    // ── Events ──────────────────────────────────────────────────────────────

    /** 主播收到观众连麦请求 */
    public static final String EVENT_CO_STREAM_REQUEST  = "kLiveCoStreamRequest";
    /** 观众收到主播连麦邀请 */
    public static final String EVENT_CO_STREAM_INVITE   = "kLiveCoStreamInvite";
    /** 连麦被对端接受 */
    public static final String EVENT_CO_STREAM_ACCEPTED = "kLiveCoStreamAccepted";
    /** 连麦被对端拒绝 */
    public static final String EVENT_CO_STREAM_REJECTED = "kLiveCoStreamRejected";

    // ── Callbacks ────────────────────────────────────────────────────────────

    /** 获取直播观众列表回调（过滤了主播自身和推流机器人） */
    public interface GetViewersCallback {
        void onSuccess(List<String> userIds);
        void onFailure();
    }

    // ── Singleton ────────────────────────────────────────────────────────────

    private static LiveStreamingKit instance;

    private LiveStreamingKit() {}

    public static void init(Application application) {
        if (instance == null) {
            instance = new LiveStreamingKit();
            ChatManager.Instance().addOnReceiveMessageListener(instance);
        }
    }

    public static LiveStreamingKit getInstance() {
        if (instance == null) throw new IllegalStateException("LiveStreamingKit.init() not called");
        return instance;
    }

    // ── State ────────────────────────────────────────────────────────────────

    private LiveSession currentSession;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable chatroomKeepaliveRunnable;

    /** 待处理的连麦请求 userId 列表（主播侧） */
    private final List<String> coStreamRequests = new ArrayList<>();
    /** 当前连麦中的 userId 列表（主播侧） */
    private final List<String> activeCoStreamers = new ArrayList<>();

    /** 当前主播侧直播会话，未开播时为 null */
    public LiveSession getCurrentSession() {
        return currentSession;
    }

    /** 待处理的连麦申请列表（主播侧） */
    public List<String> getCoStreamRequests() {
        return new ArrayList<>(coStreamRequests);
    }

    /** 当前连麦中的观众列表（主播侧） */
    public List<String> getActiveCoStreamers() {
        return new ArrayList<>(activeCoStreamers);
    }

    // ── Factory helpers ──────────────────────────────────────────────────────

    /** 生成 16 位 callId */
    public static String generateCallId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /** 生成 4 位 PIN */
    public static String generatePin() {
        return String.format("%04d", new Random().nextInt(10000));
    }

    /** 根据 callId 拼接 HLS 观看地址 */
    public static String getHlsUrl(String callId) {
        return Config.LIVE_STREAMING_ADDRESS + callId + "/stream.m3u8";
    }

    // ── Host session lifecycle ────────────────────────────────────────────────

    /**
     * AVEngineKit 会话 Connected 后调用，SDK 初始化会话状态并开始聊天室保活。
     *
     * @param callId     直播/聊天室 ID
     * @param pin        连麦 PIN
     * @param hostUserId 主播 userId
     * @param title      直播标题
     */
    public void onHostSessionConnected(String callId, String pin, String hostUserId, String title) {
        this.currentSession = new LiveSession(callId, pin, hostUserId, title);
        joinChatRoom(callId);
        startChatroomKeepalive();
    }

    /**
     * 直播结束时调用（endCall 之后），SDK 停止聊天室保活并清理状态。
     */
    public void onSessionEnded() {
        stopChatroomKeepalive();
        currentSession = null;
        coStreamRequests.clear();
        activeCoStreamers.clear();
    }

    /**
     * 向推流机器人（line=1）及当前会话发送开播消息。
     * 推流机器人收到后自动加入会议并推 HLS 流；
     * 会话成员收到后可点击消息进入观看页面。
     *
     * @param conversation 当前会话（群聊或单聊）
     * @param title        直播标题
     */
    public void sendLiveStartNotification(Conversation conversation, String title) {
        if (currentSession == null) return;
        LiveStreamingStartMessageContent content = new LiveStreamingStartMessageContent(
            currentSession.callId, currentSession.hostUserId, title,
            "", System.currentTimeMillis() / 1000,
            false, false, false, currentSession.pin, "");

        if (!TextUtils.isEmpty(Config.LIVE_STREAMING_ROBOT)) {
            Conversation robotConv = new Conversation(
                Conversation.ConversationType.Single, Config.LIVE_STREAMING_ROBOT, 1);
            ChatManager.Instance().sendMessage(robotConv, content, null, 0, null);
        }
        ChatManager.Instance().sendMessage(conversation, content, null, 0, null);
    }

    /**
     * 向推流机器人及当前会话发送结束直播消息。
     *
     * @param conversation 当前会话
     */
    public void sendLiveEndNotification(Conversation conversation) {
        if (currentSession == null) return;
        LiveStreamingEndMessageContent endContent =
            new LiveStreamingEndMessageContent(currentSession.callId);

        if (!TextUtils.isEmpty(Config.LIVE_STREAMING_ROBOT)) {
            Conversation robotConv = new Conversation(
                Conversation.ConversationType.Single, Config.LIVE_STREAMING_ROBOT, 1);
            ChatManager.Instance().sendMessage(robotConv, endContent, null, 0, null);
        }
        ChatManager.Instance().sendMessage(conversation, endContent, null, 0, null);
    }

    // ── Host: co-stream management ────────────────────────────────────────────

    /**
     * 获取当前聊天室（直播实时观众）的成员列表，用于主播发起连麦邀请。
     * 已自动过滤主播自身和推流机器人。
     *
     * @param callId   直播间 ID（即聊天室 ID）
     * @param callback 成功返回 userId 列表；失败调用 onFailure()
     */
    public void getViewers(String callId, GetViewersCallback callback) {
        String hostId = currentSession != null
            ? currentSession.hostUserId
            : ChatManager.Instance().getUserId();

        ChatManager.Instance().getChatRoomMembersInfo(callId, 200,
            new GetChatRoomMembersInfoCallback() {
                @Override
                public void onSuccess(ChatRoomMembersInfo info) {
                    List<String> viewers = new ArrayList<>();
                    if (info != null && info.members != null) {
                        for (String uid : info.members) {
                            if (!uid.equals(hostId)
                                && !uid.equals(Config.LIVE_STREAMING_ROBOT)) {
                                viewers.add(uid);
                            }
                        }
                    }
                    callback.onSuccess(viewers);
                }

                @Override
                public void onFail(int errorCode) {
                    callback.onFailure();
                }

            });
    }

    /** 主播邀请观众连麦 */
    public void inviteCoStream(String targetUserId) {
        if (currentSession == null) return;
        sendSignal(targetUserId, LiveCoStreamContent.ACTION_INVITE, targetUserId);
    }

    /** 主播接受观众的连麦请求 */
    public void acceptCoStreamRequest(String requesterId) {
        if (currentSession == null) return;
        sendSignal(requesterId, LiveCoStreamContent.ACTION_ACCEPT, requesterId);
        coStreamRequests.remove(requesterId);
        if (!activeCoStreamers.contains(requesterId)) activeCoStreamers.add(requesterId);
    }

    /** 主播拒绝观众的连麦请求 */
    public void rejectCoStreamRequest(String requesterId) {
        if (currentSession == null) return;
        sendSignal(requesterId, LiveCoStreamContent.ACTION_REJECT, requesterId);
        coStreamRequests.remove(requesterId);
    }

    /**
     * 主播结束指定观众的连麦。直接踢出 WebRTC 会议，观众侧会收到 didCallEndWithReason 回调。
     */
    public void endCoStreamForUser(String userId) {
        activeCoStreamers.remove(userId);
        try {
            AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
            if (session != null) {
                session.kickoffParticipant(userId, null);
            }
        } catch (Exception ignored) {}
    }

    /**
     * 参与者实际入会后调用（由 didParticipantJoined 触发），仅更新内部状态，不发送信令。
     */
    public void onParticipantActuallyJoined(String userId) {
        if (!activeCoStreamers.contains(userId)) {
            coStreamRequests.remove(userId);
            activeCoStreamers.add(userId);
        }
    }

    /**
     * 参与者离开会议后调用（由 didParticipantLeft 触发），仅更新内部状态。
     */
    public void onParticipantLeft(String userId) {
        activeCoStreamers.remove(userId);
    }

    // ── Audience: co-stream ───────────────────────────────────────────────────

    /**
     * 构建可通过 ForwardActivity 分享的直播消息（主播侧）。
     * 将内容构建逻辑集中在 Kit 内，避免 Activity 知道 LiveStreamingStartMessageContent 的细节。
     *
     * @return 包含 LiveStreamingStartMessageContent 的 Message，未开播时返回 null
     */
    public Message buildHostShareMessage() {
        if (currentSession == null) return null;
        LiveStreamingStartMessageContent content = new LiveStreamingStartMessageContent(
            currentSession.callId, currentSession.hostUserId, currentSession.title, "",
            System.currentTimeMillis() / 1000, false, false, false, currentSession.pin, "");
        Message msg = new Message();
        msg.content = content;
        return msg;
    }

    /** 观众向主播发起连麦申请 */
    public void requestCoStream(String hostUserId, String callId, String pin,
                                String host, String title) {
        String myUserId = ChatManager.Instance().getUserId();
        LiveCoStreamContent content = new LiveCoStreamContent(
            callId, pin, host, title, LiveCoStreamContent.ACTION_REQUEST, myUserId);
        Conversation conv = new Conversation(Conversation.ConversationType.Single, hostUserId, 0);
        ChatManager.Instance().sendMessage(conv, content, null, 0, null);
    }

    /** 观众拒绝主播的连麦邀请 */
    public void rejectCoStreamInvite(LiveCoStreamContent invite) {
        String myUserId = ChatManager.Instance().getUserId();
        LiveCoStreamContent reject = new LiveCoStreamContent(
            invite.getCallId(), invite.getPin(), invite.getHost(),
            invite.getTitle(), LiveCoStreamContent.ACTION_REJECT, myUserId);
        Conversation conv = new Conversation(
            Conversation.ConversationType.Single, invite.getHost(), 0);
        ChatManager.Instance().sendMessage(conv, reject, null, 0, null);
    }

    /**
     * 观众加入 WebRTC 连麦会议（同意连麦后调用）。
     *
     * @param content 连麦信令内容（含 callId/pin/host/title）
     * @return 成功返回 CallSession，会话创建失败返回 null
     */
    public AVEngineKit.CallSession joinConferenceForCoStream(LiveCoStreamContent content, AVEngineKit.CallSessionCallback callback) {
        return AVEngineKit.Instance().joinConference(
            content.getCallId(), false, content.getPin(), content.getHost(),
            content.getTitle(), "",
            false,   // audience=false：成为活跃连麦参与者
            false,   // advanced
            false,   // muteAudio
            false,   // muteVideo
            false,   // screenSharing
            callback);
    }

    // ── OnReceiveMessageListener ──────────────────────────────────────────────

    @Override
    public void onReceiveMessage(List<Message> messages, boolean hasMore) {
        for (Message msg : messages) {
            if (msg.content instanceof LiveCoStreamContent) {
                dispatch((LiveCoStreamContent) msg.content);
            }
        }
    }

    private void dispatch(LiveCoStreamContent content) {
        String uid = content.getTargetUserId();
        switch (content.getAction()) {
            case LiveCoStreamContent.ACTION_REQUEST:
                if (!coStreamRequests.contains(uid)) coStreamRequests.add(uid);
                LiveDataBus.setValue(EVENT_CO_STREAM_REQUEST, content);
                break;
            case LiveCoStreamContent.ACTION_INVITE:
                LiveDataBus.setValue(EVENT_CO_STREAM_INVITE, content);
                break;
            case LiveCoStreamContent.ACTION_ACCEPT:
                coStreamRequests.remove(uid);
                if (!activeCoStreamers.contains(uid)) activeCoStreamers.add(uid);
                LiveDataBus.setValue(EVENT_CO_STREAM_ACCEPTED, content);
                break;
            case LiveCoStreamContent.ACTION_REJECT:
                coStreamRequests.remove(uid);
                activeCoStreamers.remove(uid);
                LiveDataBus.setValue(EVENT_CO_STREAM_REJECTED, content);
                break;
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void sendSignal(String toUserId, int action, String targetUserId) {
        LiveSession s = currentSession;
        LiveCoStreamContent content = new LiveCoStreamContent(
            s.callId, s.pin, s.hostUserId, s.title, action, targetUserId);
        Conversation conv = new Conversation(Conversation.ConversationType.Single, toUserId, 0);
        ChatManager.Instance().sendMessage(conv, content, null, 0, null);
    }

    private void joinChatRoom(String callId) {
        ChatManager.Instance().joinChatRoom(callId, null);
    }

    private void startChatroomKeepalive() {
        stopChatroomKeepalive();
        chatroomKeepaliveRunnable = new Runnable() {
            @Override
            public void run() {
                if (currentSession != null) {
                    joinChatRoom(currentSession.callId);
                    handler.postDelayed(this, 3 * 60 * 1000L);
                }
            }
        };
        handler.postDelayed(chatroomKeepaliveRunnable, 3 * 60 * 1000L);
    }

    private void stopChatroomKeepalive() {
        if (chatroomKeepaliveRunnable != null) {
            handler.removeCallbacks(chatroomKeepaliveRunnable);
            chatroomKeepaliveRunnable = null;
        }
    }
}
