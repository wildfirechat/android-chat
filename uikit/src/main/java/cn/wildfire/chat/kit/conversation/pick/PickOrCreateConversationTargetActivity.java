/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.pick;

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
import cn.wildfire.chat.kit.contact.OrganizationServiceViewModel;
import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfire.chat.kit.contact.pick.PickConversationTargetActivity;
import cn.wildfire.chat.kit.group.GroupViewModel;
import cn.wildfire.chat.kit.organization.model.Employee;
import cn.wildfire.chat.kit.organization.model.Organization;
import cn.wildfirechat.client.GroupMemberSource;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

// conversation target could be user, group and etc.
public class PickOrCreateConversationTargetActivity extends PickConversationTargetActivity {
    // TODO 多选，单选
    // 先支持单选
    private boolean singleMode = true;

    @Override
    protected void onContactPicked(List<UIUserInfo> newlyCheckedUserInfos, List<Organization> organizations) {
        List<UserInfo> userInfos = new ArrayList<>();

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
                forwardToConversation(finalUserInfos);
            });
        } else {
            forwardToConversation(userInfos);
        }
    }

    private void forwardToConversation(List<UserInfo> userInfos) {
        if (userInfos == null || userInfos.isEmpty()) {
            return;
        }
        if (userInfos.size() == 1) {
            Intent intent = new Intent();
            intent.putExtra("userInfo", userInfos.get(0));
            setResult(RESULT_OK, intent);
            finish();
        } else {
            MaterialDialog dialog = new MaterialDialog.Builder(this)
                .content("创建中...")
                .progress(true, 100)
                .build();
            dialog.show();

            Map<String, UserInfo> userMap = new HashMap<>();
            for (UserInfo info : userInfos) {
                userMap.put(info.uid, info);
            }

            GroupViewModel groupViewModel = new ViewModelProvider(this).get(GroupViewModel.class);
            String memberExtra = GroupMemberSource.buildGroupMemberSourceExtra(GroupMemberSource.Type_Invite, ChatManager.Instance().getUserId());
            groupViewModel.createGroup(this, new ArrayList<UserInfo>(userMap.values()), null, Collections.singletonList(0), null, memberExtra).observe(this, result -> {
                dialog.dismiss();
                if (result.isSuccess()) {
                    GroupInfo groupInfo = groupViewModel.getGroupInfo(result.getResult(), false);
                    Intent intent = new Intent();
                    intent.putExtra("groupInfo", groupInfo);
                    setResult(RESULT_OK, intent);
                } else {
                    Toast.makeText(this, getString(R.string.create_group_fail), Toast.LENGTH_SHORT).show();
                }
                finish();
            });
        }
    }

    // TODO 多选
    @Override
    public void onGroupPicked(List<GroupInfo> groupInfos) {
        Intent intent = new Intent();
        intent.putExtra("groupInfo", groupInfos.get(0));
        setResult(RESULT_OK, intent);
        finish();
    }
}
