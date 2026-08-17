/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.setting;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebStorage;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.material.switchmaterial.SwitchMaterial;

import cn.wildfire.chat.app.AppService;
import cn.wildfire.chat.app.KeepAliveService;
import cn.wildfire.chat.app.OrganizationService;
import cn.wildfire.chat.app.main.MainActivity;
import cn.wildfire.chat.app.main.SplashActivity;
import cn.wildfire.chat.app.misc.DiagnoseActivity;
import cn.wildfire.chat.app.setting.backup.BackupAndRestoreActivity;
import cn.wildfire.chat.kit.ChatManagerHolder;
import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.net.OKHttpHelper;
import cn.wildfire.chat.kit.net.SimpleCallback;
import cn.wildfire.chat.kit.page.WfcPageCompat;
import cn.wildfire.chat.kit.settings.FontSizeActivity;
import cn.wildfire.chat.kit.settings.PrivacySettingActivity;
import cn.wildfire.chat.kit.utils.LocaleUtils;
import cn.wildfire.chat.kit.widget.OptionItemView;
import cn.wildfirechat.chat.R;

/**
 * 设置页。手机端装在 {@link SettingActivity} 这个空壳里，平板上同一份实现进右栏。
 */
public class SettingFragment extends Fragment {
    private static final int REQUEST_IGNORE_BATTERY_CODE = 100;

    private OptionItemView aboutOptionItemView;
    private SwitchMaterial switchKeepAlive;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.setting_activity, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        aboutOptionItemView = view.findViewById(R.id.aboutOptionItemView);
        switchKeepAlive = view.findViewById(R.id.switchKeepAlive);

        view.findViewById(R.id.themeOptionItemView).setOnClickListener(v -> theme());
        view.findViewById(R.id.languageOptionItemView).setOnClickListener(v -> selectLanguage());
        view.findViewById(R.id.fontSizeOptionItemView).setOnClickListener(v -> fontSize());
        view.findViewById(R.id.exitOptionItemView).setOnClickListener(v -> exit());
        view.findViewById(R.id.privacySettingOptionItemView).setOnClickListener(v -> privacySetting());
        view.findViewById(R.id.diagnoseOptionItemView).setOnClickListener(v -> diagnose());
        view.findViewById(R.id.uploadLogOptionItemView).setOnClickListener(v -> uploadLog());
        view.findViewById(R.id.batteryOptionItemView).setOnClickListener(v -> batteryOptimize());
        view.findViewById(R.id.backupAndRestoreOptionItemView).setOnClickListener(v -> backupAndRestore());
        aboutOptionItemView.setOnClickListener(v -> about());

        SharedPreferences sp = requireContext().getSharedPreferences(Config.SP_CONFIG_FILE_NAME, Context.MODE_PRIVATE);
        switchKeepAlive.setChecked(sp.getBoolean(KeepAliveService.PREF_KEY_KEEP_ALIVE, false));
        switchKeepAlive.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sp.edit().putBoolean(KeepAliveService.PREF_KEY_KEEP_ALIVE, isChecked).apply();
            if (isChecked) {
                KeepAliveService.start(requireContext());
            } else {
                KeepAliveService.stop(requireContext());
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshVersionBadge();
        checkVersionForRefresh();
    }

    private void refreshVersionBadge() {
        if (aboutOptionItemView == null || !isAdded()) {
            return;
        }
        SharedPreferences sp = requireContext().getSharedPreferences("version_info", Context.MODE_PRIVATE);
        aboutOptionItemView.setBadgeCount(sp.getBoolean("needUpdate", false) ? 1 : 0);
    }

