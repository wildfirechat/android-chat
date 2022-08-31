/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.message.viewholder;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.GlideApp;
import cn.wildfire.chat.kit.GlideRequest;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.annotation.MessageContextMenuItem;
import cn.wildfire.chat.kit.conversation.ConversationFragment;
import cn.wildfire.chat.kit.conversation.ConversationMessageAdapter;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfire.chat.kit.mm.MMPreviewActivity;
import cn.wildfire.chat.kit.mm.MediaEntry;
import cn.wildfirechat.message.MediaMessageContent;
import cn.wildfirechat.message.core.MessageContentType;
import cn.wildfirechat.message.core.MessageStatus;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.remote.ChatManager;

public abstract class MediaMessageContentViewHolder extends NormalMessageContentViewHolder {

    /**
     * 小视频，图片 占位图的配置
     */
    protected RequestOptions placeholderOptions = new RequestOptions();

    public MediaMessageContentViewHolder(ConversationFragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(fragment, adapter, itemView);
        placeholderOptions.diskCacheStrategy(DiskCacheStrategy.ALL);
        placeholderOptions.centerCrop();
        placeholderOptions.placeholder(R.drawable.image_chat_placeholder);
    }

    @Override
    protected void onBind(UiMessage message) {
        if (message.isDownloading || message.message.status == MessageStatus.Sending) {
            progressBar.setVisibility(View.VISIBLE);
        } else {
            progressBar.setVisibility(View.GONE);
        }
    }

    protected void previewMM() {
        List<UiMessage> messages = ((ConversationMessageAdapter) adapter).getMessages();
        List<MediaEntry> entries = new ArrayList<>();
        UiMessage msg;

        int current = 0;
        int index = 0;
        for (int i = 0; i < messages.size(); i++) {
            msg = messages.get(i);
            if (msg.message.content.getMessageContentType() != MessageContentType.ContentType_Image
                && msg.message.content.getMessageContentType() != MessageContentType.ContentType_Video) {
                continue;
            }
            MediaEntry entry = new MediaEntry(msg.message);
            entries.add(entry);

            if (message.message.messageId == msg.message.messageId) {
                current = index;
            }
            index++;
        }
        if (entries.isEmpty()) {
            return;
        }
        MMPreviewActivity.previewMedia(fragment.getContext(), entries, current, messages.get(0).message.conversation.type == Conversation.ConversationType.SecretChat);
    }

    /**
     * 图片 和小视频 加载的地方
     * 策略是先加载缩略图，在加载原图
     *
     * @param thumbnail
     * @param imagePath
     * @param imageView
     */
    protected void loadMedia(Bitmap thumbnail, String imagePath, ImageView imageView) {
        RequestBuilder<Drawable> thumbnailRequest = null;
        if (thumbnail != null) {
            thumbnailRequest = Glide
                .with(fragment)
                .load(thumbnail);
        } else {
            thumbnailRequest = Glide
                .with(fragment)
                .load(R.drawable.image_chat_placeholder);
        }
        GlideRequest<Drawable> request = GlideApp.with(fragment)
            .load(imagePath)
            .thumbnail(thumbnailRequest)
            .apply(placeholderOptions);
        if (message.message.conversation.type == Conversation.ConversationType.SecretChat) {
            request = request.diskCacheStrategy(DiskCacheStrategy.NONE);
        }
        request.into(imageView);
    }

    @MessageContextMenuItem(tag = MessageContextMenuItemTags.TAG_CANCEL_SEND, confirm = false, priority = 13)
    public void cancelSend(View itemView, UiMessage message) {
        boolean canceled = ChatManager.Instance().cancelSendingMessage(message.message.messageId);
        if (!canceled) {
            Toast.makeText(fragment.getContext(), "取消失败", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public String contextMenuTitle(Context context, String tag) {
        if (MessageContextMenuItemTags.TAG_CANCEL_SEND.equals(tag)) {
            return " 取消发送";
        }
        return super.contextMenuTitle(context, tag);
    }

    @Override
    public boolean contextMenuItemFilter(UiMessage uiMessage, String tag) {
        if (MessageContextMenuItemTags.TAG_CANCEL_SEND.equals(tag)) {
            return !(uiMessage.message.content instanceof MediaMessageContent && MessageStatus.Sending == uiMessage.message.status);
        } else {
            return super.contextMenuItemFilter(uiMessage, tag);
        }
    }
}
