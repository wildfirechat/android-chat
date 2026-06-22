/*
 * Copyright (c) 2023 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.organization.viewholder;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import cn.wildfire.chat.kit.utils.LayoutScale;

public abstract class OrganizationEntityViewHolder<T> extends RecyclerView.ViewHolder {
    public OrganizationEntityViewHolder(@NonNull View itemView) {
        super(itemView);
        // 字体放大时，按封顶比例放大组织结构列表项的行高与头像
        LayoutScale.scaleListItem(itemView);
    }

    public abstract void onBind(T t);
}
