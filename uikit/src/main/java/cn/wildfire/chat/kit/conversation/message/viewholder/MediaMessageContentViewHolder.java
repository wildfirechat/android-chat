/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.message.viewholder;

import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.conversation.ConversationFragment;
import cn.wildfire.chat.kit.conversation.ConversationMessageAdapter;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfire.chat.kit.mm.MMPreviewActivity;
import cn.wildfire.chat.kit.mm.MediaEntry;
import cn.wildfirechat.message.ImageMessageContent;
import cn.wildfirechat.message.MediaMessageContent;
import cn.wildfirechat.message.VideoMessageContent;
import cn.wildfirechat.message.core.MessageContentType;

public abstract class MediaMessageContentViewHolder extends NormalMessageContentViewHolder {

    public MediaMessageContentViewHolder(ConversationFragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(fragment, adapter, itemView);
    }

    @Override
    protected void onBind(UiMessage message) {
        if (message.isDownloading) {
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
            MediaEntry entry = new MediaEntry();
            if (msg.message.content.getMessageContentType() == cn.wildfirechat.message.core.MessageContentType.ContentType_Image) {
                entry.setType(MediaEntry.TYPE_IMAGE);
                entry.setThumbnail(((ImageMessageContent) msg.message.content).getThumbnail());

            } else {
                entry.setType(MediaEntry.TYPE_VIDEO);
                entry.setThumbnail(((VideoMessageContent) msg.message.content).getThumbnail());
            }
            entry.setMediaUrl(((MediaMessageContent) msg.message.content).remoteUrl);
            entry.setMediaLocalPath(((MediaMessageContent) msg.message.content).localPath);
            entries.add(entry);

            if (message.message.messageId == msg.message.messageId) {
                current = index;
            }
            index++;
        }
        if (entries.isEmpty()) {
            return;
        }
        MMPreviewActivity.previewMedia(fragment.getContext(), entries, current);
    }
}
