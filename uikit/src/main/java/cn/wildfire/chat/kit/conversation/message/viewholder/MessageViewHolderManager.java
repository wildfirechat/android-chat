/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.message.viewholder;

import android.util.Log;
import android.util.SparseArray;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.annotation.MessageContentType;
import cn.wildfirechat.message.MessageContent;
import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessageDirection;

public class MessageViewHolderManager {
    private static final String TAG = "MsgViewHolderManager";
    private static MessageViewHolderManager instance = new MessageViewHolderManager();

    private MessageViewHolderManager() {
        init();
    }

    public static MessageViewHolderManager getInstance() {
        return instance;
    }

    private void init() {
        registerMessageViewHolder(AudioMessageContentViewHolder.class, R.layout.conversation_item_audio_send, R.layout.conversation_item_audio_receive);
        registerMessageViewHolder(FileMessageContentViewHolder.class, R.layout.conversation_item_file_send, R.layout.conversation_item_file_receive);
        registerMessageViewHolder(ImageMessageContentViewHolder.class, R.layout.conversation_item_image_send, R.layout.conversation_item_image_receive);
        registerMessageViewHolder(StickerMessageContentViewHolder.class, R.layout.conversation_item_sticker_send, R.layout.conversation_item_sticker_receive);
        registerMessageViewHolder(TextMessageContentViewHolder.class, R.layout.conversation_item_text_send, R.layout.conversation_item_text_receive);
        registerMessageViewHolder(FriendAddedContentViewHolder.class, R.layout.conversation_item_notification, R.layout.conversation_item_friend_added_receive);
        registerMessageViewHolder(VideoMessageContentViewHolder.class, R.layout.conversation_item_video_send, R.layout.conversation_item_video_send);
        registerMessageViewHolder(VoipMessageViewHolder.class, R.layout.conversation_item_voip_send, R.layout.conversation_item_voip_receive);
        registerMessageViewHolder(SimpleNotificationMessageContentViewHolder.class, R.layout.conversation_item_notification, R.layout.conversation_item_notification);
        registerMessageViewHolder(RichNotificationMessageContentViewHolder.class, R.layout.conversation_item_rich_notification, R.layout.conversation_item_rich_notification);
        registerMessageViewHolder(RecallMessageContentViewHolder.class, R.layout.conversation_item_recall_notification, R.layout.conversation_item_recall_notification);
        registerMessageViewHolder(UserCardMessageContentViewHolder.class, R.layout.conversation_item_user_card_send, R.layout.conversation_item_user_card_receive);
        registerMessageViewHolder(ConferenceInviteMessageContentViewHolder.class, R.layout.conversation_item_conference_invite_send, R.layout.conversation_item_conference_invite_receive);
        registerMessageViewHolder(CompositeMessageContentViewHolder.class, R.layout.conversation_item_composite_send, R.layout.conversation_item_composite_receive);
        registerMessageViewHolder(LinkMessageContentViewHolder.class, R.layout.conversation_item_link_send, R.layout.conversation_item_link_receive);
        registerMessageViewHolder(ArticlesMessageContentViewHolder.class, R.layout.conversation_item_articles, R.layout.conversation_item_articles);
        registerMessageViewHolder(StreamingTextMessageContentViewHolder.class, R.layout.conversation_item_streaming_text_receive, R.layout.conversation_item_streaming_text_receive);
    }

    private SparseArray<Class<? extends MessageContentViewHolder>> messageViewHolders = new SparseArray<>();
    private SparseArray<Integer> messageSendLayoutRes = new SparseArray<>();
    private SparseArray<Integer> messageReceiveLayoutRes = new SparseArray<>();

    public void registerMessageViewHolder(Class<? extends MessageContentViewHolder> clazz, int sendLayoutRes, int receiveLayoutRes) {
        MessageContentType contentType = clazz.getAnnotation(MessageContentType.class);
        if (contentType == null) {
            throw new IllegalArgumentException("the message content viewHolder must be annotated with MessageContentType " + clazz.getSimpleName());
        }

        if (sendLayoutRes == 0 && receiveLayoutRes == 0) {
            throw new IllegalArgumentException("must set message content viewHolder layout ");
        }

        Class<? extends MessageContent> clazzes[] = contentType.value();
        for (Class<? extends MessageContent> notificationClazz : clazzes) {
            ContentTag contentTag = notificationClazz.getAnnotation(ContentTag.class);
            if (messageViewHolders.get(contentTag.type()) == null) {
                messageViewHolders.put(contentTag.type(), clazz);
                messageSendLayoutRes.put(contentTag.type(), sendLayoutRes);
                messageReceiveLayoutRes.put(contentTag.type(), receiveLayoutRes);
            } else {
                Log.e(MessageViewHolderManager.class.getSimpleName(), "re-register message view holder " + clazz.getSimpleName());
            }
        }
    }

    public @androidx.annotation.LayoutRes
    int sendLayoutResId(int messageType) {
        Integer sendLayoutResId = messageSendLayoutRes.get(messageType);
        return sendLayoutResId == null ? R.layout.conversation_item_unknown_send : sendLayoutResId;
    }

    public @androidx.annotation.LayoutRes
    int receiveLayoutResId(int messageType) {
        Integer receiveLayoutResId = messageReceiveLayoutRes.get(messageType);
        return receiveLayoutResId == null ? R.layout.conversation_item_unknown_receive : receiveLayoutResId;
    }

    public Class<? extends MessageContentViewHolder> getMessageContentViewHolder(int messageType, int direction) {
        if (messageType == cn.wildfirechat.message.core.MessageContentType.ContentType_Friend_Added) {
            return direction == MessageDirection.Receive.value() ? FriendAddedContentViewHolder.class : SimpleNotificationMessageContentViewHolder.class;
        }

        return getMessageContentViewHolder(messageType);
    }

    public Class<? extends MessageContentViewHolder> getMessageContentViewHolder(int messageType) {
        Class clazz = messageViewHolders.get(messageType);
        if (clazz == null) {
            Log.d(TAG, "not register messageContentViewHolder for messageType " + messageType + ", fall-back to UnknownMessageContentViewHolder");
            return UnkownMessageContentViewHolder.class;
        }
        return clazz;
    }
}
