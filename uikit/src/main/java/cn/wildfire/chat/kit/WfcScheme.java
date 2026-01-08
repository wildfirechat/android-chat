/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.HashMap;
import java.util.Map;

import cn.wildfire.chat.kit.channel.ChannelInfoActivity;
import cn.wildfire.chat.kit.group.GroupInfoActivity;
import cn.wildfire.chat.kit.user.UserInfoActivity;
import cn.wildfire.chat.kit.voip.conference.ConferenceInfoActivity;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

public class WfcScheme {
    public final static String QR_CODE_PREFIX_PC_SESSION = "wildfirechat://pcsession/";
    public final static String QR_CODE_PREFIX_USER = "wildfirechat://user/";
    public final static String QR_CODE_PREFIX_GROUP = "wildfirechat://group/";
    public final static String QR_CODE_PREFIX_CHANNEL = "wildfirechat://channel/";
    public final static String QR_CODE_PREFIX_CONFERENCE = "wildfirechat://conference/";

    public static String buildConferenceScheme(String conferenceId, String password) {
        String value = QR_CODE_PREFIX_CONFERENCE + conferenceId;
        if (!TextUtils.isEmpty(password)) {
            value += "/?pwd=" + password;
        }
        return value;
    }

    public static String buildGroupScheme(String groupId, String source) {
        String value = QR_CODE_PREFIX_GROUP + groupId;
        if (!TextUtils.isEmpty(source)) {
            value += "?from=" + source;
        }
        return value;
    }

    /**
     * 处理二维码识别结果
     * 参考 MainActivity.onScanPcQrCode() 的实现
     */
    public static void handleQRCodeResult(Context context, String qrCodeText) {
        String prefix;
        String value;

        try {
            int lastSlashIndex = qrCodeText.lastIndexOf('/');
            if (lastSlashIndex <= 0) {
                throw new IllegalArgumentException("Invalid QR code format");
            }

            prefix = qrCodeText.substring(0, lastSlashIndex + 1);
            value = qrCodeText.substring(lastSlashIndex + 1,
                qrCodeText.indexOf('?') > 0 ? qrCodeText.indexOf('?') : qrCodeText.length());

            Uri uri = Uri.parse(qrCodeText);
            Map<String, Object> params = new HashMap<>();
            for (String query : uri.getQueryParameterNames()) {
                params.put(query, uri.getQueryParameter(query));
            }

            Intent intent = null;

            switch (prefix) {
                case WfcScheme.QR_CODE_PREFIX_PC_SESSION:
                    try {
                        Class<?> pcLoginClass = Class.forName("cn.wildfire.chat.app.main.PCLoginActivity");
                        intent = new Intent(context, pcLoginClass)
                            .putExtra("token", value);
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                        Toast.makeText(context, "PC login not supported", Toast.LENGTH_SHORT).show();
                    }
                    break;

                case WfcScheme.QR_CODE_PREFIX_USER:
                    UserInfo userInfo = ChatManager.Instance().getUserInfo(value, true);
                    if (userInfo != null) {
                        intent = new Intent(context, UserInfoActivity.class)
                            .putExtra("userInfo", userInfo);
                    }
                    break;

                case WfcScheme.QR_CODE_PREFIX_GROUP:
                    intent = new Intent(context, GroupInfoActivity.class)
                        .putExtra("groupId", value)
                        .putExtra("from", (String) params.get("from"));
                    break;

                case WfcScheme.QR_CODE_PREFIX_CHANNEL:
                    intent = new Intent(context, ChannelInfoActivity.class)
                        .putExtra("channelId", value);
                    break;

                case WfcScheme.QR_CODE_PREFIX_CONFERENCE:
                    intent = new Intent(context, ConferenceInfoActivity.class)
                        .putExtra("conferenceId", value)
                        .putExtra("password", (String) params.get("pwd"));
                    break;
                default:
                    if (qrCodeText.startsWith("http")) {
                        intent = new Intent(context, WfcWebViewActivity.class);
                        intent.putExtra("url", qrCodeText);
                        intent.putExtra("title", "");
                    }
                    break;
            }

            if (intent != null) {
                context.startActivity(intent);
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 未知的二维码类型，显示文本内容
        new MaterialDialog.Builder(context)
            .title(R.string.qrcode)
            .content(qrCodeText)
            .positiveText(R.string.message_copy)
            .onPositive((dialog, which) -> {
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("QR Code", qrCodeText);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(context, R.string.message_copied, Toast.LENGTH_SHORT).show();
            })
            .negativeText(R.string.cancel)
            .show();
    }

}
