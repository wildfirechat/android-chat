/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.mesh;

import androidx.fragment.app.Fragment;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;

/**
 * 互联域详情页的空壳。
 * <p>
 * 页面本体在 {@link DomainInfoFragment}：手机端由本壳装着，平板上同一份实现直接进右栏，
 * 标题栏、菜单、返回都由宿主提供，两端只有这一份实现。
 */
public class DomainInfoActivity extends WfcBaseActivity {

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
        Fragment fragment = DomainInfoFragment.fromIntent(getIntent());
        if (fragment == null) {
            // 参数不全，这一页显示不出东西
            finish();
            return;
        }

        getSupportFragmentManager().beginTransaction()
            .replace(R.id.containerFrameLayout, fragment)
            .commit();
    }
}
