/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.message.viewholder;

import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.CircularProgressDrawable;

import butterknife.BindView;
import butterknife.OnClick;
import cn.wildfire.chat.kit.GlideApp;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.WfcWebViewActivity;
import cn.wildfire.chat.kit.annotation.EnableContextMenu;
import cn.wildfire.chat.kit.annotation.MessageContentType;
import cn.wildfire.chat.kit.conversation.ConversationFragment;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfire.chat.kit.utils.DownloadManager;
import cn.wildfire.chat.kit.widget.SimpleLabelView;
import cn.wildfirechat.message.notification.RichNotificationMessageContent;
import cn.wildfirechat.model.Conversation;

@MessageContentType(RichNotificationMessageContent.class)
@EnableContextMenu
public class RichNotificationMessageContentViewHolder extends NotificationMessageContentViewHolder {
    @BindView(R2.id.titleTextView)
    TextView titleTextView;
    @BindView(R2.id.descTextView)
    TextView descTextView;

    @BindView(R2.id.dataContainerLayout)
    LinearLayout dataContainerLayout;

    @BindView(R2.id.exPortraitImageView)
    ImageView exPortraitImageView;
    @BindView(R2.id.exNameTextView)
    TextView exNameTextView;


    public RichNotificationMessageContentViewHolder(ConversationFragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(fragment, adapter, itemView);
    }

    @Override
    public void onBind(UiMessage message, int position) {
        super.onBind(message, position);
        RichNotificationMessageContent rich = (RichNotificationMessageContent) message.message.content;
        titleTextView.setText(rich.title);
        descTextView.setText(rich.desc);
        if (!TextUtils.isEmpty(rich.exPortrait)) {
            exPortraitImageView.setVisibility(View.VISIBLE);
            String imagePath = rich.exPortrait;
            if (message.message.conversation.type == Conversation.ConversationType.SecretChat) {
                imagePath = DownloadManager.buildSecretChatMediaUrl(message.message);
            }
            CircularProgressDrawable progressDrawable = new CircularProgressDrawable(fragment.getContext());
            progressDrawable.setStyle(CircularProgressDrawable.DEFAULT);
            progressDrawable.start();
            GlideApp.with(fragment)
                .load(imagePath)
                .placeholder(progressDrawable)
                .into(exPortraitImageView);
        } else {
            exPortraitImageView.setVisibility(View.GONE);
        }
        if (!TextUtils.isEmpty(rich.exName)) {
            exNameTextView.setText(rich.exName);
        }

        if (rich.datas != null && rich.datas.size() > 0) {
            dataContainerLayout.removeAllViews();
            for (RichNotificationMessageContent.Data data : rich.datas) {
                SimpleLabelView simpleLabelView = new SimpleLabelView(fragment.getContext());
                simpleLabelView.setTitle(data.key);
                simpleLabelView.setDesc(data.value, data.color);
                dataContainerLayout.addView(simpleLabelView);
            }
        }
    }

    @OnClick(R2.id.richNotificationContentItemView)
    public void onClick(View view) {
        RichNotificationMessageContent rich = (RichNotificationMessageContent) message.message.content;
        WfcWebViewActivity.loadUrl(fragment.getContext(), "", rich.exUrl);
    }

    @Override
    public boolean contextMenuItemFilter(UiMessage uiMessage, String itemTitle) {
        return false;
    }

//    @MessageContextMenuItem(tag = MessageContextMenuItemTags.TAG_FORWARD, priority = 11)
//    public void forwardMessage(View itemView, UiMessage message) {
//        Intent intent = new Intent(fragment.getContext(), ForwardActivity.class);
//        intent.putExtra("message", message.message);
//        fragment.startActivity(intent);
//    }

}
