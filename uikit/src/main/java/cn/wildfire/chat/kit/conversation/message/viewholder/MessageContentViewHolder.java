/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.message.viewholder;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import cn.wildfire.chat.kit.*;
import cn.wildfire.chat.kit.conversation.ConversationFragment;
import cn.wildfire.chat.kit.conversation.ConversationMessageAdapter;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfire.chat.kit.third.utils.TimeUtils;
import cn.wildfire.chat.kit.viewmodel.MessageViewModel;
import cn.wildfirechat.message.Message;

public abstract class MessageContentViewHolder extends RecyclerView.ViewHolder {
    @NonNull
    protected ConversationFragment fragment;
    protected View itemView;
    protected UiMessage message;
    protected int position;
    protected RecyclerView.Adapter adapter;
    protected MessageViewModel messageViewModel;

    TextView timeTextView;


    public MessageContentViewHolder(@NonNull ConversationFragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(itemView);
        this.fragment = fragment;
        this.itemView = itemView;
        this.adapter = adapter;
        messageViewModel = new ViewModelProvider(fragment).get(MessageViewModel.class);
        bindViews(itemView);
    }

    private void bindViews(View itemView) {
        timeTextView = itemView.findViewById(R.id.timeTextView);
    }

    public void onBind(UiMessage message, int position) {
        this.message = message;
        this.position = position;
        setMessageTime(message.message, position);
    }

    /**
     * @param uiMessage 消息
     * @param tag       菜单项 tag
     * @return 返回true，将从context menu中排除
     */
    public abstract boolean contextMenuItemFilter(UiMessage uiMessage, String tag);

    public abstract String contextMenuTitle(Context context, String tag);

    public abstract int contextMenuIcon(Context context, String tag);

    public abstract String contextConfirmPrompt(Context context, String tag);

    public void onViewRecycled() {
        // you can do some clean up here
    }

    protected void setMessageTime(Message item, int position) {
        long msgTime = item.serverTime;
        if (position > 0) {
            Message preMsg = ((ConversationMessageAdapter) adapter).getItem(position - 1).message;
            long preMsgTime = preMsg.serverTime;
            if (msgTime - preMsgTime > (5 * 60 * 1000)) {
                timeTextView.setVisibility(View.VISIBLE);
                timeTextView.setText(TimeUtils.getMsgFormatTime(msgTime));
            } else {
                timeTextView.setVisibility(View.GONE);
            }
        } else {
            timeTextView.setVisibility(View.VISIBLE);
            timeTextView.setText(TimeUtils.getMsgFormatTime(msgTime));
        }
    }

}
