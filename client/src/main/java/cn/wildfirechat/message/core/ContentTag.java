/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.message.core;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 消息内容类型注解
 * <p>
 * 用于标注消息内容类的类型和持久化标志。
 * </p>
 *
 * @author WildFireChat
 * @since 2020
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ContentTag {
    /**
     * 消息类型
     *
     * @return 消息类型值
     */
    int type() default 0;

    /**
     * 持久化标志
     *
     * @return 持久化标志
     */
    PersistFlag flag() default PersistFlag.No_Persist;
}
