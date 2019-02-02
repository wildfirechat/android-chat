package cn.wildfire.chat.kit.search.module;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import cn.wildfire.chat.kit.conversation.ConversationActivity;
import cn.wildfire.chat.kit.search.SearchableModule;
import cn.wildfire.chat.kit.search.viewHolder.MessageViewHolder;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.remote.ChatManager;

public class ConversationMessageSearchModule extends SearchableModule<Message, MessageViewHolder> {
    private Conversation conversation;

    public ConversationMessageSearchModule(Conversation conversation) {
        this.conversation = conversation;
    }

    @Override
    public MessageViewHolder onCreateViewHolder(Fragment fragment, @NonNull ViewGroup parent, int type) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.search_item_message, parent, false);
        return new MessageViewHolder(fragment, view);
    }

    @Override
    public void onBind(Fragment fragment, MessageViewHolder holder, Message message) {
        holder.onBind(message);
    }

    @Override
    public int getViewType(Message message) {
        return R.layout.search_item_message;
    }

    @Override
    public void onClick(Fragment fragment, MessageViewHolder holder, View view, Message message) {
        Intent intent = new Intent(fragment.getContext(), ConversationActivity.class);
        intent.putExtra("conversation", message.conversation);
        intent.putExtra("toFocusMessageId", message.messageId);
        fragment.startActivity(intent);
        fragment.getActivity().finish();
    }

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public String category() {
        return "聊天记录";
    }

    @Override
    public List<Message> search(String keyword) {
        return ChatManager.Instance().searchMessage(conversation, keyword);
    }

    @Override
    public boolean expandable() {
        return false;
    }
}
