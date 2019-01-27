package cn.wildfire.chat.channel;

import java.util.List;

import cn.wildfire.chat.search.SearchActivity;
import cn.wildfire.chat.search.SearchableModule;
import cn.wildfire.chat.search.module.ChannelSearchModule;

public class SearchChannelActivity extends SearchActivity {
    @Override
    protected void initSearchModule(List<SearchableModule> modules) {
        modules.add(new ChannelSearchModule());
    }
}
