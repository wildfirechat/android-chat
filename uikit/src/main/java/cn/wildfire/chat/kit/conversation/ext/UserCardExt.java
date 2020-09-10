package cn.wildfire.chat.kit.conversation.ext;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.annotation.ExtContextMenuItem;
import cn.wildfire.chat.kit.contact.ContactListActivity;
import cn.wildfire.chat.kit.conversation.ext.core.ConversationExt;
import cn.wildfire.chat.kit.conversation.forward.ForwardPromptView;
import cn.wildfirechat.message.CardMessageContent;
import cn.wildfirechat.message.TextMessageContent;
import cn.wildfirechat.model.ChannelInfo;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.GroupInfo;
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
        startActivityForResult(intent, 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            UserInfo userInfo = data.getParcelableExtra("userInfo");
            ChannelInfo channelInfo = data.getParcelableExtra("channelInfo");
            if (userInfo != null) {
                sendUserCard(0, userInfo.uid, userInfo.name, userInfo.displayName, userInfo.portrait);
            } else if (channelInfo != null) {
                sendUserCard(3, channelInfo.channelId, channelInfo.name, channelInfo.name, channelInfo.portrait);
            }
        }
    }

    private void sendUserCard(int type, String target, String name, String displayName, String portrait) {
        ForwardPromptView view = new ForwardPromptView(fragment.getActivity());
        String desc = "";
        switch (type) {
            case 0:
                desc = "[个人名片]";
                break;
            case 1:
                desc = "[群组]";
                break;
            case 2:
                desc = "[聊天室]";
                break;
            case 3:
                desc = "[频道]";
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
            view.bind(groupInfo.name, groupInfo.portrait, desc);
        }
        MaterialDialog dialog = new MaterialDialog.Builder(fragment.getActivity())
            .customView(view, false)
            .negativeText("取消")
            .positiveText("发送")
            .onPositive(new MaterialDialog.SingleButtonCallback() {
                @Override
                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                    CardMessageContent cardMessageContent = new CardMessageContent(type, target, name, displayName, portrait);
                    messageViewModel.sendMessage(conversation, cardMessageContent);
                    if (!TextUtils.isEmpty(view.getEditText())) {
                        TextMessageContent content = new TextMessageContent(view.getEditText());
                        messageViewModel.sendMessage(conversation, content);
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
        return R.mipmap.avatar_def;
    }

    @Override
    public String title(Context context) {
        return "名片";
    }

    @Override
    public String contextMenuTitle(Context context, String tag) {
        return title(context);
    }
}
