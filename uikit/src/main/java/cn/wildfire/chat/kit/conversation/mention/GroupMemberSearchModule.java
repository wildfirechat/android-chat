package cn.wildfire.chat.kit.conversation.mention;

import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfire.chat.kit.contact.viewholder.UserViewHolder;
import cn.wildfire.chat.kit.search.SearchableModule;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.chat.R2;
import cn.wildfirechat.model.GroupMember;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

public class GroupMemberSearchModule extends SearchableModule<UserInfo, UserViewHolder> {
    private String groupId;

    public GroupMemberSearchModule(String groupId) {
        this.groupId = groupId;
    }

    @Override
    public UserViewHolder onCreateViewHolder(Fragment fragment, @NonNull ViewGroup parent, int viewType) {
        View itemView;
        itemView = LayoutInflater.from(fragment.getActivity()).inflate(R.layout.contact_item_contact, parent, false);
        return new UserViewHolder(fragment, null, itemView);
    }

    @Override
    public void onBind(Fragment fragment, UserViewHolder holder, UserInfo userInfo) {
        holder.onBind(new UIUserInfo(userInfo));
    }

    @Override
    public void onClick(Fragment fragment, UserViewHolder holder, View view, UserInfo userInfo) {
        Intent intent = new Intent();
        intent.putExtra("userId", userInfo.uid);
        fragment.getActivity().setResult(Activity.RESULT_OK, intent);
        fragment.getActivity().finish();
    }

    @Override
    public int getViewType(UserInfo userInfo) {
        return R.layout.contact_item_contact;
    }

    @Override
    public int priority() {
        return 100;
    }

    @Override
    public boolean expandable() {
        return false;
    }

    @Override
    public String category() {
        return null;
    }

    @Override
    public List<UserInfo> search(String keyword) {
        List<GroupMember> groupMembers = ChatManager.Instance().getGroupMembers(groupId, false);
        if (groupMembers == null || groupMembers.isEmpty()) {
            return null;
        }
        List<String> memberIds = new ArrayList<>(groupMembers.size());
        for (GroupMember member : groupMembers) {
            memberIds.add(member.memberId);
        }
        List<UserInfo> userInfos = ChatManager.Instance().getUserInfos(memberIds, groupId);
        List<UserInfo> resultUserInfos = new ArrayList<>();
        if (userInfos != null) {
            for (UserInfo userInfo : userInfos) {
                if ((userInfo.displayName != null && userInfo.displayName.contains(keyword))
                        || (userInfo.friendAlias != null && userInfo.friendAlias.contains(keyword))
                        || (userInfo.groupAlias != null && userInfo.groupAlias.contains(keyword))) {
                    resultUserInfos.add(userInfo);
                }
            }
        }
        return resultUserInfos;
    }
}
