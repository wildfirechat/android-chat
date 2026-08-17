/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.user;

import android.content.Intent;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfirechat.client.FriendSource;
import cn.wildfirechat.client.GroupMemberSource;
import cn.wildfirechat.model.UserInfo;

/**
 * 用户资料页在手机端的宿主壳。
 * <p>
 * 标题、菜单（{@code R.menu.user_info} 的可见性计算与点击处理）全部住在
 * {@link UserInfoFragment}（它实现了 {@code WfcPage}），由 {@link WfcBaseActivity} 统一取用，
 * 因此本类只剩「装配那个 Fragment」这一件事，与平板右栏共用同一份页面实现。
 */
public class UserInfoActivity extends WfcBaseActivity {
    // TODO
    private FriendSource friendSource;

    @Override
    protected int contentLayout() {
        return R.layout.fragment_container_activity;
    }

    @Override
    protected void afterViews() {
        if (!isDarkTheme()) {
            setTitleBackgroundResource(R.color.white, false);
        }
        Intent intent = getIntent();
        UserInfo userInfo = intent.getParcelableExtra("userInfo");
        String groupId = intent.getStringExtra("groupId");
        GroupMemberSource groupMemberSource = intent.getParcelableExtra("groupMemberSource");
        if (userInfo == null) {
            finish();
        } else {
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.containerFrameLayout,
                    UserInfoFragment.newInstance(userInfo, groupId, groupMemberSource, friendSource))
                .commit();
        }
    }
}
