/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.message.viewholder;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.io.File;

import butterknife.BindView;
import butterknife.OnClick;
import cn.wildfire.chat.kit.GlideApp;
import cn.wildfire.chat.kit.GlideRequest;
import cn.wildfire.chat.kit.annotation.EnableContextMenu;
import cn.wildfire.chat.kit.annotation.MessageContentType;
import cn.wildfire.chat.kit.conversation.ConversationFragment;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfire.chat.kit.third.utils.TimeConvertUtils;
import cn.wildfire.chat.kit.widget.BubbleImageView;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfirechat.message.VideoMessageContent;
import cn.wildfirechat.utils.WeChatImageUtils;

/**
 * 小视频尺寸展示高仿微信,并且带上时间
 * 占位图-缩略图-原图
 */
@MessageContentType(VideoMessageContent.class)
@EnableContextMenu
public class VideoMessageContentViewHolder extends MediaMessageContentViewHolder {
    private static final String TAG = "VideoMessageContentView";
    @BindView(R2.id.imageView)
    BubbleImageView imageView;
    @BindView(R2.id.playImageView)
    ImageView playImageView;

    @BindView(R2.id.time_tv)
    TextView time_tv;

    private  String imagePath ;

    public VideoMessageContentViewHolder(ConversationFragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(fragment, adapter, itemView);
    }

    @Override
    public void onBind(UiMessage message) {
        VideoMessageContent videoMessageContent = (VideoMessageContent) message.message.content;
        Bitmap thumbnail = videoMessageContent.getThumbnail();
        time_tv.setText(TimeConvertUtils.formatLongTime(videoMessageContent.getDuration()/1000));
        int imageSize[] = WeChatImageUtils.getImageSizeByOrgSizeToWeChat((int) videoMessageContent.getImageWidth(), (int) videoMessageContent.getImageHeight());
        int width = imageSize[0] >0? imageSize[0]: 200;
        int height = imageSize[1] >0 ? imageSize[1] : 200;
        imageView.getLayoutParams().width = width;
        imageView.getLayoutParams().height = height;
        playImageView.setVisibility(View.VISIBLE);
        if(!TextUtils.isEmpty(videoMessageContent.localPath)&& (new File(videoMessageContent.localPath).exists())){
            imagePath = videoMessageContent.localPath;
        }else {
            imagePath = videoMessageContent.remoteUrl;
        }
        //imagePath = videoMessageContent.remoteUrl;
        loadMedia(thumbnail,imagePath,imageView);

    }

    @OnClick(R2.id.videoContentLayout)
    void play() {
        previewMM();
    }

    @Override
    public boolean contextMenuItemFilter(UiMessage uiMessage, String tag) {
        if (MessageContextMenuItemTags.TAG_FORWARD.equals(tag)) {
            return true;
        } else {
            return super.contextMenuItemFilter(uiMessage, tag);
        }
    }

}
