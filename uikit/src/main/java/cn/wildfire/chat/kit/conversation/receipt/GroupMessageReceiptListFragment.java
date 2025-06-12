/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.receipt;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.Nullable;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.group.GroupViewModel;
import cn.wildfire.chat.kit.user.UserInfoActivity;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfire.chat.kit.widget.ProgressFragment;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.GroupMember;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

public class GroupMessageReceiptListFragment extends ProgressFragment implements GroupMessageReceiptAdapter.OnMemberClickListener {
    private GroupInfo groupInfo;
    private GroupMessageReceiptAdapter groupMemberListAdapter;
    private GroupViewModel groupViewModel;

    private boolean unread;
    private Message message;

    RecyclerView recyclerView;

    public static GroupMessageReceiptListFragment newInstance(GroupInfo groupInfo, Message message, boolean unread) {
        Bundle args = new Bundle();
        args.putParcelable("groupInfo", groupInfo);
        args.putParcelable("message", message);
        args.putBoolean("unread", unread);
        GroupMessageReceiptListFragment fragment = new GroupMessageReceiptListFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        groupInfo = getArguments().getParcelable("groupInfo");
        this.message = getArguments().getParcelable("message");
        this.unread = getArguments().getBoolean("unread");

        if (groupInfo == null || message == null) {
            getActivity().finish();
        }
    }

    @Override
    protected int contentLayout() {
        return R.layout.conversation_receipt_fragment;
    }

    @Override
    protected void afterViews(View view) {
        super.afterViews(view);
        if (groupInfo == null) {
            return;
        }
        bindViews(view);
        groupMemberListAdapter = new GroupMessageReceiptAdapter(groupInfo);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(groupMemberListAdapter);
        groupMemberListAdapter.setOnMemberClickListener(this);
        UserViewModel userViewModel = WfcUIKit.getAppScopeViewModel(UserViewModel.class);
        userViewModel.userInfoLiveData().observe(this, userInfos -> loadAndShowGroupMembers(false));

        groupViewModel = WfcUIKit.getAppScopeViewModel(GroupViewModel.class);
        loadAndShowGroupMembers(true);

        groupViewModel.groupMembersUpdateLiveData().observe(this, groupMembers -> {
            loadAndShowGroupMembers(false);
        });
    }

    private void bindViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
    }

    private void loadAndShowGroupMembers(boolean refresh) {
        // 消息回执里面，显示该消息发送后新加入的群成员
        // 如果需要补显示该消息发送后新加入的群成员，joinBeforeDt=this.message.serverTime
        groupViewModel.getGroupMemberUserInfosLiveData(groupInfo.target, refresh, Long.MAX_VALUE).observe(this, userInfos -> {
            showContent();
            groupMemberListAdapter.setMembers(filterGroupMember(userInfos, unread));
            groupMemberListAdapter.notifyDataSetChanged();
        });
    }

    private List<UserInfo> filterGroupMember(List<UserInfo> userInfos, boolean unread) {
        Map<String, Long> readEntries = ChatManager.Instance().getConversationRead(this.message.conversation);
        List<UserInfo> result = new ArrayList<>();
        for (UserInfo info : userInfos) {
            if (TextUtils.equals(this.message.sender, info.uid)) {
                if (!unread) {
                    result.add(info);
                }
                continue;
            }
            Long readDt = readEntries.get(info.uid);
            if (unread) {
                if (readDt == null || readDt < message.serverTime) {
                    result.add(info);
                }
            } else {
                if (readDt != null && readDt >= message.serverTime) {
                    result.add(info);
                }
            }
        }
        return result;
    }

    @Override
    public void onUserMemberClick(UserInfo userInfo) {
        GroupMember groupMember = ChatManager.Instance().getGroupMember(groupInfo.target, ChatManager.Instance().getUserId());
        if (groupInfo != null && groupInfo.privateChat == 1 && groupMember.type == GroupMember.GroupMemberType.Normal) {
            return;
        }
        Intent intent = new Intent(getActivity(), UserInfoActivity.class);
        intent.putExtra("userInfo", userInfo);
        intent.putExtra("groupId", groupInfo.target);
        startActivity(intent);
    }
}
