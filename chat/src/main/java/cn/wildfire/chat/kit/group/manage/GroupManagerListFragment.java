package cn.wildfire.chat.kit.group.manage;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProviders;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.contact.BaseContactFragment;
import cn.wildfire.chat.kit.contact.ContactAdapter;
import cn.wildfire.chat.kit.contact.ContactViewModel;
import cn.wildfire.chat.kit.contact.model.FooterValue;
import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfire.chat.kit.group.GroupViewModel;
import cn.wildfire.chat.kit.user.UserInfoActivity;
import cn.wildfire.chat.kit.utils.PinyinUtils;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.GroupMember;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

public class GroupManagerListFragment extends BaseContactFragment {
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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        loadAndShowGroupMembers();
        return view;
    }

    @Override
    public ContactAdapter onCreateContactAdapter() {
        return new ContactAdapter(this);
    }

    @Override
    public void initFooterViewHolders() {
        if (groupMember.type == GroupMember.GroupMemberType.Owner) {
            addFooterViewHolder(AddGroupManagerViewHolder.class, new FooterValue());
        }
    }

    @Override
    public void onContactClick(UIUserInfo userInfo) {
        Intent intent = new Intent(getActivity(), UserInfoActivity.class);
        intent.putExtra("userId", userInfo.getUserInfo());
        startActivity(intent);
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
                loadAndShowGroupMembers();
            }
        });
    }

    private void loadAndShowGroupMembers() {
        List<GroupMember> members = groupViewModel.getGroupMembers(groupInfo.target, false);
        managerMembers.clear();
        for (GroupMember member : members) {
            if (member.type == GroupMember.GroupMemberType.Owner || member.type == GroupMember.GroupMemberType.Manager) {
                managerMembers.add(member);
            }
        }
        contactAdapter.setContacts(memberToUIUserInfo(managerMembers));
        contactAdapter.notifyDataSetChanged();
    }

    private List<UIUserInfo> memberToUIUserInfo(List<GroupMember> members) {

        List<String> memberIds = new ArrayList<>(members.size());
        for (GroupMember member : members) {
            memberIds.add(member.memberId);
        }

        List<UIUserInfo> uiUserInfos = new ArrayList<>();
        ContactViewModel contactViewModel = ViewModelProviders.of(getActivity()).get(ContactViewModel.class);
        List<UserInfo> userInfos = contactViewModel.getContacts(memberIds, groupInfo.target);
        boolean showManagerCategory = false;
        for (UserInfo userInfo : userInfos) {
            UIUserInfo info = new UIUserInfo(userInfo);
            if (!TextUtils.isEmpty(userInfo.displayName)) {
                String pinyin = PinyinUtils.getPinyin(userInfo.displayName);
                char c = pinyin.toUpperCase().charAt(0);
                if (c >= 'A' && c <= 'Z') {
                    info.setSortName(pinyin);
                } else {
                    // 为了让排序排到最后
                    info.setSortName("{" + pinyin);
                }
            } else {
                info.setSortName("");
            }

            for (GroupMember member : members) {
                if (userInfo.uid.equals(member.memberId)) {
                    if (member.type == GroupMember.GroupMemberType.Manager) {
                        info.setCategory("管理员");
                        if (!showManagerCategory) {
                            showManagerCategory = true;
                            info.setShowCategory(true);
                        }
                        uiUserInfos.add(info);
                    } else {
                        info.setCategory("群主");
                        info.setShowCategory(true);
                        uiUserInfos.add(0, info);
                    }
                    break;
                }
            }
        }
        return uiUserInfos;
    }
}
