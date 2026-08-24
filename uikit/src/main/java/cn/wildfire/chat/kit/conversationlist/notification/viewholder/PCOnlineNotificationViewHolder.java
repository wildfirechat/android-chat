/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversationlist.notification.viewholder;

import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.page.WfcPageCompat;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.annotation.StatusNotificationType;
import cn.wildfire.chat.kit.conversationlist.notification.PCOnlineStatusNotification;
import cn.wildfire.chat.kit.conversationlist.notification.StatusNotification;
import cn.wildfire.chat.kit.pc.PCOnlineInfoUtil;
import cn.wildfire.chat.kit.pc.PCSessionActivity;
import cn.wildfirechat.model.PCOnlineInfo;
import cn.wildfirechat.remote.ChatManager;

/**
 * 多端登录提醒条（一条承载全部在线设备，微信风格）：
 * 单台显示「&lt;平台名&gt; 已登录」+ 平台灰色图标，多台显示「&lt;N&gt;个设备已经登录」+ 电脑图标；
 * 点击进入「已登录的设备」页并传入全部在线设备。
 */
@StatusNotificationType(PCOnlineStatusNotification.class)
public class PCOnlineNotificationViewHolder extends StatusNotificationViewHolder {
    TextView statusTextView;
    ImageView iconImageView;
    List<PCOnlineInfo> pcOnlineInfos;

    public PCOnlineNotificationViewHolder(Fragment fragment) {
        super(fragment);
    }

    @Override
    public void onBind(View view, StatusNotification notification) {
        PCOnlineStatusNotification pcOnlineStatusNotification = (PCOnlineStatusNotification) notification;
        pcOnlineInfos = pcOnlineStatusNotification.getPcOnlineInfos();
        int deviceCount = pcOnlineInfos == null ? 0 : pcOnlineInfos.size();

        String desc;
        if (deviceCount == 1) {
            // 单台：<平台名> 已登录
            desc = PCOnlineInfoUtil.deviceName(fragment.getContext(), pcOnlineInfos.get(0))
                + " " + fragment.getString(R.string.pc_online_status_logged_in);
        } else {
            // 多台：<N>个设备已经登录
            desc = fragment.getString(R.string.pc_devices_logged_in, deviceCount);
        }
        // 静音后缀（与 HarmonyOS 版一致：仅单台时拼接）
        if (deviceCount == 1 && ChatManager.Instance().isMuteNotificationWhenPcOnline()) {
            desc += fragment.getString(R.string.notification_muted_when_pc_online);
        }

        iconImageView = view.findViewById(R.id.pc_image_view);
        statusTextView = view.findViewById(R.id.statusTextView);
        statusTextView.setText(desc);
        // 单台设备显示对应平台图标；多台设备显示电脑图标
        iconImageView.setImageResource(deviceCount == 1
            ? PCOnlineInfoUtil.platformIconRes(pcOnlineInfos.get(0))
            : R.drawable.ic_pc_computer);

        view.setOnClickListener(v -> showPCSessionInfo());
    }

    public void showPCSessionInfo() {
        Intent intent = new Intent(fragment.getActivity(), PCSessionActivity.class);
        if (pcOnlineInfos != null) {
            intent.putParcelableArrayListExtra("pcOnlineInfos", new ArrayList<>(pcOnlineInfos));
        }
        WfcPageCompat.startPage(fragment, intent);
    }
}
