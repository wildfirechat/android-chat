/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.message.viewholder;

import android.graphics.Bitmap;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import cn.wildfire.chat.kit.*;
import cn.wildfire.chat.kit.annotation.EnableContextMenu;
import cn.wildfire.chat.kit.annotation.MessageContentType;
import cn.wildfire.chat.kit.conversation.ConversationFragment;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfire.chat.kit.third.utils.TimeConvertUtils;
import cn.wildfire.chat.kit.utils.FileUtils;
import cn.wildfire.chat.kit.widget.BubbleImageView;
import cn.wildfirechat.message.VideoMessageContent;
import cn.wildfirechat.message.core.MessageDirection;
import cn.wildfirechat.message.core.MessageStatus;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.utils.WeChatImageUtils;

/**
 * 小视频尺寸展示高仿微信,并且带上时间
 * 占位图-缩略图-原图
 */
@MessageContentType(VideoMessageContent.class)
@EnableContextMenu
public class VideoMessageContentViewHolder extends MediaMessageContentViewHolder {
    private static final String TAG = "VideoMessageContentView";
    BubbleImageView imageView;
    ImageView playImageView;

    TextView time_tv;

    private  String imagePath ;

    public VideoMessageContentViewHolder(ConversationFragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(fragment, adapter, itemView);
        bindViews(itemView);
        bindEvents(itemView);
    }

    private void bindEvents(View itemView) {
       itemView.findViewById(R.id.videoContentLayout).setOnClickListener(_v -> play());
    }

    private void bindViews(View itemView) {
        imageView =itemView.findViewById(R.id.imageView);
        playImageView =itemView.findViewById(R.id.playImageView);
        time_tv =itemView.findViewById(R.id.time_tv);
    }

    @Override
    public void onBind(UiMessage message) {
        VideoMessageContent videoMessageContent = (VideoMessageContent) message.message.content;
        Bitmap thumbnail = videoMessageContent.getThumbnail();
        time_tv.setText(TimeConvertUtils.formatLongTime(videoMessageContent.getDuration()/1000));
        int width = 200;
        int height = 200;
        if(thumbnail != null) {
            int imageSize[] = WeChatImageUtils.getImageSizeByOrgSizeToWeChat(thumbnail.getWidth(), thumbnail.getHeight());
            width = imageSize[0] > 0 ? imageSize[0] : 200;
            height = imageSize[1] > 0 ? imageSize[1] : 200;
        }
        imageView.getLayoutParams().width = width;
        imageView.getLayoutParams().height = height;
        playImageView.setVisibility(View.VISIBLE);
        if(FileUtils.isFileExists(videoMessageContent.localPath)){
            imagePath = videoMessageContent.localPath;
        }else {
            imagePath = videoMessageContent.remoteUrl;
        }
        loadMedia(thumbnail,imagePath,imageView);

    }

    void play() {
        previewMM();
        if (message.message.direction == MessageDirection.Receive && message.message.status != MessageStatus.Played) {
            message.message.status = MessageStatus.Played;
            ChatManager.Instance().setMediaMessagePlayed(message.message.messageId);
        }
    }
}
