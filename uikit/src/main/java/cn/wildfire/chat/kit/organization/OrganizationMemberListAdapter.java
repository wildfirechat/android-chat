/*
 * Copyright (c) 2023 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.organization;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.organization.model.Employee;
import cn.wildfire.chat.kit.organization.model.Organization;
import cn.wildfire.chat.kit.organization.model.OrganizationEx;
import cn.wildfire.chat.kit.organization.viewholder.EmployeeViewHolder;
import cn.wildfire.chat.kit.organization.viewholder.OrganizationEntityViewHolder;
import cn.wildfire.chat.kit.organization.viewholder.OrganizationViewHolder;

public class OrganizationMemberListAdapter extends RecyclerView.Adapter<OrganizationEntityViewHolder> {
    private Fragment fragment;
    private OrganizationEx organizationEx;
    private OnOrganizationMemberClickListener onOrganizationMemberClickListener;

    public OrganizationMemberListAdapter(Fragment fragment) {
        this.fragment = fragment;
    }

    public void setOrganizationEx(OrganizationEx organizationEx) {
        this.organizationEx = organizationEx;
    }

    public void setOnOrganizationMemberClickListener(OnOrganizationMemberClickListener onOrganizationMemberClickListener) {
        this.onOrganizationMemberClickListener = onOrganizationMemberClickListener;
    }

    @NonNull
    @Override
    public OrganizationEntityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        OrganizationEntityViewHolder holder;
        View view;
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == R.layout.organization_item_organization) {
            view = inflater.inflate(R.layout.organization_item_organization, parent, false);
            holder = new OrganizationViewHolder(view);
        } else {
            view = inflater.inflate(R.layout.organization_item_employee, parent, false);
            holder = new EmployeeViewHolder(view);
        }
        view.setOnClickListener(v -> {
            if (onOrganizationMemberClickListener != null) {
                int position = holder.getAdapterPosition();
                if (position < subOrganizationCount()) {
                    Organization organization = organizationEx.subOrganizations.get(position);
                    onOrganizationMemberClickListener.onOrganizationClick(organization);
                } else {
                    Employee employee = organizationEx.employees.get(position - subOrganizationCount());
                    onOrganizationMemberClickListener.onEmployeeClick(employee);
                }
            }
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull OrganizationEntityViewHolder holder, int position) {
        Object value;
        if (position < subOrganizationCount()) {
            value = organizationEx.subOrganizations.get(position);
        } else {
            value = organizationEx.employees.get(position - subOrganizationCount());
        }
        holder.onBind(value);
    }

    @Override
    public int getItemViewType(int position) {
        if (position < subOrganizationCount()) {
            return R.layout.organization_item_organization;
        } else {
            return R.layout.organization_item_employee;
        }
    }

    @Override
    public int getItemCount() {
        if (organizationEx == null) {
            return 0;
        }
        return subOrganizationCount() + employeeCount();
    }

    private int subOrganizationCount() {
        return organizationEx.subOrganizations == null ? 0 : organizationEx.subOrganizations.size();
    }

    private int employeeCount() {
        return organizationEx.employees == null ? 0 : organizationEx.employees.size();
    }

    interface OnOrganizationMemberClickListener {
        void onOrganizationClick(Organization organization);

        void onEmployeeClick(Employee employee);
    }
}
