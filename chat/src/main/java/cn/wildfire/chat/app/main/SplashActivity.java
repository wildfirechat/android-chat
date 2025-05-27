/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.main;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import cn.wildfire.chat.app.login.LoginActivity;
import cn.wildfire.chat.app.misc.KeyStoreUtil;
import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.utils.LocaleUtils;
import cn.wildfirechat.chat.R;

public class SplashActivity extends AppCompatActivity {

    private String id;
    private String token;

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        setContentView(R.layout.activity_splash);
        hideSystemUI();
        setStatusBarColor(R.color.gray5);


        try {
            id = KeyStoreUtil.getData(this, "wf_userId");
            token = KeyStoreUtil.getData(this, "wf_token");
        } catch (Exception e) {
            e.printStackTrace();
        }

        new Handler().postDelayed(this::showNextScreen, 1000);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        String language = LocaleUtils.getLanguage(newBase);
        Context context = LocaleUtils.updateResources(newBase, language);
        super.attachBaseContext(context);
    }


    private void showNextScreen() {
        if (!TextUtils.isEmpty(id) && !TextUtils.isEmpty(token)) {
            showMain();
        } else {
            SharedPreferences sp = getSharedPreferences(Config.SP_CONFIG_FILE_NAME, Context.MODE_PRIVATE);
            if (sp.getBoolean("hasReadUserAgreement", false)) {
                showLogin();
            } else {
                showAgreement();
            }
        }
    }

    private void showMain() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void showLogin() {
        Intent intent;
        intent = new Intent(this, LoginActivity.class);
        intent.putExtra("isKickedOff", getIntent().getBooleanExtra("isKickedOff", false));
        startActivity(intent);
        finish();
    }

    private void showAgreement() {
        Intent intent;
        intent = new Intent(this, AgreementActivity.class);
        startActivity(intent);
        finish();
    }

    private void hideSystemUI() {
        // Set the IMMERSIVE flag.
        // Set the content to appear under the system bars so that the content
        // doesn't resize when the system bars hide and show.
        View mDecorView = getWindow().getDecorView();
        mDecorView.setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
//                | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                | View.SYSTEM_UI_FLAG_IMMERSIVE);
    }

    protected void setStatusBarColor(int resId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, resId));
        }
    }
}
