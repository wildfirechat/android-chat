package cn.wildfire.chat.conversationlist;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.annotation.NonNull;

import java.util.List;

import cn.wildfirechat.model.Conversation;

public class ConversationListViewModelFactory extends ViewModelProvider.NewInstanceFactory {
    private List<Conversation.ConversationType> types;
    private List<Integer> lines;

    public ConversationListViewModelFactory(List<Conversation.ConversationType> types, List<Integer> lines) {
        this.types = types;
        this.lines = lines;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new ConversationListViewModel(this.types, this.lines);
    }
}
