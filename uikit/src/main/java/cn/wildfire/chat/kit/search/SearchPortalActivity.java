/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.search;

import android.content.Intent;

import androidx.fragment.app.Fragment;

/**
 * 「搜索」总入口在手机端的宿主壳。整页逻辑住在 {@link SearchPortalPageFragment}，
 * 手机端与平板右栏共用同一份。
 */
public class SearchPortalActivity extends SearchShellActivity {

    @Override
    protected Fragment createSearchPage(Intent intent) {
        return SearchPortalPageFragment.fromIntent(intent);
    }
}
