/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.search;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;

import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.search.module.ConversationMessageSearchModule;
import cn.wildfirechat.model.Conversation;

/**
 * 会话内查找聊天记录。除了搜索结果，顶部还有「按日期 / 图片与视频 / 文件 / 链接」四个快捷入口
 * —— 它们由 {@link SearchFragment} 在拿到 conversation 后才显示。
 */
public class SearchMessagePageFragment extends SearchPageFragment {

    private static final String ARG_CONVERSATION = "conversation";

    private Conversation conversation;

    @Nullable
    public static SearchMessagePageFragment fromIntent(@Nullable Intent intent) {
        if (intent == null || intent.getParcelableExtra(ARG_CONVERSATION) == null) {
            return null;
        }
        Bundle args = argsFromIntent(intent);
        args.putParcelable(ARG_CONVERSATION, intent.getParcelableExtra(ARG_CONVERSATION));
        SearchMessagePageFragment fragment = new SearchMessagePageFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        conversation = getArguments() == null ? null : getArguments().getParcelable(ARG_CONVERSATION);
    }

    @Override
    protected void initSearchModule(List<SearchableModule> modules) {
        modules.add(new ConversationMessageSearchModule(conversation));
    }

    @Override
    protected void initSearchFragment() {
        super.initSearchFragment();
        // 必须在 SearchFragment 的视图创建之前设进去：快捷入口那一栏的显隐是在 onCreateView 里定的
        searchFragment.setConversation(conversation);
    }

    @Override
    protected String searchTip() {
        return getString(R.string.search_tip);
    }
}
