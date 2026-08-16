/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.setting;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.afollestad.materialdialogs.MaterialDialog;

import cn.wildfire.chat.app.AppService;
import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.WfcWebViewActivity;
import cn.wildfire.chat.kit.page.WfcPageCompat;
import cn.wildfire.chat.kit.widget.OptionItemView;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.remote.ChatManager;

/**
 * 关于页。手机端装在 {@link AboutActivity} 这个空壳里，平板上同一份实现进右栏。
 */
public class AboutFragment extends Fragment {

    private TextView infoTextView;
    private OptionItemView currentVersionOptionItemView;
    private boolean needUpdate;
    private boolean forceUpdate;
    private String updateTitle;
    private String updateMessage;
    private String updateUrl;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_about, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        infoTextView = view.findViewById(R.id.infoTextView);
        currentVersionOptionItemView = view.findViewById(R.id.currentVersionOptionItemView);

        view.findViewById(R.id.introOptionItemView).setOnClickListener(v -> intro());
        view.findViewById(R.id.agreementOptionItemView).setOnClickListener(v -> agreement());
        view.findViewById(R.id.privacyOptionItemView).setOnClickListener(v -> privacy());
        if (currentVersionOptionItemView != null) {
            currentVersionOptionItemView.setOnClickListener(v -> showVersionUpdateDialog());
        }

        Context context = requireContext();
        PackageManager packageManager = context.getPackageManager();
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(), PackageManager.GET_CONFIGURATIONS);
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
        Context context = requireContext();
        SharedPreferences sp = context.getSharedPreferences("version_info", Context.MODE_PRIVATE);
        needUpdate = sp.getBoolean("needUpdate", false);
        forceUpdate = sp.getBoolean("forceUpdate", false);
        updateTitle = sp.getString("title", "发现新版本");
        updateMessage = sp.getString("message", "");
        updateUrl = sp.getString("url", "");
        if (infoTextView != null && needUpdate) {
            infoTextView.setTextColor(getResources().getColor(R.color.colorPrimary));
        }
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
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
        MaterialDialog.Builder builder = new MaterialDialog.Builder(requireContext())
            .title(updateTitle)
            .content(updateMessage)
            .positiveText("立即更新")
            .onPositive((dialog, which) -> {
                if (!TextUtils.isEmpty(updateUrl)) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl)));
                }
                if (forceUpdate) {
                    WfcPageCompat.finishPage(this);
                }
            });
        if (forceUpdate) {
            builder.cancelable(false);
        } else {
            builder.negativeText("以后再说");
        }
        builder.show();
    }

    private void intro() {
        if (!Config.IM_SERVER_HOST.equals("wildfirechat.net")) {
            WfcWebViewActivity.loadUrl(this, getString(R.string.about_intro_title), getString(R.string.about_intro_url));
        } else {
            Toast.makeText(getActivity(), "野火IM 功能介绍对第三方应用不适用", Toast.LENGTH_SHORT).show();
        }
    }

    private void agreement() {
        if (TextUtils.isEmpty(Config.USER_AGREEMENT_URL) || Config.USER_AGREEMENT_URL.contains("https://example.com")) {
            Toast.makeText(getActivity(), R.string.no_user_agreement_url_tip, Toast.LENGTH_SHORT).show();
            return;
        }
        WfcWebViewActivity.loadUrl(this, getString(R.string.user_agreement), Config.USER_AGREEMENT_URL);
    }

    private void privacy() {
        if (TextUtils.isEmpty(Config.PRIVACY_AGREEMENT_URL) || Config.PRIVACY_AGREEMENT_URL.contains("https://example.com")) {
            Toast.makeText(getActivity(), R.string.no_privacy_agreement_url_tip, Toast.LENGTH_SHORT).show();
            return;
        }
        WfcWebViewActivity.loadUrl(this, getString(R.string.privacy_agreement), Config.PRIVACY_AGREEMENT_URL);
    }
}
