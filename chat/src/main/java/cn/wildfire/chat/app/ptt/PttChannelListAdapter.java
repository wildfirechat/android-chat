/*
 * Copyright (c) 2021 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.ptt;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import cn.wildfire.chat.kit.GlideApp;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.ptt.ChannelInfo;

public class PttChannelListAdapter extends RecyclerView.Adapter<PttChannelListAdapter.PttChannelItemViewHolder> {
    private List<ChannelInfo> channelInfos;
    private Context context;
    private OnChannelClickListener onChannelClickListener;

    PttChannelListAdapter(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public PttChannelItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.ptt_channel_item, parent, false);
        return new PttChannelItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PttChannelItemViewHolder viewHolder, int position) {
        ChannelInfo channelInfo = channelInfos.get(position);
        viewHolder.titleTextView.setText(channelInfo.name);
        GlideApp.with(context).load(channelInfo.portrait).placeholder(R.mipmap.avatar_def).into(viewHolder.portraitImageView);
    }

    @Override
    public int getItemCount() {
        return channelInfos == null ? 0 : channelInfos.size();
    }

    public void setChannelInfos(List<ChannelInfo> channelInfos) {
        this.channelInfos = channelInfos;
        this.notifyItemRangeChanged(0, channelInfos.size());
    }

    public void setOnChannelClickListener(OnChannelClickListener onChannelClickListener) {
        this.onChannelClickListener = onChannelClickListener;
    }

    class PttChannelItemViewHolder extends RecyclerView.ViewHolder {
        ImageView portraitImageView;
        TextView titleTextView;

        public PttChannelItemViewHolder(@NonNull View itemView) {
            super(itemView);
            portraitImageView = itemView.findViewById(R.id.portraitImageView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            itemView.setOnClickListener(v -> {
                if (onChannelClickListener != null) {
                    int position = getAdapterPosition();
                    onChannelClickListener.onChannelClick(channelInfos.get(position));
                }
            });
        }
    }

    interface OnChannelClickListener {
        void onChannelClick(ChannelInfo channelInfo);
    }
}
