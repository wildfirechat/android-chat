/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.search;

import android.content.Intent;

import androidx.annotation.Nullable;

import java.util.List;

import cn.wildfire.chat.kit.search.module.ChannelSearchModule;
import cn.wildfire.chat.kit.search.module.ContactSearchModule;
import cn.wildfire.chat.kit.search.module.ConversationSearchModule;
import cn.wildfire.chat.kit.search.module.EmployeeSearchModule;
import cn.wildfire.chat.kit.search.module.GroupSearchViewModule;

/**
 * 搜索总入口（主界面右上角的放大镜）：联系人、群组、聊天记录、频道、组织架构一起搜。
 */
public class SearchPortalPageFragment extends SearchPageFragment {

    public static SearchPortalPageFragment fromIntent(@Nullable Intent intent) {
        SearchPortalPageFragment fragment = new SearchPortalPageFragment();
        fragment.setArguments(argsFromIntent(intent));
        return fragment;
    }

    @Override
    protected void initSearchModule(List<SearchableModule> modules) {

        SearchableModule module = new ContactSearchModule();
        modules.add(module);

        module = new GroupSearchViewModule();
        modules.add(module);

        module = new ConversationSearchModule();
        modules.add(module);
        modules.add(new ChannelSearchModule());

        module = new EmployeeSearchModule();
        modules.add(module);
    }
}
