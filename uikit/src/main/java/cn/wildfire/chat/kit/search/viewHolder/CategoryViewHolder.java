/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.search.viewHolder;

import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import cn.wildfire.chat.kit.*;

public class CategoryViewHolder extends RecyclerView.ViewHolder {
    TextView categoryTextView;

    public CategoryViewHolder(View itemView) {
        super(itemView);
        bindViews(itemView);
    }

    private void bindViews(View itemView) {
        categoryTextView = itemView.findViewById(R.id.categoryTextView);
    }

    public void onBind(String category) {
        if (TextUtils.isEmpty(category)) {
            categoryTextView.setVisibility(View.GONE);
            return;
        }
        categoryTextView.setVisibility(View.VISIBLE);
        categoryTextView.setText(category);
    }
}
