package cn.wildfire.chat.kit.group;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProviders;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.contact.UserListAdapter;
import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfire.chat.kit.contact.pick.CheckableUserListAdapter;
import cn.wildfire.chat.kit.contact.pick.PickUserFragment;
import cn.wildfire.chat.kit.contact.pick.PickUserViewModel;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.GroupMember;

public class PickGroupMemberFragment extends PickUserFragment {
    private GroupInfo groupInfo;

    public static PickGroupMemberFragment newInstance(GroupInfo groupInfo) {
        Bundle args = new Bundle();
        args.putParcelable("groupInfo", groupInfo);
        PickGroupMemberFragment fragment = new PickGroupMemberFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        groupInfo = getArguments().getParcelable("groupInfo");
    }

    @Override
    public void initHeaderViewHolders() {
        // do nothing
    }

    @Override
    public UserListAdapter onCreateUserListAdapter() {
        CheckableUserListAdapter checkableContactAdapter = new CheckableUserListAdapter(this);
        PickUserViewModel pickUserViewModel = ViewModelProviders.of(getActivity()).get(PickUserViewModel.class);

        GroupViewModel groupViewModel = ViewModelProviders.of(getActivity()).get(GroupViewModel.class);
        List<GroupMember> members = groupViewModel.getGroupMembers(groupInfo.target, false);
        List<String> memberIds = new ArrayList<>(members.size());
        for (GroupMember member : members) {
            memberIds.add(member.memberId);
        }
        List<UIUserInfo> users = userInfoToUIUserInfo(UserViewModel.getUsers(memberIds, groupInfo.target));
        pickUserViewModel.setUsers(users);
        checkableContactAdapter.setUsers(users);

        return checkableContactAdapter;
    }
}
