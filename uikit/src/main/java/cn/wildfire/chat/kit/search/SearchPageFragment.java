/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.search;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.page.WfcPage;
import cn.wildfire.chat.kit.page.WfcPageCompat;
import cn.wildfire.chat.kit.widget.SearchView;
import cn.wildfirechat.remote.ChatManager;

/**
 * 搜索页整页：顶部「搜索框 + 取消」，下面一个 {@link SearchFragment} 展示分类结果。
 * <p>
 * 逐行搬自 {@link SearchActivity}，唯一的结构性改动是宿主：搜索总入口、添加朋友、会话内查找、
 * 查找频道、@群成员这五个页面原先都是 Activity，因而在平板上只能全屏盖住双栏。整页下沉到
 * Fragment 之后，手机端由一个空壳 Activity 装着（{@link SearchShellActivity}），
 * 平板上直接进右栏，两端共用同一份。
 * <p>
 * 本页{@link #providesOwnToolbar() 自带标题栏}：右栏不会再给一条 toolbar，
 * 「取消」就是它的关闭入口。
 * <p>
 * 子类只需回答一件事：{@link #initSearchModule} 要搜哪些东西。
 */
public abstract class SearchPageFragment extends Fragment implements WfcPage {

    /**
     * 进入本页时就带着的关键词（从会话搜索结果点「更多聊天记录」进来时会带）。
     * 键名与改造前 {@code getIntent().getStringExtra("keyword")} 保持一致。
     */
    protected static final String ARG_KEYWORD = "keyword";

    protected SearchFragment searchFragment;
    protected SearchView searchView;

    private final List<SearchableModule> modules = new ArrayList<>();

    /**
     * 把 Activity 启动 intent 里的公共参数搬进 arguments。子类的 {@code fromIntent}
     * 在此基础上继续放自己的参数。
     */
    protected static Bundle argsFromIntent(@Nullable Intent intent) {
        Bundle args = new Bundle();
        if (intent != null) {
            args.putString(ARG_KEYWORD, intent.getStringExtra(ARG_KEYWORD));
        }
        return args;
    }

    /**
     * 子类如果替换布局，它的布局中必须要包含 {@code R.layout.search_bar}，
     * 以及承载 {@link SearchFragment} 的 {@code R.id.containerFrameLayout}。
     */
    @LayoutRes
    protected int contentLayout() {
        return R.layout.search_portal_activity;
    }

    protected boolean hideSearchDescView() {
        return false;
    }

    @Nullable
    protected String searchTip() {
        return null;
    }

    /**
     * @param modules 是一个输出参数，用来添加希望搜索的 {@link SearchableModule}
     */
    protected abstract void initSearchModule(List<SearchableModule> modules);

    @Override
    public boolean providesOwnToolbar() {
        return true;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(contentLayout(), container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        searchView = view.findViewById(R.id.search_view);
        view.findViewById(R.id.cancel).setOnClickListener(v -> onCancelClick());
        searchView.setOnQueryTextListener(this::search);
        initSearchFragment();

        String initialKeyword = getArguments() == null ? null : getArguments().getString(ARG_KEYWORD);
        // post 到 SearchFragment 那笔 commit 之后：setQuery 会同步触发一次搜索，
        // 而搜索要用到 SearchFragment 的视图。与改造前同一处理。
        ChatManager.Instance().getMainHandler().post(() -> {
            if (isAdded() && !TextUtils.isEmpty(initialKeyword)) {
                searchView.setQuery(initialKeyword);
            }
        });
        if (hideSearchDescView()) {
            searchView.clearFocus();
            hideInputMethod();
        }
    }

    protected void initSearchFragment() {
        searchFragment = new SearchFragment();
        Bundle args = new Bundle();
        args.putBoolean(SearchFragment.HIDE_SEARCH_DESC_VIEW, hideSearchDescView());
        args.putString(SearchFragment.SEARCH_TIP, searchTip());
        searchFragment.setArguments(args);
        getChildFragmentManager().beginTransaction()
            .replace(R.id.containerFrameLayout, searchFragment)
            .commit();
        modules.clear();
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
     * 「取消」。手机端 finish 掉本页，右栏里把本页出栈。
     */
    public void onCancelClick() {
        hideInputMethod();
        WfcPageCompat.finishPage(this);
    }

    protected void hideInputMethod() {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        View focus = activity.getCurrentFocus();
        if (focus == null) {
            return;
        }
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
        }
    }
}
