package cn.wildfire.chat.kit.channel;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import cn.wildfire.chat.kit.channel.viewholder.CategoryViewHolder;
import cn.wildfire.chat.kit.channel.viewholder.ChannelViewHolder;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.model.ChannelInfo;

public class ChannelListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<ChannelInfo> createdChannels;
    private List<ChannelInfo> followedChannels;
    private OnChannelClickListener onChannelClickListener;

    public void setCreatedChannels(List<ChannelInfo> createdChannels) {
        this.createdChannels = createdChannels;
    }

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
                    if (createdChannels == null || createdChannels.isEmpty()) {
                        onChannelClickListener.onChannelClick(followedChannels.get(position - 2));
                    } else {
                        if (position > createdChannels.size()) {
                            onChannelClickListener.onChannelClick(followedChannels.get(position - 2 - createdChannels.size()));
                        } else {
                            onChannelClickListener.onChannelClick(createdChannels.get(position - 1));
                        }
                    }
                }
            });
            return holder;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == R.layout.channel_item_category) {
            if (position == 0) {
                ((CategoryViewHolder) holder).bind("我创建的频道");
            } else {
                ((CategoryViewHolder) holder).bind("我订阅的频道");
            }
        } else {
            if (createdChannels == null || createdChannels.isEmpty()) {
                ((ChannelViewHolder) holder).bind(followedChannels.get(position - 2));
            } else {
                if (position > createdChannels.size()) {
                    ((ChannelViewHolder) holder).bind(followedChannels.get(position - 2 - createdChannels.size()));
                } else {
                    ((ChannelViewHolder) holder).bind(createdChannels.get(position - 1));
                }
            }
        }
    }


    @Override
    public int getItemCount() {
        return 2 + (createdChannels == null ? 0 : createdChannels.size()) + (followedChannels == null ? 0 : followedChannels.size());
    }

    @Override
    public int getItemViewType(int position) {
        if (createdChannels == null || createdChannels.isEmpty()) {
            int type;
            switch (position) {
                case 0:
                case 1:
                    type = R.layout.channel_item_category;
                    break;
                default:
                    type = R.layout.channel_item;
                    break;
            }
            return type;
        } else {
            if (position == 0 || position == createdChannels.size() + 1) {
                return R.layout.channel_item_category;
            } else {
                return R.layout.channel_item;
            }
        }
    }

    public interface OnChannelClickListener {
        void onChannelClick(ChannelInfo channelInfo);
    }
}
