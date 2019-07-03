package cn.wildfire.chat.kit.group;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProviders;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.contact.BaseContactFragment;
import cn.wildfire.chat.kit.contact.ContactAdapter;
import cn.wildfire.chat.kit.contact.ContactViewModel;
import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.GroupMember;
import cn.wildfirechat.model.UserInfo;

public class GroupMemberListFragment extends BaseContactFragment {
    private GroupInfo groupInfo;

    public static GroupMemberListFragment newInstance(GroupInfo groupInfo) {
        Bundle args = new Bundle();
        args.putParcelable("groupInfo", groupInfo);
        GroupMemberListFragment fragment = new GroupMemberListFragment();
        fragment.setArguments(args);
        return fragment;
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
        List<UserInfo> userInfos = contactViewModel.getContacts(memberIds, groupInfo.target);
        for (GroupMember member : members) {
            for (UserInfo userInfo : userInfos) {
                if (!TextUtils.isEmpty(member.alias) && member.memberId.equals(userInfo.uid)) {
                    userInfo.displayName = member.alias;
                    break;
                }
            }
        }
        List<UIUserInfo> contacts = userInfoToUIUserInfo(userInfos);
        contactAdapter.setContacts(contacts);

        return contactAdapter;
    }

    @Override
    public void onContactClick(UIUserInfo userInfo) {
        Intent intent = new Intent();
        intent.putExtra("userId", userInfo.getUserInfo().uid);
        getActivity().setResult(Activity.RESULT_OK, intent);
        getActivity().finish();
    }
}
