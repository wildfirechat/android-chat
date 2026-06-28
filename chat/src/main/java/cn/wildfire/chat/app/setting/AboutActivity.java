/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.setting;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import cn.wildfire.chat.app.AppService;
import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.WfcWebViewActivity;
import cn.wildfire.chat.kit.widget.OptionItemView;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.remote.ChatManager;
import com.afollestad.materialdialogs.MaterialDialog;

public class AboutActivity extends WfcBaseActivity {

    TextView infoTextView;
    OptionItemView currentVersionOptionItemView;
    boolean needUpdate;
    boolean forceUpdate;
    String updateTitle;
    String updateMessage;
    String updateUrl;

    protected void bindEvents() {
        super.bindEvents();
        findViewById(R.id.introOptionItemView).setOnClickListener(v -> intro());
        findViewById(R.id.agreementOptionItemView).setOnClickListener(v -> agreement());
        findViewById(R.id.privacyOptionItemView).setOnClickListener(v -> privacy());
        if (currentVersionOptionItemView != null) {
            currentVersionOptionItemView.setOnClickListener(v -> showVersionUpdateDialog());
        }
    }

    protected void bindViews() {
        super.bindViews();
        infoTextView = findViewById(R.id.infoTextView);
        currentVersionOptionItemView = findViewById(R.id.currentVersionOptionItemView);
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
                + AppService.Instance().appServerAddress() + "\n";

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
        loadVersionInfo();
    }

    private void loadVersionInfo() {
        SharedPreferences sp = getSharedPreferences("version_info", Context.MODE_PRIVATE);
        needUpdate = sp.getBoolean("needUpdate", false);
        forceUpdate = sp.getBoolean("forceUpdate", false);
        updateTitle = sp.getString("title", "发现新版本");
        updateMessage = sp.getString("message", "");
        updateUrl = sp.getString("url", "");
        if (infoTextView != null && needUpdate) {
            infoTextView.setTextColor(getResources().getColor(R.color.colorPrimary));
        }
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            if (currentVersionOptionItemView != null) {
                currentVersionOptionItemView.setDesc(packageInfo.versionName != null ? packageInfo.versionName : "");
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if (currentVersionOptionItemView != null) {
            currentVersionOptionItemView.setBadgeCount(needUpdate ? 1 : 0);
        }
    }

    private void showVersionUpdateDialog() {
        if (!needUpdate) {
            return;
        }
        if (forceUpdate) {
            new MaterialDialog.Builder(this)
                .title(updateTitle)
                .content(updateMessage)
                .cancelable(false)
                .positiveText("立即更新")
                .onPositive((dialog, which) -> {
                    if (!TextUtils.isEmpty(updateUrl)) {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl));
                        startActivity(intent);
                    }
                    finish();
                })
                .show();
        } else {
            new MaterialDialog.Builder(this)
                .title(updateTitle)
                .content(updateMessage)
                .positiveText("立即更新")
                .negativeText("以后再说")
                .onPositive((dialog, which) -> {
                    if (!TextUtils.isEmpty(updateUrl)) {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl));
                        startActivity(intent);
                    }
                })
                .show();
        }
    }

    public void intro() {
        if (!Config.IM_SERVER_HOST.equals("wildfirechat.net")) {
            WfcWebViewActivity.loadUrl(this, getString(R.string.about_intro_title), getString(R.string.about_intro_url));
        } else {
            Toast.makeText(this, "野火IM 功能介绍对第三方应用不适用", Toast.LENGTH_SHORT).show();
        }
    }

    public void agreement() {
        if (TextUtils.isEmpty(Config.USER_AGREEMENT_URL) || Config.USER_AGREEMENT_URL.indexOf("https://example.com") >= 0) {
            Toast.makeText(this, R.string.no_user_agreement_url_tip, Toast.LENGTH_SHORT).show();
            return;
        }
        WfcWebViewActivity.loadUrl(this, getString(R.string.user_agreement), Config.USER_AGREEMENT_URL);
    }

    public void privacy() {
        if (TextUtils.isEmpty(Config.PRIVACY_AGREEMENT_URL) || Config.PRIVACY_AGREEMENT_URL.indexOf("https://example.com") >= 0) {
            Toast.makeText(this, R.string.no_privacy_agreement_url_tip, Toast.LENGTH_SHORT).show();
            return;
        }
        WfcWebViewActivity.loadUrl(this, getString(R.string.privacy_agreement), Config.PRIVACY_AGREEMENT_URL);
    }
}
