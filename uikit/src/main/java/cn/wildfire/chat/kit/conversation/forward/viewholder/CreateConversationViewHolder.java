/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.forward.viewholder;

import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import cn.wildfire.chat.kit.utils.LayoutScale;

public class CreateConversationViewHolder extends RecyclerView.ViewHolder {
    public CreateConversationViewHolder(View itemView) {
        super(itemView);
        // 字体放大时，按封顶比例放大行高与图标
        LayoutScale.scaleListItem(itemView);
    }
}
