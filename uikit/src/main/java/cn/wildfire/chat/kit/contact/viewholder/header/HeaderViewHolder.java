/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.contact.viewholder.header;

import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import cn.wildfire.chat.kit.contact.UserListAdapter;
import cn.wildfire.chat.kit.contact.model.HeaderValue;
import cn.wildfire.chat.kit.utils.LayoutScale;

public abstract class HeaderViewHolder<T extends HeaderValue> extends RecyclerView.ViewHolder {
    protected Fragment fragment;
    protected UserListAdapter adapter;

    public HeaderViewHolder(Fragment fragment, UserListAdapter adapter, View itemView) {
        super(itemView);
        this.fragment = fragment;
        this.adapter = adapter;
        // 字体放大时，按封顶比例放大 header 的行高与图标
        LayoutScale.scaleListItem(itemView);
    }

    public abstract void onBind(T t);

}
