/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.search;

import android.os.Bundle;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseNoToolbarActivity;
import cn.wildfire.chat.kit.widget.SearchView;
import cn.wildfirechat.remote.ChatManager;

/**
 * 如果启动{@link android.content.Intent}里面包含keyword，直接开始搜索
 * <p>
 * <strong>本仓库内已无子类</strong>：搜索页整页下沉到了 {@link SearchPageFragment}，
 * 手机端由 {@link SearchShellActivity} 装着，平板上同一份实现直接进右栏。
 * <strong>新增搜索页请继承 {@code SearchPageFragment}</strong>，别再从这里派生
 * —— 写在 Activity 上的页面进不了右栏。
 * <p>
 * 保留本类只是为了不破坏 AAR 集成方已有的子类。
 */
public abstract class SearchActivity extends WfcBaseNoToolbarActivity {
    protected SearchFragment searchFragment;
    private List<SearchableModule> modules = new ArrayList<>();

    SearchView searchView;

    public void onCancelClick() {
        finish();
    }

    private void bindEvents() {
        findViewById(R.id.cancel).setOnClickListener(v -> onCancelClick());
    }

    private void bindViews() {
        searchView = findViewById(R.id.search_view);
    }

    protected boolean hideSearchDescView() {
        return false;
    }

    protected String searchTip() {
        return null;
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
        setStatusBarTheme(this, false);
        setStatusBarColor(R.color.gray5);
    }

    protected void afterViews() {
        super.afterViews();

        bindViews();
        bindEvents();
        initSearchView();
        initSearchFragment();
        String initialKeyword = getIntent().getStringExtra("keyword");
        ChatManager.Instance().getMainHandler().post(() -> {
            if (!TextUtils.isEmpty(initialKeyword)) {
                searchView.setQuery(initialKeyword);
            }
        });
        if (hideSearchDescView()) {
            searchView.clearFocus();
            hideInputMethod();
        }
    }

    private void initSearchView() {
        searchView.setOnQueryTextListener(this::search);
    }

    protected void initSearchFragment() {
        searchFragment = new SearchFragment();
        Bundle args = new Bundle();
        args.putBoolean(SearchFragment.HIDE_SEARCH_DESC_VIEW, hideSearchDescView());
        args.putString(SearchFragment.SEARCH_TIP, searchTip());
        searchFragment.setArguments(args);
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.containerFrameLayout, searchFragment)
            .commit();
        initSearchModule(modules);
    }

    void search(String keyword) {
        keyword = keyword.trim();
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
