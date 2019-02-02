package cn.wildfire.chat.kit.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.wildfire.chat.kit.conversation.ext.core.ConversationExt;

/**
 * 用于注解聊天扩展{@link ConversationExt}, 点击扩展之后的相应，若扩展{@link ConversationExt}只有一个菜单{@link ExtContextMenuItem}，
 * 那么直接响应；如果有多个，则弹出菜单，让进一步选择
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExtContextMenuItem {
    String title() default "";
}
