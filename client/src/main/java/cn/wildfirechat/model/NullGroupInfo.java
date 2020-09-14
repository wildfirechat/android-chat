/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.model;

public class NullGroupInfo extends GroupInfo {
    public NullGroupInfo(String groupId) {
        this.target = groupId;
        //this.name = "<" + groupId + ">";
        this.name = "群聊";
    }
}
