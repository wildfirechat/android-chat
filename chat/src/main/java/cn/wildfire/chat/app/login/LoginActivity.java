/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.login;

import static cn.wildfire.chat.app.BaseApp.getContext;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.core.app.ActivityOptionsCompat;

import com.afollestad.materialdialogs.MaterialDialog;

import cn.wildfire.chat.app.AppService;
import cn.wildfire.chat.app.login.model.LoginResult;
import cn.wildfire.chat.app.main.MainActivity;
import cn.wildfire.chat.app.misc.KeyStoreUtil;
import cn.wildfire.chat.kit.ChatManagerHolder;
import cn.wildfire.chat.kit.WfcBaseNoToolbarActivity;
import cn.wildfire.chat.kit.widget.SimpleTextWatcher;
import cn.wildfirechat.chat.R;

public class LoginActivity extends WfcBaseNoToolbarActivity {
    Button loginButton;
    EditText accountEditText;
    EditText passwordEditText;

    private void bindEvents() {
        findViewById(R.id.authCodeLoginTextView).setOnClickListener(v -> authCodeLogin());
        findViewById(R.id.registerTextView).setOnClickListener(v -> register());
        findViewById(R.id.loginButton).setOnClickListener(v -> login());
        accountEditText.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                inputAccount(s);
            }
        });
        passwordEditText.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                inputPassword(s);
            }
        });
    }

    private void bindViews() {
        loginButton = findViewById(R.id.loginButton);
        accountEditText = findViewById(R.id.phoneNumberEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
    }

    @Override
    protected int contentLayout() {
        return R.layout.login_activity_password;
    }

    @Override
    protected void afterViews() {
        bindViews();
        bindEvents();
        setStatusBarTheme(this, false);
        setStatusBarColor(R.color.gray14);
        if (getIntent().getBooleanExtra("isKickedOff", false)) {
            new MaterialDialog.Builder(this)
                .content("你的账号已在其他手机登录")
                .negativeText("知道了")
                .build()
                .show();
        }
    }

    void inputAccount(Editable editable) {
        if (!TextUtils.isEmpty(passwordEditText.getText()) && !TextUtils.isEmpty(editable)) {
            loginButton.setEnabled(true);
        } else {
            loginButton.setEnabled(false);
        }
    }

    void inputPassword(Editable editable) {
        if (!TextUtils.isEmpty(accountEditText.getText()) && !TextUtils.isEmpty(editable)) {
            loginButton.setEnabled(true);
        } else {
            loginButton.setEnabled(false);
        }
    }

    void authCodeLogin() {
        Intent intent = new Intent(this, SMSLoginActivity.class);
        startActivity(intent);
        finish();
    }

    void register() {
        MaterialDialog dialog = new MaterialDialog.Builder(this)
            .title("提示")
            .content("使用短信验证码登录，将会为您创建账户，请使用短信验证码登录")
            .cancelable(true)
            .positiveText("确定")
            .negativeText("取消")
            .onPositive((dialog1, which) -> {
                Intent intent = new Intent(LoginActivity.this, SMSLoginActivity.class);
                Bundle bundle = ActivityOptionsCompat.makeCustomAnimation(getContext(),
                    android.R.anim.fade_in, android.R.anim.fade_out).toBundle();
                startActivity(intent, bundle);
                finish();
            })
            .build();
        dialog.show();
    }

    void login() {

        String account = accountEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        MaterialDialog dialog = new MaterialDialog.Builder(this)
            .content("登录中...")
            .progress(true, 10)
            .cancelable(false)
            .build();
        dialog.show();

        AppService.Instance().passwordLogin(account, password, new AppService.LoginCallback() {
            @Override
            public void onUiSuccess(LoginResult loginResult) {
                if (isFinishing()) {
                    return;
                }

                //需要注意token跟clientId是强依赖的，一定要调用getClientId获取到clientId，然后用这个clientId获取token，这样connect才能成功，如果随便使用一个clientId获取到的token将无法链接成功。
                ChatManagerHolder.gChatManager.connect(loginResult.getUserId(), loginResult.getToken());
                try {
                    KeyStoreUtil.saveData(LoginActivity.this, "wf_userId", loginResult.getUserId());
                    KeyStoreUtil.saveData(LoginActivity.this, "wf_token", loginResult.getToken());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                dialog.dismiss();
                finish();
            }

            @Override
            public void onUiFailure(int code, String msg) {
                if (isFinishing()) {
                    return;
                }
                dialog.dismiss();

                Toast.makeText(LoginActivity.this, "网络出问题了:" + code + " " + msg, Toast.LENGTH_SHORT).show();
            }
        });

    }
}
