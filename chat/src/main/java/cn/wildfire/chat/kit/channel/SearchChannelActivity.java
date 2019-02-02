package cn.wildfire.chat.kit.channel;

import java.util.List;

import cn.wildfire.chat.kit.search.SearchActivity;
import cn.wildfire.chat.kit.search.SearchableModule;
import cn.wildfire.chat.kit.search.module.ChannelSearchModule;

public class SearchChannelActivity extends SearchActivity {
    @Override
    protected void initSearchModule(List<SearchableModule> modules) {
        modules.add(new ChannelSearchModule());
    }
}
