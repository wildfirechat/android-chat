package cn.wildfirechat.model;

/**
 * null pattern
 * <p>
 * 当本地不存在改用户信息时，返回这个类型的实例，避免上层不断的做null check
 */
public class NullUserInfo extends UserInfo {
    public NullUserInfo(String uid) {
        this.uid = uid;
        this.name = "<" + uid + ">";
        this.displayName = name;
    }
}
