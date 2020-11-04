/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.favorite.viewholder;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.favorite.FavoriteItem;
import cn.wildfire.chat.kit.third.utils.TimeUtils;

public abstract class FavContentViewHolder extends RecyclerView.ViewHolder {
    @BindView(R2.id.senderTextView)
    TextView senderTextView;
    @BindView(R2.id.timeTextView)
    TextView timeTextView;

    public FavContentViewHolder(@NonNull View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
    }

    public void bind(FavoriteItem item) {
        senderTextView.setText(item.getSender());
        timeTextView.setText((TimeUtils.getMsgFormatTime(item.getTimestamp())));
    }
}
