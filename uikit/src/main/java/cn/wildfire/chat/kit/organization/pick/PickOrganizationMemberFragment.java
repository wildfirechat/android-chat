/*
 * Copyright (c) 2023 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.organization.pick;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.contact.OrganizationServiceViewModel;
import cn.wildfire.chat.kit.organization.model.Employee;
import cn.wildfire.chat.kit.organization.model.Organization;
import cn.wildfire.chat.kit.organization.model.OrganizationEx;
import cn.wildfire.chat.kit.widget.BreadCrumbsView;
import cn.wildfire.chat.kit.widget.ProgressFragment;

public class PickOrganizationMemberFragment extends ProgressFragment implements CheckableOrganizationMemberListAdapter.OnOrganizationMemberClickListener, BreadCrumbsView.OnTabListener {
    private int currentOrgId;
    private RecyclerView recyclerView;
    private BreadCrumbsView breadCrumbsView;
    private CheckableOrganizationMemberListAdapter adapter;
    private CheckableOrganizationMemberListAdapter.OnOrganizationMemberClickListener onOrganizationMemberClickListener;

    private OrganizationServiceViewModel organizationServiceViewModel;

    private static final String TAG = "PickOrgMemberFragment";

    private List<Integer> initialCheckedOrganizationIds;
    private List<String> initialCheckedEmployeeIds;

    @Override
    protected int contentLayout() {
        return R.layout.organization_member_list_fragment;
    }

    @Override
    protected void afterViews(View view) {
        super.afterViews(view);
        Bundle bundle = getArguments();
        if (bundle != null) {
            currentOrgId = bundle.getInt("organizationId");
        }
        recyclerView = view.findViewById(R.id.recyclerView);
        adapter = new CheckableOrganizationMemberListAdapter(this);
        adapter.setOnOrganizationMemberClickListener(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        breadCrumbsView = view.findViewById(R.id.breadCrumbsView);
        breadCrumbsView.setOnTabListener(this);

        organizationServiceViewModel = new ViewModelProvider(this).get(OrganizationServiceViewModel.class);

        loadAndShowOrganizationMemberList(currentOrgId);
        loadAndShowOrganizationPath(currentOrgId);
    }


    @Override
    public void onOrganizationClick(Organization organization) {
        loadAndShowOrganizationMemberList(organization.id);
        loadAndShowOrganizationPath(organization.id);

        if (this.onOrganizationMemberClickListener != null) {
            this.onOrganizationMemberClickListener.onOrganizationClick(organization);
        }
    }

    @Override
    public void onOrganizationCheck(Organization organization, boolean checked) {
        if (this.onOrganizationMemberClickListener != null) {
            this.onOrganizationMemberClickListener.onOrganizationCheck(organization, checked);
        }
    }

    @Override
    public void onEmployeeCheck(Employee employee, boolean checked) {
        if (this.onOrganizationMemberClickListener != null) {
            this.onOrganizationMemberClickListener.onEmployeeCheck(employee, checked);
        }

    }

    public void setOnOrganizationMemberClickListener(CheckableOrganizationMemberListAdapter.OnOrganizationMemberClickListener onOrganizationMemberClickListener) {
        this.onOrganizationMemberClickListener = onOrganizationMemberClickListener;
    }

    public Collection<Organization> getCheckedOrganizations() {
        return adapter.getCheckedOrganizations();
    }

    public Collection<Employee> getCheckedMembers() {
        return adapter.getCheckedMembers();
    }

    private void loadAndShowOrganizationMemberList(int orgId) {
        organizationServiceViewModel.getOrganizationEx(orgId).observe(this, new Observer<OrganizationEx>() {
            @Override
            public void onChanged(OrganizationEx organizationEx) {
                adapter.setOrganizationEx(organizationEx);
                adapter.notifyDataSetChanged();
                showContent();
            }
        });
    }

    private void loadAndShowOrganizationPath(int orgId) {
        organizationServiceViewModel.getOrganizationPath(orgId).observe(this, new Observer<List<Organization>>() {
            @Override
            public void onChanged(List<Organization> organizations) {
                breadCrumbsView.clearAllTab();
                Log.d(TAG, "breadCrumbsView clear all tabs " + organizations.size());
                if (!organizations.isEmpty()) {
                    for (Organization org : organizations) {
                        Map<String, Object> param = new HashMap<>();
                        param.put("org", org);
                        breadCrumbsView.addTab(org.name, param);
                        Log.d(TAG, "breadCrumbsView add tab " + org.name);
                    }
                }
            }
        });
    }

    @Override
    public void onAdded(BreadCrumbsView.Tab tab) {
    }

    @Override
    public void onActivated(BreadCrumbsView.Tab tab) {
        Map<String, Object> param = tab.getValue();
        Organization org = (Organization) param.get("org");
        loadAndShowOrganizationMemberList(org.id);
    }

    @Override
    public void onRemoved(BreadCrumbsView.Tab tab) {

    }
}
