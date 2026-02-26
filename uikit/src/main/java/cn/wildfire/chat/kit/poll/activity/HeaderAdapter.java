/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.poll.activity;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * 用于在 ConcatAdapter 中显示固定视图的 Adapter
 */
public class HeaderAdapter extends RecyclerView.Adapter<HeaderAdapter.ViewHolder> {

    private final View view;

    public HeaderAdapter(View view) {
        this.view = view;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 如果 view 已经有 parent，先移除
        if (view.getParent() != null) {
            ((ViewGroup) view.getParent()).removeView(view);
        }
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // 不需要绑定数据
    }

    @Override
    public int getItemCount() {
        return 1;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
