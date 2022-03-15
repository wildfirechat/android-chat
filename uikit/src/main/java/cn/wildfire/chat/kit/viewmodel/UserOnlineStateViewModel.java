/*
 * Copyright (c) 2022 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.Map;

import cn.wildfirechat.model.UserOnlineState;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.OnUserOnlineEventListener;
import cn.wildfirechat.remote.WatchOnlineStateCallback;

public class UserOnlineStateViewModel extends ViewModel implements OnUserOnlineEventListener {
    private MutableLiveData<Map<String, UserOnlineState>> userOnlineStateLiveData = new MutableLiveData<>();

    public UserOnlineStateViewModel() {
        ChatManager.Instance().addUserOnlineEventListener(this);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        ChatManager.Instance().removeUserOnlineEventListener(this);
    }

    @Override
    public void onUserOnlineEvent(Map<String, UserOnlineState> userOnlineStateMap) {
        userOnlineStateLiveData.postValue(userOnlineStateMap);
    }

    public MutableLiveData<Map<String, UserOnlineState>> getUserOnlineStateLiveData() {
        return userOnlineStateLiveData;
    }

    public void watchUserOnlineState(int conversationType, String[] targets) {
        ChatManager.Instance().watchOnlineState(conversationType, targets, 60, new WatchOnlineStateCallback() {
            @Override
            public void onSuccess(UserOnlineState[] userOnlineStates) {
                userOnlineStateLiveData.postValue(ChatManager.Instance().getUserOnlineStateMap());
            }

            @Override
            public void onFail(int errorCode) {

            }
        });
    }

    public void unwatchOnlineState(int conversationType, String[] targets) {
        ChatManager.Instance().unWatchOnlineState(conversationType, targets, null);
    }
}
