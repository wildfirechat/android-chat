package cn.wildfire.chat.kit.group.manage;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProviders;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.wildfire.chat.kit.contact.BaseUserListFragment;
import cn.wildfire.chat.kit.contact.UserListAdapter;
import cn.wildfire.chat.kit.contact.model.FooterValue;
import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfire.chat.kit.group.GroupViewModel;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.GroupMember;
import cn.wildfirechat.remote.ChatManager;

public class GroupManagerListFragment extends BaseUserListFragment {
    private GroupViewModel groupViewModel;
    private GroupInfo groupInfo;
    private GroupMember groupMember;
    private List<GroupMember> managerMembers = new ArrayList<>();

    public static GroupManagerListFragment newInstance(GroupInfo groupInfo) {
        Bundle args = new Bundle();
        args.putParcelable("groupInfo", groupInfo);
        GroupManagerListFragment fragment = new GroupManagerListFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        groupInfo = getArguments().getParcelable("groupInfo");
        showQuickIndexBar(false);

        groupViewModel = ViewModelProviders.of(getActivity()).get(GroupViewModel.class);
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
        return new UserListAdapter(this);
    }

    @Override
    public void initFooterViewHolders() {
        if (groupMember.type == GroupMember.GroupMemberType.Owner) {
            addFooterViewHolder(AddGroupManagerViewHolder.class, new FooterValue());
        }
    }

    @Override
    public void onUserClick(UIUserInfo userInfo) {
        UserViewModel userViewModel = ViewModelProviders.of(this).get(UserViewModel.class);
        GroupMember me = groupViewModel.getGroupMember(groupInfo.target, userViewModel.getUserId());
        if (me == null || me.type != GroupMember.GroupMemberType.Owner) {
            return;
        }

        GroupMember groupMember = groupViewModel.getGroupMember(groupInfo.target, userInfo.getUserInfo().uid);
        if (groupMember.type == GroupMember.GroupMemberType.Manager) {
            new MaterialDialog.Builder(getActivity())
                    .items(Collections.singleton("移除群管理"))
                    .itemsCallback((dialog, itemView, position, text) -> {
                        groupViewModel.setGroupManager(groupInfo.target, false, Collections.singletonList(userInfo.getUserInfo().uid), null, Collections.singletonList(0));
                    })
                    .cancelable(true)
                    .build()
                    .show();
        }
    }

    @Override
    public void onFooterClick(int index) {
        Intent intent = new Intent(getActivity(), AddGroupManagerActivity.class);
        intent.putExtra("groupInfo", groupInfo);
        ArrayList<String> memberIds = new ArrayList<>(managerMembers.size());
        for (GroupMember member : managerMembers) {
            memberIds.add(member.memberId);
        }
        intent.putStringArrayListExtra("unCheckableMemberIds", memberIds);
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
        groupViewModel.getGroupManagerUIUserInfosLiveData(groupInfo.target, refresh).observe(this, uiUserInfos -> {
            showContent();
            userListAdapter.setUsers(uiUserInfos);
            userListAdapter.notifyDataSetChanged();
        });
    }
}
