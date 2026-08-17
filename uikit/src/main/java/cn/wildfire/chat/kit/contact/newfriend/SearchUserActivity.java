/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.contact.newfriend;

import android.content.Intent;

import androidx.fragment.app.Fragment;

import cn.wildfire.chat.kit.search.SearchShellActivity;

/**
 * 「添加朋友」在手机端的宿主壳。整页逻辑住在 {@link SearchUserPageFragment}，
 * 手机端与平板右栏共用同一份。
 */
public class SearchUserActivity extends SearchShellActivity {

    @Override
    protected Fragment createSearchPage(Intent intent) {
        return SearchUserPageFragment.fromIntent(intent);
    }
}
