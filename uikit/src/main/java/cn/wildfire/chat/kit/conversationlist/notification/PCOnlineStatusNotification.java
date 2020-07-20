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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        PCOnlineStatusNotification that = (PCOnlineStatusNotification) o;

        return pcOnlineInfo != null ? pcOnlineInfo.equals(that.pcOnlineInfo) : that.pcOnlineInfo == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (pcOnlineInfo != null ? pcOnlineInfo.hashCode() : 0);
        return result;
    }
}
