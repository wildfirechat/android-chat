/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.setting;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.widget.TextView;
import android.widget.Toast;

import cn.wildfire.chat.app.AppService;
import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.WfcWebViewActivity;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.chat.BuildConfig;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.remote.ChatManager;

public class AboutActivity extends WfcBaseActivity {

    TextView infoTextView;

    protected void bindEvents() {
        super.bindEvents();
        findViewById(R.id.introOptionItemView).setOnClickListener(v -> intro());
        findViewById(R.id.agreementOptionItemView).setOnClickListener(v -> agreement());
        findViewById(R.id.privacyOptionItemView).setOnClickListener(v -> privacy());
    }

    protected void bindViews() {
        super.bindViews();
        infoTextView = findViewById(R.id.infoTextView);
    }

    @Override
    protected int contentLayout() {
        return R.layout.activity_about;
    }

    @Override
    protected void afterViews() {
        PackageManager packageManager = getPackageManager();
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(getPackageName(), PackageManager.GET_CONFIGURATIONS);
            String info = packageInfo.packageName + "\n"
                + packageInfo.versionCode + " " + packageInfo.versionName + "\n"
                + ChatManager.Instance().getProtoRevision() + "\n"
                + Config.IM_SERVER_HOST + "\n"
                + AppService.APP_SERVER_ADDRESS + "\n";

            if (AVEngineKit.isSupportConference()) {
                info += "高级版音视频\n";
            } else {
                info += "多人版版音视频\n";
                for (String[] ice : Config.ICE_SERVERS) {
                    info += ice[0] + " " + ice[1] + " " + ice[2] + "\n";
                }
            }

            infoTextView.setText(info);

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void intro() {
        if (BuildConfig.APPLICATION_ID.startsWith("cn.wildfirechat.")) {
            WfcWebViewActivity.loadUrl(this, getString(R.string.about_intro_title), getString(R.string.about_intro_url));
        } else {
            Toast.makeText(this, "野火IM 功能介绍对第三方应用不适用", Toast.LENGTH_SHORT).show();
        }
    }

    public void agreement() {
        if (BuildConfig.APPLICATION_ID.startsWith("cn.wildfirechat.")) {
            WfcWebViewActivity.loadUrl(this, getString(R.string.about_agreement_title), getString(R.string.about_agreement_url));
        } else {
            Toast.makeText(this, "野火IM 用户协议对第三方应用不适用", Toast.LENGTH_SHORT).show();
        }
    }

    public void privacy() {
        if (BuildConfig.APPLICATION_ID.startsWith("cn.wildfirechat.")) {
            WfcWebViewActivity.loadUrl(this, getString(R.string.about_privacy_title), getString(R.string.about_privacy_url));
        } else {
            Toast.makeText(this, "野火IM 隐私政策对第三方应用不适用", Toast.LENGTH_SHORT).show();
        }
    }
}
