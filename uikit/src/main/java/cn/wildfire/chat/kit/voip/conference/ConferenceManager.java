/*
 * Copyright (c) 2022 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.voip.conference;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.wildfire.chat.kit.AppServiceProvider;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.livebus.LiveDataBus;
import cn.wildfire.chat.kit.voip.conference.message.ConferenceChangeModeContent;
import cn.wildfire.chat.kit.voip.conference.message.ConferenceCommandContent;
import cn.wildfire.chat.kit.voip.conference.model.ConferenceInfo;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GeneralCallback;
import cn.wildfirechat.remote.OnReceiveMessageListener;

public class ConferenceManager implements OnReceiveMessageListener {
    private static ConferenceManager manager;
    private final Application context;
    private ConferenceInfo currentConferenceInfo;
    private final List<String> applyingUnmuteAudioMembers;
    private final List<String> applyingUnmuteVideoMembers;
    private boolean isApplyingUnmuteAudio;
    private boolean isApplyingUnmuteVideo;
    private final List<String> handUpMembers;
    private boolean isHandUp;
    private boolean isMuteAllAudio;
    private boolean isMuteAllVideo;

    private boolean isAllowUnmuteAudioWhenMuteAll;
    private boolean isAllownUnMuteVideoWhenMuteAll;

    private String localFocusUserId;

    private static final String TAG = "ConferenceManager";

    private ConferenceManager(Application application) {
        this.context = application;
        this.applyingUnmuteAudioMembers = new ArrayList<>();
        this.applyingUnmuteVideoMembers = new ArrayList<>();
        this.handUpMembers = new ArrayList<>();
    }

    public static void init(Application application) {
        if (application == null) {
            throw new IllegalArgumentException("application cano be null");
        }
        manager = new ConferenceManager(application);
    }

    public static ConferenceManager getManager() {
        if (manager == null) {
            throw new IllegalStateException("not init");
        }
        return manager;
    }

    public ConferenceInfo getCurrentConferenceInfo() {
        return currentConferenceInfo;
    }


    public void setCurrentConferenceInfo(ConferenceInfo currentConferenceInfo) {
        this.currentConferenceInfo = currentConferenceInfo;
        if (currentConferenceInfo == null) {
            handUpMembers.clear();
            applyingUnmuteAudioMembers.clear();
            applyingUnmuteVideoMembers.clear();
            isHandUp = false;
            isApplyingUnmuteAudio = false;
            isApplyingUnmuteVideo = false;
        } else {
            joinChatRoom();
        }
    }

    public String getLocalFocusUserId() {
        return localFocusUserId;
    }

    public void setLocalFocusUserId(String localFocusUserId) {
        this.localFocusUserId = localFocusUserId;
    }

    public List<String> getApplyingUnmuteAudioMembers() {
        return applyingUnmuteAudioMembers;
    }

    public List<String> getApplyingUnmuteVideoMembers() {
        return applyingUnmuteVideoMembers;
    }

    public boolean isApplyingUnmuteAudio() {
        return isApplyingUnmuteAudio;
    }

    public boolean isApplyingUnmuteVideo() {
        return isApplyingUnmuteVideo;
    }

    public List<String> getHandUpMembers() {
        return handUpMembers;
    }

    public boolean isHandUp() {
        return isHandUp;
    }

    public boolean isMuteAllAudio() {
        return isMuteAllAudio;
    }

    public boolean isMuteAllVideo() {
        return isMuteAllVideo;
    }

    @Override
    public void onReceiveMessage(List<Message> messages, boolean hasMore) {
        AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
        if (session != null && session.getState() != AVEngineKit.CallState.Idle && session.isConference()) {
            for (Message msg : messages) {
                if (msg.content instanceof ConferenceChangeModeContent) {
                    ConferenceChangeModeContent content = (ConferenceChangeModeContent) msg.content;
                    if (session.getCallId().equals(content.getCallId())) {

                    }
                    // TODO
                } else if (msg.content instanceof ConferenceCommandContent) {
                    // TODO
                    ConferenceCommandContent commandContent = (ConferenceCommandContent) msg.content;
                    String senderName;
                    if (session.getCallId().equals(commandContent.getConferenceId())) {
                        switch (commandContent.getCommandType()) {
                            case ConferenceCommandContent.ConferenceCommandType.MUTE_ALL_AUDIO:
                                reloadConferenceInfo();
                                onMuteAll(true, commandContent.getBoolValue());
                                break;
                            case ConferenceCommandContent.ConferenceCommandType.MUTE_ALL_VIDEO:
                                reloadConferenceInfo();
                                onMuteAll(false, commandContent.getBoolValue());
                                break;
                            case ConferenceCommandContent.ConferenceCommandType.CANCEL_MUTE_ALL_AUDIO:
                                reloadConferenceInfo();
                                onCancelMuteAll(true, commandContent.getBoolValue());
                                break;
                            case ConferenceCommandContent.ConferenceCommandType.CANCEL_MUTE_ALL_VIDEO:
                                reloadConferenceInfo();
                                onCancelMuteAll(false, commandContent.getBoolValue());
                                break;
                            case ConferenceCommandContent.ConferenceCommandType.REQUEST_MUTE_AUDIO:
                                if (commandContent.getTargetUserId().equals(ChatManager.Instance().getUserId())) {
                                    onRequestMute(true, commandContent.getBoolValue());
                                }
                                break;
                            case ConferenceCommandContent.ConferenceCommandType.REQUEST_MUTE_VIDEO:
                                if (commandContent.getTargetUserId().equals(ChatManager.Instance().getUserId())) {
                                    onRequestMute(false, commandContent.getBoolValue());
                                }
                                break;
                            case ConferenceCommandContent.ConferenceCommandType.REJECT_UNMUTE_REQUEST_AUDIO:
                                Toast.makeText(context, context.getString(R.string.conf_reject_unmute_audio), Toast.LENGTH_SHORT).show();
                                break;
                            case ConferenceCommandContent.ConferenceCommandType.REJECT_UNMUTE_REQUEST_VIDEO:
                                Toast.makeText(context, context.getString(R.string.conf_reject_unmute_video), Toast.LENGTH_SHORT).show();
                                break;
                            case ConferenceCommandContent.ConferenceCommandType.APPLY_UNMUTE_AUDIO:
                                senderName = ChatManager.Instance().getUserDisplayName(msg.sender);
                                Toast.makeText(context, context.getString(R.string.conf_request_speak, senderName), Toast.LENGTH_SHORT).show();
                                if (commandContent.getBoolValue()) {
                                    this.applyingUnmuteAudioMembers.remove(msg.sender);
                                } else {
                                    if (!this.applyingUnmuteAudioMembers.contains(msg.sender)) {
                                        this.applyingUnmuteAudioMembers.add(msg.sender);
                                    }
                                }
                                LiveDataBus.setValue("kConferenceCommandStateChanged", new Object());
                                break;
                            case ConferenceCommandContent.ConferenceCommandType.APPLY_UNMUTE_VIDEO:
                                senderName = ChatManager.Instance().getUserDisplayName(msg.sender);
                                Toast.makeText(context, context.getString(R.string.conf_request_camera, senderName), Toast.LENGTH_SHORT).show();
                                if (commandContent.getBoolValue()) {
                                    this.applyingUnmuteVideoMembers.remove(msg.sender);
                                } else {
                                    if (!this.applyingUnmuteVideoMembers.contains(msg.sender)) {
                                        this.applyingUnmuteVideoMembers.add(msg.sender);
                                    }
                                }
                                LiveDataBus.setValue("kConferenceCommandStateChanged", new Object());
                                break;
                            case ConferenceCommandContent.ConferenceCommandType.APPROVE_UNMUTE_AUDIO:
                            case ConferenceCommandContent.ConferenceCommandType.APPROVE_ALL_UNMUTE_AUDIO:
                                if (this.isApplyingUnmuteAudio) {
                                    this.isApplyingUnmuteAudio = false;
                                    if (commandContent.getBoolValue()) {
                                        this.muteAudio(false);
                                        Toast.makeText(context, context.getString(R.string.conf_approve_speak), Toast.LENGTH_SHORT).show();
                                    }
                                }
                                break;
                            case ConferenceCommandContent.ConferenceCommandType.APPROVE_UNMUTE_VIDEO:
                            case ConferenceCommandContent.ConferenceCommandType.APPROVE_ALL_UNMUTE_VIDEO:
                                if (this.isApplyingUnmuteVideo) {
                                    this.isApplyingUnmuteVideo = false;
                                    if (commandContent.getBoolValue()) {
                                        this.muteVideo(false);
                                        Toast.makeText(context, context.getString(R.string.conf_approve_camera), Toast.LENGTH_SHORT).show();
                                    }
                                }
                                break;
                            case ConferenceCommandContent.ConferenceCommandType.HANDUP:
                                if (commandContent.getBoolValue()) {
                                    if (!this.handUpMembers.contains(msg.sender)) {
                                        this.handUpMembers.add(msg.sender);
                                    }
                                } else {
                                    this.handUpMembers.remove(msg.sender);
                                }
                                senderName = ChatManager.Instance().getUserDisplayName(msg.sender);
                                if (commandContent.getBoolValue()) {
                                    Toast.makeText(context, context.getString(R.string.conf_somebody_hand_up, senderName), Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(context, context.getString(R.string.conf_somebody_hand_down, senderName), Toast.LENGTH_SHORT).show();
                                }

                                LiveDataBus.setValue("kConferenceCommandStateChanged", new Object());

                                break;
                            case ConferenceCommandContent.ConferenceCommandType.PUT_HAND_DOWN:
                            case ConferenceCommandContent.ConferenceCommandType.PUT_ALL_HAND_DOWN:
                                if (this.isHandUp) {
                                    this.isHandUp = false;
                                    Toast.makeText(context, context.getString(R.string.conf_host_put_your_hand_down), Toast.LENGTH_SHORT).show();
                                }
                                break;
                            case ConferenceCommandContent.ConferenceCommandType.RECORDING:
                                this.reloadConferenceInfo();
                                Toast.makeText(context, commandContent.getBoolValue() ? context.getString(R.string.conf_host_start_recording) : context.getString(R.string.conf_host_stop_recording), Toast.LENGTH_SHORT).show();
                                break;

                            case ConferenceCommandContent.ConferenceCommandType.FOCUS:
                                this.currentConferenceInfo.setFocus(commandContent.getTargetUserId());
                                this.reloadConferenceInfo();
                                Toast.makeText(context, context.getString(R.string.conf_host_focus_user), Toast.LENGTH_SHORT).show();
                                LiveDataBus.setValue("kConferenceCommandStateChanged", commandContent);
                                break;
                            case ConferenceCommandContent.ConferenceCommandType.CANCEL_FOCUS:
                                this.currentConferenceInfo.setFocus(null);
                                this.reloadConferenceInfo();
                                Toast.makeText(context, context.getString(R.string.conf_host_cancel_focus), Toast.LENGTH_SHORT).show();
                                LiveDataBus.setValue("kConferenceCommandStateChanged", commandContent);
                                break;

                            default:
                                break;
                        }

                    }
                }
            }
        }
    }

    private void joinChatRoom() {
        ChatManager.Instance().joinChatRoom(currentConferenceInfo.getConferenceId(), null);
    }

    public void applyUnmute(boolean audio, boolean isCancel) {
        if (audio) {
            this.isApplyingUnmuteAudio = !isCancel;
        } else {
            this.isApplyingUnmuteVideo = !isCancel;
        }
        this.sendCommandMessage(audio ? ConferenceCommandContent.ConferenceCommandType.APPLY_UNMUTE_AUDIO : ConferenceCommandContent.ConferenceCommandType.APPLY_UNMUTE_VIDEO, null, isCancel);
    }

    public void approveUnmute(boolean audio, String userId, boolean isAllow) {
        if (!this.isOwner()) {
            return;
        }

        if (audio) {
            this.applyingUnmuteAudioMembers.remove(userId);
        } else {
            this.applyingUnmuteVideoMembers.remove(userId);
        }
        this.sendCommandMessage(audio ? ConferenceCommandContent.ConferenceCommandType.APPROVE_UNMUTE_AUDIO : ConferenceCommandContent.ConferenceCommandType.APPROVE_UNMUTE_VIDEO, userId, isAllow);
        LiveDataBus.setValue("kConferenceCommandStateChanged", new Object());
    }

    public void approveAllMemberUnmute(boolean audio, boolean isAllow) {
        if (!this.isOwner()) {
            return;
        }

        if (audio) {
            this.applyingUnmuteAudioMembers.clear();
        } else {
            this.applyingUnmuteVideoMembers.clear();
        }
        this.sendCommandMessage(audio ? ConferenceCommandContent.ConferenceCommandType.APPROVE_ALL_UNMUTE_AUDIO : ConferenceCommandContent.ConferenceCommandType.APPROVE_ALL_UNMUTE_VIDEO, null, isAllow);
        LiveDataBus.setValue("kConferenceCommandStateChanged", new Object());
    }

    public void muteAudio(boolean mute) {
        AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
        if (session != null && session.getState() == AVEngineKit.CallState.Connected) {
            if (mute) {
                if (!session.isAudience() && session.videoMuted) {
                    boolean result = session.switchAudience(true);
                    if (!result) {
                        Log.d(TAG, "switch to audience fail");
                        return;
                    }
                }
                session.muteAudio(true);
            } else {
                if (session.isAudience() && !session.canSwitchAudience()) {
                    Log.d(TAG, "can not switch to audience");
                    return;
                }
                if (session.isAudience() && isParticipantFull(session)) {
                    Toast.makeText(context, "发言人数已满，无法切换到发言人！", Toast.LENGTH_SHORT).show();
                    return;
                }

                session.muteAudio(false);
                if (session.videoMuted || session.isAudience()) {
                    session.switchAudience(false);
                }
            }
        }
        LiveDataBus.setValue("kConferenceMutedStateChanged", new Object());
    }

    public void muteVideo(boolean mute) {
        AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
        if (session != null && session.getState() == AVEngineKit.CallState.Connected) {
            if (mute) {
                if (!session.isAudience() && session.audioMuted) {
                    session.switchAudience(true);
                }
                session.muteVideo(true);
            } else {
                if (session.isAudience() && isParticipantFull(session)) {
                    Toast.makeText(context, context.getString(R.string.conf_max_participants_reached), Toast.LENGTH_SHORT).show();
                    return;
                }
                session.muteVideo(false);
                if (session.audioMuted || session.isAudience()) {
                    session.switchAudience(false);
                }
            }
        }
    }

    public void muteAudioVideo(boolean mute) {
        AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
        if (session == null || session.getState() == AVEngineKit.CallState.Idle) {
            return;
        }
        if (mute) {
            if (!session.isAudience()) {
                session.switchAudience(true);
            }
            session.muteVideo(true);
            session.muteAudio(true);
        } else {
            session.muteVideo(false);
            session.muteAudio(false);
            if (session.isAudience()) {
                session.switchAudience(false);
            }
        }
    }

    public void requestMemberMute(boolean audio, String userId, boolean mute) {
        if (!isOwner()) {
            return;
        }
        this.sendCommandMessage(audio ? ConferenceCommandContent.ConferenceCommandType.REQUEST_MUTE_AUDIO : ConferenceCommandContent.ConferenceCommandType.REQUEST_MUTE_VIDEO, userId, mute);
    }

    public void requestMuteAll(boolean audio, boolean allowMemberUnmute) {
        if (!this.isOwner()) {
            return;
        }
        if (audio) {
            this.isMuteAllAudio = true;
        } else {
            this.isMuteAllVideo = true;
        }
        currentConferenceInfo.setAudience(true);
        currentConferenceInfo.setAllowTurnOnMic(allowMemberUnmute);

        WfcUIKit.getWfcUIKit().getAppServiceProvider().updateConference(currentConferenceInfo, new GeneralCallback() {
            @Override
            public void onSuccess() {
                sendCommandMessage(audio ? ConferenceCommandContent.ConferenceCommandType.MUTE_ALL_AUDIO : ConferenceCommandContent.ConferenceCommandType.MUTE_ALL_VIDEO, null, allowMemberUnmute);
                LiveDataBus.setValue("kConferenceMutedStateChanged", new Object());
            }

            @Override
            public void onFail(int errorCode) {

            }
        });
    }

    public void requestUnmuteAll(boolean audio, boolean unmute) {
        if (!this.isOwner()) {
            return;
        }
        if (audio) {
            this.isMuteAllAudio = false;
        } else {
            this.isMuteAllVideo = false;
        }
        currentConferenceInfo.setAudience(false);
        currentConferenceInfo.setAllowTurnOnMic(true);

        WfcUIKit.getWfcUIKit().getAppServiceProvider().updateConference(currentConferenceInfo, new GeneralCallback() {
            @Override
            public void onSuccess() {
                sendCommandMessage(audio ? ConferenceCommandContent.ConferenceCommandType.CANCEL_MUTE_ALL_AUDIO : ConferenceCommandContent.ConferenceCommandType.CANCEL_MUTE_ALL_VIDEO, null, unmute);
                LiveDataBus.setValue("kConferenceMutedStateChanged", new Object());
            }

            @Override
            public void onFail(int errorCode) {

            }
        });
    }

    public void handUp(boolean isHandUp) {
        this.isHandUp = isHandUp;
        this.sendCommandMessage(ConferenceCommandContent.ConferenceCommandType.HANDUP, null, isHandUp);
        Toast.makeText(context, isHandUp ? context.getString(R.string.conf_hand_up_waiting) : context.getString(R.string.conf_hand_down_message), Toast.LENGTH_SHORT).show();
    }

    public void putMemberHandDown(String memberId) {
        if (!isOwner()) {
            return;
        }
        this.handUpMembers.remove(memberId);
        this.sendCommandMessage(ConferenceCommandContent.ConferenceCommandType.PUT_HAND_DOWN, memberId, false);
        LiveDataBus.setValue("kConferenceCommandStateChanged", this.handUpMembers);
    }

    public void putAllHandDown() {
        if (!isOwner()) {
            return;
        }
        this.handUpMembers.clear();
        this.sendCommandMessage(ConferenceCommandContent.ConferenceCommandType.PUT_ALL_HAND_DOWN, null, false);
        LiveDataBus.setValue("kConferenceCommandStateChanged", this.handUpMembers);
    }

    public void requestRecord(boolean record) {
        if (!isOwner()) {
            return;
        }

        WfcUIKit.getWfcUIKit().getAppServiceProvider().recordConference(currentConferenceInfo.getConferenceId(), record, new GeneralCallback() {
            @Override
            public void onSuccess() {
                currentConferenceInfo.setRecording(record);
                Toast.makeText(context, record ? context.getString(R.string.conf_start_recording) : context.getString(R.string.conf_stop_recording), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFail(int errorCode) {

            }
        });
    }

    public void destroyConference(String conferenceId, GeneralCallback callback) {
        if (!isOwner()) {
            return;
        }
        WfcUIKit.getWfcUIKit().getAppServiceProvider().destroyConference(conferenceId, new GeneralCallback() {
            @Override
            public void onSuccess() {
                if (callback != null) {
                    callback.onSuccess();
                }
            }

            @Override
            public void onFail(int i) {
                if (callback != null) {
                    callback.onFail(i);
                }
            }
        });
    }

    public void requestFocus(String userId, GeneralCallback callback) {
        if (!isOwner()) {
            return;
        }
        WfcUIKit.getWfcUIKit().getAppServiceProvider().setConferenceFocusUserId(currentConferenceInfo.getConferenceId(), userId, new GeneralCallback() {
            @Override
            public void onSuccess() {
                currentConferenceInfo.setFocus(userId);
                sendCommandMessage(ConferenceCommandContent.ConferenceCommandType.FOCUS, userId, false);
                ConferenceCommandContent content = new ConferenceCommandContent(currentConferenceInfo.getConferenceId(), ConferenceCommandContent.ConferenceCommandType.FOCUS);
                content.setTargetUserId(userId);
                LiveDataBus.setValue("kConferenceCommandStateChanged", content);
                if (callback != null) {
                    callback.onSuccess();
                }
            }

            @Override
            public void onFail(int i) {
                if (callback != null) {
                    callback.onFail(i);
                }
            }
        });
    }

    public void requestCancelFocus(GeneralCallback callback) {
        if (!isOwner()) {
            return;
        }
        WfcUIKit.getWfcUIKit().getAppServiceProvider().setConferenceFocusUserId(currentConferenceInfo.getConferenceId(), null, new GeneralCallback() {
            @Override
            public void onSuccess() {
                currentConferenceInfo.setFocus(null);
                sendCommandMessage(ConferenceCommandContent.ConferenceCommandType.CANCEL_FOCUS, null, false);
                ConferenceCommandContent content = new ConferenceCommandContent(currentConferenceInfo.getConferenceId(), ConferenceCommandContent.ConferenceCommandType.FOCUS);
                content.setTargetUserId(null);
                LiveDataBus.setValue("kConferenceCommandStateChanged", content);
                if (callback != null) {
                    callback.onSuccess();
                }
            }

            @Override
            public void onFail(int i) {
                if (callback != null) {
                    callback.onFail(i);
                }
            }
        });
    }

    public void addHistory(ConferenceInfo conferenceInfo, long durationMS) {
        Gson gson = new Gson();
        conferenceInfo.setEndTime(conferenceInfo.getStartTime() * 1000 + durationMS);
        SharedPreferences sp = context.getSharedPreferences("conf_history", Context.MODE_PRIVATE);
        String historyConfListStr = sp.getString("historyConfList", null);
        ArrayList<ConferenceInfo> historyConfList = new ArrayList<>();
        if (historyConfListStr != null) {
            historyConfList = gson.fromJson(historyConfListStr, new TypeToken<List<ConferenceInfo>>() {
            }.getType());
        }
        historyConfList.add(conferenceInfo);
        if (historyConfList.size() > 50) {
            historyConfList.remove(0);
        }
        historyConfListStr = gson.toJson(historyConfList);
        sp.edit().putString("historyConfList", historyConfListStr).commit();
    }

    public List<ConferenceInfo> getHistoryConference() {
        Gson gson = new Gson();
        SharedPreferences sp = context.getSharedPreferences("conf_history", Context.MODE_PRIVATE);
        String historyConfListStr = sp.getString("historyConfList", null);
        ArrayList<ConferenceInfo> historyConfList = new ArrayList<>();
        if (historyConfListStr != null) {
            historyConfList = gson.fromJson(historyConfListStr, new TypeToken<List<ConferenceInfo>>() {
            }.getType());
        }
        Collections.reverse(historyConfList);
        return historyConfList;
    }

    public boolean isOwner() {
        return currentConferenceInfo.getOwner().equals(ChatManager.Instance().getUserId());
    }

    private void onRequestMute(boolean audio, boolean mute) {
        Pair<Boolean, Boolean> value = new Pair<>(audio, mute);
        LiveDataBus.setValue("onRequestMute", value);
    }

    private void onCancelMuteAll(boolean audio, boolean requestUnmute) {
        Pair<Boolean, Boolean> value = new Pair<>(audio, requestUnmute);
        LiveDataBus.setValue("onCancelMuteAll", value);
    }


    private void onMuteAll(boolean audio, boolean allowUnmute) {
        reloadConferenceInfo();
        AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
        if (!session.isAudience()) {
            if (audio) {
                this.isAllowUnmuteAudioWhenMuteAll = allowUnmute;
                muteAudio(true);
            } else {
                this.isAllownUnMuteVideoWhenMuteAll = allowUnmute;
                muteVideo(true);
            }
        }
        Toast.makeText(context, audio ? context.getString(R.string.conf_host_muted_all_audio) : context.getString(R.string.conf_host_muted_all_video), Toast.LENGTH_SHORT).show();
    }

    private void reloadConferenceInfo() {
        WfcUIKit.getWfcUIKit().getAppServiceProvider().queryConferenceInfo(this.currentConferenceInfo.getConferenceId(), this.currentConferenceInfo.getPassword(), new AppServiceProvider.QueryConferenceInfoCallback() {
            @Override
            public void onSuccess(ConferenceInfo info) {
                currentConferenceInfo = info;
            }

            @Override
            public void onFail(int code, String msg) {

            }
        });

    }

    private void sendCommandMessage(int commandType, String targetUser, boolean value) {
        ConferenceCommandContent content = new ConferenceCommandContent(this.currentConferenceInfo.getConferenceId(), commandType);
        content.setTargetUserId(targetUser);
        content.setBoolValue(value);
        Conversation conversation = new Conversation(Conversation.ConversationType.ChatRoom, this.currentConferenceInfo.getConferenceId(), 0);
        ChatManager.Instance().sendMessage(conversation, content, null, 0, null);

    }

    private boolean isParticipantFull(AVEngineKit.CallSession session) {
        if (currentConferenceInfo.getMaxParticipants() > 0) {
            int participantCount = 0;
            List<AVEngineKit.ParticipantProfile> profiles = session.getParticipantProfiles();
            for (AVEngineKit.ParticipantProfile p : profiles) {
                if (!p.isAudience()) {
                    participantCount++;
                }
            }
            return participantCount >= currentConferenceInfo.getMaxParticipants();
        }
        return false;
    }
}
