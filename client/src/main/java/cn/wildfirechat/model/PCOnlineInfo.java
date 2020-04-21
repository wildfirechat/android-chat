package cn.wildfirechat.model;

import android.text.TextUtils;

public class PCOnlineInfo {

    /**
     * PC在线类型
     * <p>
     * - PC_Online: PC客户端在线
     * - Web_Online: Web客户端在线
     * - WX_Online: WX小程序客户端在线
     */
    public enum PCOnlineType {
        PC_Online,
        Web_Online,
        WX_Online
    }

    private PCOnlineType type;
    private boolean isOnline;
    private String clientId;
    private String clientName;
    private long timestamp;

    public PCOnlineType getType() {
        return type;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientName() {
        return clientName;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public static PCOnlineInfo infoFromStr(String value, PCOnlineType type) {
        if (TextUtils.isEmpty(value)) {
            return null;
        }
        String[] parts = value.split("\\|");
        if (parts.length >= 4) {
            PCOnlineInfo info = new PCOnlineInfo();
            info.type = type;
            info.timestamp = Long.parseLong(parts[0]);
            info.clientId = parts[2];
            info.clientName = parts[3];
            info.isOnline = true;
            return info;
        }

        return null;
    }

}
