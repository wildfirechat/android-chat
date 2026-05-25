/*
 * Copyright (c) 2023 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.organization;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

public class OrganizationMemberListFragment extends ProgressFragment implements OrganizationMemberListAdapter.OnOrganizationMemberClickListener, BreadCrumbsView.OnTabListener {
    private int orgId;
    private boolean pick;
    private RecyclerView recyclerView;
    private BreadCrumbsView breadCrumbsView;
    private OrganizationMemberListAdapter adapter;

    private OrganizationServiceViewModel organizationServiceViewModel;
    private OrganizationEx currentOrganizationEx;
    private MutableLiveData<List<Employee>> searchLiveData;
    private String currentSearchKeyword;

    private static final String TAG = "OrgMemberListFragment";

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
            pick = bundle.getBoolean("pick", false);
        }
        recyclerView = view.findViewById(R.id.recyclerView);
        adapter = new OrganizationMemberListAdapter(this);
        adapter.setOnOrganizationMemberClickListener(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        breadCrumbsView = view.findViewById(R.id.breadCrumbsView);
        breadCrumbsView.setOnTabListener(this);

        organizationServiceViewModel = new ViewModelProvider(this).get(OrganizationServiceViewModel.class);
        loadAndShowOrganizationMemberList(orgId);
        loadAndShowOrganizationPath(orgId);
    }


    @Override
    public void onOrganizationClick(Organization organization) {
        this.orgId = organization.id;
        loadAndShowOrganizationMemberList(organization.id);
        loadAndShowOrganizationPath(organization.id);
    }

    @Override
    public void onEmployeeClick(Employee employee) {
        if (pick) {
            Intent intent = new Intent();
            intent.putExtra("employee", employee);
            getActivity().setResult(Activity.RESULT_OK, intent);
            getActivity().finish();
        } else {
            Intent intent = new Intent(getContext(), EmployeeInfoActivity.class);
            UserInfo userInfo = ChatManager.Instance().getUserInfo(employee.employeeId, false);
            intent.putExtra("userInfo", userInfo);
            startActivity(intent);
        }
    }

    private void loadAndShowOrganizationMemberList(int orgId) {
        this.orgId = orgId;
        organizationServiceViewModel.getOrganizationEx(orgId).observe(this, new Observer<OrganizationEx>() {
            @Override
            public void onChanged(OrganizationEx organizationEx) {
                getActivity().setTitle(organizationEx.organization.name);
                currentOrganizationEx = organizationEx;
                adapter.setOrganizationEx(organizationEx);
                adapter.setSearchMode(false);
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
                Log.d(TAG, "breadCrumbsView clear all tab");
                if (!organizations.isEmpty()) {
                    for (Organization org : organizations) {
                        Map<String, Object> param = new HashMap<>();
                        param.put("org", org);
                        breadCrumbsView.addTab(org.name, param);
                        Log.d(TAG, "breadCrumbsView add tab" + org.name);
                    }
                }
            }
        });
    }

    public void searchEmployees(String keyword) {
        this.currentSearchKeyword = keyword;
        breadCrumbsView.setVisibility(View.GONE);
        if (searchLiveData != null) {
            searchLiveData.removeObservers(this);
        }
        searchLiveData = organizationServiceViewModel.searchEmployee(orgId, keyword);
        searchLiveData.observe(this, new Observer<List<Employee>>() {
            @Override
            public void onChanged(List<Employee> employees) {
                if (!keyword.equals(currentSearchKeyword)) {
                    return;
                }
                adapter.setSearchResults(employees);
                adapter.setSearchMode(true);
                adapter.notifyDataSetChanged();
            }
        });
    }

    public void clearSearch() {
        breadCrumbsView.setVisibility(View.GONE);
        adapter.setSearchResults(null);
        adapter.setSearchMode(true);
        adapter.notifyDataSetChanged();
    }

    public void cancelSearch() {
        breadCrumbsView.setVisibility(View.VISIBLE);
        if (currentOrganizationEx != null) {
            adapter.setOrganizationEx(currentOrganizationEx);
            adapter.setSearchMode(false);
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onAdded(BreadCrumbsView.Tab tab) {
    }

    @Override
    public void onActivated(BreadCrumbsView.Tab tab) {
        Map<String, Object> param = tab.getValue();
        Organization org = (Organization) param.get("org");
        this.orgId = org.id;
        loadAndShowOrganizationMemberList(org.id);
    }

    @Override
    public void onRemoved(BreadCrumbsView.Tab tab) {

    }
}
