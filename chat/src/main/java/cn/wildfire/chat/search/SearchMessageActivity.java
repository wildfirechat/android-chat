package cn.wildfire.chat.search;

import androidx.appcompat.widget.SearchView;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import cn.wildfirechat.chat.R;

import butterknife.ButterKnife;
import cn.wildfire.chat.WfcBaseActivity;
import cn.wildfirechat.model.Conversation;

public class SearchMessageActivity extends WfcBaseActivity {
    private Conversation conversation;
    private String keyword;
    private SearchMessageFragment searchMessageFragment;

    private SearchView searchView;

    @Override
    protected int contentLayout() {
        return R.layout.search_message_activity;
    }

    @Override
    protected void afterViews() {
        conversation = getIntent().getParcelableExtra("conversation");
        keyword = getIntent().getStringExtra("keyword");
        searchMessageFragment = new SearchMessageFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.containerFrameLayout, searchMessageFragment)
                .commit();
        ButterKnife.bind(this);
    }

    @Override
    protected int menu() {
        return R.menu.search_portal;
    }

    @Override
    protected void afterMenus(Menu menu) {
        MenuItem searchItem = menu.findItem(R.id.menu_search);
        searchView = (SearchView) searchItem.getActionView();
        initSearchView();
    }

    private void initSearchView() {
        searchView.onActionViewExpanded();
        searchView.setQueryHint("搜索历史消息");

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
        searchView.setQuery(keyword, true);
        searchView.clearFocus();
    }

    void search(String keyword) {
        if (!TextUtils.isEmpty(keyword)) {
            this.keyword = keyword;
            searchMessageFragment.search(conversation, keyword);
        } else {
            searchMessageFragment.reset();
        }
    }
}
