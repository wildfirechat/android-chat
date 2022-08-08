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

import butterknife.BindView;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import cn.wildfire.chat.app.AppService;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.net.SimpleCallback;
import cn.wildfire.chat.kit.net.base.StatusResult;
import cn.wildfirechat.chat.R;

public class ResetPasswordActivity extends WfcBaseActivity {
    @BindView(R.id.confirmButton)
    Button confirmButton;

    @BindView(R.id.authCodeEditText)
    EditText authCodeEditText;
    @BindView(R.id.newPasswordEditText)
    EditText newPasswordEditText;
    @BindView(R.id.confirmPasswordEditText)
    EditText confirmPasswordEditText;

    @BindView(R.id.requestAuthCodeButton)
    TextView requestAuthCodeButton;

    @BindView(R.id.authCodeFrameLayout)
    FrameLayout authCodeFrameLayout;

    private String resetCode;

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

    @OnTextChanged(value = R.id.authCodeEditText, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    void authCode(Editable editable) {
        if (!TextUtils.isEmpty(newPasswordEditText.getText()) && !TextUtils.isEmpty(confirmPasswordEditText.getText()) && !TextUtils.isEmpty(editable)) {
            confirmButton.setEnabled(true);
        } else {
            confirmButton.setEnabled(false);
        }
    }

    @OnTextChanged(value = R.id.newPasswordEditText, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    void newPassword(Editable editable) {
        if ((!TextUtils.isEmpty(authCodeEditText.getText()) || !TextUtils.isEmpty(resetCode)) && !TextUtils.isEmpty(confirmPasswordEditText.getText()) && !TextUtils.isEmpty(editable)) {
            confirmButton.setEnabled(true);
        } else {
            confirmButton.setEnabled(false);
        }
    }

    @OnTextChanged(value = R.id.confirmPasswordEditText, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    void confirmPassword(Editable editable) {
        if ((!TextUtils.isEmpty(authCodeEditText.getText()) || !TextUtils.isEmpty(resetCode)) && !TextUtils.isEmpty(newPasswordEditText.getText()) && !TextUtils.isEmpty(editable)) {
            confirmButton.setEnabled(true);
        } else {
            confirmButton.setEnabled(false);
        }
    }

    private Handler handler = new Handler();

    @OnClick(R.id.requestAuthCodeButton)
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

        AppService.Instance().requestResetAuthCode(null, new AppService.SendCodeCallback() {
            @Override
            public void onUiSuccess() {
                Toast.makeText(ResetPasswordActivity.this, "发送验证码成功", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onUiFailure(int code, String msg) {
                Toast.makeText(ResetPasswordActivity.this, "发送验证码失败: " + code + " " + msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @OnClick(R.id.confirmButton)
    void resetPassword() {

        String newPassword = newPasswordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();
        if (!TextUtils.equals(newPassword, confirmPassword)) {
            Toast.makeText(this, "两次输入的密码不一致", Toast.LENGTH_SHORT).show();
            return;
        }

        MaterialDialog dialog = new MaterialDialog.Builder(this)
            .content("正在修改密码...")
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
                Toast.makeText(ResetPasswordActivity.this, "重置密码成功", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                finish();
            }

            @Override
            public void onUiFailure(int code, String msg) {
                if (isFinishing()) {
                    return;
                }
                dialog.dismiss();

                Toast.makeText(ResetPasswordActivity.this, "重置密码失败:" + code + " " + msg, Toast.LENGTH_SHORT).show();
            }
        });

    }
}
