/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversationlist.notification;

import java.util.List;
import java.util.Objects;

import cn.wildfirechat.model.PCOnlineInfo;

/**
 * 多端登录提醒。与 HarmonyOS 版一致：所有在线设备合并成<strong>一条</strong>提醒，
 * 由 {@code getPcOnlineInfos()} 承载全部设备，点击后把整份列表带进「已登录的设备」页。
 */
public class PCOnlineStatusNotification extends StatusNotification {
    private List<PCOnlineInfo> pcOnlineInfos;

    public PCOnlineStatusNotification(List<PCOnlineInfo> pcOnlineInfos) {
        this.pcOnlineInfos = pcOnlineInfos;
    }

    public List<PCOnlineInfo> getPcOnlineInfos() {
        return pcOnlineInfos;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        PCOnlineStatusNotification that = (PCOnlineStatusNotification) o;

        return Objects.equals(pcOnlineInfos, that.pcOnlineInfos);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (pcOnlineInfos != null ? pcOnlineInfos.hashCode() : 0);
        return result;
    }
}
