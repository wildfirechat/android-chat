package cn.wildfire.chat.kit.contact.newfriend;

import java.util.List;

import cn.wildfire.chat.kit.search.SearchActivity;
import cn.wildfire.chat.kit.search.SearchableModule;

public class SearchUserActivity extends SearchActivity {

    @Override
    protected void beforeViews() {
    }

    @Override
    protected void initSearchModule(List<SearchableModule> modules) {
        modules.add(new UserSearchModule());
    }
}
