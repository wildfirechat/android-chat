package cn.wildfire.chat.search;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import cn.wildfirechat.chat.R;

import java.util.List;

import cn.wildfire.chat.search.viewHolder.MessageViewHolder;
import cn.wildfirechat.message.Message;

public class SearchMessageResultAdapter extends RecyclerView.Adapter<MessageViewHolder> {

    private OnMessageClickListener listener;
    private Fragment fragment;
    private List<Message> messages;

    public SearchMessageResultAdapter(Fragment fragment) {
        this.fragment = fragment;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
        notifyDataSetChanged();
    }

    public void reset() {
        this.messages = null;
        notifyDataSetChanged();
    }

    public void setOnMessageClickListener(OnMessageClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.search_item_message, parent, false);
        MessageViewHolder holder = new MessageViewHolder(fragment, view);
        processOnClick(holder);
        return holder;
    }

    private void processOnClick(RecyclerView.ViewHolder holder) {
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    Message message = messages.get(holder.getAdapterPosition());
                    listener.onMessageClick(message);
                }
            }
        });
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        holder.onBind(messages.get(position));
    }

    @Override
    public int getItemCount() {
        return messages == null ? 0 : messages.size();
    }

    public interface OnMessageClickListener {
        void onMessageClick(Message message);
    }
}
