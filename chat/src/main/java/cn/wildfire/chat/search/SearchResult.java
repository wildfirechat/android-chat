package cn.wildfire.chat.search;

import java.util.List;

class SearchResult {
    SearchableModule module;
    List<Object> result;

    public SearchResult(SearchableModule module, List<Object> result) {
        this.module = module;
        this.result = result;
    }
}
