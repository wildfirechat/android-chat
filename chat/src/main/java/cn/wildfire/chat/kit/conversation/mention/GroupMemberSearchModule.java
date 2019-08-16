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
import java.util.concurrent.CountDownLatch;

import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfire.chat.kit.contact.viewholder.UserViewHolder;
import cn.wildfire.chat.kit.search.SearchableModule;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.model.GroupMember;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.SearchUserCallback;

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
        CountDownLatch countDownLatch = new CountDownLatch(1);
        List<GroupMember> groupMembers = ChatManager.Instance().getGroupMembers(groupId, false);
        if (groupMembers == null || groupMembers.isEmpty()) {
            return null;
        }
        List<String> memberIds = new ArrayList<>(groupMembers.size());
        for (GroupMember member : groupMembers) {
            memberIds.add(member.memberId);
        }
        List<UserInfo> userInfos = new ArrayList<>();
        ChatManager.Instance().searchUser(keyword, true, new SearchUserCallback() {
            @Override
            public void onSuccess(List<UserInfo> infos) {
                for (UserInfo info : infos) {
                    if (memberIds.contains(info.uid)) {
                        userInfos.add(info);
                    }
                }
                countDownLatch.countDown();
            }

            @Override
            public void onFail(int errorCode) {
                countDownLatch.countDown();
            }
        });
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return userInfos;
    }
}
