/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversationlist.notification.viewholder;

import android.content.Intent;
import android.view.View;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.annotation.StatusNotificationType;
import cn.wildfire.chat.kit.conversationlist.notification.PCOnlineStatusNotification;
import cn.wildfire.chat.kit.conversationlist.notification.StatusNotification;
import cn.wildfire.chat.kit.pc.PCSessionActivity;
import cn.wildfirechat.model.PCOnlineInfo;
import cn.wildfirechat.remote.ChatManager;

@StatusNotificationType(PCOnlineStatusNotification.class)
public class PCOnlineNotificationViewHolder extends StatusNotificationViewHolder {
    TextView statusTextView;
    PCOnlineInfo pcOnlineInfo;

    public PCOnlineNotificationViewHolder(Fragment fragment) {
        super(fragment);
    }

    @Override
    public void onBind(View view, StatusNotification notification) {
        PCOnlineStatusNotification pcOnlineStatusNotification = (PCOnlineStatusNotification) notification;
        pcOnlineInfo = pcOnlineStatusNotification.getPcOnlineInfo();
        String desc = "";
        switch (pcOnlineStatusNotification.getPcOnlineInfo().getType()) {
            case PC_Online:
                desc = fragment.getString(R.string.pc_online);
                break;
            case Web_Online:
                desc = fragment.getString(R.string.web_online);
                break;
            case WX_Online:
                desc = fragment.getString(R.string.wx_online);
                break;
            case Pad_Online:
                desc = fragment.getString(R.string.pad_online);
                break;
            default:
                break;
        }
        if (ChatManager.Instance().isMuteNotificationWhenPcOnline()) {
            desc += fragment.getString(R.string.notification_muted_when_pc_online);
        }

        statusTextView = view.findViewById(R.id.statusTextView);
        statusTextView.setText(desc);
        statusTextView.setOnClickListener(v -> showPCSessionInfo());
    }

    public void showPCSessionInfo() {
        Intent intent = new Intent(fragment.getActivity(), PCSessionActivity.class);
        intent.putExtra("pcOnlineInfo", pcOnlineInfo);
        fragment.startActivity(intent);
    }
}
