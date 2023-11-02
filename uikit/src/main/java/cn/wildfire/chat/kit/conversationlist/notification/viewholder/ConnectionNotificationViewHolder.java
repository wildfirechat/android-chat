/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversationlist.notification.viewholder;

import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.annotation.StatusNotificationType;
import cn.wildfire.chat.kit.conversationlist.notification.ConnectionStatusNotification;
import cn.wildfire.chat.kit.conversationlist.notification.StatusNotification;

@StatusNotificationType(ConnectionStatusNotification.class)
public class ConnectionNotificationViewHolder extends StatusNotificationViewHolder {
    public ConnectionNotificationViewHolder(Fragment fragment) {
        super(fragment);
    }

    TextView statusTextView;

    @Override
    public void onBind(View view, StatusNotification notification) {
        String status = ((ConnectionStatusNotification) notification).getValue();
        statusTextView = view.findViewById(R.id.statusTextView);
        statusTextView.setText(status);
        statusTextView.setOnClickListener(this::onClick);
    }

    public void onClick(View view) {
        Toast.makeText(fragment.getContext(), "status on Click", Toast.LENGTH_SHORT).show();
    }
}
