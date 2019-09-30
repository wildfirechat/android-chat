package cn.wildfire.chat.kit.conversation.message.viewholder;

import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import cn.wildfire.chat.kit.conversation.ConversationFragment;

public abstract class NotificationMessageContentViewHolder extends MessageContentViewHolder {
    public NotificationMessageContentViewHolder(ConversationFragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(fragment, adapter, itemView);
    }
}
