/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.forward;

import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.common.OperateResult;
import cn.wildfire.chat.kit.conversation.pick.PickOrCreateConversationActivity;
import cn.wildfire.chat.kit.group.GroupViewModel;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfire.chat.kit.utils.WfcTextUtils;
import cn.wildfirechat.message.CompositeMessageContent;
import cn.wildfirechat.message.ImageMessageContent;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.TextMessageContent;
import cn.wildfirechat.message.VideoMessageContent;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.UserInfo;

public class ForwardActivity extends PickOrCreateConversationActivity {
    private List<Message> messages;
    private ForwardViewModel forwardViewModel;
    private UserViewModel userViewModel;
    private GroupViewModel groupViewModel;

    @Override
    protected void afterViews() {
        super.afterViews();
        messages = getIntent().getParcelableArrayListExtra("messages");
        if (messages == null || messages.isEmpty()) {
            Message message = getIntent().getParcelableExtra("message");
            if (message != null) {
                messages = new ArrayList<>();
                messages.add(message);
            }
        }
        if (messages == null || messages.isEmpty()) {
            finish();
        }
        forwardViewModel = ViewModelProviders.of(this).get(ForwardViewModel.class);
        userViewModel = ViewModelProviders.of(this).get(UserViewModel.class);
        groupViewModel = ViewModelProviders.of(this).get(GroupViewModel.class);
    }

    @Override
    protected void onPickOrCreateConversation(Conversation conversation) {
        forward(conversation);
    }

    public void forward(Conversation conversation) {
        switch (conversation.type) {
            case Single:
                UserInfo userInfo = userViewModel.getUserInfo(conversation.target, false);
                forward(userInfo.displayName, userInfo.portrait, conversation);
                break;
            case Group:
                GroupInfo groupInfo = groupViewModel.getGroupInfo(conversation.target, false);
                forward(!TextUtils.isEmpty(groupInfo.remark) ? groupInfo.remark : groupInfo.name, groupInfo.portrait, conversation);
                break;
            default:
                break;
        }

    }

    private void forward(String targetName, String targetPortrait, Conversation targetConversation) {
        ForwardPromptView view = new ForwardPromptView(this);
        if (messages.size() == 1) {
            Message message = messages.get(0);
            if (message.content instanceof ImageMessageContent) {
                view.bind(targetName, targetPortrait, ((ImageMessageContent) message.content).getThumbnail());
            } else if (message.content instanceof VideoMessageContent) {
                view.bind(targetName, targetPortrait, ((VideoMessageContent) message.content).getThumbnail());
            } else if (message.content instanceof CompositeMessageContent) {
                view.bind(targetName, targetPortrait, getString(R.string.forward_chat_record, ((CompositeMessageContent) message.content).getTitle()));
            } else {
                view.bind(targetName, targetPortrait, WfcTextUtils.htmlToText(message.digest()));
            }
        } else {
            view.bind(targetName, targetPortrait, getString(R.string.forward_batch_messages, messages.size()));
        }
        MaterialDialog dialog = new MaterialDialog.Builder(this)
            .customView(view, false)
            .negativeText(R.string.forward_cancel)
            .positiveText(R.string.forward_send)
            .onPositive(new MaterialDialog.SingleButtonCallback() {
                @Override
                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                    Message extraMsg = null;
                    if (!TextUtils.isEmpty(view.getEditText())) {
                        TextMessageContent content = new TextMessageContent(view.getEditText());
                        extraMsg = new Message();
                        extraMsg.content = content;
                    }
                    if (extraMsg != null) {
                        messages.add(extraMsg);
                    }
                    forwardViewModel.forward(targetConversation, messages.toArray(new Message[0]))
                        .observe(ForwardActivity.this, new Observer<OperateResult<Integer>>() {
                            @Override
                            public void onChanged(@Nullable OperateResult<Integer> integerOperateResult) {
                                if (integerOperateResult.isSuccess()) {
                                    Toast.makeText(ForwardActivity.this, R.string.forward_success, Toast.LENGTH_SHORT).show();
                                    finish();
                                } else {
                                    Toast.makeText(ForwardActivity.this, getString(R.string.forward_failed, integerOperateResult.getErrorCode()), Toast.LENGTH_SHORT).show();
                                }
                            }
                        });

                }
            })
            .build();
        dialog.show();
    }
}
