/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.contact.newfriend;

import java.util.List;

import cn.wildfire.chat.kit.search.SearchActivity;
import cn.wildfire.chat.kit.search.SearchableModule;
import cn.wildfirechat.model.DomainInfo;

public class SearchUserActivity extends SearchActivity {

    private DomainInfo domainInfo;

    @Override
    protected void beforeViews() {
        domainInfo = getIntent().getParcelableExtra("domainInfo");
    }

    @Override
    protected void initSearchModule(List<SearchableModule> modules) {
        modules.add(new UserSearchModule(this.domainInfo));
    }

    @Override
    protected String searchTip() {
        String tip;
        if (this.domainInfo == null) {
            tip = "在本单位搜索用户\n输入关键词开始搜索";
        } else {
            tip = "在 " + domainInfo.name + " 搜索用户\n输入关键词开始搜索";
        }
        return tip;
    }
}
