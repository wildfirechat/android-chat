/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.message.viewholder;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

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

    protected void previewMM(ImageView sourceView) {
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

        // Build a SourceRectProvider that dynamically looks up any entry's thumbnail
        // in the RecyclerView at dismiss time — works for any page the user swipes to.
        View fragView = fragment.getView();
        RecyclerView rv = fragView != null
            ? (RecyclerView) fragView.findViewById(R.id.msgRecyclerView)
            : null;
        WeakReference<RecyclerView> rvRef = new WeakReference<>(rv);

        MMPreviewActivity.SourceRectProvider provider = entry -> {
            RecyclerView recyclerView = rvRef.get();
            if (recyclerView == null || entry == null || entry.getMessage() == null) return null;
            long msgId = entry.getMessage().messageId;
            for (int i = 0; i < recyclerView.getChildCount(); i++) {
                RecyclerView.ViewHolder vh = recyclerView.getChildViewHolder(recyclerView.getChildAt(i));
                if (vh instanceof MediaMessageContentViewHolder) {
                    MediaMessageContentViewHolder mvh = (MediaMessageContentViewHolder) vh;
                    if (mvh.message != null && mvh.message.message.messageId == msgId) {
                        // Use the thumbnail ImageView which exists in both image and video items
                        View thumb = mvh.itemView.findViewById(R.id.imageView);
                        if (thumb != null && thumb.isShown()) {
                            int[] loc = new int[2];
                            thumb.getLocationOnScreen(loc);
                            return new Rect(loc[0], loc[1],
                                loc[0] + thumb.getWidth(), loc[1] + thumb.getHeight());
                        }
                    }
                }
            }
            return null;
        };

        boolean isSecret = messages.get(0).message.conversation.type == Conversation.ConversationType.SecretChat;
        MMPreviewActivity.previewMedia(fragment.getContext(), entries, current, isSecret, sourceView, provider);
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
        RequestBuilder<Drawable> request = Glide.with(fragment)
            .load(imagePath)
            .thumbnail(thumbnailRequest)
            .apply(placeholderOptions);
        if (message.message.conversation.type == Conversation.ConversationType.SecretChat) {
            request = request.diskCacheStrategy(DiskCacheStrategy.NONE);
        } else {
            request = request.diskCacheStrategy(DiskCacheStrategy.AUTOMATIC);
        }
        request.into(imageView);
    }

    @MessageContextMenuItem(tag = MessageContextMenuItemTags.TAG_CANCEL_SEND, confirm = false, priority = 13)
    public void cancelSend(View itemView, UiMessage message) {
        boolean canceled = ChatManager.Instance().cancelSendingMessage(message.message.messageId);
        if (!canceled) {
            Toast.makeText(fragment.getContext(), R.string.media_cancel_failed, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public String contextMenuTitle(Context context, String tag) {
        if (MessageContextMenuItemTags.TAG_CANCEL_SEND.equals(tag)) {
            return context.getString(R.string.media_cancel_send);
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
