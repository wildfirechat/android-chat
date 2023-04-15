/*
 * Copyright (c) 2023 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.receipt;


import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.model.GroupInfo;

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
            String title;
            if (position == 0) {
                title = "未读";
            } else {
                title = "已读";
            }
            return title;
        }
    }
}
