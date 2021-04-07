/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.multimsg;

import android.content.Context;
import android.view.View;

import androidx.lifecycle.ViewModelProvider;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfire.chat.kit.viewmodel.MessageViewModel;

public class DeleteMultiMessageAction extends MultiMessageAction {

    @Override
    public void onClick(List<UiMessage> messages) {
        MessageViewModel messageViewModel = new ViewModelProvider(fragment).get(MessageViewModel.class);
        new MaterialDialog.Builder(fragment.getContext())
            .items("删除本地消息", "删除远程消息")
            .itemsCallback(new MaterialDialog.ListCallback() {
                @Override
                public void onSelection(MaterialDialog dialog, View itemView, int position, CharSequence text) {
                    if (position == 0) {
                        for (UiMessage message : messages) {
                            messageViewModel.deleteMessage(message.message);
                        }
                    } else {
                        for (UiMessage message : messages) {
                            messageViewModel.deleteRemoteMessage(message.message);
                        }
                    }
                }
            })
            .show();
    }

    @Override
    public int iconResId() {
        return R.mipmap.ic_delete;
    }

    @Override
    public String title(Context context) {
        return "删除";
    }

    @Override
    public boolean confirm() {
        return true;
    }

    @Override
    public String confirmPrompt() {
        return "确认删除?";
    }
}
