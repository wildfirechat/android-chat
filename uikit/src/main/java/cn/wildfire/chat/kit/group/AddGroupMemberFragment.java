/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.group;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;


import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.contact.OrganizationServiceViewModel;
import cn.wildfire.chat.kit.contact.model.OrganizationValue;
import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfire.chat.kit.contact.pick.PickContactFragment;
import cn.wildfire.chat.kit.contact.pick.PickUserViewModel;
import cn.wildfire.chat.kit.contact.viewholder.header.DepartViewHolder;
import cn.wildfire.chat.kit.contact.viewholder.header.HeaderViewHolder;
import cn.wildfire.chat.kit.contact.viewholder.header.OrganizationViewHolder;
import cn.wildfire.chat.kit.organization.model.Employee;
import cn.wildfire.chat.kit.organization.model.Organization;
import cn.wildfire.chat.kit.organization.pick.PickOrganizationMemberActivity;
import cn.wildfirechat.model.GroupInfo;

public class AddGroupMemberFragment extends PickContactFragment {
    private GroupInfo groupInfo;
    private ArrayList<String> disabledEmployeeIds;
    private static final int REQUEST_CODE_PICK_ORGANIZATION_MEMBER = 101;

    public static AddGroupMemberFragment newInstance(GroupInfo groupInfo) {
        Bundle args = new Bundle();
        args.putParcelable("groupInfo", groupInfo);
        AddGroupMemberFragment fragment = new AddGroupMemberFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        groupInfo = getArguments().getParcelable("groupInfo");
    }


    @Override
    protected void setupPickFromUsers() {
        super.setupPickFromUsers();
        PickUserViewModel pickUserViewModel = new ViewModelProvider(getActivity()).get(PickUserViewModel.class);

        GroupViewModel groupViewModel = WfcUIKit.getAppScopeViewModel(GroupViewModel.class);

        groupViewModel.getGroupMemberUIUserInfosLiveData(groupInfo.target, false).observe(this, uiUserInfos -> {
            if (uiUserInfos == null || uiUserInfos.isEmpty()) {
                return;
            }
            List<String> memberIds = new ArrayList<>(uiUserInfos.size());
            for (UIUserInfo uiUserInfo : uiUserInfos) {
                memberIds.add(uiUserInfo.getUserInfo().uid);
            }
            disabledEmployeeIds = new ArrayList<>(memberIds);
            pickUserViewModel.setUncheckableIds(memberIds);
            pickUserViewModel.setInitialCheckedIds(memberIds);
            userListAdapter.notifyDataSetChanged();
        });
    }

    @Override
    public void initHeaderViewHolders() {
        if (!TextUtils.isEmpty(Config.ORG_SERVER_ADDRESS)) {
            OrganizationServiceViewModel organizationServiceViewModel = new ViewModelProvider(this).get(OrganizationServiceViewModel.class);
            organizationServiceViewModel.rootOrganizationLiveData().observe(this, organizations -> {
                if (!organizations.isEmpty()) {
                    for (Organization org : organizations) {
                        OrganizationValue value = new OrganizationValue();
                        value.setValue(org);
                        addHeaderViewHolder(OrganizationViewHolder.class, R.layout.contact_header_organization, value);
                    }
                }
            });
            organizationServiceViewModel.myOrganizationLiveData().observe(this, organizations -> {
                if (!organizations.isEmpty()) {
                    for (Organization org : organizations) {
                        OrganizationValue value = new OrganizationValue();
                        value.setValue(org);
                        addHeaderViewHolder(DepartViewHolder.class, R.layout.contact_header_department, value);
                    }
                }
            });
        }
    }

