/*
 * Copyright (c) 2021 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.voip.conference;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.wildfire.chat.kit.contact.BaseUserListFragment;
import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.avenginekit.PeerConnectionClient;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

public class ConferenceParticipantListFragment extends BaseUserListFragment {
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showQuickIndexBar(false);
    }

    @Override
    protected void afterViews(View view) {
        super.afterViews(view);
        loadAndShowConferenceParticipants();
    }

    private void loadAndShowConferenceParticipants() {
        AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
        if (session == null || session.state == AVEngineKit.CallState.Idle) {
            Activity activity = getActivity();
            if (activity != null && !activity.isFinishing()) {
                activity.finish();
            }
            return;
        }

        List<String> participantIds = session.getParticipantIds();
        String selfUid = ChatManager.Instance().getUserId();
        List<UserInfo> participantUserInfos = ChatManager.Instance().getUserInfos(participantIds, null);
        List<UIUserInfo> uiUserInfos = new ArrayList<>();
        for (UserInfo userInfo : participantUserInfos) {
            PeerConnectionClient client = session.getClient(userInfo.uid);
            UIUserInfo uiUserInfo = new UIUserInfo(userInfo);
            uiUserInfo.setCategory(client.audience ? "听众" : "互动成员");

            if (session.initiator.equals(userInfo.uid)) {
                uiUserInfo.setDesc("主持人");
                uiUserInfos.add(0, uiUserInfo);
            } else {
                uiUserInfos.add(uiUserInfo);
            }
        }
        UIUserInfo selfUiUserInfo = new UIUserInfo(ChatManager.Instance().getUserInfo(selfUid, false));
        selfUiUserInfo.setDesc("我");
        selfUiUserInfo.setCategory(session.isAudience() ? "听众" : "互动成员");
        uiUserInfos.add(selfUiUserInfo);

        Collections.sort(uiUserInfos, (o1, o2) -> {
            if (o1.getCategory().equals("听众")) {
                return 0;
            } else {
                return 1;
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
}
