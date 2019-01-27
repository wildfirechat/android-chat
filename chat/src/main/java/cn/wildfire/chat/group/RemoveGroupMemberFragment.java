package cn.wildfire.chat.group;

import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProviders;
import cn.wildfire.chat.contact.ContactAdapter;
import cn.wildfire.chat.contact.ContactViewModel;
import cn.wildfire.chat.contact.model.UIUserInfo;
import cn.wildfire.chat.contact.pick.CheckableContactAdapter;
import cn.wildfire.chat.contact.pick.PickContactFragment;
import cn.wildfire.chat.contact.pick.PickContactViewModel;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.GroupMember;

public class RemoveGroupMemberFragment extends PickContactFragment {
    private GroupInfo groupInfo;

    public static RemoveGroupMemberFragment newInstance(GroupInfo groupInfo) {
        Bundle args = new Bundle();
        args.putParcelable("groupInfo", groupInfo);
        RemoveGroupMemberFragment fragment = new RemoveGroupMemberFragment();
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
    public ContactAdapter onCreateContactAdapter() {
        CheckableContactAdapter checkableContactAdapter = new CheckableContactAdapter(this);
        PickContactViewModel pickContactViewModel = ViewModelProviders.of(getActivity()).get(PickContactViewModel.class);

        GroupViewModel groupViewModel = ViewModelProviders.of(getActivity()).get(GroupViewModel.class);
        List<GroupMember> members = groupViewModel.getGroupMembers(groupInfo.target, false);
        List<String> memberIds = new ArrayList<>(members.size());
        for (GroupMember member : members) {
            memberIds.add(member.memberId);
        }
        ContactViewModel contactViewModel = ViewModelProviders.of(getActivity()).get(ContactViewModel.class);
        List<UIUserInfo> contacts = userInfoToUIUserInfo(contactViewModel.getContacts(memberIds));
        pickContactViewModel.setContacts(contacts);
        checkableContactAdapter.setContacts(contacts);

        return checkableContactAdapter;
    }
}
