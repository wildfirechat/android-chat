/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.pc;

import android.content.Intent;
import android.widget.Button;
import android.widget.ImageView;
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
    ImageView muteImageView;

    private PCOnlineInfo pcOnlineInfo;
    private boolean isMuteWhenPCOnline = false;

    protected void bindEvents() {
        super.bindEvents();
        findViewById(R.id.kickOffPCButton).setOnClickListener(v -> kickOffPC());
        findViewById(R.id.muteImageView).setOnClickListener(v -> mutePhone());
        findViewById(R.id.fileHelperImageView).setOnClickListener(v -> fileHelper());
    }

    protected void bindViews() {
        super.bindViews();
        kickOffPCButton = findViewById(R.id.kickOffPCButton);
        descTextView = findViewById(R.id.descTextView);
        muteImageView = findViewById(R.id.muteImageView);
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
        if (pcOnlineInfo == null){
            return;
        }
        Platform platform = pcOnlineInfo.getPlatform();
        setTitle(platform.getPlatFormName() + " 已登录");
        kickOffPCButton.setText("退出 " + platform.getPlatFormName() + " 登录");
        descTextView.setText(platform.getPlatFormName() + " 已登录");

        isMuteWhenPCOnline = ChatManager.Instance().isMuteNotificationWhenPcOnline();
        muteImageView.setImageResource(isMuteWhenPCOnline ? R.mipmap.ic_turn_off_ringer_hover : R.mipmap.ic_turn_off_ringer);
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
                Toast.makeText(PCSessionActivity.this, pcOnlineInfo.getPlatform() + " 已踢下线", Toast.LENGTH_SHORT).show();
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

    void mutePhone() {
        ChatManager.Instance().muteNotificationWhenPcOnline(!isMuteWhenPCOnline, new GeneralCallback() {
            @Override
            public void onSuccess() {
                if (isFinishing()) {
                    return;
                }
                Toast.makeText(PCSessionActivity.this, "操作成功", Toast.LENGTH_SHORT).show();
                isMuteWhenPCOnline = !isMuteWhenPCOnline;
                muteImageView.setImageResource(isMuteWhenPCOnline ? R.mipmap.ic_turn_off_ringer_hover : R.mipmap.ic_turn_off_ringer);
            }

            @Override
            public void onFail(int errorCode) {
                if (isFinishing()) {
                    return;
                }
                Toast.makeText(PCSessionActivity.this, "操作失败 " + errorCode, Toast.LENGTH_SHORT).show();
            }
        });
    }

    void fileHelper() {
        Intent intent = ConversationActivity.buildConversationIntent(this, Conversation.ConversationType.Single, Config.FILE_TRANSFER_ID, 0);
        startActivity(intent);
    }
}
