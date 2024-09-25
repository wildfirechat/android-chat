/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit;

import android.text.TextUtils;

public interface WfcScheme {
    String QR_CODE_PREFIX_PC_SESSION = "wildfirechat://pcsession/";
    String QR_CODE_PREFIX_USER = "wildfirechat://user/";
    String QR_CODE_PREFIX_GROUP = "wildfirechat://group/";
    String QR_CODE_PREFIX_CHANNEL = "wildfirechat://channel/";
    String QR_CODE_PREFIX_CONFERENCE = "wildfirechat://conference/";

    static String buildConferenceScheme(String conferenceId, String password) {
        String value = QR_CODE_PREFIX_CONFERENCE + conferenceId;
        if (!TextUtils.isEmpty(password)) {
            value += "/?pwd=" + password;
        }
        return value;
    }

    static String buildGroupScheme(String groupId, String source) {
        String value = QR_CODE_PREFIX_GROUP + groupId;
        if (!TextUtils.isEmpty(source)) {
            value += "?from=" + source;
        }
        return value;
    }
}
