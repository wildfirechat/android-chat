/*
 * Copyright (c) 2023 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.organization;

import android.os.Bundle;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.net.SimpleCallback;
import cn.wildfire.chat.kit.organization.model.Employee;
import cn.wildfire.chat.kit.organization.model.Organization;
import cn.wildfire.chat.kit.organization.model.OrganizationEx;
import cn.wildfire.chat.kit.widget.ProgressFragment;

public class OrganizationMemberListFragment extends ProgressFragment implements OrganizationMemberAdapter.OnOrganizationMemberClickListener {
    private int orgId;
    private RecyclerView recyclerView;
    private OrganizationMemberAdapter adapter;

    @Override
    protected int contentLayout() {
        return R.layout.organization_member_list_fragment;
    }

    @Override
    protected void afterViews(View view) {
        super.afterViews(view);
        Bundle bundle = getArguments();
        if (bundle != null) {
            orgId = bundle.getInt("organizationId");
        }
        recyclerView = view.findViewById(R.id.recyclerView);
        adapter = new OrganizationMemberAdapter(this);
        adapter.setOnOrganizationMemberClickListener(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        loadAndShowOrganizationMemberList(orgId);
    }


    @Override
    public void onOrganizationClick(Organization organization) {
        loadAndShowOrganizationMemberList(organization.id);
    }

    @Override
    public void onEmployeeClick(Employee employee) {

    }

    private void loadAndShowOrganizationMemberList(int orgId) {
        showLoading();
        OrganizationServiceProvider organizationServiceProvider = WfcUIKit.getWfcUIKit().getOrganizationServiceProvider();
        organizationServiceProvider.getOrganizationEx(orgId, new SimpleCallback<OrganizationEx>() {
            @Override
            public void onUiSuccess(OrganizationEx organizationEx) {
                adapter.setOrganizationEx(organizationEx);
                adapter.notifyDataSetChanged();
                showContent();
            }

            @Override
            public void onUiFailure(int code, String msg) {

            }
        });

    }
}
