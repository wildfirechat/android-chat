/*
 * Copyright (c) 2023 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.organization.viewholder;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public abstract class OrganizationEntityViewHolder<T> extends RecyclerView.ViewHolder {
    public OrganizationEntityViewHolder(@NonNull View itemView) {
        super(itemView);
    }

    public abstract void onBind(T t);
}
