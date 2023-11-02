/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.channel.viewholder;

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

    public void bind(String category) {
        categoryTextView.setText(category);
    }
}
