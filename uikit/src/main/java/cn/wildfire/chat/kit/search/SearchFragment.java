package cn.wildfire.chat.kit.search;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.chat.R2;

public class SearchFragment extends Fragment {
    @BindView(R2.id.recyclerView)
    RecyclerView recyclerView;
    @BindView(R2.id.emptyLinearLayout)
    LinearLayout emptyLinearLayout;
    @BindView(R2.id.descLinearLayout)
    LinearLayout descLinearLayout;
    private SearchResultAdapter adapter;
    private SearchViewModel searchViewModel;
    private Observer<SearchResult> searchResultObserver = this::onSearchResult;
    private InputMethodManager inputManager;

    private boolean hideSearchDescView = false;

    public static final String HIDE_SEARCH_DESC_VIEW = "hideSearchDescView";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        hideSearchDescView = args != null && args.getBoolean(HIDE_SEARCH_DESC_VIEW);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.search_fragment, container, false);
        searchViewModel = ViewModelProviders.of(this).get(SearchViewModel.class);
        searchViewModel.getResultLiveData().observeForever(searchResultObserver);
        ButterKnife.bind(this, view);
        inputManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                inputManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        });
        descLinearLayout.setVisibility(hideSearchDescView ? View.GONE: View.VISIBLE);
        return view;
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
