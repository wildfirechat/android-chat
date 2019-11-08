package cn.wildfire.chat.kit;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.OnConnectionStatusChangeListener;

public class IMConnectionStatusViewModel extends ViewModel implements OnConnectionStatusChangeListener {
    private MutableLiveData<Integer> connectionStatusLiveData = new MutableLiveData<>();

    public IMConnectionStatusViewModel() {
        ChatManager.Instance().addConnectionChangeListener(this);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        ChatManager.Instance().removeConnectionChangeListener(this);
    }


    public MutableLiveData<Integer> connectionStatusLiveData() {
        int status = ChatManager.Instance().getConnectionStatus();
        connectionStatusLiveData.setValue(status);
        return connectionStatusLiveData;
    }


    @Override
    public void onConnectionStatusChange(int status) {
        if (connectionStatusLiveData != null) {
            connectionStatusLiveData.setValue(status);
        }
    }
}
