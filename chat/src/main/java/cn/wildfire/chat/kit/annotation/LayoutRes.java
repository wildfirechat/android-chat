package cn.wildfire.chat.kit.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 当发出和收到的消息UI一致时，可使用{@link LayoutRes} 代替同时设置{@link SendLayoutRes} 和 {@link ReceiveLayoutRes}
 * <p>
 * 一般用户设置通知类消息和对应消息体的映射关系
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface LayoutRes {
    int resId() default 0;
}
