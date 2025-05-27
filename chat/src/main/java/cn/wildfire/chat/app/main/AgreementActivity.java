/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.main;

import static cn.wildfire.chat.app.BaseApp.getContext;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.core.app.ActivityOptionsCompat;

import cn.wildfire.chat.app.login.LoginActivity;
import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.WfcBaseNoToolbarActivity;
import cn.wildfire.chat.kit.WfcWebViewActivity;
import cn.wildfirechat.chat.R;

public class AgreementActivity extends WfcBaseNoToolbarActivity {

    @Override
    protected int contentLayout() {
        return R.layout.agreement_activity;
    }

    @Override
    protected void afterViews() {
        super.afterViews();
        setStatusBarTheme(this, false);
        setStatusBarColor(R.color.gray14);

        SharedPreferences sp = getSharedPreferences(Config.SP_CONFIG_FILE_NAME, Context.MODE_PRIVATE);
        sp.edit().putBoolean("hasReadUserAgreement", true).apply();

        bindEvents();
    }

    private void bindEvents() {
        findViewById(R.id.agreeTextView).setOnClickListener(v -> {
            showLogin();
        });
        findViewById(R.id.disagreeTextView).setOnClickListener(v -> {
            System.exit(0);
        });

        findViewById(R.id.privacyAgreementTextView).setOnClickListener(v -> {
            WfcWebViewActivity.loadUrl(this, getString(R.string.privacy_agreement), Config.PRIVACY_AGREEMENT_URL);

        });
        findViewById(R.id.userAgreementTextView).setOnClickListener(v -> {
            WfcWebViewActivity.loadUrl(this, getString(R.string.user_agreement), Config.USER_AGREEMENT_URL);
        });

    }

    private void showLogin() {
        Intent intent;
        intent = new Intent(this, LoginActivity.class);
        intent.putExtra("isKickedOff", getIntent().getBooleanExtra("isKickedOff", false));
        Bundle bundle = ActivityOptionsCompat.makeCustomAnimation(getContext(),
            android.R.anim.fade_in, android.R.anim.fade_out).toBundle();
        startActivity(intent, bundle);
        finish();
    }
}
