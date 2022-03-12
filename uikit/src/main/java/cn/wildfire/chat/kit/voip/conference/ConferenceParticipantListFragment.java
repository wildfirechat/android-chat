/*
 * Copyright (c) 2021 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.voip.conference;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.wildfire.chat.kit.contact.BaseUserListFragment;
import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfire.chat.kit.user.UserInfoActivity;
import cn.wildfire.chat.kit.voip.conference.message.ConferenceChangeModeContent;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

public class ConferenceParticipantListFragment extends BaseUserListFragment {
    private AVEngineKit.CallSession session;
    private String selfUid;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showQuickIndexBar(false);
    }

    @Override
    protected void afterViews(View view) {
        super.afterViews(view);
        session = AVEngineKit.Instance().getCurrentSession();
        if (session == null || session.getState() == AVEngineKit.CallState.Idle) {
            Activity activity = getActivity();
            if (activity != null && !activity.isFinishing()) {
                activity.finish();
            }
            return;
        }

        selfUid = ChatManager.Instance().getUserId();
        loadAndShowConferenceParticipants();
    }

    @Override
    public void onUserClick(UIUserInfo userInfo) {
        List<String> items = new ArrayList<>();
        items.add("查看用户信息");
        if (selfUid.equals(session.getHost()) && !selfUid.equals(userInfo.getUserInfo().uid)) {
            if ("audience".equals(userInfo.getExtra())) {
                items.add("邀请参与互动");
            } else {
                items.add("取消互动");
            }
            items.add("移除成员");
        } else if (selfUid.equals(userInfo.getUserInfo().uid)) {
            AVEngineKit.ParticipantProfile profile = session.getParticipantProfile(userInfo.getUserInfo().uid);
            if (!profile.isAudience()) {
                items.add("结束互动");
            } else {
                items.add("参与互动");
            }
        }

        new MaterialDialog.Builder(getActivity())
            .cancelable(true)
            .items(items)
            .itemsCallback((dialog, itemView, position, text) -> {
                switch (position) {
                    case 0:
                        Intent intent = new Intent(getActivity(), UserInfoActivity.class);
                        intent.putExtra("userInfo", userInfo.getUserInfo());
                        startActivity(intent);
                        break;
                    case 1:
                        AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
                        AVEngineKit.ParticipantProfile profile = session.getParticipantProfile(userInfo.getUserInfo().uid);
                        if (selfUid.equals(session.getHost()) && !selfUid.equals(userInfo.getUserInfo().uid)) {
                            requestChangeMode(session.getCallId(), userInfo.getUserInfo().uid, !profile.isAudience());
                            if (profile.isAudience()) {
                                Toast.makeText(getActivity(), "已经请求用户，等待用户同意...", Toast.LENGTH_SHORT).show();
                            } else {
                                new Handler().postDelayed(this::loadAndShowConferenceParticipants, 1500);
                            }
                        } else if (selfUid.equals(userInfo.getUserInfo().uid)) {
                            session.switchAudience(!profile.isAudience());
                            new Handler().postDelayed(this::loadAndShowConferenceParticipants, 1500);
                        }
                        break;
                    case 2:
                        AVEngineKit.CallSession session2 = AVEngineKit.Instance().getCurrentSession();
                        session2.kickoffParticipant(userInfo.getUserInfo().uid, new AVEngineKit.GeneralCallback() {
                            @Override
                            public void onSuccess() {
                                new Handler().postDelayed(ConferenceParticipantListFragment.this::loadAndShowConferenceParticipants, 1500);
                            }

                            @Override
                            public void onFailure(int error_code) {

                            }
                        });
                        break;
                    default:
                        break;
                }
            })
            .show();
    }

    private void loadAndShowConferenceParticipants() {
        List<AVEngineKit.ParticipantProfile> profiles = session.getParticipantProfiles();
        List<UIUserInfo> uiUserInfos = new ArrayList<>();
        if (profiles != null && profiles.size() > 0) {
            for (AVEngineKit.ParticipantProfile p : profiles) {
                UserInfo userInfo = ChatManager.Instance().getUserInfo(p.getUserId(), false);
                UIUserInfo uiUserInfo = new UIUserInfo(userInfo);
                uiUserInfo.setCategory(p.isAudience() ? "听众" : "互动成员");
                uiUserInfo.setExtra(p.isAudience() ? "audience" : "");

                String desc = "";
                if (session.getHost().equals(userInfo.uid)) {
                    desc = "主持人";
                    uiUserInfos.add(0, uiUserInfo);
                } else {
                    uiUserInfos.add(uiUserInfo);
                }
                if (p.isScreenSharing()) {
                    desc += " 屏幕共享";
                }
                if (p.isVideoMuted()){
                    desc += " 关闭摄像头";
                }
                if (p.isAudioMuted()){
                    desc += " 静音";
                }
                uiUserInfo.setDesc(desc);
            }
        }
        UIUserInfo selfUiUserInfo = new UIUserInfo(ChatManager.Instance().getUserInfo(selfUid, false));
        AVEngineKit.ParticipantProfile mp = session.getMyProfile();
        String desc = "";
        if (session.getHost().equals(selfUid)) {
            desc = "我、主持人";
        } else {
            desc = "我";
        }
        if (mp.isScreenSharing()) {
            desc += " 屏幕共享";
        }
        if (mp.isVideoMuted()){
            desc += " 关闭摄像头";
        }
        if (mp.isAudioMuted()){
            desc += " 静音";
        }
        selfUiUserInfo.setDesc(desc);
        selfUiUserInfo.setCategory(session.isAudience() ? "听众" : "互动成员");
        uiUserInfos.add(selfUiUserInfo);

        Collections.sort(uiUserInfos, (o1, o2) -> {
            if (o1.getCategory().equals("听众")) {
                return 1;
            } else {
                return -1;
            }
        });

        String lastCategory = null;
        for (UIUserInfo uiUserInfo : uiUserInfos) {
            if (lastCategory == null) {
                uiUserInfo.setShowCategory(true);
                lastCategory = uiUserInfo.getCategory();
            } else if (!lastCategory.equals(uiUserInfo.getCategory())) {
                uiUserInfo.setShowCategory(true);
                lastCategory = uiUserInfo.getCategory();
            } else {
                uiUserInfo.setShowCategory(false);
            }
        }

        showContent();
        userListAdapter.setUsers(uiUserInfos);
    }

    private void requestChangeMode(String conferenceId, String userId, boolean audience) {
        ConferenceChangeModeContent content = new ConferenceChangeModeContent(conferenceId, audience);
        Conversation conversation = new Conversation(Conversation.ConversationType.Single, userId);
        ChatManager.Instance().sendMessage(conversation, content, null, 0, null);
    }

}
