/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.pick;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;

/**
 * 「选一个会话对象」页在手机端的外壳，页面本体见
 * {@link PickOrCreateConversationTargetPageFragment}。
 * <p>
 * 结果 extra 与改造前完全一致：单选一个人回传 {@code userInfo}，多选（建群后）或选中一个群
 * 回传 {@code groupInfo}，调用方不用改。
 */
public class PickOrCreateConversationTargetActivity extends WfcBaseActivity {

    @Override
    protected int contentLayout() {
        return R.layout.fragment_container_activity;
    }

    @Override
    protected void afterViews() {
        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.containerFrameLayout,
                PickOrCreateConversationTargetPageFragment.fromIntent(getIntent()))
            .commit();
    }
}
