/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.favorite;

import android.view.View;
import android.widget.Toast;

import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import cn.wildfire.chat.kit.AppServiceProvider;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.net.SimpleCallback;
import cn.wildfire.chat.kit.widget.ProgressFragment;

public class FavoriteListFragment extends ProgressFragment {
    private FavoriteListAdapter favoriteListAdapter;
    RecyclerView recyclerView;

    private boolean hasMore = true;
    private boolean isLoading = false;
    private LinearLayoutManager layoutManager;

    @Override
    protected int contentLayout() {
        return R.layout.fav_list_frament;
    }

    @Override
    protected void afterViews(View view) {
        super.afterViews(view);
        bindViews(view);
        init();
    }

    private void bindViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
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
                showContent();
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

    public void delFav(FavoriteItem item) {
        AppServiceProvider appServiceProvider = WfcUIKit.getWfcUIKit().getAppServiceProvider();
        appServiceProvider.removeFavoriteItem(item.getFavId(), new SimpleCallback<Void>() {
            @Override
            public void onUiSuccess(Void aVoid) {
                favoriteListAdapter.removeFavoriteItem(item.getFavId());
                Toast.makeText(getContext(), "删除收藏成功", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onUiFailure(int code, String msg) {
                Toast.makeText(getContext(), "删除收藏失败: " + code, Toast.LENGTH_SHORT).show();
            }
        });
    }

}
