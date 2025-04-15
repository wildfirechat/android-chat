/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.login;

import android.content.Intent;
import android.os.Handler;
import android.text.Editable;
import android.widget.Button;
import android.widget.CheckBox;
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
import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.WfcBaseNoToolbarActivity;
import cn.wildfire.chat.kit.WfcWebViewActivity;
import cn.wildfire.chat.kit.widget.SimpleTextWatcher;
import cn.wildfirechat.chat.R;

public class SMSLoginActivity extends WfcBaseNoToolbarActivity {

    private static final String TAG = "SMSLoginActivity";

    Button loginButton;
    EditText phoneNumberEditText;
    EditText authCodeEditText;
    TextView requestAuthCodeButton;
    CheckBox checkBox;

    private void bindEvents() {
        findViewById(R.id.passwordLoginTextView).setOnClickListener(v -> authCodeLogin());
        findViewById(R.id.loginButton).setOnClickListener(v -> {
            if (checkBox.isChecked()) {
                login();
            } else {
                Toast.makeText(this, R.string.check_agreement_tip, Toast.LENGTH_SHORT).show();
            }
        });
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

        findViewById(R.id.privacyAgreementTextView).setOnClickListener(v -> {
            WfcWebViewActivity.loadUrl(this, getString(R.string.privacy_agreement), Config.PRIVACY_AGREEMENT_URL);

        });
        findViewById(R.id.userAgreementTextView).setOnClickListener(v -> {
            WfcWebViewActivity.loadUrl(this, getString(R.string.user_agreement), Config.USER_AGREEMENT_URL);
        });
    }

    private void bindViews() {
        loginButton = findViewById(R.id.loginButton);
        phoneNumberEditText = findViewById(R.id.phoneNumberEditText);
        authCodeEditText = findViewById(R.id.authCodeEditText);
        requestAuthCodeButton = findViewById(R.id.requestAuthCodeButton);
        checkBox = findViewById(R.id.agreementCheckBox);
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
        if (phone.length() == 11 && countdownRunnable == null) {
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
            .content(R.string.login_progress)
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
                Toast.makeText(SMSLoginActivity.this, getString(R.string.sms_login_failure, code, msg), Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                loginButton.setEnabled(true);
            }
        });
    }

    private Handler handler = new Handler();
    private int countdownSeconds = 60;
    private Runnable countdownRunnable;

    void requestAuthCode() {
        String phoneNumber = phoneNumberEditText.getText().toString().trim();

        // Disable button immediately
        requestAuthCodeButton.setEnabled(false);

        // Start countdown
        countdownSeconds = 60;
        updateCountdownText();

        // Create countdown runnable if not exists
        if (countdownRunnable == null) {
            countdownRunnable = new Runnable() {
                @Override
                public void run() {
                    if (isFinishing()) return;

                    countdownSeconds--;
                    updateCountdownText();

                    if (countdownSeconds > 0) {
                        // Continue countdown
                        handler.postDelayed(this, 1000);
                    } else {
                        // Reset button text and enable it
                        requestAuthCodeButton.setText(getString(R.string.requesting_auth_code));
                        requestAuthCodeButton.setEnabled(true);
                    }
                }
            };
        }

        // Start the countdown timer
        handler.postDelayed(countdownRunnable, 1000);

        // Request the auth code
        Toast.makeText(this, getString(R.string.requesting_auth_code), Toast.LENGTH_SHORT).show();

        AppService.Instance().requestAuthCode(phoneNumber, new AppService.SendCodeCallback() {
            @Override
            public void onUiSuccess() {
                Toast.makeText(SMSLoginActivity.this, R.string.auth_code_request_success, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onUiFailure(int code, String msg) {
                Toast.makeText(SMSLoginActivity.this, getString(R.string.auth_code_request_failure, code, msg), Toast.LENGTH_SHORT).show();
                // Reset countdown on failure
                resetCountdown();
            }
        });
    }

    private void updateCountdownText() {
        if (countdownSeconds > 0) {
            requestAuthCodeButton.setText(getString(R.string.retry_after_seconds, countdownSeconds));
        }
    }

    private void resetCountdown() {
        // Remove callbacks
        if (countdownRunnable != null) {
            handler.removeCallbacks(countdownRunnable);
        }
        // Reset button
        requestAuthCodeButton.setText(R.string.requesting_auth_code);
        requestAuthCodeButton.setEnabled(true);
        countdownSeconds = 60;
        countdownRunnable = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null && countdownRunnable != null) {
            handler.removeCallbacks(countdownRunnable);
        }
    }
}
