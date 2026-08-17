/*
 * Copyright (c) 2022 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.pick;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;

/**
 * 「挑一个已有会话」页在手机端的外壳，页面本体见 {@link PickConversationPageFragment}。
 */
public class PickConversationActivity extends WfcBaseActivity {

    @Override
    protected int contentLayout() {
        return R.layout.fragment_container_activity;
    }

    @Override
    protected void afterViews() {
        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.containerFrameLayout, new PickConversationPageFragment())
            .commit();
    }
}
