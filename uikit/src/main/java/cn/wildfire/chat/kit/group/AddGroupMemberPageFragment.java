/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.group;

import android.content.Intent;
import android.os.Bundle;
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
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.contact.OrganizationServiceViewModel;
import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfire.chat.kit.organization.model.Employee;
import cn.wildfire.chat.kit.organization.model.Organization;
import cn.wildfire.chat.kit.page.WfcPage;
import cn.wildfire.chat.kit.page.WfcPageCompat;
import cn.wildfirechat.ErrorCode;
import cn.wildfirechat.client.GroupMemberSource;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.remote.ChatManager;

/**
 * 「加群成员」整页：联系人列表 + 确定菜单 + 真正的加人逻辑。
 * <p>
 * 逐行搬自 {@link AddGroupMemberActivity}，只是宿主从 Activity 换成
 * {@link cn.wildfire.chat.kit.page.WfcPageHost} —— 于是手机端（{@code AddGroupMemberActivity} 这个壳）
 * 与平板右栏共用同一份实现。
 * <p>
 * 结果仍按原来的 {@code RESULT_ADD_SUCCESS} / {@code RESULT_ADD_FAIL} 回传，
 * 走 {@link WfcPageCompat#setPageResult}：手机端就是 {@code setResult}，
 * 右栏则在本页出栈时投递回发起方。
 */
public class AddGroupMemberPageFragment extends AddGroupMemberFragment implements WfcPage {

    private TextView confirmTextView;
    private GroupInfo groupInfo;
    private GroupViewModel groupViewModel;

    private final Observer<Object> checkStatusObserver = obj -> updateConfirmStatus();

    public static AddGroupMemberPageFragment newInstance(GroupInfo groupInfo) {
        Bundle args = new Bundle();
        args.putParcelable("groupInfo", groupInfo);
        AddGroupMemberPageFragment fragment = new AddGroupMemberPageFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    public static AddGroupMemberPageFragment fromIntent(Intent intent) {
        GroupInfo groupInfo = intent.getParcelableExtra("groupInfo");
        return groupInfo == null ? null : newInstance(groupInfo);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        groupInfo = getArguments() == null ? null : getArguments().getParcelable("groupInfo");
        groupViewModel = WfcUIKit.getAppScopeViewModel(GroupViewModel.class);
        pickUserViewModel.userCheckStatusUpdateLiveData().observeForever(checkStatusObserver);
    }

    @Override
    public void onDestroy() {
        pickUserViewModel.userCheckStatusUpdateLiveData().removeObserver(checkStatusObserver);
        super.onDestroy();
    }

    // ==================== WfcPage ====================

    @Override
    public int pageMenu() {
        return R.menu.group_add_member;
    }

    @Override
    public void onPreparePageMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.add);
        if (item == null) {
            return;
        }
        View actionView = item.getActionView();
        confirmTextView = actionView == null ? null : actionView.findViewById(R.id.confirm_tv);
        if (confirmTextView != null) {
            confirmTextView.setOnClickListener(v -> addMember());
        }
        updateConfirmStatus();
    }

    @Override
    public boolean onPageMenuItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.add) {
            addMember();
            return true;
        }
        return false;
    }

    private void updateConfirmStatus() {
        if (confirmTextView == null || pickUserViewModel == null) {
            return;
        }
        int count = pickUserViewModel.getCheckedUsers().size()
            + pickUserViewModel.getCheckedEmployees().size()
            + pickUserViewModel.getCheckedOrganizations().size();
        if (count == 0) {
            confirmTextView.setText(R.string.complete);
            confirmTextView.setEnabled(false);
        } else {
            confirmTextView.setText(getString(R.string.complete_with_count, count));
            confirmTextView.setEnabled(true);
        }
    }

    // ==================== 加人 ====================

    private void addMember() {
        if (groupInfo == null) {
            return;
        }
        MaterialDialog dialog = new MaterialDialog.Builder(requireContext())
            .content(R.string.adding)
            .progress(true, 100)
            .cancelable(false)
            .build();
        dialog.show();

        Set<String> checkedIds = new HashSet<>();
        for (UIUserInfo user : pickUserViewModel.getCheckedUsers()) {
            checkedIds.add(user.getUserInfo().uid);
        }
        for (Employee e : pickUserViewModel.getCheckedEmployees()) {
            checkedIds.add(e.employeeId);
        }

        List<Organization> organizations = pickUserViewModel.getCheckedOrganizations();
        if (organizations != null && !organizations.isEmpty() && Config.ENABLE_SELECT_ORGANIZATION) {
            OrganizationServiceViewModel organizationServiceViewModel =
                new ViewModelProvider(this).get(OrganizationServiceViewModel.class);
            List<Integer> orgIds = new ArrayList<>();
            for (Organization org : organizations) {
                orgIds.add(org.id);
            }
            organizationServiceViewModel.getOrganizationEmployees(orgIds, true)
                .observe(getViewLifecycleOwner(), es -> {
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
        if (userIds.isEmpty()) {
            dialog.dismiss();
            return;
        }
        String memberExtra = GroupMemberSource.buildGroupMemberSourceExtra(
            GroupMemberSource.Type_Invite, ChatManager.Instance().getUserId());
        groupViewModel.addGroupMemberEx(groupInfo, userIds, null, Collections.singletonList(0), memberExtra)
            .observe(getViewLifecycleOwner(), result -> {
                dialog.dismiss();
                if (result.isSuccess()) {
                    Intent data = new Intent();
                    data.putStringArrayListExtra("memberIds", userIds);
                    Toast.makeText(getActivity(), getString(R.string.add_member_success), Toast.LENGTH_SHORT).show();
                    finishWithResult(AddGroupMemberActivity.RESULT_ADD_SUCCESS, data);
                } else if (result.getErrorCode() == ErrorCode.JOIN_GROUP_FAILED_NEED_VERIFY) {
                    new MaterialDialog.Builder(requireContext())
                        .title(R.string.join_group_need_verify)
                        .input(getString(R.string.join_group_reason_hint), "", (dialog1, input) ->
                            groupViewModel.sendJoinGroupRequest(groupInfo.target, userIds, input.toString(), memberExtra)
                                .observe(getViewLifecycleOwner(), rst -> {
                                    if (rst.isSuccess()) {
                                        Toast.makeText(getActivity(), R.string.request_sent, Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(getActivity(), getString(R.string.request_failed, result.getErrorCode()), Toast.LENGTH_SHORT).show();
                                    }
                                    dialog1.dismiss();
                                    finishWithResult(AddGroupMemberActivity.RESULT_ADD_FAIL, null);
                                })).show();
                } else {
                    Toast.makeText(getActivity(), getString(R.string.add_member_fail), Toast.LENGTH_SHORT).show();
                    finishWithResult(AddGroupMemberActivity.RESULT_ADD_FAIL, null);
                }
            });
    }

    private void finishWithResult(int resultCode, @Nullable Intent data) {
        WfcPageCompat.setPageResult(this, resultCode, data);
        WfcPageCompat.finishPage(this);
    }
}
