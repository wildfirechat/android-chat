package cn.wildfire.chat.kit.conversation.multimsg;

import android.content.Context;

import androidx.lifecycle.ViewModelProvider;

import java.util.List;

import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfire.chat.kit.viewmodel.MessageViewModel;
import cn.wildfirechat.chat.R;

public class DeleteMultiMessageAction extends MultiMessageAction {

    @Override
    public void onClick(List<UiMessage> messages) {
        MessageViewModel messageViewModel = new ViewModelProvider(conversationActivity).get(MessageViewModel.class);
        for (UiMessage message : messages) {
            messageViewModel.deleteMessage(message.message);
        }
    }

    @Override
    public int iconResId() {
        return R.mipmap.ic_delete;
    }

    @Override
    public String title(Context context) {
        return "删除";
    }
}
