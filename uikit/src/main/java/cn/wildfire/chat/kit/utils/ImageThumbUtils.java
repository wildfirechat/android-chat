/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.utils;

import android.text.TextUtils;

import cn.wildfirechat.message.ImageMessageContent;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.remote.ChatManager;

public class ImageThumbUtils {
    private static final String THUMB_PARA_MINIO = ".thumb";

    /**
     * 获取图片消息的远程缩略图地址
     * <p>
     * 当 getImageThumbPara 返回 .thumb 时，协议栈发送图片消息会丢弃缩略图，
     * 由 minio 在上传图片时自动生成缩略图，地址为 remoteUrl + .thumb
     *
     * @param message 图片消息
     * @return 远程缩略图地址，不支持时返回 null，此时应显示占位图
     */
    public static String getRemoteThumbnailUrl(Message message) {
        if (message == null || !(message.content instanceof ImageMessageContent)) {
            return null;
        }
        // 密聊图片是加密后上传的，服务端无法生成缩略图
        if (message.conversation != null && message.conversation.type == Conversation.ConversationType.SecretChat) {
            return null;
        }
        String remoteUrl = ((ImageMessageContent) message.content).remoteUrl;
        if (TextUtils.isEmpty(remoteUrl)) {
            return null;
        }
        if (!THUMB_PARA_MINIO.equals(ChatManager.Instance().getImageThumbPara())) {
            return null;
        }
        return remoteUrl + THUMB_PARA_MINIO;
    }
}
