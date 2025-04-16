/*
 * Copyright (c) 2025 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.uikit.menu;

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

public class PopupMenuAdapter extends RecyclerView.Adapter<PopupMenuAdapter.ViewHolder> {
    private final Context mContext;
    private final List<Pair<Integer, String>> mMenuItems;
    private onClickItemListener listener;
    private final boolean isGridMenu;

    public PopupMenuAdapter(Context context, List<Pair<Integer, String>> menuItems, boolean gridMenu) {
        this.mContext = context;
        this.mMenuItems = menuItems;
        this.isGridMenu = gridMenu;
    }

    public void setOnclickItemListener(onClickItemListener listener) {
        this.listener = listener;
    }

    public interface onClickItemListener {
        void onClick(int position);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(isGridMenu ? R.layout.popup_grid_menu_item : R.layout.popup_list_menu_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        int drawableId = mMenuItems.get(position).first;
        String text = mMenuItems.get(position).second;
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