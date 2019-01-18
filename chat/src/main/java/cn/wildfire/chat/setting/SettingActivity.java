package cn.wildfire.chat.setting;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import cn.wildfirechat.chat.R;
import cn.wildfire.chat.ChatManagerHolder;

import butterknife.OnClick;
import cn.wildfire.chat.WfcBaseActivity;
import cn.wildfire.chat.main.SplashActivity;

public class SettingActivity extends WfcBaseActivity {

    @Override
    protected int contentLayout() {
        return R.layout.setting_activity;
    }

    @OnClick(R.id.exitOptionItemView)
    void exit() {
        ChatManagerHolder.gChatManager.disconnect(true);
        SharedPreferences sp = getSharedPreferences("config", Context.MODE_PRIVATE);
        sp.edit().clear().apply();

        Intent intent = new Intent(this, SplashActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @OnClick(R.id.newMsgNotifyOptionItemView)
    void notifySetting() {

    }
}
