/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.model;

/**
 * 空群组信息类
 * <p>
 * 当群组信息不存在时返回的空对象实现。
 * 使用Null Object模式，避免上层代码不断的做空值检查。
 * </p>
 *
 * @author WildFireChat
 * @since 2020
 */
public class NullGroupInfo extends GroupInfo {
    public NullGroupInfo(String groupId) {
        this.target = groupId;
        //this.name = "<" + groupId + ">";
        this.name = "群聊";
        this.type = GroupType.Normal;
    }
}
