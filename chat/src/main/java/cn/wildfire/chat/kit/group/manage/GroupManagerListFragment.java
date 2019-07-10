package cn.wildfire.chat.kit.group.manage;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

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
    private GroupInfo groupInfo;
    private GroupMember groupMember;

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
    }

    @Override
    public ContactAdapter onCreateContactAdapter() {
        ContactAdapter contactAdapter = new ContactAdapter(this);

        GroupViewModel groupViewModel = ViewModelProviders.of(getActivity()).get(GroupViewModel.class);
        groupMember = groupViewModel.getGroupMember(groupInfo.target, ChatManager.Instance().getUserId());
        List<GroupMember> members = groupViewModel.getGroupMembers(groupInfo.target, false);
        contactAdapter.setContacts(memberToUIUserInfo(members));
        return contactAdapter;
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
        Toast.makeText(getActivity(), "hello world", Toast.LENGTH_SHORT).show();
    }

    private List<UIUserInfo> memberToUIUserInfo(List<GroupMember> members) {

        List<String> memberIds = new ArrayList<>(members.size());
        for (GroupMember member : members) {
            if (member.type == GroupMember.GroupMemberType.Owner || member.type == GroupMember.GroupMemberType.Manager) {
                memberIds.add(member.memberId);
            }
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
                    } else {
                        info.setCategory("群主");
                        info.setShowCategory(true);
                    }
                    break;
                }
            }
            uiUserInfos.add(info);
        }
        return uiUserInfos;
    }
}
