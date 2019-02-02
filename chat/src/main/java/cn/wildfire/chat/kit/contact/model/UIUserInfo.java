package cn.wildfire.chat.kit.contact.model;

import cn.wildfirechat.model.UserInfo;

public class UIUserInfo {
    private String category = "";
    // 用来排序的字段
    private String sortName;
    private boolean showCategory;
    private UserInfo userInfo;
    private boolean isChecked;
    private boolean isCheckable = true;

    public UIUserInfo(UserInfo userInfo) {
        this.userInfo = userInfo;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setShowCategory(boolean showCategory) {
        this.showCategory = showCategory;
    }

    public String getCategory() {
        return category;
    }

    public boolean isShowCategory() {
        return showCategory;
    }

    public UserInfo getUserInfo() {
        return userInfo;
    }

    public String getSortName() {
        return sortName;
    }

    public void setSortName(String displayNamePinyin) {
        this.sortName = displayNamePinyin;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }

    public boolean isCheckable() {
        return isCheckable;
    }

    public void setCheckable(boolean checkable) {
        isCheckable = checkable;
    }
}
