/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.channel;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.channel.viewholder.CategoryViewHolder;
import cn.wildfire.chat.kit.channel.viewholder.ChannelViewHolder;
import cn.wildfirechat.model.ChannelInfo;

public class ChannelListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<ChannelInfo> followedChannels;
    private OnChannelClickListener onChannelClickListener;


    public void setFollowedChannels(List<ChannelInfo> followedChannels) {
        this.followedChannels = followedChannels;
    }

    public void setOnChannelClickListener(OnChannelClickListener listener) {
        this.onChannelClickListener = listener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == R.layout.channel_item_category) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.channel_item_category, parent, false);
            return new CategoryViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.channel_item, parent, false);
            RecyclerView.ViewHolder holder = new ChannelViewHolder(view);
            view.setOnClickListener(v -> {
                if (onChannelClickListener != null) {

                    int position = holder.getAdapterPosition();
                    onChannelClickListener.onChannelClick(followedChannels.get(position - 1));
                }
            });
            return holder;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == R.layout.channel_item_category) {
            if (position == 0) {
                ((CategoryViewHolder) holder).bind(holder.itemView.getContext().getString(R.string.my_subscribed_channels));
            }
        } else {
            ((ChannelViewHolder) holder).bind(followedChannels.get(position - 1));
        }
    }


    @Override
    public int getItemCount() {
        return 1 + (followedChannels == null ? 0 : followedChannels.size());
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return R.layout.channel_item_category;
        } else {
            return R.layout.channel_item;
        }
    }

    public interface OnChannelClickListener {
        void onChannelClick(ChannelInfo channelInfo);
    }
}
