/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.setting;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.Nullable;

import butterknife.BindView;
import butterknife.OnClick;
import cn.wildfire.chat.app.AppService;
import cn.wildfire.chat.app.main.SplashActivity;
import cn.wildfire.chat.kit.ChatManagerHolder;
import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.net.OKHttpHelper;
import cn.wildfire.chat.kit.net.SimpleCallback;
import cn.wildfire.chat.kit.settings.PrivacySettingActivity;
import cn.wildfire.chat.kit.widget.OptionItemView;
import cn.wildfirechat.chat.R;

public class SettingActivity extends WfcBaseActivity {
    private final int REQUEST_IGNORE_BATTERY_CODE = 100;
    @BindView(R.id.diagnoseOptionItemView)
    OptionItemView diagnoseOptionItemView;

    @Override
    protected int contentLayout() {
        return R.layout.setting_activity;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            case REQUEST_IGNORE_BATTERY_CODE:
                if (resultCode == RESULT_CANCELED) {
                    Toast.makeText(this, "允许野火IM后台运行，更能保证消息的实时性", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    @OnClick(R.id.exitOptionItemView)
    void exit() {
        //不要清除session，这样再次登录时能够保留历史记录。如果需要清除掉本地历史记录和服务器信息这里使用true
        ChatManagerHolder.gChatManager.disconnect(true, false);
        SharedPreferences sp = getSharedPreferences(Config.SP_CONFIG_FILE_NAME, Context.MODE_PRIVATE);
        sp.edit().clear().apply();

        sp = getSharedPreferences("moment", Context.MODE_PRIVATE);
        sp.edit().clear().apply();

        OKHttpHelper.clearCookies();

        Intent intent = new Intent(this, SplashActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @OnClick(R.id.privacySettingOptionItemView)
    void privacySetting() {
        Intent intent = new Intent(this, PrivacySettingActivity.class);
        startActivity(intent);
    }

    @OnClick(R.id.diagnoseOptionItemView)
    void diagnose() {
        long start = System.currentTimeMillis();
        OKHttpHelper.get("http://" + Config.IM_SERVER_HOST + "/api/version", null, new SimpleCallback<String>() {
            @Override
            public void onUiSuccess(String s) {
                long duration = (System.currentTimeMillis() - start) / 2;
                diagnoseOptionItemView.setDesc(duration + "ms");
                Toast.makeText(SettingActivity.this, "服务器延迟为：" + duration + "ms", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onUiFailure(int code, String msg) {
                diagnoseOptionItemView.setDesc("test failed");
                Toast.makeText(SettingActivity.this, "访问IM Server失败", Toast.LENGTH_SHORT).show();
            }
        });

    }

    @OnClick(R.id.uploadLogOptionItemView)
    void uploadLog() {
        AppService.Instance().uploadLog(new SimpleCallback<String>() {
            @Override
            public void onUiSuccess(String path) {
                if (!isFinishing()) {
                    Toast.makeText(SettingActivity.this, "上传日志" + path + "成功", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onUiFailure(int code, String msg) {
                if (!isFinishing()) {
                    Toast.makeText(SettingActivity.this, "上传日志失败" + code + msg, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @SuppressLint("BatteryLife")
    @OnClick(R.id.batteryOptionItemView)
    void batteryOptimize() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                Intent intent = new Intent();
                String packageName = getPackageName();
                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                    intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + packageName));
                    startActivityForResult(intent, REQUEST_IGNORE_BATTERY_CODE);
                }else {
                    Toast.makeText(this, "已忽略电池优化，允许野火IM后台运行，更能保证消息的实时性", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(this, "系统不支持", Toast.LENGTH_SHORT).show();
        }
    }

    @OnClick(R.id.aboutOptionItemView)
    void about() {
        Intent intent = new Intent(this, AboutActivity.class);
        startActivity(intent);
    }
}
