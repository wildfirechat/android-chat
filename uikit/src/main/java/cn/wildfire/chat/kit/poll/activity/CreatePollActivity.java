/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.poll.activity;

import androidx.fragment.app.Fragment;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;

/**
 * 创建投票页的外壳，页面本体见 {@link CreatePollPageFragment}。
 */
public class CreatePollActivity extends WfcBaseActivity {

    public static final String EXTRA_CONVERSATION = "conversation";

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
        Fragment fragment = CreatePollPageFragment.fromIntent(getIntent());
        if (fragment == null) {
            finish();
            return;
        }
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.containerFrameLayout, fragment)
            .commit();
    }
}
