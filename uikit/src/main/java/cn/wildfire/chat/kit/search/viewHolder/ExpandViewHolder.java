/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.search.viewHolder;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import cn.wildfire.chat.kit.*;

public class ExpandViewHolder extends RecyclerView.ViewHolder {
    TextView expandTextView;

    public ExpandViewHolder(View itemView) {
        super(itemView);
        bindViews(itemView);
    }

    private void bindViews(View itemView) {
        expandTextView = itemView.findViewById(R.id.expandTextView);
    }

    /**
     * @param category
     * @param count    被折叠了的搜索结果数量
     */
    public void onBind(String category, int count) {
        // todo
        expandTextView.setText("点击展开剩余" + count + "项");
    }
}
