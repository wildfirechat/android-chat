/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.message.viewholder;

import android.graphics.Bitmap;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.blankj.utilcode.util.FileUtils;

import butterknife.BindView;
import butterknife.OnClick;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.annotation.EnableContextMenu;
import cn.wildfire.chat.kit.annotation.MessageContentType;
import cn.wildfire.chat.kit.conversation.ConversationFragment;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfire.chat.kit.widget.BubbleImageView;
import cn.wildfirechat.message.ImageMessageContent;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.MessageContent;
import cn.wildfirechat.message.core.MessageDirection;
import cn.wildfirechat.message.core.MessageStatus;
import cn.wildfirechat.utils.WeChatImageUtils;

/**
 * 图片尺寸展示高仿微信
 * 占位图-缩略图-原图
 */
@MessageContentType(ImageMessageContent.class)
@EnableContextMenu
public class ImageMessageContentViewHolder extends MediaMessageContentViewHolder {

    private static final String TAG = "ImageMessageContentView";
    @BindView(R2.id.imageView)
    BubbleImageView imageView;

    private String imagePath;

    public ImageMessageContentViewHolder(ConversationFragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(fragment, adapter, itemView);
    }

    @Override
    public void onBind(UiMessage message) {
        ImageMessageContent imageMessage = (ImageMessageContent) message.message.content;
        Bitmap thumbnail = imageMessage.getThumbnail();
        int imageSize[] = WeChatImageUtils.getImageSizeByOrgSizeToWeChat((int) imageMessage.getImageWidth(), (int) imageMessage.getImageHeight());
        int width = imageSize[0] > 0 ? imageSize[0] : 200;
        int height = imageSize[1] > 0 ? imageSize[1] : 200;
        imageView.getLayoutParams().width = width;
        imageView.getLayoutParams().height = height;
        if (FileUtils.isFileExists(imageMessage.localPath)) {
            imagePath = imageMessage.localPath;
        } else {
            imagePath = imageMessage.remoteUrl;
        }
        loadMedia(thumbnail, imagePath, imageView);

    }

    @OnClick(R2.id.imageView)
    void preview() {
        previewMM();
    }

    @Override
    protected void setSendStatus(Message item) {
        super.setSendStatus(item);
        MessageContent msgContent = item.content;
        if (msgContent instanceof ImageMessageContent) {
            boolean isSend = item.direction == MessageDirection.Send;
            if (isSend) {
                MessageStatus sentStatus = item.status;
                if (sentStatus == MessageStatus.Sending) {
                    imageView.setPercent(message.progress);
                    imageView.setProgressVisible(true);
                    imageView.showShadow(true);
                } else if (sentStatus == MessageStatus.Send_Failure) {
                    imageView.setProgressVisible(false);
                    imageView.showShadow(false);
                } else if (sentStatus == MessageStatus.Sent) {
                    imageView.setProgressVisible(false);
                    imageView.showShadow(false);
                }
            } else {
                imageView.setProgressVisible(false);
                imageView.showShadow(false);
            }
        }
    }

}
