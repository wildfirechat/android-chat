/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.user;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.common.AppScopeViewModel;
import cn.wildfire.chat.kit.common.OperateResult;
import cn.wildfire.chat.kit.third.utils.FileUtils;
import cn.wildfirechat.message.MessageContentMediaType;
import cn.wildfirechat.model.DomainInfo;
import cn.wildfirechat.model.ModifyMyInfoEntry;
import cn.wildfirechat.model.ModifyMyInfoType;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GeneralCallback;
import cn.wildfirechat.remote.OnDomainInfoUpdateListener;
import cn.wildfirechat.remote.OnUserInfoUpdateListener;
import cn.wildfirechat.remote.UploadMediaCallback;
import cn.wildfirechat.utils.WfcUtils;

public class UserViewModel extends ViewModel implements AppScopeViewModel, OnUserInfoUpdateListener, OnDomainInfoUpdateListener {
    private MutableLiveData<List<UserInfo>> userInfoLiveData;
    private MutableLiveData<DomainInfo> domainInfoLiveData;

    public UserViewModel() {
        ChatManager.Instance().addUserInfoUpdateListener(this);
        ChatManager.Instance().addDomainInfoUpdateListener(this);
    }

    public static List<UserInfo> getUsers(List<String> ids, String groupId) {
        return ChatManager.Instance().getUserInfos(ids, groupId);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        ChatManager.Instance().removeUserInfoUpdateListener(this);
        ChatManager.Instance().removeDomainInfoUpdateListener(this);
    }

    public MutableLiveData<List<UserInfo>> userInfoLiveData() {
        if (userInfoLiveData == null) {
            userInfoLiveData = new MutableLiveData<>();
        }
        return userInfoLiveData;
    }

    public MutableLiveData<DomainInfo> domainInfoLiveData() {
        if (domainInfoLiveData == null) {
            domainInfoLiveData = new MutableLiveData<>();
        }
        return domainInfoLiveData;
    }

    public MutableLiveData<OperateResult<Boolean>> updateUserPortrait(String localImagePath) {
        MutableLiveData<OperateResult<Boolean>> resultLiveData = new MutableLiveData<>();
        byte[] content = FileUtils.readFile(localImagePath);
        if (content != null) {
            ChatManager.Instance().uploadMediaFile(localImagePath, MessageContentMediaType.PORTRAIT.getValue(), new UploadMediaCallback() {
                @Override
                public void onSuccess(String result) {
                    List<ModifyMyInfoEntry> entries = new ArrayList<>();
                    entries.add(new ModifyMyInfoEntry(ModifyMyInfoType.Modify_Portrait, result));
                    ChatManager.Instance().modifyMyInfo(entries, new GeneralCallback() {
                        @Override
                        public void onSuccess() {
                            resultLiveData.setValue(new OperateResult<Boolean>(true, 0));
                        }

                        @Override
                        public void onFail(int errorCode) {
                            resultLiveData.setValue(new OperateResult<>(errorCode));
                        }
                    });
                }

                @Override
                public void onProgress(long uploaded, long total) {

                }


                @Override
                public void onFail(int errorCode) {
                    resultLiveData.setValue(new OperateResult<>(errorCode));
                }
            });
        }
        return resultLiveData;
    }

    public MutableLiveData<OperateResult<Boolean>> modifyMyInfo(List<ModifyMyInfoEntry> values) {
        MutableLiveData<OperateResult<Boolean>> result = new MutableLiveData<>();
        ChatManager.Instance().modifyMyInfo(values, new GeneralCallback() {
            @Override
            public void onSuccess() {
                result.setValue(new OperateResult<>(true, 0));
            }

            @Override
            public void onFail(int errorCode) {
                result.setValue(new OperateResult<>(false, errorCode));
            }
        });
        return result;
    }

    public LiveData<UserInfo> getUserInfoAsync(String userId, boolean refresh) {
        MutableLiveData<UserInfo> data = new MutableLiveData<>();
        ChatManager.Instance().getWorkHandler().post(() -> {
            UserInfo userInfo = ChatManager.Instance().getUserInfo(userId, refresh);
            data.postValue(userInfo);
        });
        return data;
    }

    public UserInfo getUserInfo(String userId, boolean refresh) {
        return ChatManager.Instance().getUserInfo(userId, refresh);
    }
    public void refreshUserInfo(String userId) {
        ChatManager.Instance().getUserInfo(userId, true); // 强制从服务器获取
    }

    public UserInfo getUserInfo(String userId, String groupId, boolean refresh) {
        return ChatManager.Instance().getUserInfo(userId, groupId, refresh);
    }

    public String getUserDisplayName(UserInfo userInfo) {
        return ChatManager.Instance().getUserDisplayName(userInfo);
    }

    public CharSequence getUserDisplayNameEx(UserInfo userInfo) {
        String displayName = ChatManager.Instance().getUserDisplayName(userInfo);
        if (WfcUtils.isExternalTarget(userInfo.uid)) {
            return WfcUtils.buildExternalDisplayNameSpannableString(displayName, 14);
        } else {
            return displayName;
        }
    }

    public List<UserInfo> getUserInfos(List<String> userIds) {
        return ChatManager.Instance().getUserInfos(userIds, null);
    }

    public String getUserId() {
        return ChatManager.Instance().getUserId();
    }

    public String getFriendAlias(String userId) {
        return ChatManager.Instance().getFriendAlias(userId);
    }

    public MutableLiveData<OperateResult<Integer>> setFriendAlias(String userId, String alias) {
        MutableLiveData<OperateResult<Integer>> data = new MutableLiveData<>();
        ChatManager.Instance().setFriendAlias(userId, alias, new GeneralCallback() {
            @Override
            public void onSuccess() {
                data.setValue(new OperateResult<>(0));
                UserInfo userInfo = ChatManager.Instance().getUserInfo(userId, false);
                if (userInfo != null) {
                    List<UserInfo> userInfos = new ArrayList<>();
                    userInfos.add(userInfo);
                    userInfoLiveData.setValue(userInfos);
                }
            }

            @Override
            public void onFail(int errorCode) {
                data.setValue(new OperateResult<>(errorCode));
            }
        });
        return data;
    }

    public String getUserSetting(int scope, String key) {
        return ChatManager.Instance().getUserSetting(scope, key);
    }

    public MutableLiveData<OperateResult<Integer>> setUserSetting(int scope, String key, String value) {
        MutableLiveData<OperateResult<Integer>> result = new MutableLiveData<>();
        ChatManager.Instance().setUserSetting(scope, key, value, new GeneralCallback() {
            @Override
            public void onSuccess() {
                result.setValue(new OperateResult<>(0));
            }

            @Override
            public void onFail(int errorCode) {
                result.setValue(new OperateResult<>(errorCode));

            }
        });
        return result;
    }

    @Override
    public void onUserInfoUpdate(List<UserInfo> userInfos) {
        if (userInfoLiveData != null && userInfos != null && !userInfos.isEmpty()) {
            userInfoLiveData.setValue(userInfos);
        }
    }

    @Override
    public void onDomainInfoUpdate(DomainInfo domainInfo) {
        if (domainInfo != null && domainInfoLiveData != null) {
            domainInfoLiveData.setValue(domainInfo);
        }
    }
}
