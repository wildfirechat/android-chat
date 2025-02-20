/*
 * Copyright (c) 2023 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.receipt;


import android.content.Intent;
import android.text.TextUtils;

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
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.GroupMember;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GetGroupMembersCallback;

public class GroupMessageReceiptActivity extends WfcBaseActivity {

    ViewPager viewPager;
    TabLayout tabLayout;

    private Message message;
    private GroupInfo groupInfo;

    protected void bindViews() {
        super.bindViews();
        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);
    }

    @Override
    protected int contentLayout() {
        return R.layout.conversation_receipt_activity;
    }

    @Override
    protected void afterViews() {
        viewPager.setAdapter(new ReceiptFragmentPagerAdapter(getSupportFragmentManager()));
        tabLayout.setupWithViewPager(viewPager);

        Intent intent = getIntent();
        this.message = intent.getParcelableExtra("message");
        this.groupInfo = intent.getParcelableExtra("groupInfo");
        updateTabTitles();
    }

    private void updateTabTitles() {
        ChatManager.Instance().getGroupMembers(this.message.conversation.target, false, new GetGroupMembersCallback() {
            @Override
            public void onSuccess(List<GroupMember> groupMembers) {
                if (isFinishing()) {
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
                tabLayout.getTabAt(0).setText(getString(R.string.message_receipt_unread, unreadCount));
                tabLayout.getTabAt(1).setText(getString(R.string.message_receipt_read, readCount));
            }

            @Override
            public void onFail(int errorCode) {

            }
        });
    }

    private class ReceiptFragmentPagerAdapter extends FragmentStatePagerAdapter {
        private GroupMessageReceiptListFragment unreadUserListFragment;
        private GroupMessageReceiptListFragment readUserListFragment;

        public ReceiptFragmentPagerAdapter(@NonNull FragmentManager fm) {
            super(fm);
        }

        public ReceiptFragmentPagerAdapter(@NonNull FragmentManager fm, int behavior) {
            super(fm, behavior);
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
            return getString(position == 0 ? R.string.message_receipt_unread_tab : R.string.message_receipt_read_tab);
        }
    }
}
