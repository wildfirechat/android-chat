package cn.wildfire.chat.kit.conversationlist.viewholder;

import android.util.Log;
import android.util.SparseArray;

import cn.wildfire.chat.kit.annotation.ConversationInfoType;

public class ConversationViewHolderManager {
    private static ConversationViewHolderManager instance = new ConversationViewHolderManager();

    private ConversationViewHolderManager() {
        registerConversationViewHolder(SingleConversationViewHolder.class);
        registerConversationViewHolder(GroupConversationViewHolder.class);
        registerConversationViewHolder(ChannelConversationViewHolder.class);
        registerConversationViewHolder(ChatRoomConversationViewHolder.class);
    }

    public static ConversationViewHolderManager getInstance() {
        return instance;
    }

    private SparseArray<Class<? extends ConversationViewHolder>> messageViewHolders = new SparseArray<>();

    public void registerConversationViewHolder(Class<? extends ConversationViewHolder> clazz) {
        ConversationInfoType conversationInfoType = clazz.getAnnotation(ConversationInfoType.class);
        int type = conversationInfoType.type().getValue() << 24 | conversationInfoType.line();
        if (messageViewHolders.get(type) != null) {
            throw new RuntimeException("type is already registered or viewHolder's annotation is error " + clazz.getSimpleName());
        }
        messageViewHolders.put(type, clazz);
    }

    public Class<? extends ConversationViewHolder> getConversationContentViewHolder(int type) {
        Class clazz = messageViewHolders.get(type);
        if (clazz == null) {
            type = type & 0xFFFFFF00;
            clazz = messageViewHolders.get(type);
            if (clazz == null) {
                clazz = UnknownConversationViewHolder.class;
            }
            Log.e("wfc", "未配置对应的ConversationViewHolder，" + (type >> 24) + " " + (type & 0xFF) + " " + clazz.getSimpleName());
        }
        return clazz;
    }
}
