/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.ext;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.annotation.ExtContextMenuItem;
import cn.wildfire.chat.kit.contact.ContactListActivity;
import cn.wildfire.chat.kit.conversation.ext.core.ConversationExt;
import cn.wildfire.chat.kit.conversation.forward.ForwardPromptView;
import cn.wildfire.chat.kit.organization.model.Employee;
import cn.wildfirechat.message.CardMessageContent;
import cn.wildfirechat.message.TextMessageContent;
import cn.wildfirechat.model.ChannelInfo;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.SecretChatInfo;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

public class UserCardExt extends ConversationExt {

    /**
     * @param containerView 扩展view的container
     * @param conversation
     */
    @ExtContextMenuItem
    public void pickContact(View containerView, Conversation conversation) {
        Intent intent = new Intent(fragment.getActivity(), ContactListActivity.class);
        ArrayList<String> filterUserList = new ArrayList<>();
        if (conversation.type == Conversation.ConversationType.Single) {
            filterUserList.add(conversation.target);
            intent.putExtra(ContactListActivity.FILTER_USER_LIST, filterUserList);
        } else if (conversation.type == Conversation.ConversationType.SecretChat) {
            SecretChatInfo secretChatInfo = ChatManager.Instance().getSecretChatInfo(conversation.target);
            if (secretChatInfo != null) {
                filterUserList.add(secretChatInfo.getUserId());
                intent.putExtra(ContactListActivity.FILTER_USER_LIST, filterUserList);
            }
        }
        startActivityForResult(intent, 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            UserInfo userInfo = data.getParcelableExtra("userInfo");
            ChannelInfo channelInfo = data.getParcelableExtra("channelInfo");
            Employee employee = data.getParcelableExtra("employee");
            if (userInfo != null) {
                sendUserCard(0, userInfo.uid, userInfo.name, userInfo.displayName, userInfo.portrait);
            } else if (channelInfo != null) {
                sendUserCard(3, channelInfo.channelId, channelInfo.name, channelInfo.name, channelInfo.portrait);
            } else if (employee != null) {
                sendUserCard(0, employee.employeeId, employee.name, employee.name, employee.portraitUrl);
            }
        }
    }

    /**
     * @param type
     * @param target
     * @param name        用户名，只有显示联系人名片时有效
     * @param displayName
     * @param portrait
     */
    private void sendUserCard(int type, String target, String name, String displayName, String portrait) {
        ForwardPromptView view = new ForwardPromptView(fragment.getActivity());
        String desc = "";
        switch (type) {
            case 0:
                desc = fragment.getString(R.string.user_card_personal);
                break;
            case 1:
                desc = fragment.getString(R.string.user_card_group);
                break;
            case 2:
                desc = fragment.getString(R.string.user_card_chatroom);
                break;
            case 3:
                desc = fragment.getString(R.string.user_card_channel);
                break;
            default:
                break;
        }

        desc += displayName;

        if (conversation.type == Conversation.ConversationType.Single) {
            UserInfo targetUser = ChatManager.Instance().getUserInfo(conversation.target, false);
            view.bind(targetUser.displayName, portrait, desc);
        } else if (conversation.type == Conversation.ConversationType.Group) {
            GroupInfo groupInfo = ChatManager.Instance().getGroupInfo(conversation.target, false);
            view.bind(!TextUtils.isEmpty(groupInfo.remark) ? groupInfo.remark : groupInfo.name, groupInfo.portrait, desc);
        }
        MaterialDialog dialog = new MaterialDialog.Builder(fragment.getActivity())
            .customView(view, false)
            .negativeText(R.string.user_card_cancel)
            .positiveText(R.string.user_card_send)
            .onPositive(new MaterialDialog.SingleButtonCallback() {
                @Override
                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                    CardMessageContent cardMessageContent = new CardMessageContent(type, target, displayName, portrait, ChatManager.Instance().getUserId());
                    cardMessageContent.setName(name);
                    messageViewModel.sendMessage(conversation, toUsers(), cardMessageContent);
                    if (!TextUtils.isEmpty(view.getEditText())) {
                        TextMessageContent content = new TextMessageContent(view.getEditText());
                        messageViewModel.sendMessage(conversation, toUsers(), content);
                    }
                    dialog.dismiss();
                }
            })
            .build();
        dialog.show();
    }

    @Override
    public int priority() {
        return 100;
    }

    @Override
    public int iconResId() {
        return R.mipmap.ic_user_card;
    }

    @Override
    public String title(Context context) {
        return context.getString(R.string.user_card_title);
    }

    @Override
    public String contextMenuTitle(Context context, String tag) {
        return title(context);
    }
}
