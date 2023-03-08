/*
 * Copyright (c) 2023 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.organization.pick;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.organization.model.Employee;
import cn.wildfire.chat.kit.organization.model.Organization;
import cn.wildfire.chat.kit.organization.model.OrganizationEx;
import cn.wildfire.chat.kit.organization.viewholder.EmployeeViewHolder;
import cn.wildfire.chat.kit.organization.viewholder.OrganizationEntityViewHolder;
import cn.wildfire.chat.kit.organization.viewholder.OrganizationViewHolder;

public class CheckableOrganizationMemberListAdapter extends RecyclerView.Adapter<OrganizationEntityViewHolder> {
    private Fragment fragment;
    private OrganizationEx organizationEx;
    private OnOrganizationMemberClickListener onOrganizationMemberClickListener;

    private Map<String, Employee> checkedMembers;
    private Map<Integer, Organization> checkedOrganizations;

    public CheckableOrganizationMemberListAdapter(Fragment fragment) {
        this.fragment = fragment;
        this.checkedMembers = new HashMap<>();
        this.checkedOrganizations = new HashMap<>();
    }

    public void setOrganizationEx(OrganizationEx organizationEx) {
        this.organizationEx = organizationEx;
    }

    public void setOnOrganizationMemberClickListener(OnOrganizationMemberClickListener onOrganizationMemberClickListener) {
        this.onOrganizationMemberClickListener = onOrganizationMemberClickListener;
    }


    public Collection<Employee> getCheckedMembers() {
        return checkedMembers.values();
    }

    public Collection<Organization> getCheckedOrganizations() {
        return checkedOrganizations.values();
    }

    @NonNull
    @Override
    public OrganizationEntityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        OrganizationEntityViewHolder holder;
        View view;
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == R.layout.organization_item_checkable_organization) {
            view = inflater.inflate(R.layout.organization_item_checkable_organization, parent, false);
            holder = new OrganizationViewHolder(view);

            TextView childOrganizationTextView = view.findViewById(R.id.childOrganizationTextView);
            childOrganizationTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (onOrganizationMemberClickListener != null) {
                        int position = holder.getAdapterPosition();
                        onOrganizationMemberClickListener.onOrganizationClick(organizationEx.subOrganizations.get(position));
                    }
                }
            });
        } else {
            view = inflater.inflate(R.layout.organization_item_checkable_employee, parent, false);
            holder = new EmployeeViewHolder(view);
        }
        CheckBox checkBox = view.findViewById(R.id.checkbox);
        view.setOnClickListener(v -> {
            boolean toCheck = !checkBox.isChecked();
            checkBox.setChecked(toCheck);
            int position = holder.getAdapterPosition();
            if (position < subOrganizationCount()) {
                Organization organization = organizationEx.subOrganizations.get(position);
                if (toCheck) {
                    checkedOrganizations.put(organization.id, organization);
                } else {
                    checkedOrganizations.remove(organization.id);
                }
                if (onOrganizationMemberClickListener != null) {
                    onOrganizationMemberClickListener.onOrganizationCheck(organization, toCheck);
                }

            } else {
                Employee employee = organizationEx.employees.get(position - subOrganizationCount());

                if (toCheck) {
                    checkedMembers.put(employee.employeeId, employee);
                } else {
                    checkedMembers.remove(employee.employeeId);
                }
                if (onOrganizationMemberClickListener != null) {
                    onOrganizationMemberClickListener.onEmployeeCheck(employee, toCheck);
                }
            }
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull OrganizationEntityViewHolder holder, int position) {
        Object value;
        CheckBox checkBox = holder.itemView.findViewById(R.id.checkbox);
        if (position < subOrganizationCount()) {
            value = organizationEx.subOrganizations.get(position);
            Organization org = (Organization) value;
            checkBox.setChecked(checkedOrganizations.containsKey(org.id));
        } else {
            value = organizationEx.employees.get(position - subOrganizationCount());
            Employee employee = (Employee) value;
            checkBox.setChecked(checkedMembers.containsKey(employee.employeeId));
        }

        holder.onBind(value);
    }

    @Override
    public int getItemViewType(int position) {
        if (position < subOrganizationCount()) {
            return R.layout.organization_item_checkable_organization;
        } else {
            return R.layout.organization_item_checkable_employee;
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

        void onOrganizationCheck(Organization organization, boolean checked);

        void onEmployeeCheck(Employee employee, boolean checked);
    }
}
