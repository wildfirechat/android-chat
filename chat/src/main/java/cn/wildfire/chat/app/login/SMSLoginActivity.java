/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.login;

import android.content.Intent;
import android.os.Handler;
import android.text.Editable;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import cn.wildfire.chat.app.AppService;
import cn.wildfire.chat.app.login.model.LoginResult;
import cn.wildfire.chat.app.main.MainActivity;
import cn.wildfire.chat.app.misc.KeyStoreUtil;
import cn.wildfire.chat.app.setting.ResetPasswordActivity;
import cn.wildfire.chat.kit.ChatManagerHolder;
import cn.wildfire.chat.kit.WfcBaseNoToolbarActivity;
import cn.wildfire.chat.kit.widget.SimpleTextWatcher;
import cn.wildfirechat.chat.R;

public class SMSLoginActivity extends WfcBaseNoToolbarActivity {

    private static final String TAG = "SMSLoginActivity";

    Button loginButton;
    EditText phoneNumberEditText;
    EditText authCodeEditText;
    TextView requestAuthCodeButton;


    private void bindEvents() {
        findViewById(R.id.passwordLoginTextView).setOnClickListener(v -> authCodeLogin());
        findViewById(R.id.loginButton).setOnClickListener(v -> login());
        findViewById(R.id.requestAuthCodeButton).setOnClickListener(v -> requestAuthCode());
        phoneNumberEditText.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                inputPhoneNumber(s);
            }
        });

        authCodeEditText.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                inputAuthCode(s);
            }
        });
    }

    private void bindViews() {
        loginButton = findViewById(R.id.loginButton);
        phoneNumberEditText = findViewById(R.id.phoneNumberEditText);
        authCodeEditText = findViewById(R.id.authCodeEditText);
        requestAuthCodeButton = findViewById(R.id.requestAuthCodeButton);
    }

    @Override
    protected int contentLayout() {
        return R.layout.login_activity_sms;
    }

    @Override
    protected void afterViews() {
        bindViews();
        bindEvents();
        setStatusBarTheme(this, false);
        setStatusBarColor(R.color.gray14);
    }

    void inputPhoneNumber(Editable editable) {
        String phone = editable.toString().trim();
        if (phone.length() == 11) {
            requestAuthCodeButton.setEnabled(true);
        } else {
            requestAuthCodeButton.setEnabled(false);
            loginButton.setEnabled(false);
        }
    }

    void inputAuthCode(Editable editable) {
        if (editable.toString().length() > 2) {
            loginButton.setEnabled(true);
        }
    }

    void authCodeLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }


    void login() {
        String phoneNumber = phoneNumberEditText.getText().toString().trim();
        String authCode = authCodeEditText.getText().toString().trim();

        loginButton.setEnabled(false);
        MaterialDialog dialog = new MaterialDialog.Builder(this)
            .content("登录中...")
            .progress(true, 100)
            .cancelable(false)
            .build();
        dialog.show();


        AppService.Instance().smsLogin(phoneNumber, authCode, new AppService.LoginCallback() {
            @Override
            public void onUiSuccess(LoginResult loginResult) {
                if (isFinishing()) {
                    return;
                }
                dialog.dismiss();
                //需要注意token跟clientId是强依赖的，一定要调用getClientId获取到clientId，然后用这个clientId获取token，这样connect才能成功，如果随便使用一个clientId获取到的token将无法链接成功。
                ChatManagerHolder.gChatManager.connect(loginResult.getUserId(), loginResult.getToken());
                try {
                    KeyStoreUtil.saveData(SMSLoginActivity.this, "wf_userId", loginResult.getUserId());
                    KeyStoreUtil.saveData(SMSLoginActivity.this, "wf_token", loginResult.getToken());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Intent intent = new Intent(SMSLoginActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);

                Intent resetPasswordIntent = new Intent(SMSLoginActivity.this, ResetPasswordActivity.class);
                resetPasswordIntent.putExtra("resetCode", loginResult.getResetCode());
                startActivity(resetPasswordIntent);

                finish();
            }

            @Override
            public void onUiFailure(int code, String msg) {
                if (isFinishing()) {
                    return;
                }
                Toast.makeText(SMSLoginActivity.this, "登录失败：" + code + " " + msg, Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                loginButton.setEnabled(true);
            }
        });
    }

    private Handler handler = new Handler();

    void requestAuthCode() {
        requestAuthCodeButton.setEnabled(false);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isFinishing()) {
                    requestAuthCodeButton.setEnabled(true);
                }
            }
        }, 60 * 1000);

        Toast.makeText(this, "请求验证码...", Toast.LENGTH_SHORT).show();
        String phoneNumber = phoneNumberEditText.getText().toString().trim();

        AppService.Instance().requestAuthCode(phoneNumber, new AppService.SendCodeCallback() {
            @Override
            public void onUiSuccess() {
                Toast.makeText(SMSLoginActivity.this, "发送验证码成功", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onUiFailure(int code, String msg) {
                Toast.makeText(SMSLoginActivity.this, "发送验证码失败: " + code + " " + msg, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
