/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.pc;

import android.content.Intent;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.conversation.ConversationActivity;
import cn.wildfirechat.client.Platform;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.PCOnlineInfo;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GeneralCallback;

public class PCSessionActivity extends WfcBaseActivity {

    Button kickOffPCButton;
    TextView descTextView;
    ImageView pcImageView;
    Switch muteSwitch;
    Switch lockSwitch;
    LinearLayout fileHelperLayout;

    private PCOnlineInfo pcOnlineInfo;
    private boolean isMuteWhenPCOnline = false;

    protected void bindEvents() {
        super.bindEvents();
        kickOffPCButton.setOnClickListener(v -> kickOffPC());
        muteSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> mutePhone(isChecked));
        lockSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> lockPC(isChecked));
        fileHelperLayout.setOnClickListener(v -> fileHelper());
    }

    protected void bindViews() {
        super.bindViews();
        kickOffPCButton = findViewById(R.id.kickOffPCButton);
        descTextView = findViewById(R.id.descTextView);
        pcImageView = findViewById(R.id.pcImageView);
        muteSwitch = findViewById(R.id.muteSwitch);
        lockSwitch = findViewById(R.id.lockSwitch);
        fileHelperLayout = findViewById(R.id.fileHelperLayout);
    }

    @Override
    protected void beforeViews() {
        pcOnlineInfo = getIntent().getParcelableExtra("pcOnlineInfo");
        if (pcOnlineInfo == null) {
            finish();
        }
    }

    @Override
    protected void afterViews() {
        if (pcOnlineInfo == null) {
            return;
        }
        Platform platform = pcOnlineInfo.getPlatform();
        setTitle(platform.getPlatFormName() + " " + getString(R.string.pc_online_status_logged_in));
        kickOffPCButton.setText(getString(R.string.pc_session_logout_button, platform.getPlatFormName()));
        descTextView.setText(platform.getPlatFormName() + " " + getString(R.string.pc_online_status_logged_in));

        isMuteWhenPCOnline = ChatManager.Instance().isMuteNotificationWhenPcOnline();
        muteSwitch.setChecked(isMuteWhenPCOnline);
        
        // 读取锁定状态
        boolean isLocked = ChatManager.Instance().isLockPCClient(pcOnlineInfo.getClientId());
        lockSwitch.setChecked(isLocked);
    }

    @Override
    protected int contentLayout() {
        return R.layout.pc_session_activity;
    }

    void kickOffPC() {
        ChatManager.Instance().kickoffPCClient(pcOnlineInfo.getClientId(), new GeneralCallback() {
            @Override
            public void onSuccess() {
                if (isFinishing()) {
                    return;
                }
                Toast.makeText(PCSessionActivity.this, pcOnlineInfo.getPlatform() + " " + getString(R.string.pc_kicked_offline), Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFail(int errorCode) {
                if (isFinishing()) {
                    return;
                }
                Toast.makeText(PCSessionActivity.this, "" + errorCode, Toast.LENGTH_SHORT).show();
            }
        });
    }

    void mutePhone(boolean isMute) {
        ChatManager.Instance().muteNotificationWhenPcOnline(isMute, new GeneralCallback() {
            @Override
            public void onSuccess() {
                if (isFinishing()) {
                    return;
                }
                Toast.makeText(PCSessionActivity.this, getString(R.string.operation_success), Toast.LENGTH_SHORT).show();
                isMuteWhenPCOnline = isMute;
            }

            @Override
            public void onFail(int errorCode) {
                if (isFinishing()) {
                    return;
                }
                Toast.makeText(PCSessionActivity.this, getString(R.string.operation_failed) + " " + errorCode, Toast.LENGTH_SHORT).show();
                // 恢复开关状态
                muteSwitch.setChecked(!isMute);
            }
        });
    }

    void lockPC(boolean isLock) {
        ChatManager.Instance().lockPCClient(pcOnlineInfo.getClientId(), isLock, new GeneralCallback() {
            @Override
            public void onSuccess() {
                if (isFinishing()) {
                    return;
                }
                Toast.makeText(PCSessionActivity.this, getString(R.string.operation_success), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFail(int errorCode) {
                if (isFinishing()) {
                    return;
                }
                Toast.makeText(PCSessionActivity.this, getString(R.string.operation_failed) + " " + errorCode, Toast.LENGTH_SHORT).show();
                // 恢复开关状态
                lockSwitch.setChecked(!isLock);
            }
        });
    }

    void fileHelper() {
        Intent intent = ConversationActivity.buildConversationIntent(this, Conversation.ConversationType.Single, Config.FILE_TRANSFER_ID, 0);
        startActivity(intent);
    }
}
