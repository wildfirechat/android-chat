/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.forward;

import androidx.fragment.app.Fragment;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;

/**
 * 「转发到…」页在手机端的外壳。
 * <p>
 * 页面本体整个在 {@link ForwardPageFragment} 里：标题栏、菜单（多选/取消多选）、返回键
 * 都通过 {@code WfcPage} 声明，本类只负责把它装进一个全屏 Activity。平板右栏里同一个
 * Fragment 由 {@code PanePageFragment} 装载，两端共用一份实现。
 */
public class ForwardActivity extends WfcBaseActivity {

    @Override
    protected int contentLayout() {
        return R.layout.fragment_container_activity;
    }

    @Override
    protected void afterViews() {
        Fragment fragment = ForwardPageFragment.fromIntent(getIntent());
        if (fragment == null) {
            // 一条待转发的消息都没有，与改造前一致：直接关掉
            finish();
            return;
        }
        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.containerFrameLayout, fragment)
            .commit();
    }
}
