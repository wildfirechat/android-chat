/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.search.module;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.net.SimpleCallback;
import cn.wildfire.chat.kit.organization.EmployeeInfoActivity;
import cn.wildfire.chat.kit.organization.OrganizationServiceProvider;
import cn.wildfire.chat.kit.organization.model.Employee;
import cn.wildfire.chat.kit.organization.model.Organization;
import cn.wildfire.chat.kit.organization.viewholder.EmployeeViewHolder;
import cn.wildfire.chat.kit.search.SearchableModule;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

public class EmployeeSearchModule extends SearchableModule<Employee, EmployeeViewHolder> {

    @Override
    public EmployeeViewHolder onCreateViewHolder(Fragment fragment, @NonNull ViewGroup parent, int viewType) {
        View itemView;
        itemView = LayoutInflater.from(fragment.getActivity()).inflate(R.layout.organization_item_employee, parent, false);
        return new EmployeeViewHolder(itemView);
    }

    @Override
    public void onBind(Fragment fragment, EmployeeViewHolder holder, Employee employee) {
        holder.onBind(employee);
    }

    @Override
    public void onClick(Fragment fragment, EmployeeViewHolder holder, View view, Employee employee) {
        Intent intent = new Intent(fragment.getContext(), EmployeeInfoActivity.class);
        UserInfo userInfo = ChatManager.Instance().getUserInfo(employee.employeeId, false);
        intent.putExtra("userInfo", userInfo);
        fragment.startActivity(intent);
        fragment.getActivity().finish();
    }

    @Override
    public int getViewType(Employee employee) {
        return R.layout.organization_item_employee;
    }

    @Override
    public int priority() {
        return 100;
    }

    @Override
    public String category() {
        return WfcUIKit.getWfcUIKit().getApplication().getString(R.string.organization_directory);
    }

    @Override
    public List<Employee> search(String keyword) {
        List<Employee> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
        OrganizationServiceProvider organizationServiceProvider = WfcUIKit.getWfcUIKit().getOrganizationServiceProvider();
        if (!organizationServiceProvider.isServiceAvailable()) {
            return results;
        }

        List<Organization> orgs = new ArrayList<>();
        CountDownLatch finalLatch1 = latch;
        organizationServiceProvider.getRootOrganization(new SimpleCallback<List<Organization>>() {
            @Override
            public void onUiSuccess(List<Organization> organizations) {
                orgs.addAll(organizations);
                finalLatch1.countDown();
            }

            @Override
            public void onUiFailure(int code, String msg) {
                finalLatch1.countDown();
            }
        });
        try {
            latch.await();
        } catch (InterruptedException ignored) {
        }

        if (!orgs.isEmpty()) {
            latch = new CountDownLatch(orgs.size());
            CountDownLatch finalLatch = latch;
            for (Organization org : orgs) {
                organizationServiceProvider.searchEmployee(org.id, keyword, new SimpleCallback<List<Employee>>() {
                    @Override
                    public void onUiSuccess(List<Employee> employees) {
                        results.addAll(employees);
                        finalLatch.countDown();
                    }

                    @Override
                    public void onUiFailure(int code, String msg) {
                        finalLatch.countDown();
                    }
                });
            }
        }

        try {
            latch.await();
        } catch (InterruptedException ignored) {
        }

        return results;
    }
}
