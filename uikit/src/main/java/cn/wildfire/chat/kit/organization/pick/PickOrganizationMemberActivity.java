/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.organization.pick;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.Collection;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.organization.model.Employee;
import cn.wildfire.chat.kit.organization.model.Organization;

public class PickOrganizationMemberActivity extends WfcBaseActivity implements CheckableOrganizationMemberListAdapter.OnOrganizationMemberClickListener {
    private int organizationId;
    private MenuItem menuItem;
    private PickOrganizationMemberFragment pickOrganizationMemberFragment;

    @Override
    protected void afterViews() {
        this.organizationId = getIntent().getIntExtra("organizationId", 0);
        pickOrganizationMemberFragment = new PickOrganizationMemberFragment();
        pickOrganizationMemberFragment.setOnOrganizationMemberClickListener(this);
        Bundle args = new Bundle();
        args.putInt("organizationId", organizationId);
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
        menuItem = menu.findItem(R.id.confirm);
        menuItem.setEnabled(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.confirm) {
            onConfirmClick();
            return true;
        }
        return super.onOptionsItemSelected(item);
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
        updateMenuState();
    }

    @Override
    public void onEmployeeCheck(Employee employee, boolean checked) {
        updateMenuState();
    }

    private void updateMenuState() {
        if (!pickOrganizationMemberFragment.getCheckedOrganizations().isEmpty() || !pickOrganizationMemberFragment.getCheckedMembers().isEmpty()) {
            menuItem.setEnabled(true);
        } else {
            menuItem.setEnabled(false);
        }
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
