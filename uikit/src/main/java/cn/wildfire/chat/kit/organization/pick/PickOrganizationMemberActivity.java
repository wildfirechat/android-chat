/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
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
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.organization.model.Employee;
import cn.wildfire.chat.kit.organization.model.Organization;

public class PickOrganizationMemberActivity extends WfcBaseActivity implements CheckableOrganizationMemberListAdapter.OnOrganizationMemberClickListener {
    public static final String PARAM_ORGANIZATION_ID = "organizationId";
    public static final String PARAM_MAX_PICK_COUNT = "maxPickCount";
    public static final String PARAM_INITIAL_CHECKED_EMPLOYEES = "initialCheckedEmployees";
    public static final String PARAM_INITIAL_CHECKED_ORANIZATIONS = "initialCheckedOrganizations";
    public static final String PARAM_UNCHECKABLE_IDS = "uncheckableIds";

    private int organizationId;
    private TextView confirmTv;
    private PickOrganizationMemberFragment pickOrganizationMemberFragment;
    private ArrayList<Employee> initialCheckedEmployees;
    private ArrayList<Organization> initialCheckedOrganizations;
    private ArrayList<String> uncheckableIds;

    @Override
    protected void afterViews() {
        this.organizationId = getIntent().getIntExtra(PARAM_ORGANIZATION_ID, 0);
        int maxPickCount = getIntent().getIntExtra(PARAM_MAX_PICK_COUNT, Integer.MAX_VALUE);

        initialCheckedEmployees = getIntent().getParcelableArrayListExtra(PARAM_INITIAL_CHECKED_EMPLOYEES);
        initialCheckedOrganizations = getIntent().getParcelableArrayListExtra(PARAM_INITIAL_CHECKED_ORANIZATIONS);
        uncheckableIds = getIntent().getStringArrayListExtra(PARAM_UNCHECKABLE_IDS);

        pickOrganizationMemberFragment = new PickOrganizationMemberFragment();
        pickOrganizationMemberFragment.setOnOrganizationMemberClickListener(this);
        Bundle args = new Bundle();
        args.putInt(PARAM_ORGANIZATION_ID, organizationId);
        args.putInt(PARAM_MAX_PICK_COUNT, maxPickCount);
        if (initialCheckedEmployees != null) {
            args.putParcelableArrayList(PARAM_INITIAL_CHECKED_EMPLOYEES, initialCheckedEmployees);
        }
        if (initialCheckedOrganizations != null) {
            args.putParcelableArrayList(PARAM_INITIAL_CHECKED_ORANIZATIONS, initialCheckedOrganizations);
        }
        if (uncheckableIds != null) {
            args.putStringArrayList(PARAM_UNCHECKABLE_IDS, uncheckableIds);
        }
        pickOrganizationMemberFragment.setArguments(args);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.containerFrameLayout, pickOrganizationMemberFragment)
                .commit();
    }

    @Override
    protected int menu() {
        return R.menu.organization_member_pick;
    }

    @Override
    protected void afterMenus(Menu menu) {
        MenuItem item = menu.findItem(R.id.confirm);
        View actionView = item.getActionView();
        confirmTv = actionView.findViewById(R.id.confirm_tv);
        confirmTv.setOnClickListener(v -> onConfirmClick());
        updateMenuItemState();
    }

    @Override
    protected int contentLayout() {
        return R.layout.fragment_container_activity;
    }

    @Override
    public void onOrganizationClick(Organization organization) {
        // do nothing
    }

    @Override
    public void onOrganizationCheck(Organization organization, boolean checked) {
        updateMenuItemState();
    }

    @Override
    public void onEmployeeCheck(Employee employee, boolean checked) {
        updateMenuItemState();
    }

    private void updateMenuItemState() {
        if (pickOrganizationMemberFragment == null) {
            return;
        }
        int count = pickOrganizationMemberFragment.getCheckedMembers().size() + pickOrganizationMemberFragment.getCheckedOrganizations().size();
        if (count > 0) {
            confirmTv.setText(getString(R.string.complete_with_count, count));
            confirmTv.setEnabled(true);
        } else {
            confirmTv.setText(R.string.complete);
            confirmTv.setEnabled(false);
        }
    }

    @Override
    public void onPickLimitExceeded() {
        Toast.makeText(this, R.string.pick_user_limit_exceeded, Toast.LENGTH_SHORT).show();
    }

    private void onConfirmClick() {
        Intent data = new Intent();
        Collection<Organization> checkedOrganizations = pickOrganizationMemberFragment.getCheckedOrganizations();
        Collection<Employee> checkedMembers = pickOrganizationMemberFragment.getCheckedMembers();
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
        setResult(Activity.RESULT_OK, data);

        finish();
    }
}
