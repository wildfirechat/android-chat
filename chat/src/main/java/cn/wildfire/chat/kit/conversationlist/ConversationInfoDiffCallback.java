package cn.wildfire.chat.kit.conversationlist;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import cn.wildfirechat.model.ConversationInfo;

public class ConversationInfoDiffCallback extends DiffUtil.ItemCallback<Object> {

    @Override
    public boolean areItemsTheSame(@NonNull Object oldItem, @NonNull Object newItem) {
        if (oldItem.getClass() != newItem.getClass()) {
            return false;
        }
        if (oldItem instanceof ConversationInfo) {
            ConversationInfo oldCv = (ConversationInfo) oldItem;
            ConversationInfo newCv = (ConversationInfo) newItem;
            return oldCv.conversation.equals(newCv.conversation);
        }
        return oldItem.equals(newItem);
    }

    @SuppressLint("DiffUtilEquals")
    @Override
    public boolean areContentsTheSame(@NonNull Object oldItem, @NonNull Object newItem) {
        return oldItem.equals(newItem);
    }
}
