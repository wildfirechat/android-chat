/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.group.manage;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;


import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;
import java.util.Collections;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.contact.BaseUserListFragment;
import cn.wildfire.chat.kit.contact.UserListAdapter;
import cn.wildfire.chat.kit.contact.model.HeaderValue;
import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfire.chat.kit.contact.viewholder.header.HeaderViewHolder;
import cn.wildfire.chat.kit.group.BasePickGroupMemberActivity;
import cn.wildfire.chat.kit.group.GroupViewModel;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.GroupMember;
import cn.wildfirechat.remote.ChatManager;

public class GroupMemberMuteOrAllowListFragment extends BaseUserListFragment implements UserListAdapter.OnUserLongClickListener {
    private GroupViewModel groupViewModel;
    private GroupInfo groupInfo;
    private GroupMember groupMember;
    private boolean groupMuted = false;

    public static GroupMemberMuteOrAllowListFragment newInstance(GroupInfo groupInfo, boolean mute) {
        Bundle args = new Bundle();
        args.putParcelable("groupInfo", groupInfo);
        GroupMemberMuteOrAllowListFragment fragment = new GroupMemberMuteOrAllowListFragment();
        fragment.groupMuted = mute;
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        groupInfo = getArguments().getParcelable("groupInfo");
        showQuickIndexBar(false);

        groupViewModel = WfcUIKit.getAppScopeViewModel(GroupViewModel.class);;
        groupMember = groupViewModel.getGroupMember(groupInfo.target, ChatManager.Instance().getUserId());
        observerGroupMemberUpdate();
    }

    @Override
    protected void afterViews(View view) {
        super.afterViews(view);
        loadAndShowGroupMembers(true);
    }

    @Override
    public UserListAdapter onCreateUserListAdapter() {
        UserListAdapter adapter = new UserListAdapter(this);
        adapter.setOnUserLongClickListener(this);
        return adapter;
    }

    @Override
    public void initHeaderViewHolders() {
        if (groupMember.type == GroupMember.GroupMemberType.Owner || groupMember.type == GroupMember.GroupMemberType.Manager) {
            addHeaderViewHolder(MuteGroupMemberViewHolder.class, R.layout.group_manage_item_mute_member, new HeaderValue(groupInfo, groupMuted));
        }
    }

    @Override
    public void onUserClick(UIUserInfo userInfo) {
        UserViewModel userViewModel = WfcUIKit.getAppScopeViewModel(UserViewModel.class);
        GroupMember me = groupViewModel.getGroupMember(groupInfo.target, userViewModel.getUserId());
        if (me == null || me.type != GroupMember.GroupMemberType.Owner) {
            return;
        }

        new MaterialDialog.Builder(getActivity())
            .items(Collections.singleton(groupMuted ? getString(R.string.remove_whitelist) : getString(R.string.unmute_member)))
            .itemsCallback((dialog, itemView, position, text) -> {
                if (groupMuted) {
                    groupViewModel.allowGroupMember(groupInfo.target, false, Collections.singletonList(userInfo.getUserInfo().uid), null, Collections.singletonList(0));
                } else {
                    groupViewModel.muteGroupMember(groupInfo.target, false, Collections.singletonList(userInfo.getUserInfo().uid), null, Collections.singletonList(0));
                }
            })
            .cancelable(true)
            .build()
            .show();
    }

    @Override
    public void onUserLongClick(UIUserInfo userInfo) {
        new MaterialDialog.Builder(getActivity())
            .content(!groupMuted ? getString(R.string.unmute_member) : getString(R.string.remove_whitelist))
            .positiveText(getString(R.string.confirm))
            .negativeText(getString(R.string.cancel))
            .onPositive((dialog, which) -> {
                if (!groupMuted) {
                    groupViewModel.muteGroupMember(groupInfo.target, false, Collections.singletonList(userInfo.getUserInfo().uid), null, Collections.singletonList(0))
                        .observe(this, booleanOperateResult -> {
                            if (!booleanOperateResult.isSuccess()) {
                                Toast.makeText(getActivity(), getString(R.string.operation_failed), Toast.LENGTH_SHORT).show();
                            } else {
                                this.loadAndShowGroupMembers(true);
                            }
                        });
                } else {
                    groupViewModel.allowGroupMember(groupInfo.target, false, Collections.singletonList(userInfo.getUserInfo().uid), null, Collections.singletonList(0))
                        .observe(this, booleanOperateResult -> {
                            if (!booleanOperateResult.isSuccess()) {
                                Toast.makeText(getActivity(), getString(R.string.operation_failed), Toast.LENGTH_SHORT).show();
                            } else {
                                this.loadAndShowGroupMembers(true);
                            }
                        });
                }
            })
            .show();
    }

    @Override
    public void onHeaderClick(HeaderViewHolder holder) {
        Intent intent = new Intent(getActivity(), MuteGroupMemberActivity.class);
        intent.putExtra(BasePickGroupMemberActivity.GROUP_INFO, groupInfo);
        intent.putExtra("groupMuted", groupMuted);

        ArrayList<String> uncheckableMemberIds = new ArrayList<>();
        uncheckableMemberIds.add(groupInfo.owner);
        uncheckableMemberIds.addAll(groupViewModel.getGroupManagerIds(groupInfo.target));
        uncheckableMemberIds.addAll(groupViewModel.getMutedOrAllowedMemberIds(groupInfo.target, groupMuted));
        intent.putExtra(BasePickGroupMemberActivity.UNCHECKABLE_MEMBER_IDS, uncheckableMemberIds);
        startActivity(intent);
    }

    private void observerGroupMemberUpdate() {
        groupViewModel.groupMembersUpdateLiveData().observe(this, groupMembers -> {
            if (groupInfo.target.equals(groupMembers.get(0).groupId)) {
                loadAndShowGroupMembers(false);
            }
        });
    }

    private void loadAndShowGroupMembers(boolean refresh) {
        groupViewModel.getMutedOrAllowedMemberUIUserInfosLiveData(groupInfo.target, groupMuted, refresh).observe(this, uiUserInfos -> {
            showContent();
            userListAdapter.setUsers(uiUserInfos);
            userListAdapter.notifyDataSetChanged();
        });
    }
}
