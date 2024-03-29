/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.chatme.model;

public class NullGroupMember extends GroupMember {
    public NullGroupMember(String groupId, String memberId) {
        this.groupId = groupId;
        this.memberId = memberId;
        this.type = GroupMemberType.Normal;
    }
}
