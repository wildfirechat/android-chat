package cn.wildfire.chat.kit.search;

import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.wildfire.chat.kit.widget.SearchView;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.remote.ChatManager;

/**
 * 如果启动{@link android.content.Intent}里面包含keyword，直接开始搜索
 */
public abstract class SearchActivity extends AppCompatActivity {
    private SearchFragment searchFragment;
    private List<SearchableModule> modules = new ArrayList<>();

    @BindView(R.id.search_view)
    SearchView searchView;

    @OnClick(R.id.cancel)
    public void onCancelClick() {
        finish();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStatusBarColor(R.color.gray5);
        beforeViews();
        setContentView(contentLayout());
        ButterKnife.bind(this);
        initView();
        initSearchView();
        afterViews();
    }

    /**
     * 子类如果替换布局，它的布局中必须要包含 R.layout.search_bar
     *
     * @return 布局资源id
     */
    protected int contentLayout() {
        return R.layout.search_portal_activity;
    }

    protected void beforeViews() {

    }

    protected void afterViews() {
        String initialKeyword = getIntent().getStringExtra("keyword");
        ChatManager.Instance().getMainHandler().post(() -> {
            if (!TextUtils.isEmpty(initialKeyword)) {
                searchView.setQuery(initialKeyword);
            }
        });
    }

    /**
     * 设置状态栏的颜色
     *
     * @param resId 颜色资源id
     */
    protected void setStatusBarColor(int resId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, resId));
        }
    }

    private void initSearchView() {
        searchView.setOnQueryTextListener(this::search);
    }

    private void initView() {
        searchFragment = new SearchFragment();
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.containerFrameLayout, searchFragment)
            .commit();
        initSearchModule(modules);
    }

    void search(String keyword) {
        if (!TextUtils.isEmpty(keyword)) {
            searchFragment.search(keyword, modules);
        } else {
            searchFragment.reset();
        }
    }

    /**
     * @param modules 是一个输出参数，用来添加希望搜索的{@link SearchableModule}
     */
    protected abstract void initSearchModule(List<SearchableModule> modules);
}
