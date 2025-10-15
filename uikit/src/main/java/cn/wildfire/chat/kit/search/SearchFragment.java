/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.search;

import android.content.Context;
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

public class SearchFragment extends Fragment {
    RecyclerView recyclerView;
    LinearLayout emptyLinearLayout;
    LinearLayout descLinearLayout;
    TextView searchTipTextView;
    private SearchResultAdapter adapter;
    private SearchViewModel searchViewModel;
    private Observer<SearchResult> searchResultObserver = this::onSearchResult;
    private InputMethodManager inputManager;

    private boolean hideSearchDescView = false;
    private String searchTip;

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
        return view;
    }

    private void bindViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        emptyLinearLayout = view.findViewById(R.id.emptyLinearLayout);
        descLinearLayout = view.findViewById(R.id.descLinearLayout);
        searchTipTextView = view.findViewById(R.id.searchTipTextView);
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
