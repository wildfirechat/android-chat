/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.search.viewHolder;

import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import cn.wildfire.chat.kit.utils.LayoutScale;

public abstract class ResultItemViewHolder<R> extends RecyclerView.ViewHolder {
    protected Fragment fragment;

    public ResultItemViewHolder(Fragment fragment, View itemView) {
        super(itemView);
        this.fragment = fragment;

        // 字体放大时，按封顶比例放大搜索结果项的头像及其所在固定高度的行
        // 注意：类型参数 <R> 会遮蔽资源类 R，这里使用全限定名
        View portrait = itemView.findViewById(cn.wildfire.chat.kit.R.id.portraitImageView);
        if (portrait != null) {
            LayoutScale.scaleViewSize(portrait, LayoutScale.CAP);
            if (portrait.getParent() instanceof View) {
                LayoutScale.scaleViewHeight((View) portrait.getParent(), LayoutScale.ROW);
            }
        }
    }

    public abstract void onBind(String keyword, R r);
}
