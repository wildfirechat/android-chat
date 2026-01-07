/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.widget.selecttext;

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
public class SelectTextPopAdapter extends RecyclerView.Adapter<SelectTextPopAdapter.ViewHolder> {
    private Context mContext;
    private List<Pair<Integer, String>> mList;
    private boolean itemWrapContent = false;
    private onClickItemListener listener;

    public SelectTextPopAdapter(Context mContext, List<Pair<Integer, String>> mList) {
        this.mContext = mContext;
        this.mList = mList;
    }

    public void setItemWrapContent(boolean itemWrapContent) {
        this.itemWrapContent = itemWrapContent;
    }

    public void setOnclickItemListener(onClickItemListener l) {
        listener = l;
    }

    public interface onClickItemListener {
        void onClick(int position);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(cn.wildfire.chat.kit.R.layout.item_select_text_pop, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        int drawableId = mList.get(position).first;
        String text = mList.get(position).second;
        if (itemWrapContent) {
            ViewGroup.LayoutParams params = holder.tv_pop_func.getLayoutParams();
            params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            holder.tv_pop_func.setLayoutParams(params);
            holder.tv_pop_func.setPadding(SelectUtils.dp2px(8f), 0,
                    SelectUtils.dp2px(8f), 0);
        }
        if (drawableId != 0) {
            holder.iv_pop_icon.setBackgroundResource(drawableId);
        }
        holder.tv_pop_func.setText(text);
        holder.ll_pop_item.setOnClickListener(v -> {
            if (listener != null) {
                listener.onClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mList != null ? mList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public LinearLayout ll_pop_item;
        public ImageView iv_pop_icon;
        public TextView tv_pop_func;

        public ViewHolder(View itemView) {
            super(itemView);
            ll_pop_item = itemView.findViewById(cn.wildfire.chat.kit.R.id.ll_pop_item);
            iv_pop_icon = itemView.findViewById(cn.wildfire.chat.kit.R.id.iv_pop_icon);
            tv_pop_func = itemView.findViewById(cn.wildfire.chat.kit.R.id.tv_pop_func);
        }
    }
}
