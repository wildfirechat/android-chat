package cn.wildfire.chat.kit.conversation.multimsg;

import android.content.Context;

import java.util.List;

import cn.wildfire.chat.kit.conversation.ConversationFragment;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfirechat.model.Conversation;

public abstract class MultiMessageAction {
    protected Conversation conversation;
    protected ConversationFragment fragment;

    public MultiMessageAction() {
    }

    public final void onBind(ConversationFragment fragment, Conversation conversation) {
        this.fragment = fragment;
        this.conversation = conversation;

    }

    public abstract void onClick(List<UiMessage> messages);

    public int priority() {
        return 0;
    }

    public boolean confirm() {
        return false;
    }

    public boolean filter(Conversation conversation) {
        return false;
    }

    public abstract int iconResId();

    public abstract String title(Context context);

    public String confirmPrompt() {
        return "";
    }
}
