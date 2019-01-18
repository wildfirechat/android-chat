package cn.wildfire.chat.search;

import androidx.appcompat.widget.SearchView;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import cn.wildfirechat.chat.R;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.WfcBaseActivity;
import cn.wildfire.chat.search.module.ChannelSearchModule;
import cn.wildfire.chat.search.module.ContactSearchModule;
import cn.wildfire.chat.search.module.ConversationSearchModule;
import cn.wildfire.chat.search.module.GroupSearchViewModule;

public class SearchPortalActivity extends WfcBaseActivity {
    private SearchPortalFragment searchPortalFragment;
    private List<SearchableModule> modules;
    private SearchView searchView;

    @Override
    protected int contentLayout() {
        return R.layout.search_portal_activity;
    }

    @Override
    protected void afterViews() {
        initView();
    }

    @Override
    protected int menu() {
        return R.menu.search_portal;
    }

    @Override
    protected void afterMenus(Menu menu) {
        MenuItem searchItem = menu.findItem(R.id.menu_search);
        //通过MenuItem得到SearchView
        searchView = (SearchView) searchItem.getActionView();
        initSearchView();
    }

    private void initSearchView() {
        searchView.onActionViewExpanded();
        searchView.setQueryHint("Search");

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                search(s);
                return true;
            }
        });

    }

    private void initView() {
        searchPortalFragment = new SearchPortalFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.containerFrameLayout, searchPortalFragment)
                .commit();
        initSearchModule();
    }

    void search(String keyword) {
        if (!TextUtils.isEmpty(keyword)) {
            searchPortalFragment.search(keyword, modules);
        } else {
            searchPortalFragment.reset();
        }
    }

    private void initSearchModule() {
        modules = new ArrayList<>();
        SearchableModule module = new ContactSearchModule();
        modules.add(module);

        module = new GroupSearchViewModule();
        modules.add(module);

        module = new ConversationSearchModule();
        // TODO 用下面这种方法，可以不需要{@code searchMessageActivity}, 并做的更好。
//        module.setOnResultItemListener(new OnResultItemClickListener<ConversationSearchResult>() {
//            @Override
//            public void onResultItemClick(Fragment fragment, View itemView, View view, ConversationSearchResult r) {
//
//            }
//        });
        modules.add(module);
        modules.add(new ChannelSearchModule());
    }
}