    private void checkVersionForRefresh() {
        try {
            Context context = requireContext();
            String currentVersion = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
            int buildNumber = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
            AppService.Instance().checkVersion(currentVersion, buildNumber, new AppService.CheckVersionCallback() {
                @Override
                public void onUiSuccess(boolean needUpdate, boolean forceUpdate, String latestVersion, String title, String message, String url) {
                    if (!isAdded()) {
                        return;
                    }
                    SharedPreferences sp = requireContext().getSharedPreferences("version_info", Context.MODE_PRIVATE);
                    sp.edit()
                        .putBoolean("needUpdate", needUpdate)
                        .putBoolean("forceUpdate", forceUpdate)
                        .putString("latestVersion", latestVersion)
                        .putString("title", title)
                        .putString("message", message)
                        .putString("url", url)
                        .apply();
                    refreshVersionBadge();
                }

                @Override
                public void onUiFailure(int code, String msg) {
                    // 静默处理
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_IGNORE_BATTERY_CODE) {
            if (resultCode == android.app.Activity.RESULT_CANCELED && isAdded()) {
                Toast.makeText(getActivity(), R.string.battery_optimize_tip, Toast.LENGTH_SHORT).show();
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void exit() {
        //不要清除session，这样再次登录时能够保留历史记录。如果需要清除掉本地历史记录和服务器信息这里使用true
        ChatManagerHolder.gChatManager.disconnect(true, false);
        Context context = requireContext();
        SharedPreferences sp = context.getSharedPreferences(Config.SP_CONFIG_FILE_NAME, Context.MODE_PRIVATE);
        sp.edit()
            .clear()
            .putBoolean("hasReadUserAgreement", true)
            .apply();

        sp = context.getSharedPreferences("moment", Context.MODE_PRIVATE);
        sp.edit().clear().apply();

        OKHttpHelper.clearCookies();

        WebStorage.getInstance().deleteAllData();
        CookieManager.getInstance().removeAllCookies(null);
        CookieManager.getInstance().flush();

        OrganizationService.Instance().reset();
        Intent intent = new Intent(context, SplashActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        // CLEAR_TASK 已经把整个任务栈清掉了，这里的 finish 只是让当前界面立刻消失
        requireActivity().finish();
    }

    private void privacySetting() {
        WfcPageCompat.startPage(this, new Intent(getActivity(), PrivacySettingActivity.class));
    }

    private void diagnose() {
        WfcPageCompat.startPage(this, new Intent(getActivity(), DiagnoseActivity.class));
    }

    private void fontSize() {
        WfcPageCompat.startPage(this, new Intent(getActivity(), FontSizeActivity.class));
    }

    private void about() {
        WfcPageCompat.startPage(this, new Intent(getActivity(), AboutActivity.class));
    }

    private void backupAndRestore() {
        // 备份与恢复是一条自带多步进度页的流程，尚未接入右栏，仍然全屏打开
        startActivity(new Intent(getActivity(), BackupAndRestoreActivity.class));
    }

    private void uploadLog() {
        AppService.Instance().uploadLog(new SimpleCallback<String>() {
            @Override
            public void onUiSuccess(String path) {
                if (isAdded()) {
                    Toast.makeText(getActivity(), getString(R.string.upload_log_success, path), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onUiFailure(int code, String msg) {
                if (isAdded()) {
                    Toast.makeText(getActivity(), getString(R.string.upload_log_failed, code, msg), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @SuppressLint("BatteryLife")
    private void batteryOptimize() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Toast.makeText(getActivity(), R.string.system_version_not_support, Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            String packageName = requireContext().getPackageName();
            PowerManager pm = (PowerManager) requireContext().getSystemService(Context.POWER_SERVICE);
            if (pm.isIgnoringBatteryOptimizations(packageName)) {
                Toast.makeText(getActivity(), R.string.battery_optimize_allowed, Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + packageName));
            // 系统页面，不走 WfcPageCompat：它只认本应用内登记过的页面
            startActivityForResult(intent, REQUEST_IGNORE_BATTERY_CODE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void theme() {
        SharedPreferences sp = requireContext().getSharedPreferences("wfc_kit_config", Context.MODE_PRIVATE);
        boolean darkTheme = sp.getBoolean("darkTheme", false);
        new MaterialDialog.Builder(requireContext()).items(R.array.themes).itemsCallback((dialog, v, position, text) -> {
            if (position == 0 && darkTheme) {
                sp.edit().putBoolean("darkTheme", false).apply();
                restart();
                return;
            }
            if (position == 1 && !darkTheme) {
                sp.edit().putBoolean("darkTheme", true).apply();
                restart();
            }
        }).show();
    }

    private void selectLanguage() {
        String savedLanguage = LocaleUtils.getSavedLanguage(requireContext());
        new MaterialDialog.Builder(requireContext()).items(R.array.languages).itemsCallback((dialog, v, position, text) -> {
            String selectedLanguage = null;
            switch (position) {
                case 0:
                    selectedLanguage = LocaleUtils.LANGUAGE_FOLLOW_SYSTEM;
                    break;
                case 1:
                    selectedLanguage = LocaleUtils.LANGUAGE_CHINESE;
                    break;
                case 2:
                    selectedLanguage = LocaleUtils.LANGUAGE_ENGLISH;
                    break;
            }
            if (selectedLanguage != null && !selectedLanguage.equals(savedLanguage)) {
                LocaleUtils.setLocale(requireContext(), selectedLanguage);
                restart();
            }
        }).show();
    }

    /**
     * 切换主题/语言后重启应用以全局生效。
     */
    private void restart() {
        Intent intent = new Intent(getActivity(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}
