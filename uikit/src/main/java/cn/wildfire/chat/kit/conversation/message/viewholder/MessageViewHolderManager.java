package cn.wildfire.chat.kit.conversation.message.viewholder;

import android.util.Log;
import android.util.SparseArray;

import cn.wildfire.chat.kit.annotation.MessageContentType;
import cn.wildfire.chat.kit.R;
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
        registerMessageViewHolder(AudioMessageContentViewHolder.class, R.layout.conversation_item_audio_send, R.layout.conversation_item_audio_receive);
        registerMessageViewHolder(FileMessageContentViewHolder.class, R.layout.conversation_item_file_send, R.layout.conversation_item_file_receive);
        registerMessageViewHolder(ImageMessageContentViewHolder.class, R.layout.conversation_item_image_send, R.layout.conversation_item_image_receive);
        registerMessageViewHolder(StickerMessageContentViewHolder.class, R.layout.conversation_item_sticker_send, R.layout.conversation_item_sticker_receive);
        registerMessageViewHolder(TextMessageContentViewHolder.class, R.layout.conversation_item_text_send, R.layout.conversation_item_text_receive);
        registerMessageViewHolder(VideoMessageContentViewHolder.class, R.layout.conversation_item_video_send, R.layout.conversation_item_video_send);
        registerMessageViewHolder(VoipMessageViewHolder.class, R.layout.conversation_item_voip_send, R.layout.conversation_item_voip_receive);
        registerMessageViewHolder(SimpleNotificationMessageContentViewHolder.class, R.layout.conversation_item_notification, R.layout.conversation_item_notification);
        registerMessageViewHolder(RecallMessageContentViewHolder.class, R.layout.conversation_item_recall_notification, R.layout.conversation_item_recall_notification);
    }

    private SparseArray<Class<? extends MessageContentViewHolder>> messageViewHolders = new SparseArray<>();
    private SparseArray<Integer> messageSendLayoutRes = new SparseArray<>();
    private SparseArray<Integer> messageReceiveLayoutRes = new SparseArray<>();

    public void registerMessageViewHolder(Class<? extends MessageContentViewHolder> clazz, int sendLayoutRes, int receiveLayoutRes) {
        MessageContentType contentType = clazz.getAnnotation(MessageContentType.class);
        if (contentType == null) {
            throw new IllegalArgumentException("the message content viewHolder must be annotated with MessageContentType");
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
        return messageSendLayoutRes.get(messageType);
    }

    public @androidx.annotation.LayoutRes
    int receiveLayoutResId(int messageType) {
        return messageReceiveLayoutRes.get(messageType);
    }

    public Class<? extends MessageContentViewHolder> getMessageContentViewHolder(int messageType) {
        Class clazz = messageViewHolders.get(messageType);
        if (clazz == null) {
            return UnkownMessageContentViewHolder.class;
        }
        return clazz;
    }
}
