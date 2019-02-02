package cn.wildfire.chat.kit.conversation;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import cn.wildfirechat.model.Conversation;

public class ConversationViewModelFactory extends ViewModelProvider.NewInstanceFactory {
    private Conversation conversation;
    private String channelPrivateChatUser;

    public ConversationViewModelFactory(Conversation conversation) {
        this.conversation = conversation;
        this.channelPrivateChatUser = null;
    }

    public ConversationViewModelFactory(Conversation conversation, String channelPrivateChatUser) {
        super();
        this.conversation = conversation;
        this.channelPrivateChatUser = channelPrivateChatUser;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new ConversationViewModel(conversation, channelPrivateChatUser);
    }
}
