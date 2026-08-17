/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.organization.pick;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collection;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.organization.model.Employee;
import cn.wildfire.chat.kit.organization.model.Organization;
import cn.wildfire.chat.kit.page.WfcPage;
import cn.wildfire.chat.kit.page.WfcPageCompat;

/**
 * 「在组织架构里勾人回传给调用方」整页：部门层级列表 + 选择菜单 + 回传结果。
 * <p>
 * 逐行搬自 {@link PickOrganizationMemberActivity}，那个类现在只是手机端的壳。
 * 两个入口（发起群聊里点部门、群里加成员点部门）本身都已经在右栏，不迁的话选人选到一半会跳全屏。
 * <p>
 * 改造前 Activity 是通过 {@code setOnOrganizationMemberClickListener(this)} 监听列表的勾选的；
 * 本页与列表是同一个对象，改成<strong>覆写父类的回调</strong>——再挂监听器会指向自己，无限递归。
 * 外部监听器那条路径原样保留（super 里仍会转发），供别的调用方使用。
 */
public class PickOrganizationMemberPageFragment extends PickOrganizationMemberFragment implements WfcPage {

    private TextView confirmTextView;

    public static PickOrganizationMemberPageFragment fromIntent(Intent intent) {
        PickOrganizationMemberPageFragment fragment = new PickOrganizationMemberPageFragment();
        Bundle args = new Bundle();
        args.putInt(PickOrganizationMemberActivity.PARAM_ORGANIZATION_ID,
            intent.getIntExtra(PickOrganizationMemberActivity.PARAM_ORGANIZATION_ID, 0));
        args.putInt(PickOrganizationMemberActivity.PARAM_MAX_PICK_COUNT,
            intent.getIntExtra(PickOrganizationMemberActivity.PARAM_MAX_PICK_COUNT, Integer.MAX_VALUE));

        ArrayList<Employee> initialCheckedEmployees =
            intent.getParcelableArrayListExtra(PickOrganizationMemberActivity.PARAM_INITIAL_CHECKED_EMPLOYEES);
        if (initialCheckedEmployees != null) {
            args.putParcelableArrayList(PickOrganizationMemberActivity.PARAM_INITIAL_CHECKED_EMPLOYEES, initialCheckedEmployees);
        }
        ArrayList<Organization> initialCheckedOrganizations =
            intent.getParcelableArrayListExtra(PickOrganizationMemberActivity.PARAM_INITIAL_CHECKED_ORANIZATIONS);
        if (initialCheckedOrganizations != null) {
            args.putParcelableArrayList(PickOrganizationMemberActivity.PARAM_INITIAL_CHECKED_ORANIZATIONS, initialCheckedOrganizations);
        }
        ArrayList<String> uncheckableIds =
            intent.getStringArrayListExtra(PickOrganizationMemberActivity.PARAM_UNCHECKABLE_IDS);
        if (uncheckableIds != null) {
            args.putStringArrayList(PickOrganizationMemberActivity.PARAM_UNCHECKABLE_IDS, uncheckableIds);
        }
        fragment.setArguments(args);
        return fragment;
    }

    // ==================== 列表回调 ====================

    @Override
    public void onOrganizationCheck(Organization organization, boolean checked) {
        super.onOrganizationCheck(organization, checked);
        updateConfirmStatus();
    }

    @Override
    public void onEmployeeCheck(Employee employee, boolean checked) {
        super.onEmployeeCheck(employee, checked);
        updateConfirmStatus();
    }

    @Override
    public void onPickLimitExceeded() {
        super.onPickLimitExceeded();
        Toast.makeText(getActivity(), R.string.pick_user_limit_exceeded, Toast.LENGTH_SHORT).show();
    }

    // ==================== WfcPage ====================

    @Override
    public int pageMenu() {
        return R.menu.organization_member_pick;
    }

    @Override
    public void onPreparePageMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.confirm);
        if (item == null) {
            return;
        }
        View actionView = item.getActionView();
        confirmTextView = actionView == null ? null : actionView.findViewById(R.id.confirm_tv);
        if (confirmTextView != null) {
            confirmTextView.setOnClickListener(v -> onConfirmClick());
        }
        updateConfirmStatus();
    }

    @Override
    public boolean onPageMenuItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.confirm) {
            onConfirmClick();
            return true;
        }
        return false;
    }

    private void updateConfirmStatus() {
        // 列表 adapter 是父类在 onCreateView 里建的，视图还没有就问不出勾选数。
        // 右栏恢复已有页面时 setupMenu 会先于视图创建跑一次，这里必须挡住。
        if (confirmTextView == null || getView() == null) {
            return;
        }
        int count = getCheckedMembers().size() + getCheckedOrganizations().size();
        if (count > 0) {
            confirmTextView.setText(getString(R.string.complete_with_count, count));
            confirmTextView.setEnabled(true);
        } else {
            confirmTextView.setText(R.string.complete);
            confirmTextView.setEnabled(false);
        }
    }

    private void onConfirmClick() {
        Intent data = new Intent();
        Collection<Organization> checkedOrganizations = getCheckedOrganizations();
        Collection<Employee> checkedMembers = getCheckedMembers();
        // 逐字节照抄改造前的写法，接收方（PickConversationTargetFragment / AddGroupMemberFragment）
        // 按 getParcelableArrayListExtra 取，键名和类型都不能动
        if (checkedOrganizations != null && !checkedOrganizations.isEmpty()) {
            ArrayList<Organization> organizations = new ArrayList<>(checkedOrganizations.size());
            organizations.addAll(checkedOrganizations);
            data.putExtra("organizations", organizations);
        }
        if (checkedMembers != null && !checkedMembers.isEmpty()) {
            ArrayList<Employee> employees = new ArrayList<>(checkedMembers.size());
            employees.addAll(checkedMembers);
            data.putExtra("employees", employees);
        }
        WfcPageCompat.setPageResult(this, Activity.RESULT_OK, data);
        WfcPageCompat.finishPage(this);
    }
}
