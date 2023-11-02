/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.message.viewholder;

import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.CircularProgressDrawable;

import com.bumptech.glide.Glide;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.annotation.EnableContextMenu;
import cn.wildfire.chat.kit.annotation.MessageContentType;
import cn.wildfire.chat.kit.conversation.ConversationFragment;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfire.chat.kit.third.utils.UIUtils;
import cn.wildfire.chat.kit.utils.DownloadManager;
import cn.wildfirechat.message.StickerMessageContent;
import cn.wildfirechat.message.core.MessageDirection;
import cn.wildfirechat.message.core.MessageStatus;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.remote.ChatManager;

@MessageContentType(StickerMessageContent.class)
@EnableContextMenu
public class StickerMessageContentViewHolder extends NormalMessageContentViewHolder {
    private String path;
    ImageView imageView;

    public StickerMessageContentViewHolder(ConversationFragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(fragment, adapter, itemView);
        bindViews(itemView);
    }

    private void bindViews(View itemView) {
        imageView =itemView.findViewById(R.id.stickerImageView);
    }

    @Override
    public void onBind(UiMessage message) {
        StickerMessageContent stickerMessage = (StickerMessageContent) message.message.content;
        imageView.getLayoutParams().width = UIUtils.dip2Px(stickerMessage.width > 150 ? 150 : stickerMessage.width);
        imageView.getLayoutParams().height = UIUtils.dip2Px(stickerMessage.height > 150 ? 150 : stickerMessage.height);

        if (!TextUtils.isEmpty(stickerMessage.localPath)) {
            if (stickerMessage.localPath.equals(path)) {
                return;
            }
            Glide.with(fragment).load(stickerMessage.localPath)
                .into(imageView);
            path = stickerMessage.localPath;
        } else {
            String imagePath = stickerMessage.remoteUrl;
            if (message.message.conversation.type == Conversation.ConversationType.SecretChat) {
                imagePath = DownloadManager.buildSecretChatMediaUrl(message.message);
            }
            CircularProgressDrawable progressDrawable = new CircularProgressDrawable(fragment.getContext());
            progressDrawable.setStyle(CircularProgressDrawable.DEFAULT);
            progressDrawable.start();
            Glide.with(fragment)
                .load(imagePath)
                .placeholder(progressDrawable)
                .into(imageView);
        }
        if (message.message.conversation.type == Conversation.ConversationType.SecretChat) {
            if (message.message.direction == MessageDirection.Receive && message.message.status != MessageStatus.Played) {
                message.message.status = MessageStatus.Played;
                ChatManager.Instance().setMediaMessagePlayed(message.message.messageId);
            }
        }
    }

    // 其实也没啥用，有itemView了，可以直接setXXXListener了, 只是简化了，不用调用setXXXListener
    // 更复杂的，比如设置播放进度条的滑动等，在通过itemView设置相关listener吧
    public void onClick(View view) {
        // TODO
    }

}
