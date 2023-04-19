/*
 * Copyright (c) 2022 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfirechat.message.JoinCallRequestMessageContent;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.MultiCallOngoingMessageContent;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

public class OngoingCallAdapter extends RecyclerView.Adapter<OngoingCallAdapter.OngoingCallViewHolder> {

    private List<Message> ongoingCalls;

    public void setOngoingCalls(List<Message> ongoingCalls) {
        this.ongoingCalls = ongoingCalls;
    }

    @NonNull
    @Override
    public OngoingCallViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.conversation_item_ongoing_call, parent, false);
        return new OngoingCallViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OngoingCallViewHolder holder, int position) {
        Message message = ongoingCalls.get(position);
        MultiCallOngoingMessageContent ongoingMessageContent = (MultiCallOngoingMessageContent) message.content;
        UserInfo userInfo = ChatManager.Instance().getUserInfo(ongoingMessageContent.getInitiator(), false);
        holder.descTextView.setText(userInfo.displayName + " 发起的通话");

        holder.joinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                JoinCallRequestMessageContent messageContent = new JoinCallRequestMessageContent(ongoingMessageContent.getCallId(), ChatManager.Instance().getClientId());
                ChatManager.Instance().sendMessage(message.conversation, messageContent, new String[]{ongoingMessageContent.getInitiator()}, 0, null);
            }
        });
    }

    @Override
    public int getItemCount() {
        return ongoingCalls != null ? ongoingCalls.size() : 0;
    }

    static class OngoingCallViewHolder extends RecyclerView.ViewHolder {
        TextView descTextView;
        Button joinButton;

        public OngoingCallViewHolder(@NonNull View itemView) {
            super(itemView);
            bindViews(itemView);
        }

        private void bindViews(View itemView) {
            descTextView = itemView.findViewById(R.id.descTextView);
            joinButton = itemView.findViewById(R.id.joinButton);
        }
    }
}
