/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.setting;

import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import cn.wildfire.chat.app.AppService;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.net.SimpleCallback;
import cn.wildfire.chat.kit.net.base.StatusResult;
import cn.wildfire.chat.kit.widget.SimpleTextWatcher;
import cn.wildfirechat.chat.R;

public class ResetPasswordActivity extends WfcBaseActivity {
    Button confirmButton;

    EditText authCodeEditText;
    EditText newPasswordEditText;
    EditText confirmPasswordEditText;

    TextView requestAuthCodeButton;

    FrameLayout authCodeFrameLayout;

    private String resetCode;

    protected void bindEvents() {
        super.bindEvents();
        requestAuthCodeButton.setOnClickListener(v -> requestAuthCode());
        confirmButton.setOnClickListener(v -> resetPassword());
        authCodeEditText.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                authCode(s);
            }
        });
        newPasswordEditText.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                newPassword(s);
            }
        });
        confirmPasswordEditText.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                confirmPassword(s);
            }
        });
    }

    protected void bindViews() {
        super.bindViews();
        confirmButton = findViewById(R.id.confirmButton);
        authCodeEditText = findViewById(R.id.authCodeEditText);
        newPasswordEditText = findViewById(R.id.newPasswordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        requestAuthCodeButton = findViewById(R.id.requestAuthCodeButton);
        authCodeFrameLayout = findViewById(R.id.authCodeFrameLayout);
    }

    @Override
    protected int contentLayout() {
        return R.layout.reset_password_activity;
    }

    @Override
    protected void afterViews() {
        resetCode = getIntent().getStringExtra("resetCode");
        if (!TextUtils.isEmpty(resetCode)) {
            authCodeFrameLayout.setVisibility(View.GONE);
        }
    }

    void authCode(Editable editable) {
        if (!TextUtils.isEmpty(newPasswordEditText.getText()) && !TextUtils.isEmpty(confirmPasswordEditText.getText()) && !TextUtils.isEmpty(editable)) {
            confirmButton.setEnabled(true);
        } else {
            confirmButton.setEnabled(false);
        }
    }

    void newPassword(Editable editable) {
        if ((!TextUtils.isEmpty(authCodeEditText.getText()) || !TextUtils.isEmpty(resetCode)) && !TextUtils.isEmpty(confirmPasswordEditText.getText()) && !TextUtils.isEmpty(editable)) {
            confirmButton.setEnabled(true);
        } else {
            confirmButton.setEnabled(false);
        }
    }

    void confirmPassword(Editable editable) {
        if ((!TextUtils.isEmpty(authCodeEditText.getText()) || !TextUtils.isEmpty(resetCode)) && !TextUtils.isEmpty(newPasswordEditText.getText()) && !TextUtils.isEmpty(editable)) {
            confirmButton.setEnabled(true);
        } else {
            confirmButton.setEnabled(false);
        }
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

        Toast.makeText(this, R.string.requesting_reset_code, Toast.LENGTH_SHORT).show();

        AppService.Instance().requestResetAuthCode(null, new AppService.SendCodeCallback() {
            @Override
            public void onUiSuccess() {
                Toast.makeText(ResetPasswordActivity.this, R.string.reset_code_send_success, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onUiFailure(int code, String msg) {
                Toast.makeText(ResetPasswordActivity.this, getString(R.string.reset_code_send_failure, code, msg), Toast.LENGTH_SHORT).show();
            }
        });
    }

    void resetPassword() {
        String newPassword = newPasswordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();
        if (!TextUtils.equals(newPassword, confirmPassword)) {
            Toast.makeText(this, R.string.password_not_match, Toast.LENGTH_SHORT).show();
            return;
        }

        MaterialDialog dialog = new MaterialDialog.Builder(this)
            .content(R.string.reset_password_progress)
            .progress(true, 10)
            .cancelable(false)
            .build();
        dialog.show();

        String code = TextUtils.isEmpty(resetCode) ? authCodeEditText.getText().toString() : resetCode;

        AppService.Instance().resetPassword(null, code, newPassword, new SimpleCallback<StatusResult>() {
            @Override
            public void onUiSuccess(StatusResult result) {
                if (isFinishing()) {
                    return;
                }
                Toast.makeText(ResetPasswordActivity.this, R.string.reset_password_success, Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                finish();
            }

            @Override
            public void onUiFailure(int code, String msg) {
                if (isFinishing()) {
                    return;
                }
                dialog.dismiss();
                Toast.makeText(ResetPasswordActivity.this, getString(R.string.reset_password_failure, code, msg), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
