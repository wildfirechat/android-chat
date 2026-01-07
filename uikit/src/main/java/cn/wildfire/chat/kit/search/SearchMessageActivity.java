/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.search;

import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.search.module.ConversationMessageSearchModule;
import cn.wildfirechat.model.Conversation;

public class SearchMessageActivity extends SearchActivity {
    private Conversation conversation;

    @Override
    protected void beforeViews() {
        super.beforeViews();
        conversation = getIntent().getParcelableExtra("conversation");
    }

    @Override
    protected void initSearchModule(List<SearchableModule> modules) {
        modules.add(new ConversationMessageSearchModule(conversation));

        // 设置会话对象到SearchFragment
        if (searchFragment != null) {
            searchFragment.setConversation(conversation);
        }
    }

    @Override
    protected void initSearchFragment() {
        super.initSearchFragment();
        // 设置会话对象
        if (searchFragment != null) {
            searchFragment.setConversation(conversation);
        }
    }

    @Override
    protected String searchTip() {
        return getString(R.string.search_tip);
    }
}
