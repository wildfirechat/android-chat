package cn.wildfirechat.model;

public class NullGroupInfo extends GroupInfo {
    public NullGroupInfo(String groupId) {
        this.target = groupId;
        this.name = "<" + groupId + ">";
    }
}
