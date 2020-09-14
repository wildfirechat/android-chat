/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.model;

public class NullGroupMember extends GroupMember {
    public NullGroupMember(String groupId, String memberId) {
        this.groupId = groupId;
        this.memberId = memberId;
        this.type = GroupMemberType.Normal;
    }
}
