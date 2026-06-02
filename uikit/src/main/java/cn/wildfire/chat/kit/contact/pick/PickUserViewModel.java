/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.contact.pick;

import android.text.TextUtils;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfire.chat.kit.net.Callback;
import cn.wildfire.chat.kit.organization.OrganizationServiceProvider;
import cn.wildfire.chat.kit.organization.model.Employee;
import cn.wildfire.chat.kit.organization.model.Organization;
import cn.wildfire.chat.kit.utils.PinyinUtils;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

public class PickUserViewModel extends ViewModel {

    // pick source users
    private List<UIUserInfo> users;

    private final List<UIUserInfo> checkedUsers = new ArrayList<>();
    private List<Employee> checkedEmployees = new ArrayList<>();
    private List<Organization> checkedOrganizations = new ArrayList<>();
    private List<String> uncheckableIds;
    private List<String> initialCheckedIds;
    private MutableLiveData<Object> userCheckStatusUpdateLiveData;
    private int maxPickCount = Integer.MAX_VALUE;

    public PickUserViewModel() {
        super();
    }

    public MutableLiveData<Object> userCheckStatusUpdateLiveData() {
        if (userCheckStatusUpdateLiveData == null) {
            userCheckStatusUpdateLiveData = new MutableLiveData<>();
        }
        return userCheckStatusUpdateLiveData;
    }

    public void setMaxPickCount(int maxPickCount) {
        this.maxPickCount = maxPickCount;
    }

    public int getMaxPickCount() {
        return maxPickCount;
    }

    public void setUncheckableIds(List<String> uncheckableIds) {
        this.uncheckableIds = uncheckableIds;
        updateUserStatus();
    }

    public void addUncheckableIds(List<String> uncheckableIds) {
        if (uncheckableIds == null || uncheckableIds.isEmpty()) {
            return;
        }
        if (this.uncheckableIds == null) {
            this.uncheckableIds = new ArrayList<>();
        }
        this.uncheckableIds.addAll(uncheckableIds);
    }

    public void setInitialCheckedIds(List<String> checkedIds) {
        this.initialCheckedIds = checkedIds;
        updateUserStatus();
    }

    public List<String> getInitialCheckedIds() {
        return initialCheckedIds;
    }

    public void setUsers(List<UIUserInfo> users) {
        this.users = users;
        updateUserStatus();
    }

    private void updateUserStatus() {
        if (users == null || users.isEmpty()) {
            return;
        }
        for (UIUserInfo info : users) {
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

    public MutableLiveData<Pair<List<UIUserInfo>, List<Employee>>> searchUser(String keyword) {
        MutableLiveData<Pair<List<UIUserInfo>, List<Employee>>> result = new MutableLiveData<>();

        List<UIUserInfo> userResult = new ArrayList<>();
        List<Employee> employeeResult = new ArrayList<>();
        if (users != null) {
            for (UIUserInfo info : users) {
                UserInfo userInfo = info.getUserInfo();
                String name = ChatManager.Instance().getUserDisplayName(userInfo);
                String pinyin = PinyinUtils.getPinyin(name);
                if (name.contains(keyword) || pinyin.contains(keyword.toUpperCase())) {
                    userResult.add(info);
                    if (uncheckableIds != null && uncheckableIds.contains(userInfo.uid)) {
                        info.setCheckable(false);
                    }
                    if (initialCheckedIds != null && initialCheckedIds.contains(userInfo.uid)) {
                        info.setChecked(true);
                    }
                }
            }
        }
        OrganizationServiceProvider organizationServiceProvider = WfcUIKit.getWfcUIKit().getOrganizationServiceProvider();
        if (organizationServiceProvider.isServiceAvailable()) {
            organizationServiceProvider.searchEmployee(keyword, new Callback<List<Employee>>() {
                @Override
                public void onSuccess(List<Employee> employees) {
                    employeeResult.addAll(employees);
                    result.postValue(new Pair<>(userResult, employeeResult));
                }

                @Override
                public void onFailure(int code, String msg) {
                    result.postValue(new Pair<>(userResult, employeeResult));
                }
            });
        } else {
            result.postValue(new Pair<>(userResult, employeeResult));
        }

        return result;
    }

    /**
     * include initial checked users
     *
     * @return
     */
    public @NonNull List<UIUserInfo> getCheckedUsers() {
        return checkedUsers == null ? new ArrayList<>() : checkedUsers;
    }

    public @NonNull List<Employee> getCheckedEmployees() {
        return checkedEmployees == null ? new ArrayList<>() : checkedEmployees;
    }

    public void setCheckedEmployees(List<Employee> employees) {
        this.checkedEmployees = employees;
        if (userCheckStatusUpdateLiveData != null) {
            userCheckStatusUpdateLiveData.setValue(new Object());
        }
    }

    public List<Organization> getCheckedOrganizations() {
        return checkedOrganizations == null ? new ArrayList<>() : checkedOrganizations;
    }

    public void setCheckedOrganizations(List<Organization> checkedOrganizations) {
        this.checkedOrganizations = checkedOrganizations;
        if (userCheckStatusUpdateLiveData != null) {
            userCheckStatusUpdateLiveData.setValue(new Object());
        }
    }

    private int getTotalCheckedCount() {
        int count = getCheckedUsers().size() + getCheckedEmployees().size();
        for (Organization org : getCheckedOrganizations()) {
            count += org.memberCount;
        }
        return count;
    }

    /**
     * @param userInfo
     * @param checked
     * @return 选择成功，则返回true；否则，失败
     */
    public boolean checkUser(UIUserInfo userInfo, boolean checked) {
        if (checked) {
            if (getTotalCheckedCount() >= maxPickCount) {
                return false;
            }
        }
        if (checked) {
            if (!checkedUsers.stream().anyMatch(ui -> TextUtils.equals(userInfo.getUserInfo().uid, ui.getUserInfo().uid))) {
                checkedUsers.add(userInfo);
            }
        } else {
            checkedUsers.removeIf(e -> TextUtils.equals(e.getUserInfo().uid, userInfo.getUserInfo().uid));
        }
        if (userCheckStatusUpdateLiveData != null) {
            userCheckStatusUpdateLiveData.setValue(new Object());
        }
        return true;
    }

    public boolean checkEmployee(Employee employee, boolean checked) {
        if (checked) {
            if (getTotalCheckedCount() >= maxPickCount) {
                return false;
            }
        }
        if (checked) {
            if (!checkedEmployees.stream().anyMatch(e -> TextUtils.equals(employee.employeeId, e.employeeId))) {
                checkedEmployees.add(employee);
            }
        } else {
            checkedEmployees.removeIf(e -> TextUtils.equals(e.employeeId, employee.employeeId));
        }
        if (userCheckStatusUpdateLiveData != null) {
            userCheckStatusUpdateLiveData.setValue(new Object());
        }
        return true;
    }

    public boolean checkOrganization(Organization org, boolean checked) {
        if (checked) {
            if (getTotalCheckedCount() >= maxPickCount) {
                return false;
            }
        }
        if (checked) {
            if (!checkedOrganizations.stream().anyMatch(o -> o.id == org.id)) {
                checkedOrganizations.add(org);
            }
        } else {
            checkedOrganizations.removeIf(o -> o.id == org.id);
        }
        if (userCheckStatusUpdateLiveData != null) {
            userCheckStatusUpdateLiveData.setValue(new Object());
        }
        return true;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
    }
}
