/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.voip.conference;

import androidx.fragment.app.Fragment;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;

/**
 * 会议详情页的外壳，页面本体见 {@link ConferenceInfoPageFragment}。
 */
public class ConferenceInfoActivity extends WfcBaseActivity {

    @Override
    protected int contentLayout() {
        return R.layout.fragment_container_activity;
    }

    @Override
    protected void afterViews() {
        // 配置变化后 FragmentManager 已经把页面恢复出来了，无条件 add 会再叠一层
        if (getSupportFragmentManager().findFragmentById(R.id.containerFrameLayout) != null) {
            return;
        }
        Fragment fragment = ConferenceInfoPageFragment.fromIntent(getIntent());
        if (fragment == null) {
            finish();
            return;
        }
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.containerFrameLayout, fragment)
            .commit();
    }
}
