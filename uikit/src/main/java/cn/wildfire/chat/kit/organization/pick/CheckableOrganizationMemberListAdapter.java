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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.organization.model.Employee;
import cn.wildfire.chat.kit.organization.model.Organization;
import cn.wildfire.chat.kit.organization.model.OrganizationEx;
import cn.wildfire.chat.kit.organization.viewholder.EmployeeViewHolder;
import cn.wildfire.chat.kit.organization.viewholder.OrganizationEntityViewHolder;
import cn.wildfire.chat.kit.organization.viewholder.OrganizationViewHolder;

public class CheckableOrganizationMemberListAdapter extends RecyclerView.Adapter<OrganizationEntityViewHolder> {
    private OrganizationEx organizationEx;
    private OnOrganizationMemberClickListener onOrganizationMemberClickListener;

    private final Map<String, Employee> checkedMembers;
    private final Map<Integer, Organization> checkedOrganizations;
    private Set<String> uncheckableEmployeeIds;
    private int maxPickCount;

    public CheckableOrganizationMemberListAdapter(Fragment fragment) {
        this.checkedMembers = new HashMap<>();
        this.checkedOrganizations = new HashMap<>();
        this.uncheckableEmployeeIds = new HashSet<>();
        this.maxPickCount = Integer.MAX_VALUE;
    }

    public void setInitialCheckedEmployees(List<Employee> employees) {
        if (employees != null) {
            for (Employee e : employees) {
                checkedMembers.put(e.employeeId, e);
            }
            notifyDataSetChanged();
        }
    }

    public void setInitialCheckedOrganizations(List<Organization> organizations) {
        if (organizations != null) {
            for (Organization org : organizations) {
                checkedOrganizations.put(org.id, org);
            }
            notifyDataSetChanged();
        }
    }

    public void setUncheckableEmployeeIds(Set<String> uncheckableEmployeeIds) {
        this.uncheckableEmployeeIds = uncheckableEmployeeIds != null ? uncheckableEmployeeIds : new HashSet<>();
        notifyDataSetChanged();
    }

    public void setMaxPickCount(int maxPickCount) {
        if (maxPickCount <= 0) {
            this.maxPickCount = Integer.MAX_VALUE;
        } else {
            this.maxPickCount = maxPickCount;
        }
    }

    public void setDisabledEmployeeIds(Set<String> disabledEmployeeIds) {
        setUncheckableEmployeeIds(disabledEmployeeIds);
    }

    private int getPickedCount() {
        int pickedCount = checkedMembers.size();
        for (Organization organization : checkedOrganizations.values()) {
            pickedCount += Math.max(organization.memberCount, 0);
        }
        return pickedCount;
    }

    private boolean canPickOrganization(Organization organization) {
        if (maxPickCount == Integer.MAX_VALUE) {
            return true;
        }
        return getPickedCount() + Math.max(organization.memberCount, 0) <= maxPickCount;
    }

    private boolean canPickEmployee() {
        if (maxPickCount == Integer.MAX_VALUE) {
            return true;
        }
        return getPickedCount() + 1 <= maxPickCount;
    }

    private void notifyPickLimitExceeded() {
        if (onOrganizationMemberClickListener != null) {
            onOrganizationMemberClickListener.onPickLimitExceeded();
        }
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
            if (!Config.ENABLE_SELECT_ORGANIZATION) {
                CheckBox orgCheckBox = view.findViewById(R.id.checkbox);
                orgCheckBox.setVisibility(View.GONE);
            }

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
            int position = holder.getAdapterPosition();
            if (organizationEx == null || position == RecyclerView.NO_POSITION) {
                return;
            }
            if (position < subOrganizationCount()) {
                if (!Config.ENABLE_SELECT_ORGANIZATION) {
                    return;
                }
            } else {
                Employee employee = organizationEx.employees.get(position - subOrganizationCount());
                if (uncheckableEmployeeIds.contains(employee.employeeId)) {
                    return;
                }
            }
            boolean toCheck = !checkBox.isChecked();
            if (position < subOrganizationCount()) {
                Organization organization = organizationEx.subOrganizations.get(position);
                if (toCheck && !canPickOrganization(organization)) {
                    notifyPickLimitExceeded();
                    return;
                }
                checkBox.setChecked(toCheck);
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
                if (toCheck && !canPickEmployee()) {
                    notifyPickLimitExceeded();
                    return;
                }
                checkBox.setChecked(toCheck);

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
            checkBox.setEnabled(true);
            holder.itemView.setAlpha(1.0f);
        } else {
            value = organizationEx.employees.get(position - subOrganizationCount());
            Employee employee = (Employee) value;
            boolean isUncheckable = uncheckableEmployeeIds.contains(employee.employeeId);
            checkBox.setChecked(checkedMembers.containsKey(employee.employeeId));
            checkBox.setEnabled(!isUncheckable);
            holder.itemView.setAlpha(isUncheckable ? 0.5f : 1.0f);
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

        void onPickLimitExceeded();
    }
}
