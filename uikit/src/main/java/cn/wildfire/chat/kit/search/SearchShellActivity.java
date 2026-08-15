/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.search;

import android.content.Intent;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseNoToolbarActivity;

/**
 * 搜索类页面在手机端的宿主壳。整页逻辑住在 {@link SearchPageFragment}，
 * 手机端与平板右栏共用同一份。
 * <p>
 * 没有 toolbar：这类页面顶部是搜索框本身。状态栏配色沿用改造前 {@link SearchActivity} 的设置。
 */
public abstract class SearchShellActivity extends WfcBaseNoToolbarActivity {

    /**
     * 由启动 intent 造出本页。返回 null 表示参数不全，直接关掉。
     */
    @Nullable
    protected abstract Fragment createSearchPage(Intent intent);

    @Override
    protected void beforeViews() {
        setStatusBarTheme(this, false);
        setStatusBarColor(R.color.gray5);
    }

    @Override
    protected int contentLayout() {
        return R.layout.fragment_container_no_toolbar_activity;
    }

    @Override
    protected void afterViews() {
        Fragment fragment = createSearchPage(getIntent());
        if (fragment == null) {
            finish();
            return;
        }
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.containerFrameLayout, fragment)
            .commit();
    }
}
