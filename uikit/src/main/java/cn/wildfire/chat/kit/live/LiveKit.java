/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.live;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.live.model.CreateLiveRequest;
import cn.wildfire.chat.kit.live.model.LiveInfo;
import cn.wildfire.chat.kit.livebus.LiveDataBus;
import cn.wildfire.chat.kit.net.OKHttpHelper;
import cn.wildfire.chat.kit.net.SimpleCallback;
import cn.wildfire.chat.kit.net.base.StatusResult;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.model.ChatRoomMembersInfo;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GeneralCallback2;
import cn.wildfirechat.remote.GetChatRoomMembersInfoCallback;
import cn.wildfirechat.remote.OnReceiveMessageListener;

/**
 * 直播流媒体 SDK 唯一入口
 *
 * <h2>LiveDataBus 事件</h2>
 * <ul>
 *   <li>{@link #EVENT_CO_STREAM_REQUEST}  主播收到观众连麦请求，value = {@link LiveCoStreamContent}</li>
 *   <li>{@link #EVENT_CO_STREAM_INVITE}   观众收到主播连麦邀请，value = {@link LiveCoStreamContent}</li>
 *   <li>{@link #EVENT_CO_STREAM_ACCEPTED} 连麦被对端接受，value = {@link LiveCoStreamContent}</li>
 *   <li>{@link #EVENT_CO_STREAM_REJECTED} 连麦被对端拒绝，value = {@link LiveCoStreamContent}</li>
 * </ul>
 */
public class LiveKit implements OnReceiveMessageListener {

    private boolean isServiceAvailable;

    // ── Events ──────────────────────────────────────────────────────────────

    /** 主播收到观众连麦请求 */
    public static final String EVENT_CO_STREAM_REQUEST = "kLiveCoStreamRequest";
    /** 观众收到主播连麦邀请 */
    public static final String EVENT_CO_STREAM_INVITE = "kLiveCoStreamInvite";
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

    private static LiveKit instance;

    private LiveKit() {
    }

