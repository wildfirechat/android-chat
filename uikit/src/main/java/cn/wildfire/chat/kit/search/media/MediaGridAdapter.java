package cn.wildfire.chat.kit.search.media;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

import java.util.List;

import cn.wildfire.chat.kit.R;

public class MediaGridAdapter extends RecyclerView.Adapter<MediaGridAdapter.ViewHolder> {

    private List<ConversationMediaViewModel.MediaItem> items;
    private ConversationMediaAdapter.OnMediaClickListener listener;

    private RequestOptions placeholderOptions = new RequestOptions();

    public MediaGridAdapter(List<ConversationMediaViewModel.MediaItem> items,
                            ConversationMediaAdapter.OnMediaClickListener listener) {
        this.items = items;
        this.listener = listener;
        placeholderOptions.diskCacheStrategy(DiskCacheStrategy.ALL);
        placeholderOptions.centerCrop();
        placeholderOptions.placeholder(R.drawable.image_chat_placeholder);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_media_grid, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ConversationMediaViewModel.MediaItem item = items.get(position);

        holder.videoIndicator.setVisibility(item.isVideo() ? View.VISIBLE : View.GONE);

        loadMedia(item.thumbnail, item.getImagePath(), holder.imageView);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMediaClick(item);
            }
        });
    }

    private void loadMedia(android.graphics.Bitmap thumbnail, String imagePath, ImageView imageView) {
        RequestBuilder<Drawable> thumbnailRequest = null;
        if (thumbnail != null) {
            thumbnailRequest = Glide
                    .with(imageView.getContext())
                    .load(thumbnail);
        } else {
            thumbnailRequest = Glide
                    .with(imageView.getContext())
                    .load(R.drawable.image_chat_placeholder);
        }
        Glide.with(imageView.getContext())
                .load(imagePath)
                .thumbnail(thumbnailRequest)
                .apply(placeholderOptions)
                .into(imageView);
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ImageView videoIndicator;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
            videoIndicator = itemView.findViewById(R.id.videoIndicator);
        }
    }
}

