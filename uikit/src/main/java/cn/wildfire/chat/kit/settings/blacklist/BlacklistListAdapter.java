package cn.wildfire.chat.kit.settings.blacklist;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import cn.wildfirechat.chat.R;
import cn.wildfirechat.chat.R2;

public class BlacklistListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<String> blackedUserIds;
    private OnBlacklistItemClickListener onBlacklistItemClickListener;

    public void setBlackedUserIds(List<String> blackedUserIds) {
        this.blackedUserIds = blackedUserIds;
    }

    public List<String> getBlackedUserIds() {
        return blackedUserIds;
    }

    public void setOnBlacklistItemClickListener(OnBlacklistItemClickListener listener) {
        this.onBlacklistItemClickListener = listener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.blacklist_item, parent, false);
        RecyclerView.ViewHolder holder = new BlacklistViewHolder(view);
        view.setOnClickListener(v -> {
            if (onBlacklistItemClickListener != null) {
                int position = holder.getAdapterPosition();
                onBlacklistItemClickListener.onItemClick(blackedUserIds.get(position), v);
            }
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ((BlacklistViewHolder) holder).bind(blackedUserIds.get(position));
    }


    @Override
    public int getItemCount() {
        return blackedUserIds == null ? 0 : blackedUserIds.size();
    }

    @Override
    public int getItemViewType(int position) {
        return R.layout.blacklist_item;
    }

    public interface OnBlacklistItemClickListener {
        void onItemClick(String userId, View view);
    }
}
