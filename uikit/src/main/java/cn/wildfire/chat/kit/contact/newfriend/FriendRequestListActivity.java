/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.contact.newfriend;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;

/**
 * 「新的朋友」在手机端的宿主壳。菜单（添加朋友）住在 {@link FriendRequestListFragment}，两端共用。
 */
public class FriendRequestListActivity extends WfcBaseActivity {

    @Override
    protected int contentLayout() {
        return R.layout.fragment_container_activity;
    }

    @Override
    protected void afterViews() {
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.containerFrameLayout, new FriendRequestListFragment())
            .commit();
    }
}
