package cn.wildfire.chat.moment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.moment.third.interfaces.OnFeedUserClickListener;
import cn.wildfire.chat.kit.R;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.MessageContent;
import cn.wildfirechat.moment.message.FeedCommentMessageContent;
import cn.wildfirechat.moment.message.FeedMessageContent;

public class FeedMessagAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<Message> feedMessages;
    private OnFeedMessageClickListener onFeedMessageClickListener;
    private OnFeedMessageLongClickListener onFeedMessageLongClickListener;
    private OnFeedUserClickListener onFeedUserClickListener;
    private OnLoadMoreMessageClickListener onLoadMoreMessageClickListener;

    public void setOnFeedMessageClickListener(OnFeedMessageClickListener onFeedMessageClickListener) {
        this.onFeedMessageClickListener = onFeedMessageClickListener;
    }

    public void setOnFeedMessageLongClickListener(OnFeedMessageLongClickListener onFeedMessageLongClickListener) {
        this.onFeedMessageLongClickListener = onFeedMessageLongClickListener;
    }

    public void setOnFeedUserClickListener(OnFeedUserClickListener onFeedUserClickListener) {
        this.onFeedUserClickListener = onFeedUserClickListener;
    }

    public void setOnLoadMoreMessageClickListener(OnLoadMoreMessageClickListener onLoadMoreMessageClickListener) {
        this.onLoadMoreMessageClickListener = onLoadMoreMessageClickListener;
    }

    public List<Message> getFeedMessages() {
        return feedMessages;
    }

    public void setFeedMessages(List<Message> feedMessages) {
        this.feedMessages = feedMessages;
    }

    public void addFeedMessages(List<Message> feedMessages) {
        if (feedMessages == null) {
            return;
        }
        if (this.feedMessages == null) {
            this.feedMessages = new ArrayList<>();
        }
        this.feedMessages.addAll(feedMessages);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder holder;
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == R.layout.moment_message_item) {
            View view = inflater.inflate(R.layout.moment_message_item, parent, false);
            holder = new FeedMessageViewHolder(view);
            view.setOnClickListener(v -> onFeedMessageClick(holder));
            view.setOnLongClickListener(v -> onFeedMessageLongClick(holder));
        } else {
            View view = inflater.inflate(R.layout.moment_message_load_more, parent, false);
            holder = new FeedMessageLoadMoreViewHolder(view);
            view.setOnClickListener(v -> onLoadMoreFeedMessageClick(holder));
        }
        return holder;
    }

    private void onFeedMessageClick(RecyclerView.ViewHolder holder) {
        if (onFeedMessageClickListener == null) {
            return;
        }
        Message message = feedMessages.get(holder.getAdapterPosition());
        MessageContent content = message.content;
        if (content instanceof FeedMessageContent) {
            onFeedMessageClickListener.onFeedMessageClick(((FeedMessageContent) content).getFeedId());
        } else if (content instanceof FeedCommentMessageContent) {
            onFeedMessageClickListener.onFeedCommentMessageClick(((FeedCommentMessageContent) content).getFeedId(), ((FeedCommentMessageContent) content).getCommentId());
        }
    }

    private boolean onFeedMessageLongClick(RecyclerView.ViewHolder holder) {
        if (onFeedMessageLongClickListener == null) {
            return false;
        }
        Message message = feedMessages.get(holder.getAdapterPosition());
        MessageContent content = message.content;
        if (content instanceof FeedMessageContent) {
            onFeedMessageLongClickListener.onFeedMessageLongClick(((FeedMessageContent) content).getFeedId());
        } else if (content instanceof FeedCommentMessageContent) {
            onFeedMessageLongClickListener.onFeedCommentMessageLongClick(((FeedCommentMessageContent) content).getFeedId(), ((FeedCommentMessageContent) content).getCommentId());
        }
        return true;
    }

    private void onLoadMoreFeedMessageClick(RecyclerView.ViewHolder holder) {
        if (onLoadMoreMessageClickListener != null) {
            onLoadMoreMessageClickListener.onLoadMoreFeedMessageClick();
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof FeedMessageViewHolder) {
            ((FeedMessageViewHolder) holder).onBind(feedMessages.get(position));
        } else {
            // do nothing
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position < feedMessages.size()) {
            return R.layout.moment_message_item;
        } else {
            return 0;
        }
    }

    @Override
    public int getItemCount() {
        return (feedMessages != null && !feedMessages.isEmpty()) ? feedMessages.size() + 1 : 0;
    }

    public interface OnLoadMoreMessageClickListener {
        void onLoadMoreFeedMessageClick();
    }

    public interface OnFeedMessageClickListener {
        void onFeedMessageClick(long feedId);

        void onFeedCommentMessageClick(long feedId, long commentId);
    }

    public interface OnFeedMessageLongClickListener {
        void onFeedMessageLongClick(long feedId);

        void onFeedCommentMessageLongClick(long feedId, long commentId);
    }
}
