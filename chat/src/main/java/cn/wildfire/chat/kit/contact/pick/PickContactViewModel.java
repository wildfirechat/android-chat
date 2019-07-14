package cn.wildfire.chat.kit.contact.pick;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

public class PickContactViewModel extends ViewModel {
    private List<UIUserInfo> contacts;
    private List<String> uncheckableIds;
    private List<String> initialCheckedIds;
    private MutableLiveData<UIUserInfo> contactCheckStatusUpdateLiveData;
    private int maxPickCount = Integer.MAX_VALUE;

    public PickContactViewModel() {
        super();
    }

    public MutableLiveData<UIUserInfo> contactCheckStatusUpdateLiveData() {
        if (contactCheckStatusUpdateLiveData == null) {
            contactCheckStatusUpdateLiveData = new MutableLiveData<>();
        }
        return contactCheckStatusUpdateLiveData;
    }

    public void setMaxPickCount(int maxPickCount) {
        this.maxPickCount = maxPickCount;
    }

    public int getMaxPickCount() {
        return maxPickCount;
    }

    public void setUncheckableIds(List<String> uncheckableIds) {
        this.uncheckableIds = uncheckableIds;
        updateContactStatus();
    }

    public void setInitialCheckedIds(List<String> checkedIds) {
        this.initialCheckedIds = checkedIds;
        updateContactStatus();
    }

    public void setContacts(List<UIUserInfo> contacts) {
        this.contacts = contacts;
        updateContactStatus();
    }

    private void updateContactStatus() {
        if (contacts == null || contacts.isEmpty()) {
            return;
        }
        for (UIUserInfo info : contacts) {
            if (initialCheckedIds != null && !initialCheckedIds.isEmpty()) {
                if (initialCheckedIds.contains(info.getUserInfo().uid)) {
                    info.setChecked(true);
                }
            }
            if (uncheckableIds != null && !uncheckableIds.isEmpty()) {
                if (uncheckableIds.contains(info.getUserInfo().uid)) {
                    info.setCheckable(false);
                }
            }
        }
    }

    public List<UIUserInfo> searchContact(String keyword) {
        if (contacts == null || contacts.isEmpty()) {
            return null;
        }

        List<UserInfo> tmpList = ChatManager.Instance().searchFriends(keyword);
        if (tmpList == null || tmpList.isEmpty()) {
            return null;
        }

        List<UIUserInfo> resultList = new ArrayList<>();
        for (UserInfo userInfo : tmpList) {
            for (UIUserInfo info : contacts) {
                if (info.getUserInfo().uid.equals(userInfo.uid)) {
                    resultList.add(info);
                    if (uncheckableIds != null && uncheckableIds.contains(userInfo.uid)) {
                        info.setCheckable(false);
                    }
                    if (initialCheckedIds != null && initialCheckedIds.contains(userInfo.uid)) {
                        info.setChecked(true);
                    }
                    break;
                }
            }
        }
        resultList.get(0).setShowCategory(true);
        resultList.get(0).setCategory("搜索结果");
        return resultList;
    }

    /**
     * not include initial checked contacts
     *
     * @return
     */
    public List<UIUserInfo> getCheckedContacts() {
        List<UIUserInfo> checkedContacts = new ArrayList<>();
        if (contacts == null) {
            return checkedContacts;
        }
        for (UIUserInfo info : contacts) {
            if (info.isCheckable() && info.isChecked()) {
                checkedContacts.add(info);
            }
        }
        return checkedContacts;
    }

    public List<UIUserInfo> getInitialCheckedContacts() {
        List<UIUserInfo> checkedContacts = new ArrayList<>();
        if (contacts == null || initialCheckedIds == null) {
            return checkedContacts;
        }
        for (UIUserInfo info : contacts) {
            if (initialCheckedIds.contains(info.getUserInfo().uid)) {
                checkedContacts.add(info);
            }
        }
        return checkedContacts;
    }

    /**
     * @param userInfo
     * @param checked
     * @return 选择成功，则返回true；否则，失败
     */
    public boolean checkContact(UIUserInfo userInfo, boolean checked) {
        if (checked && getCheckedContacts() != null && getCheckedContacts().size() >= maxPickCount) {
            return false;
        }
        userInfo.setChecked(checked);
        if (contactCheckStatusUpdateLiveData != null) {
            contactCheckStatusUpdateLiveData.setValue(userInfo);
        }
        return true;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
    }
}
