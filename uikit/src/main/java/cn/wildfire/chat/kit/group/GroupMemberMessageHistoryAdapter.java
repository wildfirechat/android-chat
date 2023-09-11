/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.group;

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
import cn.wildfire.chat.kit.third.utils.TimeUtils;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.notification.NotificationMessageContent;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

public class GroupMemberMessageHistoryAdapter extends RecyclerView.Adapter<GroupMemberMessageHistoryAdapter.MessageViewHolder> {
    private List<Message> messages;
    private OnMessageClickListener onMessageClickListener;

    public GroupMemberMessageHistoryAdapter() {
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
        notifyDataSetChanged();
    }


    public void addMessages(List<Message> messages) {
        int startIndex = this.messages.size();
        this.messages.addAll(messages);
        notifyItemRangeInserted(startIndex, messages.size());
    }

    public List<Message> getMessages() {
        return this.messages;
    }

    public void setOnMessageClickListener(OnMessageClickListener onMessageClickListener) {
        this.onMessageClickListener = onMessageClickListener;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.group_member_message_history_item, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        holder.onBind(messages.get(position));
    }

    @Override
    public int getItemCount() {
        if (messages == null) {
            return 0;
        }
        return messages.size();
    }

    public class MessageViewHolder extends RecyclerView.ViewHolder {
        protected ImageView portraitImageView;
        protected TextView nameTextView;
        protected TextView contentTextView;
        protected TextView timeTextView;

        public MessageViewHolder(View itemView) {
            super(itemView);
            bindViews(itemView);
        }

        private void bindViews(View itemView) {
            portraitImageView = itemView.findViewById(R.id.portraitImageView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            contentTextView = itemView.findViewById(R.id.contentTextView);
            timeTextView = itemView.findViewById(R.id.timeTextView);
        }

        public void onBind(Message message) {
            UserInfo sender = ChatManager.Instance().getUserInfo(message.sender, false);
            if (sender != null) {
                String senderName;
                if (message.conversation.type == Conversation.ConversationType.Group) {
                    senderName = ChatManager.Instance().getGroupMemberDisplayName(message.conversation.target, sender.uid);
                } else {
                    senderName = ChatManager.Instance().getUserDisplayName(sender);
                }
                nameTextView.setText(senderName);
                Glide.with(portraitImageView).load(sender.portrait).placeholder(R.mipmap.avatar_def).into(portraitImageView);
            }
            if (message.content instanceof NotificationMessageContent) {
                contentTextView.setText(((NotificationMessageContent) message.content).formatNotification(message));
            } else {
                contentTextView.setText(message.digest());
            }
            timeTextView.setText(TimeUtils.getMsgFormatTime(message.serverTime));
        }
    }


    public interface OnMessageClickListener {
        void onUserMessageClick(Message message);
    }
}
