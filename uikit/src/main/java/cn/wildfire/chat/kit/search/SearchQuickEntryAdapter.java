/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.search;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import cn.wildfire.chat.kit.R;

/**
 * 搜索快捷入口适配器
 */
public class SearchQuickEntryAdapter extends RecyclerView.Adapter<SearchQuickEntryAdapter.ViewHolder> {

    private List<QuickEntryItem> entries;

    public SearchQuickEntryAdapter(List<QuickEntryItem> entries) {
        this.entries = entries;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_quick_entry, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        QuickEntryItem item = entries.get(position);
        holder.titleTextView.setText(item.title);
        holder.iconImageView.setImageResource(item.iconRes);
        holder.itemView.setOnClickListener(item.onClickListener);
    }

    @Override
    public int getItemCount() {
        return entries != null ? entries.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView iconImageView;
        TextView titleTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            iconImageView = itemView.findViewById(R.id.entryIconImageView);
            titleTextView = itemView.findViewById(R.id.entryTitleTextView);
        }
    }

    /**
     * 快捷入口项数据类
     */
    public static class QuickEntryItem {
        public String title;
        public int iconRes;
        public View.OnClickListener onClickListener;

        public QuickEntryItem(String title, int iconRes, View.OnClickListener onClickListener) {
            this.title = title;
            this.iconRes = iconRes;
            this.onClickListener = onClickListener;
        }
    }
}
