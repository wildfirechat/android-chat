package cn.wildfire.chat.moment.third.beans;

import cn.wildfirechat.model.UserInfo;

public class UserBean {
    private String userAvatarUrl;

    private String userName;

    private String userId;

    public String getUserAvatarUrl() {
        return userAvatarUrl;
    }

    public void setUserAvatarUrl(String userAvatarUrl) {
        this.userAvatarUrl = userAvatarUrl;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public static UserBean fromUserInfo(UserInfo userInfo) {
        UserBean userBean = new UserBean();
        userBean.setUserId(userInfo.uid);
        userBean.setUserAvatarUrl(userInfo.portrait);
        userBean.setUserName(userInfo.displayName);
        return userBean;
    }
}
