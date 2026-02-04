package cn.wildfire.chat.app.setting.backup;

import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

import cn.wildfirechat.model.ConversationInfo;

public class PickBackupConversationViewModel extends ViewModel {
    private List<ConversationInfo> selectedConversations;
    private boolean includeMedia = true;

    public List<ConversationInfo> getSelectedConversations() {
        return selectedConversations;
    }

    public void setSelectedConversations(List<ConversationInfo> selectedConversations) {
        this.selectedConversations = selectedConversations;
    }

    public boolean isIncludeMedia() {
        return includeMedia;
    }

    public void setIncludeMedia(boolean includeMedia) {
        this.includeMedia = includeMedia;
    }
}
