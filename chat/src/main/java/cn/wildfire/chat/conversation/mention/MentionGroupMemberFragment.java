package cn.wildfire.chat.conversation.mention;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProviders;
import cn.wildfire.chat.contact.BaseContactFragment;
import cn.wildfire.chat.contact.ContactAdapter;
import cn.wildfire.chat.contact.ContactViewModel;
import cn.wildfire.chat.contact.model.HeaderValue;
import cn.wildfire.chat.contact.model.UIUserInfo;
import cn.wildfire.chat.group.GroupViewModel;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.GroupMember;

public class MentionGroupMemberFragment extends BaseContactFragment {
    private GroupInfo groupInfo;

    public static MentionGroupMemberFragment newInstance(GroupInfo groupInfo) {
        Bundle args = new Bundle();
        args.putParcelable("groupInfo", groupInfo);
        MentionGroupMemberFragment fragment = new MentionGroupMemberFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void initHeaderViewHolders() {
        addHeaderViewHolder(MentionAllHeaderViewHolder.class, new HeaderValue());
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        groupInfo = getArguments().getParcelable("groupInfo");
    }

    @Override
    public ContactAdapter onCreateContactAdapter() {
        ContactAdapter contactAdapter = new ContactAdapter(this);

        GroupViewModel groupViewModel = ViewModelProviders.of(getActivity()).get(GroupViewModel.class);
        List<GroupMember> members = groupViewModel.getGroupMembers(groupInfo.target, false);
        List<String> memberIds = new ArrayList<>(members.size());
        for (GroupMember member : members) {
            memberIds.add(member.memberId);
        }
        ContactViewModel contactViewModel = ViewModelProviders.of(getActivity()).get(ContactViewModel.class);
        List<UIUserInfo> contacts = userInfoToUIUserInfo(contactViewModel.getContacts(memberIds));
        contactAdapter.setContacts(contacts);

        return contactAdapter;
    }

    @Override
    public void onContactClick(UIUserInfo userInfo) {
        Intent intent = new Intent();
        intent.putExtra("contact", userInfo.getUserInfo());
        getActivity().setResult(Activity.RESULT_OK, intent);
        getActivity().finish();
    }

    @Override
    public void onHeaderClick(int index) {
        Intent intent = new Intent();
        intent.putExtra("mentionAll", true);
        getActivity().setResult(Activity.RESULT_OK, intent);
        getActivity().finish();
    }
}
