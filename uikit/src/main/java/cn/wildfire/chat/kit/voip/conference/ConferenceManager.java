/*
 * Copyright (c) 2022 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.voip.conference;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.wildfire.chat.kit.AppServiceProvider;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.livebus.LiveDataBus;
import cn.wildfire.chat.kit.voip.conference.message.ConferenceChangeModeContent;
import cn.wildfire.chat.kit.voip.conference.message.ConferenceCommandContent;
import cn.wildfire.chat.kit.voip.conference.model.ConferenceInfo;
import cn.wildfire.chat.kit.widget.AlertDialogActivity;
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
    private final List<String> applyingUnmuteMembers;
    private boolean isApplyingUnmute;
    private final List<String> handUpMembers;
    private boolean isHandUp;
    private boolean isMuteAll;

    private ConferenceManager(Application application) {
        this.context = application;
        this.applyingUnmuteMembers = new ArrayList<>();
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
            applyingUnmuteMembers.clear();
            isHandUp = false;
            isApplyingUnmute = false;
        }
    }

    public List<String> getApplyingUnmuteMembers() {
        return applyingUnmuteMembers;
    }

    public boolean isApplyingUnmute() {
        return isApplyingUnmute;
    }

    public List<String> getHandUpMembers() {
        return handUpMembers;
    }

    public boolean isHandUp() {
        return isHandUp;
    }

    public boolean isMuteAll() {
        return isMuteAll;
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
                            case ConferenceCommandContent.ConferenceCommandType.MUTE_ALL:
                                reloadConferenceInfo();
                                onMuteAll();
                                break;
                            case ConferenceCommandContent.ConferenceCommandType.CANCEL_MUTE_ALL:
                                reloadConferenceInfo();
                                onCancelMuteAll(commandContent.getBoolValue());
                                break;
                            case ConferenceCommandContent.ConferenceCommandType.REQUEST_MUTE:
                                if (commandContent.getTargetUserId().equals(ChatManager.Instance().getUserId())) {
                                    onRequestMute(commandContent.getBoolValue());
                                }
                                break;
                            case ConferenceCommandContent.ConferenceCommandType.REJECT_UNMUTE_REQUEST:
                                Toast.makeText(context, "主持人拒绝了你的发言请求", Toast.LENGTH_SHORT).show();
                                break;
                            case ConferenceCommandContent.ConferenceCommandType.APPLY_UNMUTE:
                                senderName = ChatManager.Instance().getUserDisplayName(msg.sender);
                                Toast.makeText(context, senderName + " 请求发言", Toast.LENGTH_SHORT).show();
                                if (commandContent.getBoolValue()) {
                                    this.applyingUnmuteMembers.remove(msg.sender);
                                } else {
                                    if (!this.applyingUnmuteMembers.contains(msg.sender)) {
                                        this.applyingUnmuteMembers.add(msg.sender);
                                    }
                                }
                                LiveDataBus.setValue("kConferenceCommandStateChanged", new Object());
                                break;
                            case ConferenceCommandContent.ConferenceCommandType.APPROVE_UNMUTE:
                            case ConferenceCommandContent.ConferenceCommandType.APPROVE_ALL_UNMUTE:
                                if (this.isApplyingUnmute) {
                                    this.isApplyingUnmute = false;
                                    if (commandContent.getBoolValue()) {
                                        this.muteAudio(false);
                                        Toast.makeText(context, "主持人同意了你的发言请求", Toast.LENGTH_SHORT).show();
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
                                    Toast.makeText(context, senderName + " 举手", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(context, senderName + " 放下举手", Toast.LENGTH_SHORT).show();
                                }

                                LiveDataBus.setValue("kConferenceCommandStateChanged", new Object());

                                break;
                            case ConferenceCommandContent.ConferenceCommandType.PUT_HAND_DOWN:
                            case ConferenceCommandContent.ConferenceCommandType.PUT_ALL_HAND_DOWN:
                                if (this.isHandUp) {
                                    this.isHandUp = false;
                                    Toast.makeText(context, "主持人放下你的举手", Toast.LENGTH_SHORT).show();
                                }
                                break;
                            case ConferenceCommandContent.ConferenceCommandType.RECORDING:
                                this.reloadConferenceInfo();
                                Toast.makeText(context, commandContent.getBoolValue() ? "主持人开始录制" : "主持人结束录制", Toast.LENGTH_SHORT).show();
                                break;

                            case ConferenceCommandContent.ConferenceCommandType.FOCUS:
                                this.currentConferenceInfo.setFocus(commandContent.getTargetUserId());
                                this.reloadConferenceInfo();
                                Toast.makeText(context, "主持人锁定焦点用户", Toast.LENGTH_SHORT).show();
                                LiveDataBus.setValue("kConferenceCommandStateChanged", commandContent);
                                break;
                            case ConferenceCommandContent.ConferenceCommandType.CANCEL_FOCUS:
                                this.currentConferenceInfo.setFocus(null);
                                this.reloadConferenceInfo();
                                Toast.makeText(context, "主持人取消锁定焦点用户", Toast.LENGTH_SHORT).show();
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

    public void joinChatRoom() {
        ChatManager.Instance().joinChatRoom(currentConferenceInfo.getConferenceId(), null);
    }

    public void applyUnmute(boolean isCancel) {
        this.isApplyingUnmute = !isCancel;
        this.sendCommandMessage(ConferenceCommandContent.ConferenceCommandType.APPLY_UNMUTE, null, isCancel);
    }

    public void approveUnmute(String userId, boolean isAllow) {
        if (!this.isOwner()) {
            return;
        }

        this.applyingUnmuteMembers.remove(userId);
        this.sendCommandMessage(ConferenceCommandContent.ConferenceCommandType.APPROVE_UNMUTE, userId, isAllow);
        LiveDataBus.setValue("kConferenceCommandStateChanged", new Object());
    }

    public void approveAllMemberUnmute(boolean isAllow) {
        if (!this.isOwner()) {
            return;
        }

        this.applyingUnmuteMembers.clear();
        this.sendCommandMessage(ConferenceCommandContent.ConferenceCommandType.APPROVE_ALL_UNMUTE, null, isAllow);
        LiveDataBus.setValue("kConferenceCommandStateChanged", new Object());
    }

    public void muteAudio(boolean mute) {
        AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
        if (session != null && session.getState() == AVEngineKit.CallState.Connected) {
            if (mute) {
                if (!session.isAudience() && session.videoMuted) {
                    session.switchAudience(true);
                }
                session.muteAudio(true);
            } else {
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

    public void requestMemberMute(String userId, boolean mute) {
        if (!isOwner()) {
            return;
        }
        this.sendCommandMessage(ConferenceCommandContent.ConferenceCommandType.REQUEST_MUTE, userId, mute);
    }

    public void requestMuteAll(boolean allowMemberUnmute) {
        if (!this.isOwner()) {
            return;
        }
        this.isMuteAll = true;
        currentConferenceInfo.setAudience(true);
        currentConferenceInfo.setAllowTurnOnMic(allowMemberUnmute);

        WfcUIKit.getWfcUIKit().getAppServiceProvider().updateConference(currentConferenceInfo, new GeneralCallback() {
            @Override
            public void onSuccess() {
                sendCommandMessage(ConferenceCommandContent.ConferenceCommandType.MUTE_ALL, null, allowMemberUnmute);
                LiveDataBus.setValue("kConferenceMutedStateChanged", new Object());
            }

            @Override
            public void onFail(int errorCode) {

            }
        });
    }

    public void requestUnmuteAll(boolean unmute) {
        if (!this.isOwner()) {
            return;
        }
        this.isMuteAll = false;
        currentConferenceInfo.setAudience(false);
        currentConferenceInfo.setAllowTurnOnMic(true);

        WfcUIKit.getWfcUIKit().getAppServiceProvider().updateConference(currentConferenceInfo, new GeneralCallback() {
            @Override
            public void onSuccess() {
                sendCommandMessage(ConferenceCommandContent.ConferenceCommandType.CANCEL_MUTE_ALL, null, unmute);
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
        Toast.makeText(context, isHandUp ? "已举手，等待管理员处理" : "已放下举手", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(context, record ? "开始录制" : "结束录制", Toast.LENGTH_SHORT).show();
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

    private boolean isOwner() {
        return currentConferenceInfo.getOwner().equals(ChatManager.Instance().getUserId());
    }

    private void onRequestMute(boolean mute) {
        if (!mute) {
            ConferenceAlertDialogActivity.showAlterDialog(context, "主持人邀请你发言", false, "拒绝", "接受",
                () -> Toast.makeText(context, "你拒绝了发言邀请", Toast.LENGTH_SHORT).show(),
                () -> {
                    Toast.makeText(context, "你接受了发言邀请", Toast.LENGTH_SHORT).show();
                    muteAudioVideo(false);
                });
        } else {
            Toast.makeText(context, "主持人关闭了你的发言", Toast.LENGTH_SHORT).show();
            muteAudioVideo(true);
        }
    }

    private void onCancelMuteAll(boolean requestUnmute) {
        if (requestUnmute) {
            ConferenceAlertDialogActivity.showAlterDialog(context, "主持人关闭了全员静音，是否要打开麦克风", false, "忽略", "打开",
                () -> {
                },
                () -> {
                    muteAudio(false);
                });
        }
        Toast.makeText(context, "主持人关闭了全员静音", Toast.LENGTH_SHORT).show();
    }


    private void onMuteAll() {
        reloadConferenceInfo();
        AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
        if (!session.isAudience()) {
            session.switchAudience(true);
            session.muteAudio(true);
            session.muteVideo(true);
        }
        Toast.makeText(context, "主持人开启了全员静音", Toast.LENGTH_SHORT).show();
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
}
