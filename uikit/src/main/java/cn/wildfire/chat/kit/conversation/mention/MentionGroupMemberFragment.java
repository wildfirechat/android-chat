/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.mention;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.contact.BaseUserListFragment;
import cn.wildfire.chat.kit.contact.ContactViewModel;
import cn.wildfire.chat.kit.contact.UserListAdapter;
import cn.wildfire.chat.kit.contact.model.HeaderValue;
import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfire.chat.kit.contact.viewholder.header.HeaderViewHolder;
import cn.wildfire.chat.kit.group.GroupViewModel;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.GroupMember;
import cn.wildfirechat.remote.ChatManager;

// ATTENTION
// 现在单聊也支持@机器人，但为方便客户合并代码，本类的类名保持原样，不进行重构
public class MentionGroupMemberFragment extends BaseUserListFragment {
    private GroupInfo groupInfo;
    private GroupMember groupMember;
    private GroupViewModel groupViewModel;
    private ContactViewModel contactViewModel;

    public static MentionGroupMemberFragment newInstance(GroupInfo groupInfo) {
        Bundle args = new Bundle();
        args.putParcelable("groupInfo", groupInfo);
        MentionGroupMemberFragment fragment = new MentionGroupMemberFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showQuickIndexBar(true);
        groupInfo = getArguments().getParcelable("groupInfo");
        contactViewModel = WfcUIKit.getAppScopeViewModel(ContactViewModel.class);
        if (groupInfo != null) {
            groupViewModel = WfcUIKit.getAppScopeViewModel(GroupViewModel.class);
            groupMember = groupViewModel.getGroupMember(groupInfo.target, ChatManager.Instance().getUserId());
        }
    }

    @Override
    protected void afterViews(View view) {
        super.afterViews(view);
        if (groupInfo != null) {
            groupViewModel.getGroupMemberUIUserInfosLiveData(groupInfo.target, false).observe(this, uiUserInfos -> {
                showContent();
                userListAdapter.setUsers(uiUserInfos);
            });
        }

        contactViewModel.aiRobotUserInfosLiveData().observe(this, uiUserInfos -> {
            showContent();
            userListAdapter.setAIRobotUsers(uiUserInfos);
        });
    }

    @Override
    public UserListAdapter onCreateUserListAdapter() {
        return new UserListAdapter(this);
    }

    @Override
    public void initHeaderViewHolders() {
        if (groupMember != null) {
            if (groupMember.type == GroupMember.GroupMemberType.Manager || groupMember.type == GroupMember.GroupMemberType.Owner) {
                addHeaderViewHolder(MentionAllHeaderViewHolder.class, R.layout.conversation_header_mention_all, new HeaderValue());
            }
        }
    }

    @Override
    public void onHeaderClick(HeaderViewHolder holder) {
        Intent intent = new Intent();
        intent.putExtra("mentionAll", true);
        getActivity().setResult(Activity.RESULT_OK, intent);
        getActivity().finish();
    }

    @Override
    public void onUserClick(UIUserInfo userInfo) {
        Intent intent = new Intent();
        intent.putExtra("userId", userInfo.getUserInfo().uid);
        getActivity().setResult(Activity.RESULT_OK, intent);
        getActivity().finish();
    }
}
