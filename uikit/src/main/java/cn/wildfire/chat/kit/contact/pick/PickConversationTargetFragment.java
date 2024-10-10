/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.contact.pick;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.contact.ContactViewModel;
import cn.wildfire.chat.kit.contact.OrganizationServiceViewModel;
import cn.wildfire.chat.kit.contact.model.GroupValue;
import cn.wildfire.chat.kit.contact.model.OrganizationValue;
import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfire.chat.kit.contact.pick.viewholder.PickGroupViewHolder;
import cn.wildfire.chat.kit.contact.viewholder.header.DepartViewHolder;
import cn.wildfire.chat.kit.contact.viewholder.header.HeaderViewHolder;
import cn.wildfire.chat.kit.contact.viewholder.header.OrganizationViewHolder;
import cn.wildfire.chat.kit.group.GroupListActivity;
import cn.wildfire.chat.kit.organization.model.Employee;
import cn.wildfire.chat.kit.organization.model.Organization;
import cn.wildfire.chat.kit.organization.pick.PickOrganizationMemberActivity;
import cn.wildfirechat.model.GroupInfo;

public class PickConversationTargetFragment extends PickUserFragment {
    private final static int REQUEST_CODE_PICK_GROUP = 100;
    private final static int REQUEST_CODE_PICK_ORGANIZATION_MEMBER = 101;
    private boolean pickGroupForResult;
    private boolean multiGroupMode;
    private OnGroupPickListener groupPickListener;

    private OrganizationServiceViewModel organizationServiceViewModel;

    /**
     * @param pickGroupForResult 为true时，点击group item不直接启动群会话；否则直接启动群会话
     * @param multiGroupMode     {@code pickGroupForResult} 为{@code true}时，才生效，表示群多选
     * @return
     */
    public static PickConversationTargetFragment newInstance(boolean pickGroupForResult, boolean multiGroupMode) {
        PickConversationTargetFragment fragment = new PickConversationTargetFragment();
        Bundle args = new Bundle();
        args.putBoolean("pickGroupForResult", pickGroupForResult);
        args.putBoolean("multiGroupMode", multiGroupMode);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            pickGroupForResult = args.getBoolean("pickGroupForResult", false);
            multiGroupMode = args.getBoolean("multiGroupMode", false);
        }
        organizationServiceViewModel = new ViewModelProvider(this).get(OrganizationServiceViewModel.class);
    }

    @Override
    protected void setupPickFromUsers() {
        ContactViewModel contactViewModel = ViewModelProviders.of(getActivity()).get(ContactViewModel.class);
        contactViewModel.contactListLiveData().observe(this, userInfos -> {
            showContent();
            pickUserViewModel.setUsers(userInfos);
            userListAdapter.setUsers(userInfos);
        });
    }

    public void setOnGroupPickListener(OnGroupPickListener listener) {
        this.groupPickListener = listener;
    }

    @Override
    public void initHeaderViewHolders() {
        // 选择一个群
        addHeaderViewHolder(PickGroupViewHolder.class, R.layout.contact_header_group, new GroupValue());
        // 选取组织成员
        if (!TextUtils.isEmpty(Config.ORG_SERVER_ADDRESS)) {
            organizationServiceViewModel.rootOrganizationLiveData().observe(this, new Observer<List<Organization>>() {
                @Override
                public void onChanged(List<Organization> organizations) {
                    if (!organizations.isEmpty()) {
                        for (Organization org : organizations) {
                            OrganizationValue value = new OrganizationValue();
                            value.setValue(org);
                            addHeaderViewHolder(OrganizationViewHolder.class, R.layout.contact_header_organization, value);
                        }
                    }
                }
            });
            organizationServiceViewModel.myOrganizationLiveData().observe(this, new Observer<List<Organization>>() {
                @Override
                public void onChanged(List<Organization> organizations) {
                    if (!organizations.isEmpty()) {
                        for (Organization org : organizations) {
                            OrganizationValue value = new OrganizationValue();
                            value.setValue(org);
                            addHeaderViewHolder(DepartViewHolder.class, R.layout.contact_header_department, value);
                        }
                    }

                }
            });
        }
    }

    @Override
    public void onHeaderClick(HeaderViewHolder holder) {
        // 选择一个群
        if (holder instanceof PickGroupViewHolder) {
            Intent intent = new Intent(getActivity(), GroupListActivity.class);
            if (pickGroupForResult) {
                intent.putExtra(GroupListActivity.INTENT_FOR_RESULT, true);
            }
            startActivityForResult(intent, REQUEST_CODE_PICK_GROUP);
        } else if (holder instanceof OrganizationViewHolder) {
            Organization organization = ((OrganizationViewHolder) holder).getOrganization();
            Intent intent = new Intent(getActivity(), PickOrganizationMemberActivity.class);
            intent.putExtra("organizationId", organization.id);
            startActivityForResult(intent, REQUEST_CODE_PICK_ORGANIZATION_MEMBER);
        } else if (holder instanceof DepartViewHolder) {
            Organization organization = ((DepartViewHolder) holder).getOrganization();
            Intent intent = new Intent(getActivity(), PickOrganizationMemberActivity.class);
            intent.putExtra("organizationId", organization.id);
            startActivityForResult(intent, REQUEST_CODE_PICK_ORGANIZATION_MEMBER);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        if (requestCode == REQUEST_CODE_PICK_GROUP) {
            // TODO 多选
            ArrayList<GroupInfo> groupInfos = data.getParcelableArrayListExtra("groupInfos");
            if (groupPickListener != null) {
                groupPickListener.onGroupPicked(groupInfos);
            }
        } else if (requestCode == REQUEST_CODE_PICK_ORGANIZATION_MEMBER) {
            ArrayList<Organization> organizations = data.getParcelableArrayListExtra("organizations");
            ArrayList<Employee> employees = data.getParcelableArrayListExtra("employees");
            if (employees != null) {
                for (Employee employee : employees) {
                    UIUserInfo uiUserInfo = new UIUserInfo(employee.toUserInfo());
                    pickUserViewModel.checkUser(uiUserInfo, true);
                }
            }
            if (organizations != null) {
                pickedUserAdapter.setOrganizations(organizations);

                // 没有的话，搜索提示框布局会错乱
                handleHintView(false);
                handleEditText();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public List<Organization> getCheckedOrganizations() {
        return pickedUserAdapter.organizations;
    }

    public List<UIUserInfo> getCheckedUserInfos() {
        return pickedUserAdapter.users;
    }

    public interface OnGroupPickListener {
        void onGroupPicked(List<GroupInfo> groupInfos);
    }
}
