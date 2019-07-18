package cn.wildfire.chat.kit.group;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProviders;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.contact.pick.PickUserFragment;
import cn.wildfire.chat.kit.contact.pick.PickUserViewModel;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.GroupMember;

public class AddGroupMemberFragment extends PickUserFragment {
    private GroupInfo groupInfo;

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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        init();
        return view;
    }

    private void init() {
        PickUserViewModel pickUserViewModel = ViewModelProviders.of(getActivity()).get(PickUserViewModel.class);

        GroupViewModel groupViewModel = ViewModelProviders.of(this).get(GroupViewModel.class);

        List<GroupMember> groupMembers = groupViewModel.getGroupMembers(groupInfo.target, false);
        if (groupMembers == null || groupMembers.isEmpty()) {
            Toast.makeText(getActivity(), "get groupMembers error", Toast.LENGTH_SHORT).show();
            return;
        }
        List<String> memberIds = new ArrayList<>(groupMembers.size());
        for (GroupMember member : groupMembers) {
            memberIds.add(member.memberId);
        }
        pickUserViewModel.setUncheckableIds(memberIds);
    }

    @Override
    public void initHeaderViewHolders() {
        // do nothing
    }
}
