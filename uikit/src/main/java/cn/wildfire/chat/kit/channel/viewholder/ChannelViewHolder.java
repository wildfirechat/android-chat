/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.channel.viewholder;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import cn.wildfire.chat.kit.*;
import cn.wildfirechat.model.ChannelInfo;

public class ChannelViewHolder extends RecyclerView.ViewHolder {
    ImageView portraitImageView;
    TextView channelNameTextView;

    public ChannelViewHolder(View itemView) {
        super(itemView);
        bindViews(itemView);
    }

    private void bindViews(View itemView) {
        portraitImageView = itemView.findViewById(R.id.portraitImageView);
        channelNameTextView = itemView.findViewById(R.id.channelNameTextView);
    }

    public void bind(ChannelInfo channelInfo) {
        channelNameTextView.setText(channelInfo.name == null ? "< " + channelInfo.channelId + "> " : channelInfo.name);
        Glide.with(itemView.getContext()).load(channelInfo.portrait).into(portraitImageView);
    }
}
