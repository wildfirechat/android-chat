package cn.wildfire.chat.app.setting;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import butterknife.OnClick;
import cn.wildfire.chat.kit.ChatManagerHolder;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.app.main.SplashActivity;
import cn.wildfirechat.chat.R;

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

    @OnClick(R.id.aboutOptionItemView)
    void about() {
        Intent intent = new Intent(this, AboutActivity.class);
        startActivity(intent);
    }
}
