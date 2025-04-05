/*
 * Copyright (c) 2025 WildFireChat. All rights reserved.
 */

package com.noober.menu;

import android.content.Context;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * 弹窗 适配器
 * hxg 2020.9.13 qq:929842234@qq.com
 */
public class HorizontalContextMenuAdapter extends RecyclerView.Adapter<HorizontalContextMenuAdapter.ViewHolder> {
    private final Context mContext;
    private final List<Pair<Integer, String>> mMenuItems;
    private boolean itemWrapContent = false;
    private onClickItemListener listener;

    public HorizontalContextMenuAdapter(Context context, List<Pair<Integer, String>> menuItems) {
        this.mContext = context;
        this.mMenuItems = menuItems;
    }

    public void setItemWrapContent(boolean itemWrapContent) {
        this.itemWrapContent = itemWrapContent;
    }

    public void setOnclickItemListener(onClickItemListener listener) {
        this.listener = listener;
    }

    public interface onClickItemListener {
        void onClick(int position);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.horizontal_context_menu_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        int drawableId = mMenuItems.get(position).first;
        String text = mMenuItems.get(position).second;
        if (itemWrapContent) {
            ViewGroup.LayoutParams params = holder.tv_pop_func.getLayoutParams();
            params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            holder.tv_pop_func.setLayoutParams(params);
            holder.tv_pop_func.setPadding(Display.dip2px(mContext, 8f), 0, Display.dip2px(mContext, 8f), 0);
        }
        holder.iv_pop_icon.setBackgroundResource(drawableId);
        holder.tv_pop_func.setText(text);
        holder.ll_pop_item.setOnClickListener(v -> listener.onClick(position));
    }

    @Override
    public int getItemCount() {
        return mMenuItems != null ? mMenuItems.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout ll_pop_item;
        ImageView iv_pop_icon;
        TextView tv_pop_func;

        public ViewHolder(View itemView) {
            super(itemView);
            ll_pop_item = itemView.findViewById(R.id.ll_pop_item);
            iv_pop_icon = itemView.findViewById(R.id.iv_pop_icon);
            tv_pop_func = itemView.findViewById(R.id.tv_pop_func);
        }
    }
}