    public static void init(Application application) {
        if (instance == null) {
            instance = new LiveKit();

            Config.LIVE_ADDRESS = Config.LIVE_ADDRESS.endsWith("/") ? Config.LIVE_ADDRESS.substring(0, Config.LIVE_ADDRESS.length() - 1) : Config.LIVE_ADDRESS;
            String host = extractHost(Config.LIVE_ADDRESS);
            ChatManager.Instance().getAuthCode("admin", 2, host, new GeneralCallback2() {
                @Override
                public void onSuccess(String result) {

                    Map<String, Object> params = new HashMap<>(1);
                    params.put("authCode", result);
                    String url = Config.LIVE_ADDRESS + "/api/user_login";
                    OKHttpHelper.post(url, params, new SimpleCallback<StatusResult>() {
                        @Override
                        public void onUiSuccess(StatusResult r) {
                            if (r.isSuccess()) {
                                instance.isServiceAvailable = true;
                            } else {
                                Toast.makeText(application, "登录直播服务失败 " + r.getCode(), Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onUiFailure(int code, String msg) {
                            Toast.makeText(application, "直播服务初始化失败" + msg, Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onFail(int errorCode) {
                    Toast.makeText(application, "直播服务初始化失败" + errorCode, Toast.LENGTH_SHORT).show();
                }
            });
            ChatManager.Instance().addOnReceiveMessageListener(instance);
        }
    }

    public static LiveKit getInstance() {
        if (instance == null) throw new IllegalStateException("LiveStreamingKit.init() not called");
        return instance;
    }

    public boolean isServiceAvailable() {
        return isServiceAvailable;
    }

    public void createLive(CreateLiveRequest request, SimpleCallback<LiveInfo> callback) {
        if (!isServiceAvailable) {
            callback.onUiFailure(-1, "未登录，或服务不可用");
            return;
        }
        String url = Config.LIVE_ADDRESS + "/api/live";
        OKHttpHelper.post(url, request, callback);
    }

    public void getLiveInfo(String liveId, SimpleCallback<LiveInfo> callback) {
        if (!isServiceAvailable) {
            callback.onUiFailure(-1, "未登录，或服务不可用");
            return;
        }
        String url = Config.LIVE_ADDRESS + "/api/live/" + liveId;
        OKHttpHelper.get(url, null, callback);
    }

    public void startLiveStream(String liveId, SimpleCallback<Void> callback) {
        if (!isServiceAvailable) {
            callback.onUiFailure(-1, "未登录，或服务不可用");
            return;
        }
        String url = Config.LIVE_ADDRESS + "/api/live/" + liveId;
        OKHttpHelper.put(url, null, callback);
    }

    // ── State ────────────────────────────────────────────────────────────────

    private LiveInfo currentLiveInfo;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable chatroomKeepaliveRunnable;

    /**
     * 待处理的连麦请求（主播侧）：userId → 原始请求内容（保留 audioOnly 等参数，accept 时回传给观众）。
     * 使用 LinkedHashMap 保证顺序，便于 UI 按请求先后排序。
     */
    private final Map<String, LiveCoStreamContent> coStreamRequestMap = new LinkedHashMap<>();
    /** 当前连麦中的 userId 列表（主播侧） */
    private final List<String> activeCoStreamers = new ArrayList<>();

    /** 当前主播侧直播信息，未开播时为 null */
    public LiveInfo getCurrentLiveInfo() {
        return currentLiveInfo;
    }

    public void setCurrentLiveInfo(LiveInfo liveInfo) {
        this.currentLiveInfo = liveInfo;
    }

    /** 待处理的连麦申请列表（主播侧） */
    public List<String> getCoStreamRequests() {
        return new ArrayList<>(coStreamRequestMap.keySet());
    }

    /** 当前连麦中的观众列表（主播侧） */
    public List<String> getActiveCoStreamers() {
        return new ArrayList<>(activeCoStreamers);
    }

    // ── Host session lifecycle ────────────────────────────────────────────────

    /**
     * AVEngineKit 会话 Connected 后调用，SDK 初始化会话状态并开始聊天室保活。
     *
     * @param liveInfo 直播信息
     */
    public void onHostSessionConnected(LiveInfo liveInfo) {
        this.currentLiveInfo = liveInfo;
        joinLiveChatRoom();
    }

    /**
     * 直播结束时调用（endCall 之后），SDK 停止聊天室保活并清理状态。
     */
    public void reset() {
        ChatManager.Instance().quitChatRoom(currentLiveInfo.getLiveId(), null);
        stopLiveChatroomKeepalive();
        currentLiveInfo = null;
        coStreamRequestMap.clear();
        activeCoStreamers.clear();
    }

    // ── Host: co-stream management ────────────────────────────────────────────

    /**
     * 获取当前聊天室（直播实时观众）的成员列表，用于主播发起连麦邀请。
     * 已自动过滤主播自身和推流机器人。
     *
     * @param liveInfo
     * @param callback 成功返回 userId 列表；失败调用 onFailure()
     */
    public void getViewers(LiveInfo liveInfo, GetViewersCallback callback) {
        String hostId = currentLiveInfo != null
                ? currentLiveInfo.getHost()
                : ChatManager.Instance().getUserId();

        ChatManager.Instance().getChatRoomMembersInfo(liveInfo.getLiveId(), 200,
                new GetChatRoomMembersInfoCallback() {
                    @Override
                    public void onSuccess(ChatRoomMembersInfo info) {
                        List<String> viewers = new ArrayList<>();
                        if (info != null && info.members != null) {
                            for (String uid : info.members) {
                                if (!uid.equals(hostId)
                                        && !uid.equals(liveInfo.getLiveBotId())) {
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

    /** 主播邀请观众视频连麦（主播侧默认发起视频邀请，audioOnly=false） */
    public void inviteCoStream(String targetUserId) {
        if (currentLiveInfo == null) return;
        sendSignal(targetUserId, LiveCoStreamContent.ACTION_INVITE, targetUserId, false);
    }

    /**
     * 主播接受观众的连麦请求。
     * 将原始请求中的 audioOnly 标志回传给观众，观众据此决定以音频还是视频模式加入会议。
     */
    public void acceptCoStreamRequest(String requesterId) {
        if (currentLiveInfo == null) return;
        LiveCoStreamContent original = coStreamRequestMap.get(requesterId);
        boolean audioOnly = original != null && original.isAudioOnlyRequest();
        sendSignal(requesterId, LiveCoStreamContent.ACTION_ACCEPT, requesterId, audioOnly);
        coStreamRequestMap.remove(requesterId);
        if (!activeCoStreamers.contains(requesterId)) activeCoStreamers.add(requesterId);
    }

    /** 主播拒绝观众的连麦请求 */
    public void rejectCoStreamRequest(String requesterId) {
        if (currentLiveInfo == null) return;
        sendSignal(requesterId, LiveCoStreamContent.ACTION_REJECT, requesterId, false);
        coStreamRequestMap.remove(requesterId);
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
        } catch (Exception ignored) {
        }
    }

    /**
     * 参与者实际入会后调用（由 didParticipantJoined 触发），仅更新内部状态，不发送信令。
     */
    public void onParticipantActuallyJoined(String userId) {
        if (!activeCoStreamers.contains(userId)) {
            coStreamRequestMap.remove(userId);
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
     * 观众向主播发起连麦申请。
     *
     * @param audioOnlyRequest true=音频连麦，false=视频连麦
     */
    public void requestCoStream(String host, String callId, boolean audioOnly, String pin,
                                String title, boolean audioOnlyRequest) {
        String myUserId = ChatManager.Instance().getUserId();
        LiveCoStreamContent content = new LiveCoStreamContent(
                callId, audioOnly, pin, host, title, LiveCoStreamContent.ACTION_REQUEST, myUserId, audioOnlyRequest);
        Conversation conv = new Conversation(Conversation.ConversationType.Single, host, 0);
        ChatManager.Instance().sendMessage(conv, content, null, 0, null);
    }

    /** 观众拒绝主播的连麦邀请 */
    public void rejectCoStreamInvite(LiveCoStreamContent invite) {
        String myUserId = ChatManager.Instance().getUserId();
        LiveCoStreamContent reject = new LiveCoStreamContent(
                invite.getCallId(), invite.isAudioOnly(), invite.getPin(), invite.getHost(),
                invite.getTitle(), LiveCoStreamContent.ACTION_REJECT, myUserId, invite.isAudioOnlyRequest());
        Conversation conv = new Conversation(
                Conversation.ConversationType.Single, invite.getHost(), 0);
        ChatManager.Instance().sendMessage(conv, reject, null, 0, null);
    }

    /**
     * 观众加入 WebRTC 连麦会议（同意连麦后调用）。
     * audioOnly 由信令内容中的字段决定（主播 accept 时回传观众的原始选择）。
     *
     * @param content 连麦信令内容（含 callId/pin/host/title/audioOnly）
     * @return 成功返回 CallSession，会话创建失败返回 null
     */
    public AVEngineKit.CallSession joinConferenceForCoStream(LiveCoStreamContent content, AVEngineKit.CallSessionCallback callback) {
        // audioOnly=false regardless of coStream mode: we always want to receive the host's video.
        // muteVideo controls whether WE send video (true = audio-only co-stream, we mute our camera).
        return AVEngineKit.Instance().joinConference(
                content.getCallId(), content.isAudioOnly(), content.getPin(), content.getHost(),
                content.getTitle(), "",
                false,                    // audience=false：成为活跃连麦参与者
                false,                    // advanced
                false,                    // muteAudio
                content.isAudioOnlyRequest(),                    // muteVideo
                false,                    // webCamera
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
                // 保存完整内容，accept 时回传 audioOnly 给观众
                coStreamRequestMap.put(uid, content);
                LiveDataBus.setValue(EVENT_CO_STREAM_REQUEST, content);
                break;
            case LiveCoStreamContent.ACTION_INVITE:
                LiveDataBus.setValue(EVENT_CO_STREAM_INVITE, content);
                break;
            case LiveCoStreamContent.ACTION_ACCEPT:
                coStreamRequestMap.remove(uid);
                if (!activeCoStreamers.contains(uid)) activeCoStreamers.add(uid);
                LiveDataBus.setValue(EVENT_CO_STREAM_ACCEPTED, content);
                break;
            case LiveCoStreamContent.ACTION_REJECT:
                coStreamRequestMap.remove(uid);
                activeCoStreamers.remove(uid);
                LiveDataBus.setValue(EVENT_CO_STREAM_REJECTED, content);
                break;
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void sendSignal(String toUserId, int action, String targetUserId, boolean audioOnlyRequest) {
        LiveInfo info = currentLiveInfo;
        if (info == null) return;
        LiveCoStreamContent content = new LiveCoStreamContent(
                info.getLiveId(), info.isAudioOnly(), info.getPin(), info.getHost(), info.getTitle(), action, targetUserId, audioOnlyRequest);
        Conversation conv = new Conversation(Conversation.ConversationType.Single, toUserId, 0);
        ChatManager.Instance().sendMessage(conv, content, null, 0, null);
    }

    public void joinLiveChatRoom() {
        ChatManager.Instance().joinChatRoom(currentLiveInfo.getLiveId(), null);
        startChatroomKeepalive();
    }

    private void startChatroomKeepalive() {
        stopLiveChatroomKeepalive();
        chatroomKeepaliveRunnable = new Runnable() {
            @Override
            public void run() {
                if (currentLiveInfo != null) {
                    ChatManager.Instance().joinChatRoom(currentLiveInfo.getLiveId(), null);
                    handler.postDelayed(this, 3 * 60 * 1000L);
                }
            }
        };
        handler.postDelayed(chatroomKeepaliveRunnable, 3 * 60 * 1000L);
    }

    private void stopLiveChatroomKeepalive() {
        if (chatroomKeepaliveRunnable != null) {
            handler.removeCallbacks(chatroomKeepaliveRunnable);
            chatroomKeepaliveRunnable = null;
        }
    }

    /**
     * 从URL中提取host
     */
    private static String extractHost(String url) {
        if (TextUtils.isEmpty(url)) {
            return "";
        }
        String host = url;
        // 去除协议前缀
        if (host.startsWith("https://")) {
            host = host.substring(8);
        } else if (host.startsWith("http://")) {
            host = host.substring(7);
        }
        // 去除路径部分
        int slashIndex = host.indexOf('/');
        if (slashIndex > 0) {
            host = host.substring(0, slashIndex);
        }
        return host;
    }
}
