package cn.wildfire.chat.kit;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.IMServiceStatusListener;

// application scope
public class IMServiceStatusViewModel extends ViewModel implements IMServiceStatusListener {
    private MutableLiveData<Boolean> imServiceStatusLiveData = new MutableLiveData<>();

    public IMServiceStatusViewModel() {
        ChatManager.Instance().addIMServiceStatusListener(this);
    }


    @Override
    protected void onCleared() {
        ChatManager.Instance().removeIMServiceStatusListener(this);
    }

    public MutableLiveData<Boolean> imServiceStatusLiveData() {
        boolean binded = ChatManager.Instance().isIMServiceConnected();
        imServiceStatusLiveData.setValue(binded);
        return imServiceStatusLiveData;
    }

    @Override
    public void onServiceConnected() {
        imServiceStatusLiveData.postValue(true);
    }

    @Override
    public void onServiceDisconnected() {
        imServiceStatusLiveData.postValue(false);
    }
}
