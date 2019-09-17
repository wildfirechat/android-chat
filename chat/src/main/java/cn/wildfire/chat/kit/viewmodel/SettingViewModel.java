package cn.wildfire.chat.kit.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import cn.wildfire.chat.kit.common.OperateResult;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GeneralCallback;
import cn.wildfirechat.remote.OnSettingUpdateListener;

public class SettingViewModel extends ViewModel implements OnSettingUpdateListener {
    private MutableLiveData<Object> settingUpdatedLiveData;

    public SettingViewModel() {
        ChatManager.Instance().addSettingUpdateListener(this);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        ChatManager.Instance().removeSettingUpdateListener(this);
    }

    public MutableLiveData<Object> settingUpdatedLiveData() {
        if (settingUpdatedLiveData == null) {
            settingUpdatedLiveData = new MutableLiveData<>();
        }
        return settingUpdatedLiveData;
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
    public void onSettingUpdate() {
        if (settingUpdatedLiveData != null) {
            settingUpdatedLiveData.setValue(new Object());
        }
    }
}
