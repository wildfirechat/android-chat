/*
 * Copyright (c) 2022 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.voip.conference;

import android.app.Application;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.AppServiceProvider;
import cn.wildfire.chat.kit.WfcUIKit;
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
    private final List<String> handupMembers;
    private boolean isHandup;

    private ConferenceManager(Application application) {
        this.context = application;
        this.applyingUnmuteMembers = new ArrayList<>();
        this.handupMembers = new ArrayList<>();
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
            handupMembers.clear();
            applyingUnmuteMembers.clear();
            isHandup = false;
            isApplyingUnmute = false;
        }
    }

    public List<String> getApplyingUnmuteMembers() {
        return applyingUnmuteMembers;
    }

    public boolean isApplyingUnmute() {
        return isApplyingUnmute;
    }

    public void setApplyingUnmute(boolean applyingUnmute) {
        isApplyingUnmute = applyingUnmute;
    }

    public List<String> getHandupMembers() {
        return handupMembers;
    }

    public boolean isHandup() {
        return isHandup;
    }

    public void setHandup(boolean handup) {
        isHandup = handup;
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
                    if (session.getCallId().equals(commandContent.getConferenceId())) {
                        switch (commandContent.getCommandType()) {
                            case ConferenceCommandContent.ConferenceCommandType.MUTE_ALL:
                                reloadConferenceInfo();
                                onMuteAll();
                                break;
                            case ConferenceCommandContent.ConferenceCommandType.CANCEL_MUTE_ALL:
                                reloadConferenceInfo();
                                break;
                            case ConferenceCommandContent.ConferenceCommandType.REQUEST_MUTE:
                                if (commandContent.getTargetUserId().equals(ChatManager.Instance().getUserId())) {
                                    onRequestMute(commandContent.getBoolValue());
                                }
                                break;
                            case ConferenceCommandContent.ConferenceCommandType.REJECT_UNMUTE_REQUEST:
                                break;
                            case ConferenceCommandContent.ConferenceCommandType.APPLY_UNMUTE:
                                if (commandContent.getBoolValue()) {
                                    this.applyingUnmuteMembers.remove(msg.sender);
                                } else {
                                    if (!this.applyingUnmuteMembers.contains(msg.sender)) {
                                        this.applyingUnmuteMembers.add(msg.sender);
                                    }
                                }
                                // TODO 通知上层申请列表变化
                                break;
                            case ConferenceCommandContent.ConferenceCommandType.APPROVE_UNMUTE:
                            case ConferenceCommandContent.ConferenceCommandType.APPROVE_ALL_UNMUTE:
                                if (this.isApplyingUnmute) {
                                    this.isApplyingUnmute = false;
                                    if (commandContent.getBoolValue()) {
                                        this.muteAudio(false);
                                    }
                                }
                                break;
                            case ConferenceCommandContent.ConferenceCommandType.HANDUP:
                                if (!this.handupMembers.contains(msg.sender)) {
                                    this.handupMembers.add(msg.sender);
                                }
                                // TODO 通知上层申请列表变化

                                break;
                            case ConferenceCommandContent.ConferenceCommandType.PUT_HAND_DOWN:
                            case ConferenceCommandContent.ConferenceCommandType.PUT_ALL_HAND_DOWN:
                                if (this.isHandup) {
                                    this.isHandup = false;
                                }
                                break;
                            case ConferenceCommandContent.ConferenceCommandType.RECORDING:
                                this.reloadConferenceInfo();
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
            if (session.isAudience()) {
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
        currentConferenceInfo.setAudience(true);
        currentConferenceInfo.setAllowTurnOnMic(allowMemberUnmute);

        WfcUIKit.getWfcUIKit().getAppServiceProvider().updateConference(currentConferenceInfo, new GeneralCallback() {
            @Override
            public void onSuccess() {
                sendCommandMessage(ConferenceCommandContent.ConferenceCommandType.MUTE_ALL, null, allowMemberUnmute);
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
        currentConferenceInfo.setAudience(false);
        currentConferenceInfo.setAllowTurnOnMic(true);

        WfcUIKit.getWfcUIKit().getAppServiceProvider().updateConference(currentConferenceInfo, new GeneralCallback() {
            @Override
            public void onSuccess() {
                sendCommandMessage(ConferenceCommandContent.ConferenceCommandType.CANCEL_MUTE_ALL, null, unmute);
            }

            @Override
            public void onFail(int errorCode) {

            }
        });
    }

    private boolean isOwner() {
        return currentConferenceInfo.getOwner().equals(ChatManager.Instance().getUserId());
    }

    private void onRequestMute(boolean mute) {
        if (!mute) {
            AlertDialogActivity.showAlterDialog(context, "主持人邀请你发言", false, "拒绝", "接受",
                () -> Toast.makeText(context, "你拒绝了发言邀请", Toast.LENGTH_SHORT).show(),
                () -> {
                    Toast.makeText(context, "你接受了发言邀请", Toast.LENGTH_SHORT).show();
                    muteAudioVideo(false);
                });
        } else {
            muteAudioVideo(true);
        }
    }

    private void onMuteAll() {
        reloadConferenceInfo();
        AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
        if (!session.isAudience()) {
            session.switchAudience(true);
        }
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
