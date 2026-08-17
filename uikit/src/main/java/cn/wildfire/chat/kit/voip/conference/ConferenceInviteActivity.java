/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.voip.conference;

import androidx.fragment.app.Fragment;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;

/**
 * 「会议邀请发给谁」页的外壳，页面本体见 {@link ConferenceInvitePageFragment}。
 */
public class ConferenceInviteActivity extends WfcBaseActivity {

    @Override
    protected int contentLayout() {
        return R.layout.fragment_container_activity;
    }

    @Override
    protected void afterViews() {
        Fragment fragment = ConferenceInvitePageFragment.fromIntent(getIntent());
        if (fragment == null) {
            finish();
            return;
        }
        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.containerFrameLayout, fragment)
            .commit();
    }
}
