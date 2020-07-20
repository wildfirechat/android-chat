package cn.wildfire.chat.kit.settings.blacklist;

import androidx.lifecycle.ViewModel;

import java.util.List;

import cn.wildfirechat.remote.ChatManager;

public class BlacklistViewModel extends ViewModel {

    public BlacklistViewModel() {
        super();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
    }

    public List<String> getBlacklists() {
        return ChatManager.Instance().getBlackList(true);
    }

}
