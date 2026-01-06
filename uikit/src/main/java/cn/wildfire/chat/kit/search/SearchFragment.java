/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.search;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.conversation.file.FileRecordActivity;
import cn.wildfire.chat.kit.search.bydate.ConversationMessageByDateActivity;
import cn.wildfire.chat.kit.search.link.ConversationLinkRecordActivity;
import cn.wildfire.chat.kit.search.media.ConversationMediaActivity;
import cn.wildfirechat.model.Conversation;

public class SearchFragment extends Fragment {
    RecyclerView recyclerView;
    LinearLayout emptyLinearLayout;
    LinearLayout descLinearLayout;
    LinearLayout quickEntryLinearLayout;

    TextView searchTipTextView;
    private SearchResultAdapter adapter;
    private SearchViewModel searchViewModel;
    private Observer<SearchResult> searchResultObserver = this::onSearchResult;
    private InputMethodManager inputManager;

    private boolean hideSearchDescView = false;
    private String searchTip;

    // 会话对象，用于跳转到各个查找页面
    private Conversation conversation;

    public static final String HIDE_SEARCH_DESC_VIEW = "hideSearchDescView";
    public static final String SEARCH_TIP = "searchTip";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            hideSearchDescView = args.getBoolean(HIDE_SEARCH_DESC_VIEW);
            searchTip = args.getString(SEARCH_TIP);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.search_fragment, container, false);
        searchViewModel = new ViewModelProvider(this).get(SearchViewModel.class);
        searchViewModel.getResultLiveData().observeForever(searchResultObserver);
        bindViews(view);
        inputManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                inputManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        });
        descLinearLayout.setVisibility(hideSearchDescView ? View.GONE : View.VISIBLE);

        if (!TextUtils.isEmpty(searchTip)) {
            searchTipTextView.setText(searchTip);
        }
        // 设置快捷入口点击事件
        if(conversation != null){
            quickEntryLinearLayout.setVisibility(View.VISIBLE);
            setupQuickEntryClicks(view);
        }else {
            quickEntryLinearLayout.setVisibility(View.GONE);
        }
        return view;
    }

    private void bindViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        emptyLinearLayout = view.findViewById(R.id.emptyLinearLayout);
        descLinearLayout = view.findViewById(R.id.descLinearLayout);
        quickEntryLinearLayout = view.findViewById(R.id.quickEntryLinearLayout);
        searchTipTextView = view.findViewById(R.id.searchTipTextView);

    }

    /**
     * 设置快捷入口点击事件
     */
    private void setupQuickEntryClicks(View view) {
        android.util.Log.d("SearchFragment", "setupQuickEntryClicks() called");

        // 按日期查找
        view.findViewById(R.id.searchByDateEntry).setOnClickListener(v -> {
            android.util.Log.d("SearchFragment", "searchByDateEntry clicked");
            openConversationSearchByDate();
        });

        // 图片与视频
        view.findViewById(R.id.searchMediaEntry).setOnClickListener(v -> {
            android.util.Log.d("SearchFragment", "searchMediaEntry clicked");
            openConversationMedia();
        });

        // 文件记录
        view.findViewById(R.id.searchFileEntry).setOnClickListener(v -> {
            android.util.Log.d("SearchFragment", "searchFileEntry clicked");
            openFileRecord();
        });

        // 链接记录
        view.findViewById(R.id.searchLinkEntry).setOnClickListener(v -> {
            android.util.Log.d("SearchFragment", "searchLinkEntry clicked");
            openConversationLinkRecord();
        });

        android.util.Log.d("SearchFragment", "All quick entry clicks set up successfully");
    }

    /**
     * 打开按日期查找页面
     */
    private void openConversationSearchByDate() {
        if (conversation == null) {
            // 如果没有设置会话，显示提示
            return;
        }
        try {
            Intent intent = new Intent();
            intent.setClass(getContext(), ConversationMessageByDateActivity.class);
            intent.putExtra("conversation", conversation);
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
            // Activity还未实现，暂时显示Toast提示
            android.widget.Toast.makeText(getContext(), "按日期查找功能开发中...", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 打开图片与视频页面
     */
    private void openConversationMedia() {
        if (conversation == null) {
            return;
        }
        try {
            Intent intent = new Intent();
            intent.setClass(getContext(), ConversationMediaActivity.class);
            intent.putExtra("conversation", conversation);
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
            android.widget.Toast.makeText(getContext(), "图片与视频功能开发中...", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 打开文件记录页面
     */
    private void openFileRecord() {
        if (conversation == null) {
            return;
        }
        try {
            Intent intent = new Intent(getActivity(), FileRecordActivity.class);
            intent.putExtra("conversation", conversation);
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 打开链接记录页面
     */
    private void openConversationLinkRecord() {
        if (conversation == null) {
            return;
        }
        try {
            Intent intent = new Intent();
            intent.setClass(getContext(), ConversationLinkRecordActivity.class);
            intent.putExtra("conversation", conversation);
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
            android.widget.Toast.makeText(getContext(), "链接记录功能开发中...", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 设置会话对象
     * @param conversation 会话对象
     */
    public void setConversation(Conversation conversation) {
        this.conversation = conversation;
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        searchViewModel.getResultLiveData().removeObserver(searchResultObserver);
    }

    public void search(String keyword, List<SearchableModule> searchableModules) {
        if (adapter != null) {
            adapter.reset();
        }
        if(TextUtils.isEmpty(keyword)){
            return;
        }
        keyword = keyword.trim();
        descLinearLayout.setVisibility(View.GONE);
        searchViewModel.search(keyword, searchableModules);
    }

    public void reset() {
        if (adapter != null) {
            adapter.reset();
        }
        descLinearLayout.setVisibility(View.VISIBLE);
        emptyLinearLayout.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
    }

    private void onSearchResult(SearchResult result) {
        if (result == null) {
            recyclerView.setVisibility(View.GONE);
            emptyLinearLayout.setVisibility(View.VISIBLE);
            return;
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyLinearLayout.setVisibility(View.GONE);
        }
        if (adapter == null) {
            adapter = new SearchResultAdapter(this);
            recyclerView.setAdapter(adapter);
            recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        }
        adapter.submitSearResult(result);
    }
}
