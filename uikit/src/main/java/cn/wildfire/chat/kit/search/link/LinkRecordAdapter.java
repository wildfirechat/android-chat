package cn.wildfire.chat.kit.search.link;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import cn.wildfire.chat.kit.R;

public class LinkRecordAdapter extends RecyclerView.Adapter<LinkRecordAdapter.ViewHolder> {

    private List<ConversationLinkRecordViewModel.LinkItem> linkItems;
    private OnLinkClickListener listener;

    public interface OnLinkClickListener {
        void onLinkClick(ConversationLinkRecordViewModel.LinkItem linkItem);
    }

    public LinkRecordAdapter(List<ConversationLinkRecordViewModel.LinkItem> linkItems,
                            OnLinkClickListener listener) {
        this.linkItems = linkItems;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_link_record, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ConversationLinkRecordViewModel.LinkItem item = linkItems.get(position);

        holder.titleTextView.setText(item.title);
        holder.digestTextView.setText(item.contentDigest);
        holder.urlTextView.setText(item.url);

        if (item.thumbnailUrl != null && !item.thumbnailUrl.isEmpty()) {
            holder.thumbnailImageView.setVisibility(View.VISIBLE);
            Glide.with(holder.itemView.getContext())
                    .load(item.thumbnailUrl)
                    .centerCrop()
                    .placeholder(R.drawable.image_chat_placeholder)
                    .into(holder.thumbnailImageView);
        } else {
            holder.thumbnailImageView.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onLinkClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return linkItems != null ? linkItems.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView digestTextView;
        TextView urlTextView;
        ImageView thumbnailImageView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            digestTextView = itemView.findViewById(R.id.digestTextView);
            urlTextView = itemView.findViewById(R.id.urlTextView);
            thumbnailImageView = itemView.findViewById(R.id.thumbnailImageView);
        }
    }
}
