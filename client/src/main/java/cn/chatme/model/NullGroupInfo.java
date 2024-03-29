/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.chatme.model;

public class NullGroupInfo extends GroupInfo {
    public NullGroupInfo(String groupId) {
        this.target = groupId;
        //this.name = "<" + groupId + ">";
        this.name = "群聊";
        this.type = GroupType.Normal;
    }
}
