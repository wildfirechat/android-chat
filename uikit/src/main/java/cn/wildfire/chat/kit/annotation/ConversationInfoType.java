package cn.wildfire.chat.kit.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import cn.wildfirechat.model.Conversation;

/**
 * 用户设置会话UI({@link cn.wildfire.chat.kit.conversationlist.viewholder.ConversationViewHolder})和会话({@link cn.wildfirechat.model.Conversation.ConversationType} + 会话线路)之间的对应关系
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ConversationInfoType {
    Conversation.ConversationType type();

    int line();
}
