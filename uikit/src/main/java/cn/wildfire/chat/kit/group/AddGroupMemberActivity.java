/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.group;

import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.contact.OrganizationServiceViewModel;
import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfire.chat.kit.contact.pick.PickUserViewModel;
import cn.wildfire.chat.kit.organization.model.Employee;
import cn.wildfire.chat.kit.organization.model.Organization;
import cn.wildfirechat.ErrorCode;
import cn.wildfirechat.client.GroupMemberSource;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.remote.ChatManager;

public class AddGroupMemberActivity extends WfcBaseActivity {
    private TextView confirmTv;

    private GroupInfo groupInfo;
    public static final int RESULT_ADD_SUCCESS = 2;
    public static final int RESULT_ADD_FAIL = 3;

    private PickUserViewModel pickUserViewModel;
    private GroupViewModel groupViewModel;
    private AddGroupMemberFragment addGroupMemberFragment;
    private Observer<Object> contactCheckStatusUpdateLiveDataObserver = new Observer<Object>() {
        @Override
        public void onChanged(@Nullable Object object) {
            updateConfirmStatus();
        }
    };

    @Override
    protected int contentLayout() {
        return R.layout.fragment_container_activity;
    }

    @Override
    protected void afterViews() {
        groupInfo = getIntent().getParcelableExtra("groupInfo");
        if (groupInfo == null) {
            finish();
            return;
        }

        pickUserViewModel = new ViewModelProvider(this).get(PickUserViewModel.class);
        pickUserViewModel.userCheckStatusUpdateLiveData().observeForever(contactCheckStatusUpdateLiveDataObserver);
        groupViewModel = WfcUIKit.getAppScopeViewModel(GroupViewModel.class);
        addGroupMemberFragment = AddGroupMemberFragment.newInstance(groupInfo);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.containerFrameLayout, addGroupMemberFragment)
                .commit();
    }

    @Override
    protected int menu() {
        return R.menu.group_add_member;
    }

    @Override
    protected void afterMenus(Menu menu) {
        super.afterMenus(menu);
        MenuItem item = menu.findItem(R.id.add);
        View actionView = item.getActionView();
        confirmTv = actionView.findViewById(R.id.confirm_tv);
        confirmTv.setOnClickListener(v -> addMember());
        updateConfirmStatus();
    }

    private List<UIUserInfo> getAllCheckedUsers() {
        List<UIUserInfo> checkedUsers = new ArrayList<>(pickUserViewModel.getCheckedUsers());
        for (UIUserInfo picked : addGroupMemberFragment.getPickedUsers()) {
            boolean exists = false;
            for (UIUserInfo checked : checkedUsers) {
                if (checked.getUserInfo().uid.equals(picked.getUserInfo().uid)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                checkedUsers.add(picked);
            }
        }
        return checkedUsers;
    }

    void updateConfirmStatus() {
        if (confirmTv == null || pickUserViewModel == null) {
            return;
        }
        int count = pickUserViewModel.getCheckedUsers().size() + pickUserViewModel.getCheckedEmployees().size() + pickUserViewModel.getCheckedOrganizations().size();
        if (count == 0) {
            confirmTv.setText(R.string.complete);
            confirmTv.setEnabled(false);
        } else {
            confirmTv.setText(getString(R.string.complete_with_count, count));
            confirmTv.setEnabled(true);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        pickUserViewModel.userCheckStatusUpdateLiveData().removeObserver(contactCheckStatusUpdateLiveDataObserver);
    }

    void addMember() {
        MaterialDialog dialog = new MaterialDialog.Builder(this)
                .content(R.string.adding)
                .progress(true, 100)
                .cancelable(false)
                .build();
        dialog.show();
        Set<String> checkedIds = new HashSet<>();
        List<UIUserInfo> checkedUsers = pickUserViewModel.getCheckedUsers();
        for (UIUserInfo user : checkedUsers) {
            checkedIds.add(user.getUserInfo().uid);
        }
        List<Employee> employees = pickUserViewModel.getCheckedEmployees();
        for (Employee e : employees) {
            checkedIds.add(e.employeeId);
        }

        List<Organization> organizations = pickUserViewModel.getCheckedOrganizations();
        if (organizations != null && !organizations.isEmpty() && Config.ENABLE_SELECT_ORGANIZATION) {
            OrganizationServiceViewModel organizationServiceViewModel = new ViewModelProvider(this).get(OrganizationServiceViewModel.class);
            List<Integer> orgIds = new ArrayList<>();
            for (Organization org : organizations) {
                orgIds.add(org.id);
            }
            organizationServiceViewModel.getOrganizationEmployees(orgIds, true).observe(this, es -> {
                if (es != null) {
                    for (Employee e : es) {
                        checkedIds.add(e.employeeId);
                    }
                }
                addMember(dialog, new ArrayList<>(checkedIds));
            });
        } else {
            addMember(dialog, new ArrayList<>(checkedIds));
        }
    }

    private void addMember(MaterialDialog dialog, ArrayList<String> userIds) {
        if (!userIds.isEmpty()) {
            String memberExtra = GroupMemberSource.buildGroupMemberSourceExtra(GroupMemberSource.Type_Invite, ChatManager.Instance().getUserId());
            groupViewModel.addGroupMemberEx(groupInfo, userIds, null, Collections.singletonList(0), memberExtra).observe(this, result -> {
                dialog.dismiss();
                Intent intent = new Intent();
                if (result.isSuccess()) {
                    intent.putStringArrayListExtra("memberIds", userIds);
                    setResult(RESULT_ADD_SUCCESS, intent);
                    Toast.makeText(this, getString(R.string.add_member_success), Toast.LENGTH_SHORT).show();
                    finish();
                } else if (result.getErrorCode() == ErrorCode.JOIN_GROUP_FAILED_NEED_VERIFY) {
                    new MaterialDialog.Builder(this)
                            .title(R.string.join_group_need_verify)
                            .input(getString(R.string.join_group_reason_hint), "", (dialog1, input) -> {
                                groupViewModel.sendJoinGroupRequest(groupInfo.target, userIds, input.toString(), memberExtra).observe(this, rst -> {
                                    if (rst.isSuccess()) {
                                        Toast.makeText(this, R.string.request_sent, Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(this, getString(R.string.request_failed, result.getErrorCode()), Toast.LENGTH_SHORT).show();
                                    }
                                    dialog1.dismiss();
                                    setResult(RESULT_ADD_FAIL);
                                    finish();
                                });
                            }).show();
                } else {
                    Toast.makeText(this, getString(R.string.add_member_fail), Toast.LENGTH_SHORT).show();
                    setResult(RESULT_ADD_FAIL);
                    finish();
                }
            });
        }
    }

}
