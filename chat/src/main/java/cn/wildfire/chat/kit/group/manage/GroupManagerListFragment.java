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

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.contact.BaseUserListFragment;
import cn.wildfire.chat.kit.contact.UserListAdapter;
import cn.wildfire.chat.kit.contact.model.FooterValue;
import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfire.chat.kit.group.GroupViewModel;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfire.chat.kit.utils.PinyinUtils;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.GroupMember;
import cn.wildfirechat.model.UserInfo;
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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        loadAndShowGroupMembers(true);
        return view;
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
        UserViewModel userViewModel = WfcUIKit.getAppScopeViewModel(UserViewModel.class);
        GroupMember me = groupViewModel.getGroupMember(groupInfo.target, userViewModel.getUserId());
        if (me == null || me.type != GroupMember.GroupMemberType.Owner) {
            return;
        }

        GroupMember groupMember = groupViewModel.getGroupMember(groupInfo.target, userInfo.getUserInfo().uid);
        if (groupMember.type == GroupMember.GroupMemberType.Manager) {
            new MaterialDialog.Builder(getActivity())
                    .items(Collections.singleton("移除群管理"))
                    .itemsCallback((dialog, itemView, position, text) -> {
                        groupViewModel.setGroupManager(groupInfo.target, false, Collections.singletonList(userInfo.getUserInfo().uid), Collections.singletonList(0), null);
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
        List<GroupMember> members = groupViewModel.getGroupMembers(groupInfo.target, refresh);
        managerMembers.clear();
        for (GroupMember member : members) {
            if (member.type == GroupMember.GroupMemberType.Owner || member.type == GroupMember.GroupMemberType.Manager) {
                managerMembers.add(member);
            }
        }
        userListAdapter.setUsers(memberToUIUserInfo(managerMembers));
        userListAdapter.notifyDataSetChanged();
    }

    private List<UIUserInfo> memberToUIUserInfo(List<GroupMember> members) {

        List<String> memberIds = new ArrayList<>(members.size());
        for (GroupMember member : members) {
            memberIds.add(member.memberId);
        }

        List<UIUserInfo> uiUserInfos = new ArrayList<>();
        List<UserInfo> userInfos = UserViewModel.getUsers(memberIds, groupInfo.target);
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
