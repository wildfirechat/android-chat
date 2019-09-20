package cn.wildfire.chat.kit.search.module;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Arrays;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import cn.wildfire.chat.kit.conversation.ConversationActivity;
import cn.wildfire.chat.kit.search.SearchMessageActivity;
import cn.wildfire.chat.kit.search.SearchableModule;
import cn.wildfire.chat.kit.search.viewHolder.ConversationViewHolder;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.model.ConversationSearchResult;
import cn.wildfirechat.remote.ChatManager;

import static cn.wildfirechat.model.Conversation.ConversationType.Group;
import static cn.wildfirechat.model.Conversation.ConversationType.Single;

public class ConversationSearchModule extends SearchableModule<ConversationSearchResult, ConversationViewHolder> {
    @Override
    public ConversationViewHolder onCreateViewHolder(Fragment fragment, @NonNull ViewGroup parent, int type) {
        View itemView;
        itemView = LayoutInflater.from(fragment.getActivity()).inflate(R.layout.search_item_conversation, parent, false);
        return new ConversationViewHolder(fragment, itemView);
    }

    @Override
    public void onBind(Fragment fragment, ConversationViewHolder holder, ConversationSearchResult conversationSearchResult) {
        holder.onBind(keyword, conversationSearchResult);
    }

    @Override
    public int getViewType(ConversationSearchResult conversationSearchResult) {
        return R.layout.search_item_conversation;
    }

    @Override
    public int priority() {
        return 80;
    }

    @Override
    public void onClick(Fragment fragment, ConversationViewHolder holder, View view, ConversationSearchResult conversationSearchResult) {
        if (conversationSearchResult.marchedCount == 1) {
            // TODO hide fragment
            Intent intent = new Intent(fragment.getActivity(), ConversationActivity.class);
            intent.putExtra("conversation", conversationSearchResult.conversation);
            intent.putExtra("toFocusMessageId", conversationSearchResult.marchedMessage.messageId);
            fragment.startActivity(intent);
            fragment.getActivity().finish();
        } else {
            Intent intent = new Intent(fragment.getActivity(), SearchMessageActivity.class);
            intent.putExtra("conversation", conversationSearchResult.conversation);
            intent.putExtra("keyword", keyword);
            fragment.startActivity(intent);
        }
    }

    @Override
    public String category() {
        return "聊天记录";
    }

    @Override
    public List<ConversationSearchResult> search(String keyword) {
        return ChatManager.Instance().searchConversation(keyword, Arrays.asList(Single, Group), Arrays.asList(0, 1));
    }
}
