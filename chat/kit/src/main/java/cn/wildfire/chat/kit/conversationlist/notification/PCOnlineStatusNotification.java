package cn.wildfire.chat.kit.conversationlist.notification;

import cn.wildfirechat.model.PCOnlineInfo;

public class PCOnlineStatusNotification extends StatusNotification {
    private PCOnlineInfo pcOnlineInfo;

    public PCOnlineStatusNotification(PCOnlineInfo info) {
        this.pcOnlineInfo = info;
    }

    public PCOnlineInfo getPcOnlineInfo() {
        return pcOnlineInfo;
    }
}
