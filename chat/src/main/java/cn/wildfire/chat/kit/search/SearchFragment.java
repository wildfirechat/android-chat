package cn.wildfire.chat.kit.search;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.Bind;
import butterknife.ButterKnife;
import cn.wildfirechat.chat.R;

public class SearchFragment extends Fragment {
    @Bind(R.id.recyclerView)
    RecyclerView recyclerView;
    @Bind(R.id.emptyLinearLayout)
    LinearLayout emptyLinearLayout;
    private SearchResultAdapter adapter;
    private SearchViewModel searchViewModel;
    private Observer<SearchResult> searchResultObserver = this::onSearchResult;
    private InputMethodManager inputManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.search_result_fragment, container, false);
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
        searchViewModel.search(keyword, searchableModules);
    }

    public void reset() {
        if (adapter != null) {
            adapter.reset();
        }
    }

    private void onSearchResult(SearchResult result) {
        if (result == null) {
            emptyLinearLayout.setVisibility(View.VISIBLE);
            return;
        } else {
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
