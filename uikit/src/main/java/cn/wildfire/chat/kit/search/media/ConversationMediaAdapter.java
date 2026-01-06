package cn.wildfire.chat.kit.search.media;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import cn.wildfire.chat.kit.R;

public class ConversationMediaAdapter extends RecyclerView.Adapter<ConversationMediaAdapter.ViewHolder> {

    private List<ConversationMediaViewModel.MediaMonthGroup> groups;
    private OnMediaClickListener onMediaClickListener;

    public interface OnMediaClickListener {
        void onMediaClick(ConversationMediaViewModel.MediaItem mediaItem);
    }

    public ConversationMediaAdapter(List<ConversationMediaViewModel.MediaMonthGroup> groups,
                                    OnMediaClickListener listener) {
        this.groups = groups;
        this.onMediaClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_media_month_group, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ConversationMediaViewModel.MediaMonthGroup group = groups.get(position);
        holder.titleTextView.setText(group.getTitle());

        GridLayoutManager gridLayoutManager = new GridLayoutManager(
                holder.itemView.getContext(), 3);
        holder.mediaRecyclerView.setLayoutManager(gridLayoutManager);

        MediaGridAdapter gridAdapter = new MediaGridAdapter(group.items, onMediaClickListener);
        holder.mediaRecyclerView.setAdapter(gridAdapter);
    }

    @Override
    public int getItemCount() {
        return groups != null ? groups.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        RecyclerView mediaRecyclerView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            mediaRecyclerView = itemView.findViewById(R.id.mediaRecyclerView);
        }
    }
}
