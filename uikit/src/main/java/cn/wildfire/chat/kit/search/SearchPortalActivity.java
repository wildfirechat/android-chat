package cn.wildfire.chat.kit.search;

import java.util.List;

import cn.wildfire.chat.kit.search.module.ChannelSearchModule;
import cn.wildfire.chat.kit.search.module.ContactSearchModule;
import cn.wildfire.chat.kit.search.module.ConversationSearchModule;
import cn.wildfire.chat.kit.search.module.GroupSearchViewModule;

public class SearchPortalActivity extends SearchActivity {
    @Override
    protected void initSearchModule(List<SearchableModule> modules) {

        SearchableModule module = new ContactSearchModule();
        modules.add(module);

        module = new GroupSearchViewModule();
        modules.add(module);

        module = new ConversationSearchModule();
        modules.add(module);
        modules.add(new ChannelSearchModule());
    }
}
