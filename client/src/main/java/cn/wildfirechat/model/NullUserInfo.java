/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.model;

/**
 * 空用户信息类
 * <p>
 * 当本地不存在该用户信息时返回的空对象实现。
 * 使用Null Object模式，避免上层代码不断的做空值检查。
 * </p>
 *
 * @author WildFireChat
 * @since 2020
 */
public class NullUserInfo extends UserInfo {
    public NullUserInfo(String uid) {
        this.uid = uid;
        //this.name = "<" + uid + ">";
        this.name = "用户";
        this.displayName = name;
    }
}
