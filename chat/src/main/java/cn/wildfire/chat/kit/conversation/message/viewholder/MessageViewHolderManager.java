package cn.wildfire.chat.kit.conversation.message.viewholder;

import android.util.Log;
import android.util.SparseArray;

import cn.wildfire.chat.kit.annotation.MessageContentType;
import cn.wildfirechat.message.MessageContent;
import cn.wildfirechat.message.core.ContentTag;

public class MessageViewHolderManager {
    private static MessageViewHolderManager instance = new MessageViewHolderManager();

    private MessageViewHolderManager() {
        init();
    }

    public static MessageViewHolderManager getInstance() {
        return instance;
    }

    private void init() {
        registerMessageViewHolder(AudioMessageContentViewHolder.class);
        registerMessageViewHolder(FileMessageContentViewHolder.class);
        registerMessageViewHolder(ImageMessageContentViewHolder.class);
        registerMessageViewHolder(StickerMessageContentViewHolder.class);
        registerMessageViewHolder(TextMessageContentViewHolder.class);
        registerMessageViewHolder(VideoMessageContentViewHolder.class);
        registerMessageViewHolder(LocationMessageContentViewHolder.class);
        registerMessageViewHolder(VoipMessageViewHolder.class);
        registerMessageViewHolder(SimpleNotificationMessageContentViewHolder.class);
    }

    private SparseArray<Class<? extends MessageContentViewHolder>> messageViewHolders = new SparseArray<>();

    public void registerMessageViewHolder(Class<? extends MessageContentViewHolder> clazz) {
        MessageContentType contentType = clazz.getAnnotation(MessageContentType.class);
        Class<? extends MessageContent> clazzes[] = contentType.value();
        for (Class<? extends MessageContent> notificationClazz : clazzes) {
            ContentTag contentTag = notificationClazz.getAnnotation(ContentTag.class);
            if (messageViewHolders.get(contentTag.type()) == null) {
                messageViewHolders.put(contentTag.type(), clazz);
            } else {
                Log.e(MessageViewHolderManager.class.getSimpleName(), "re-register message view holder " + clazz.getSimpleName());
            }
        }
    }

    public Class<? extends MessageContentViewHolder> getMessageContentViewHolder(int messageType) {
        Class clazz = messageViewHolders.get(messageType);
        if (clazz == null) {
            return UnkownMessageContentViewHolder.class;
        }
        return clazz;
    }
}
