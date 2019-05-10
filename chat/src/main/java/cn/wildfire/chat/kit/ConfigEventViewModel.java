package cn.wildfire.chat.kit;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import cn.wildfire.chat.kit.common.AppScopeViewModel;

public class ConfigEventViewModel extends ViewModel implements AppScopeViewModel {
    private MutableLiveData<Event<Boolean>> showGroupAliasLiveData;

    public MutableLiveData<Event<Boolean>> showGroupAliasLiveData() {
        if (showGroupAliasLiveData == null) {
            showGroupAliasLiveData = new MutableLiveData<>();
        }
        return showGroupAliasLiveData;
    }

    public void postGroupAliasEvent(String groupId, boolean show) {
        if (showGroupAliasLiveData != null) {
            showGroupAliasLiveData.setValue(new Event<>(show));
        }
    }
}
