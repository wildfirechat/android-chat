/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.favorite;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.wildfire.chat.kit.AppServiceProvider;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.WfcUIKit;

public class FavoriteListFragment extends Fragment {
    private FavoriteListAdapter favoriteListAdapter;
    @BindView(R2.id.recyclerView)
    RecyclerView recyclerView;

    private boolean hasMore = true;
    private boolean isLoading = false;
    private LinearLayoutManager layoutManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fav_list_frament, container, false);
        ButterKnife.bind(this, view);
        init();
        return view;
    }

    private void init() {
        favoriteListAdapter = new FavoriteListAdapter(this);
        recyclerView.setAdapter(favoriteListAdapter);
        layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL));

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState != RecyclerView.SCROLL_STATE_IDLE) {
                    return;
                }
                if (!recyclerView.canScrollVertically(1)) {
                    if (!isLoading && hasMore) {
                        int lastVisibleItem = layoutManager.findLastCompletelyVisibleItemPosition();
                        if (lastVisibleItem > favoriteListAdapter.getItemCount() - 3) {
                            loadFavoriteItems();
                        }
                    }
                }
            }
        });
        loadFavoriteItems();
    }

    private void loadFavoriteItems() {
        if (isLoading || !hasMore) {
            return;
        }

        List<FavoriteItem> oldItems = favoriteListAdapter.getFavoriteItems();
        int startId = 0;
        if (oldItems != null && oldItems.size() > 0) {
            startId = oldItems.get(oldItems.size() - 1).getFavId();
        }

        AppServiceProvider appServiceProvider = WfcUIKit.getWfcUIKit().getAppServiceProvider();
        appServiceProvider.getFavoriteItems(startId, 20, new AppServiceProvider.GetFavoriteItemCallback() {
            @Override
            public void onUiSuccess(List<FavoriteItem> items, boolean hasMore) {
                if (getActivity() == null || getActivity().isFinishing()) {
                    return;
                }
                favoriteListAdapter.addFavoriteItems(items);
                favoriteListAdapter.notifyItemRangeChanged(oldItems.size(), items.size());

                FavoriteListFragment.this.hasMore = hasMore;
                isLoading = false;
            }

            @Override
            public void onUiFailure(int code, String msg) {
                if (getActivity() == null || getActivity().isFinishing()) {
                    return;
                }
                Toast.makeText(getActivity(), "加载收藏失败 " + code, Toast.LENGTH_SHORT).show();
                isLoading = false;
            }
        });
    }
}
