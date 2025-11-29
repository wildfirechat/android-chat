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
import android.webkit.CookieManager;
import android.webkit.WebStorage;
import android.widget.Toast;

import androidx.annotation.Nullable;

import cn.wildfire.chat.app.AppService;
import cn.wildfire.chat.app.OrganizationService;
import cn.wildfire.chat.app.main.SplashActivity;
import cn.wildfire.chat.app.misc.DiagnoseActivity;
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
    OptionItemView diagnoseOptionItemView;


    protected void bindEvents() {
        super.bindEvents();
        findViewById(R.id.exitOptionItemView).setOnClickListener(v -> exit());
        findViewById(R.id.privacySettingOptionItemView).setOnClickListener(v -> privacySetting());
        findViewById(R.id.diagnoseOptionItemView).setOnClickListener(v -> diagnose());
        findViewById(R.id.uploadLogOptionItemView).setOnClickListener(v -> uploadLog());
        findViewById(R.id.batteryOptionItemView).setOnClickListener(v -> batteryOptimize());
        findViewById(R.id.aboutOptionItemView).setOnClickListener(v -> about());
    }

    protected void bindViews() {
        super.bindViews();
        diagnoseOptionItemView = findViewById(R.id.diagnoseOptionItemView);
    }

    @Override
    protected int contentLayout() {
        return R.layout.setting_activity;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            case REQUEST_IGNORE_BATTERY_CODE:
                if (resultCode == RESULT_CANCELED) {
                    Toast.makeText(this, R.string.battery_optimize_tip, Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    void exit() {
        //不要清除session，这样再次登录时能够保留历史记录。如果需要清除掉本地历史记录和服务器信息这里使用true
        ChatManagerHolder.gChatManager.disconnect(true, false);
        SharedPreferences sp = getSharedPreferences(Config.SP_CONFIG_FILE_NAME, Context.MODE_PRIVATE);
        sp.edit()
            .clear()
            .putBoolean("hasReadUserAgreement", true)
            .apply();

        sp = getSharedPreferences("moment", Context.MODE_PRIVATE);
        sp.edit().clear().apply();

        OKHttpHelper.clearCookies();

        WebStorage.getInstance().deleteAllData();
        CookieManager.getInstance().removeAllCookies(null);
        CookieManager.getInstance().flush();

        OrganizationService.Instance().reset();
        Intent intent = new Intent(this, SplashActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    void privacySetting() {
        Intent intent = new Intent(this, PrivacySettingActivity.class);
        startActivity(intent);
    }

    void diagnose() {
        Intent intent = new Intent(this, DiagnoseActivity.class);
        startActivity(intent);
    }

    void uploadLog() {
        AppService.Instance().uploadLog(new SimpleCallback<String>() {
            @Override
            public void onUiSuccess(String path) {
                if (!isFinishing()) {
                    Toast.makeText(SettingActivity.this, getString(R.string.upload_log_success, path), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onUiFailure(int code, String msg) {
                if (!isFinishing()) {
                    Toast.makeText(SettingActivity.this, getString(R.string.upload_log_failed, code, msg), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @SuppressLint("BatteryLife")
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
                } else {
                    Toast.makeText(this, R.string.battery_optimize_allowed, Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(this, R.string.system_version_not_support, Toast.LENGTH_SHORT).show();
        }
    }

    void about() {
        Intent intent = new Intent(this, AboutActivity.class);
        startActivity(intent);
    }
}
