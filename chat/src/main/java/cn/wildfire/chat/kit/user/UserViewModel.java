package cn.wildfire.chat.kit.user;

import java.util.ArrayList;
import java.util.List;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import cn.wildfire.chat.kit.common.OperateResult;
import cn.wildfire.chat.kit.third.utils.FileUtils;
import cn.wildfirechat.message.MessageContentMediaType;
import cn.wildfirechat.model.ModifyMyInfoEntry;
import cn.wildfirechat.model.ModifyMyInfoType;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GeneralCallback;
import cn.wildfirechat.remote.GeneralCallback2;
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

    public MutableLiveData<OperateResult<Boolean>> updateUserPortrait(String localImagePath) {
        MutableLiveData<OperateResult<Boolean>> resultLiveData = new MutableLiveData<>();
        byte[] content = FileUtils.readFile(localImagePath);
        if (content != null) {
            ChatManager.Instance().uploadMedia(content, MessageContentMediaType.PORTRAIT.getValue(), new GeneralCallback2() {
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
                        public void onFailure(int errorCode) {
                            resultLiveData.setValue(new OperateResult<>(errorCode));
                        }
                    });
                }

                @Override
                public void onFailure(int errorCode) {
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
            public void onFailure(int errorCode) {
                result.setValue(new OperateResult<>(false, errorCode));
            }
        });
        return result;
    }

    public UserInfo getUserInfo(String userId, boolean refresh) {
        return ChatManager.Instance().getUserInfox(userId, refresh);
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
