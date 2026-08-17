/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.contact.newfriend;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;

import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.search.SearchPageFragment;
import cn.wildfire.chat.kit.search.SearchableModule;
import cn.wildfirechat.model.DomainInfo;

/**
 * 「添加朋友」：按账号/手机号搜人。{@code domainInfo} 非空时搜的是互联域里的用户。
 */
public class SearchUserPageFragment extends SearchPageFragment {

    private static final String ARG_DOMAIN_INFO = "domainInfo";

    private DomainInfo domainInfo;

    public static SearchUserPageFragment fromIntent(@Nullable Intent intent) {
        Bundle args = argsFromIntent(intent);
        if (intent != null) {
            args.putParcelable(ARG_DOMAIN_INFO, intent.getParcelableExtra(ARG_DOMAIN_INFO));
        }
        SearchUserPageFragment fragment = new SearchUserPageFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        domainInfo = getArguments() == null ? null : getArguments().getParcelable(ARG_DOMAIN_INFO);
    }

    @Override
    protected void initSearchModule(List<SearchableModule> modules) {
        modules.add(new UserSearchModule(this.domainInfo));
    }

    @Override
    protected String searchTip() {
        if (this.domainInfo == null) {
            return getString(R.string.search_user_tip_local);
        }
        return getString(R.string.search_user_tip_domain, domainInfo.name);
    }
}
