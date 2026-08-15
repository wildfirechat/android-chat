/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.channel;

import android.content.Intent;

import androidx.annotation.Nullable;

import java.util.List;

import cn.wildfire.chat.kit.search.SearchPageFragment;
import cn.wildfire.chat.kit.search.SearchableModule;
import cn.wildfire.chat.kit.search.module.ChannelSearchModule;

/**
 * 查找频道。
 */
public class SearchChannelPageFragment extends SearchPageFragment {

    public static SearchChannelPageFragment fromIntent(@Nullable Intent intent) {
        SearchChannelPageFragment fragment = new SearchChannelPageFragment();
        fragment.setArguments(argsFromIntent(intent));
        return fragment;
    }

    @Override
    protected void initSearchModule(List<SearchableModule> modules) {
        modules.add(new ChannelSearchModule());
    }
}
