/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.settings;

import android.content.SharedPreferences;
import android.widget.Toast;

import com.kyleduo.switchbutton.SwitchButton;

import butterknife.BindView;
import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GeneralCallback;

public class MessageNotifySettingActivity extends WfcBaseActivity {
    @BindView(R2.id.switchMsgNotification)
    SwitchButton switchMsgNotification;
    @BindView(R2.id.switchVoipNotification)
    SwitchButton switchVoipNotification;
    @BindView(R2.id.switchShowMsgDetail)
    SwitchButton switchShowMsgDetail;
    @BindView(R2.id.switchUserReceipt)
    SwitchButton switchUserReceipt;
    @BindView(R2.id.switchSyncDraft)
    SwitchButton switchSyncDraft;
    @BindView(R2.id.switchPtt)
    SwitchButton switchPtt;

    @Override
    protected int contentLayout() {
        return R.layout.activity_msg_notify_settings;
    }

    @Override
    protected void afterViews() {
        super.afterViews();

        switchMsgNotification.setChecked(!ChatManager.Instance().isGlobalSilent());
        switchShowMsgDetail.setChecked(!ChatManager.Instance().isHiddenNotificationDetail());

        switchMsgNotification.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ChatManager.Instance().setGlobalSilent(!isChecked, new GeneralCallback() {
                @Override
                public void onSuccess() {

                }

                @Override
                public void onFail(int errorCode) {
                    Toast.makeText(MessageNotifySettingActivity.this, "网络错误", Toast.LENGTH_SHORT);
                }
            });
        });

        switchVoipNotification.setChecked(!ChatManager.Instance().isVoipSilent());
        switchVoipNotification.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ChatManager.Instance().setVoipSilent(!isChecked, new GeneralCallback() {
                @Override
                public void onSuccess() {

                }

                @Override
                public void onFail(int errorCode) {
                    Toast.makeText(MessageNotifySettingActivity.this, "网络错误", Toast.LENGTH_SHORT);
                }
            });
        });

        switchShowMsgDetail.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ChatManager.Instance().setHiddenNotificationDetail(!isChecked, new GeneralCallback() {
                @Override
                public void onSuccess() {

                }

                @Override
                public void onFail(int errorCode) {
                    Toast.makeText(MessageNotifySettingActivity.this, "网络错误", Toast.LENGTH_SHORT);
                }
            });
        });

        switchUserReceipt.setChecked(ChatManager.Instance().isUserEnableReceipt());
        switchUserReceipt.setOnCheckedChangeListener((compoundButton, b) -> ChatManager.Instance().setUserEnableReceipt(b, new GeneralCallback() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onFail(int errorCode) {
                Toast.makeText(MessageNotifySettingActivity.this, "网络错误", Toast.LENGTH_SHORT);
            }
        }));

        switchSyncDraft.setChecked(!ChatManager.Instance().isDisableSyncDraft());
        switchSyncDraft.setOnCheckedChangeListener((buttonView, isChecked) -> ChatManager.Instance().setDisableSyncDraft(!isChecked, new GeneralCallback() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onFail(int errorCode) {

            }
        }));

        SharedPreferences sp = getSharedPreferences(Config.SP_CONFIG_FILE_NAME, MODE_PRIVATE);
        boolean pttEnabled = sp.getBoolean("pttEnabled", true);
        switchPtt.setChecked(pttEnabled);
        switchSyncDraft.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sp.edit().putBoolean("pttEnabled", isChecked).apply();
            Toast.makeText(this, "开关对讲功能，重新启动应用生效", Toast.LENGTH_SHORT).show();
        });
    }
}
