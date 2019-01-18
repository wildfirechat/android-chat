package cn.wildfire.chat.user;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import cn.wildfire.chat.common.OperateResult;
import cn.wildfirechat.model.ModifyMyInfoEntry;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GeneralCallback;
import cn.wildfirechat.remote.OnSettingUpdateListener;
import cn.wildfirechat.remote.OnUserInfoUpdateListener;

// FIXME: 2019/1/4 应该是个单例的
public class UserViewModel extends ViewModel implements OnUserInfoUpdateListener, OnSettingUpdateListener {
    private MutableLiveData<List<UserInfo>> userInfoLiveData;
    private MutableLiveData<Object> settingUpdatedLiveData;

    public UserViewModel() {
        ChatManager.Instance().addUserInfoUpdateListener(this);
        ChatManager.Instance().addSettingUpdateListener(this);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        ChatManager.Instance().removeUserInfoUpdateListener(this);
        ChatManager.Instance().removeSettingUpdateListener(this);
    }

    public MutableLiveData<List<UserInfo>> userInfoLiveData() {
        if (userInfoLiveData == null) {
            userInfoLiveData = new MutableLiveData<>();
        }
        return userInfoLiveData;
    }

    public MutableLiveData<Object> settingUpdatedLiveData() {
        if (settingUpdatedLiveData == null) {
            settingUpdatedLiveData = new MutableLiveData<>();
        }
        return settingUpdatedLiveData;
    }

    public MutableLiveData<OperateResult<Boolean>> modifyMyInfo(List<ModifyMyInfoEntry> values) {
        MutableLiveData<OperateResult<Boolean>> result = new MutableLiveData<>();
        ChatManager.Instance().modifyMyInfo(values, new GeneralCallback() {
            @Override
            public void onSuccess() {
                result.setValue(new OperateResult<>(true, 0));
            }

            @Override
            public void onFailure(int errorCode) {
                result.setValue(new OperateResult<>(false, errorCode));
            }
        });
        return result;
    }

    public UserInfo getUserInfo(String userId, boolean refresh) {
        return ChatManager.Instance().getUserInfo(userId, refresh);
    }

    public List<UserInfo> getUserInfos(List<String> userIds) {
        return ChatManager.Instance().getUserInfos(userIds);
    }

    public String getUserId() {
        return ChatManager.Instance().getUserId();
    }

    @Override
    public void onUserInfoUpdated(List<UserInfo> userInfos) {
        if (userInfoLiveData != null) {
            userInfoLiveData.setValue(userInfos);
        }
    }

    @Override
    public void onSettingUpdated() {
        if (settingUpdatedLiveData != null) {
            settingUpdatedLiveData.setValue(new Object());
        }
    }
}
