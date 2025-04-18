/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation;

import android.content.Intent;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;


import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.contact.OrganizationServiceViewModel;
import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfire.chat.kit.contact.pick.PickConversationTargetActivity;
import cn.wildfire.chat.kit.group.GroupViewModel;
import cn.wildfire.chat.kit.organization.model.Employee;
import cn.wildfire.chat.kit.organization.model.Organization;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfirechat.client.GroupMemberSource;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

public class CreateConversationActivity extends PickConversationTargetActivity {
    private GroupViewModel groupViewModel;

    @Override
    protected void afterViews() {
        super.afterViews();
        groupViewModel = WfcUIKit.getAppScopeViewModel(GroupViewModel.class);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onContactPicked(List<UIUserInfo> newlyCheckedUserInfos, List<Organization> organizations) {
        List<String> initialCheckedIds = pickUserViewModel.getInitialCheckedIds();
        List<UserInfo> userInfos = null;
        if (initialCheckedIds != null && !initialCheckedIds.isEmpty()) {
            UserViewModel userViewModel = WfcUIKit.getAppScopeViewModel(UserViewModel.class);
            userInfos = userViewModel.getUserInfos(initialCheckedIds);
        }
        userInfos = userInfos == null ? new ArrayList<>() : userInfos;

        for (UIUserInfo uiUserinfo : newlyCheckedUserInfos) {
            userInfos.add(uiUserinfo.getUserInfo());
        }

        if (organizations != null && !organizations.isEmpty()) {
            OrganizationServiceViewModel organizationServiceViewModel = new ViewModelProvider(this).get(OrganizationServiceViewModel.class);
            List<Integer> orgIds = new ArrayList<>();
            for (Organization org : organizations) {
                orgIds.add(org.id);
            }
            List<UserInfo> finalUserInfos = userInfos;
            organizationServiceViewModel.getOrganizationEmployees(orgIds, true).observe(this, employees -> {
                if (employees != null) {
                    for (Employee e : employees) {
                        finalUserInfos.add(e.toUserInfo());
                    }
                }
                startConversation(finalUserInfos);
            });
        } else {
            startConversation(userInfos);
        }
    }

    private void startConversation(List<UserInfo> userInfos) {
        if (userInfos == null || userInfos.isEmpty()) {
            return;
        }
        if (userInfos.size() == 1) {
            Toast.makeText(this, R.string.create_conversation_multi_contact_hint, Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, ConversationActivity.class);
            Conversation conversation = new Conversation(Conversation.ConversationType.Single, userInfos.get(0).uid);
            intent.putExtra("conversation", conversation);
            startActivity(intent);
            finish();
        } else {
            MaterialDialog dialog = new MaterialDialog.Builder(this)
                .content(R.string.creating_conversation)
                .progress(true, 100)
                .build();
            dialog.show();

            Map<String, UserInfo> userMap = new HashMap<>();
            for (UserInfo info : userInfos) {
                userMap.put(info.uid, info);
            }

            String memberExtra = GroupMemberSource.buildGroupMemberSourceExtra(GroupMemberSource.Type_Invite, ChatManager.Instance().getUserId());
            groupViewModel.createGroup(this, new ArrayList<UserInfo>(userMap.values()), null, Collections.singletonList(0), null, memberExtra).observe(this, result -> {
                dialog.dismiss();
                if (result.isSuccess()) {
                    Toast.makeText(this, getString(R.string.create_group_success), Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(CreateConversationActivity.this, ConversationActivity.class);
                    Conversation conversation = new Conversation(Conversation.ConversationType.Group, result.getResult(), 0);
                    intent.putExtra("conversation", conversation);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, getString(R.string.create_group_fail), Toast.LENGTH_SHORT).show();
                }
                finish();
            });
        }
    }

    @Override
    public void onGroupPicked(List<GroupInfo> groupInfos) {
        Intent intent = new Intent(this, ConversationActivity.class);
        Conversation conversation = new Conversation(Conversation.ConversationType.Group, groupInfos.get(0).target);
        intent.putExtra("conversation", conversation);
        startActivity(intent);
        finish();
    }
}