    @Override
    public void onHeaderClick(HeaderViewHolder holder) {
        if (holder instanceof OrganizationViewHolder) {
            Organization organization = ((OrganizationViewHolder) holder).getOrganization();
            Intent intent = new Intent(getActivity(), PickOrganizationMemberActivity.class);
            intent.putExtra("organizationId", organization.id);
            if (disabledEmployeeIds != null && !disabledEmployeeIds.isEmpty()) {
                intent.putStringArrayListExtra("disabledEmployeeIds", disabledEmployeeIds);
            }
            startActivityForResult(intent, REQUEST_CODE_PICK_ORGANIZATION_MEMBER);
        } else if (holder instanceof DepartViewHolder) {
            Organization organization = ((DepartViewHolder) holder).getOrganization();
            Intent intent = new Intent(getActivity(), PickOrganizationMemberActivity.class);
            intent.putExtra("organizationId", organization.id);
            if (disabledEmployeeIds != null && !disabledEmployeeIds.isEmpty()) {
                intent.putStringArrayListExtra("disabledEmployeeIds", disabledEmployeeIds);
            }
            startActivityForResult(intent, REQUEST_CODE_PICK_ORGANIZATION_MEMBER);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_PICK_ORGANIZATION_MEMBER && data != null) {
            ArrayList<Employee> employees = data.getParcelableArrayListExtra("employees");
            if (employees != null) {
                for (Employee employee : employees) {
                    checkOrganizationEmployee(employee);
                }
                List<UIUserInfo> currentUsers = userListAdapter.getUsers();
                if (currentUsers != null) {
                    userListAdapter.notifyDataSetChanged();
                }
            }
            if (getActivity() instanceof AddGroupMemberActivity) {
                ((AddGroupMemberActivity) getActivity()).updateConfirmStatus();
            }
            if (Config.ENABLE_SELECT_ORGANIZATION) {
                ArrayList<Organization> organizations = data.getParcelableArrayListExtra("organizations");
                if (organizations != null && !organizations.isEmpty()) {
                    OrganizationServiceViewModel orgViewModel = new ViewModelProvider(this).get(OrganizationServiceViewModel.class);
                    List<Integer> orgIds = new ArrayList<>();
                    for (Organization org : organizations) {
                        orgIds.add(org.id);
                    }
                    orgViewModel.getOrganizationEmployees(orgIds, true).observe(this, orgEmployees -> {
                        if (orgEmployees != null && isAdded()) {
                            for (Employee e : orgEmployees) {
                                checkOrganizationEmployee(e);
                            }
                            List<UIUserInfo> currentUsers = userListAdapter.getUsers();
                            if (currentUsers != null) {
                                userListAdapter.notifyDataSetChanged();
                            }
                            if (getActivity() instanceof AddGroupMemberActivity) {
                                ((AddGroupMemberActivity) getActivity()).updateConfirmStatus();
                            }
                        }
                    });
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public List<UIUserInfo> getPickedUsers() {
        if (pickedUserAdapter != null && pickedUserAdapter.getPickedUsers() != null) {
            return new ArrayList<>(pickedUserAdapter.getPickedUsers());
        }
        return new ArrayList<>();
    }

    private void checkOrganizationEmployee(Employee employee) {
        boolean alreadyPicked = false;
        if (pickedUserAdapter.getPickedUsers() != null) {
            for (UIUserInfo picked : pickedUserAdapter.getPickedUsers()) {
                if (picked.getUserInfo().uid.equals(employee.employeeId)) {
                    alreadyPicked = true;
                    break;
                }
            }
        }
        if (!alreadyPicked) {
            UIUserInfo uiUserInfo = new UIUserInfo(employee.toUserInfo());
            List<UIUserInfo> currentUsers = userListAdapter.getUsers();
            if (currentUsers != null) {
                boolean exists = false;
                for (UIUserInfo u : currentUsers) {
                    if (u.getUserInfo().uid.equals(employee.employeeId)) {
                        u.setChecked(true);
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    uiUserInfo.setChecked(true);
                    currentUsers.add(uiUserInfo);
                }
            }
            pickUserViewModel.checkUser(uiUserInfo, true);
        }
    }
}
