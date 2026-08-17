/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.receipt;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import java.util.List;
import java.util.Map;

import cn.wildfire.chat.kit.R;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.GroupMember;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GetGroupMembersCallback;

/**
 * 群消息已读回执页：两个 tab，分别列出未读和已读的群成员。
 * <p>
 * 手机端装在 {@link GroupMessageReceiptActivity} 这个空壳里，平板上同一份实现进右栏。
 */
public class GroupMessageReceiptFragment extends Fragment {

    private ViewPager viewPager;
    private TabLayout tabLayout;

    private Message message;
    private GroupInfo groupInfo;

    /**
     * 没有 message 就算不出回执，返回 null 让调用方放弃。
     */
    @Nullable
    public static GroupMessageReceiptFragment fromIntent(@Nullable Intent intent) {
        Message message = intent == null ? null : intent.getParcelableExtra("message");
        if (message == null || message.conversation == null) {
            return null;
        }
        GroupMessageReceiptFragment fragment = new GroupMessageReceiptFragment();
        Bundle args = new Bundle();
        args.putParcelable("message", message);
        args.putParcelable("groupInfo", intent.getParcelableExtra("groupInfo"));
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.conversation_receipt_activity, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewPager = view.findViewById(R.id.viewPager);
        tabLayout = view.findViewById(R.id.tabLayout);

        Bundle args = getArguments() == null ? Bundle.EMPTY : getArguments();
        message = args.getParcelable("message");
        groupInfo = args.getParcelable("groupInfo");
        if (message == null) {
            return;
        }

        // 子 FragmentManager：两个 tab 是本页内部的东西，挂到 Activity 上在右栏里会活得比本页还久
        viewPager.setAdapter(new ReceiptFragmentPagerAdapter(getChildFragmentManager()));
        tabLayout.setupWithViewPager(viewPager);
        updateTabTitles();
    }

    private void updateTabTitles() {
        ChatManager.Instance().getGroupMembers(message.conversation.target, false, new GetGroupMembersCallback() {
            @Override
            public void onSuccess(List<GroupMember> groupMembers) {
                // 回调在页面关掉之后才到：此时 getString 会抛 IllegalStateException（Activity 版本
                // 用的是 isFinishing，Fragment 这边只能靠 isAdded）
                if (!isAdded()) {
                    return;
                }

                Map<String, Long> readEntries = ChatManager.Instance().getConversationRead(message.conversation);
                int unreadCount = 0;
                int readCount = 0;

                String selfUid = ChatManager.Instance().getUserId();
                for (GroupMember member : groupMembers) {
                    if (TextUtils.equals(message.sender, selfUid) && TextUtils.equals(selfUid, member.memberId)) {
                        readCount++;
                        continue;
                    }
                    Long readDt = readEntries.get(member.memberId);
                    if (readDt == null || readDt < message.serverTime) {
                        unreadCount++;
                    } else {
                        readCount++;
                    }
                }
                TabLayout.Tab unreadTab = tabLayout.getTabAt(0);
                TabLayout.Tab readTab = tabLayout.getTabAt(1);
                if (unreadTab != null) {
                    unreadTab.setText(getString(R.string.message_receipt_unread_count, unreadCount));
                }
                if (readTab != null) {
                    readTab.setText(getString(R.string.message_receipt_read_count, readCount));
                }
            }

            @Override
            public void onFail(int errorCode) {

            }
        });
    }

    private class ReceiptFragmentPagerAdapter extends FragmentStatePagerAdapter {
        private GroupMessageReceiptListFragment unreadUserListFragment;
        private GroupMessageReceiptListFragment readUserListFragment;

        ReceiptFragmentPagerAdapter(@NonNull FragmentManager fm) {
            super(fm);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            if (position == 0) {
                if (unreadUserListFragment == null) {
                    unreadUserListFragment = GroupMessageReceiptListFragment.newInstance(groupInfo, message, true);
                }
                return unreadUserListFragment;
            } else {
                if (readUserListFragment == null) {
                    readUserListFragment = GroupMessageReceiptListFragment.newInstance(groupInfo, message, false);
                }
                return readUserListFragment;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            return getString(position == 0 ? R.string.message_receipt_unread : R.string.message_receipt_read);
        }
    }
}
