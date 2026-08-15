/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.search;

import android.content.Intent;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * 「查找聊天内容」在手机端的宿主壳。整页逻辑住在 {@link SearchMessagePageFragment}，
 * 手机端与平板右栏共用同一份。
 */
public class SearchMessageActivity extends SearchShellActivity {

    @Nullable
    @Override
    protected Fragment createSearchPage(Intent intent) {
        return SearchMessagePageFragment.fromIntent(intent);
    }
}
