/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.pc;

import android.content.Intent;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.OnClick;
import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.conversation.ConversationActivity;
import cn.wildfirechat.client.Platform;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.PCOnlineInfo;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GeneralCallback;

public class PCSessionActivity extends WfcBaseActivity {

    @BindView(R2.id.kickOffPCButton)
    Button kickOffPCButton;
    @BindView(R2.id.descTextView)
    TextView descTextView;
    @BindView(R2.id.muteImageView)
    ImageView muteImageView;

    private PCOnlineInfo pcOnlineInfo;
    private boolean isMuteWhenPCOnline = false;


    @Override
    protected void beforeViews() {
        pcOnlineInfo = getIntent().getParcelableExtra("pcOnlineInfo");
        if (pcOnlineInfo == null) {
            finish();
        }

    }

    @Override
    protected void afterViews() {
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

    @OnClick(R2.id.kickOffPCButton)
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

    @OnClick(R2.id.muteImageView)
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

    @OnClick(R2.id.fileHelperImageView)
    void fileHelper() {
        Intent intent = ConversationActivity.buildConversationIntent(this, Conversation.ConversationType.Single, Config.FILE_TRANSFER_ID, 0);
        startActivity(intent);
    }
}
