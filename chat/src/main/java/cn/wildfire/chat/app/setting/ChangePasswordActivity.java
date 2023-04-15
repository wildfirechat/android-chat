/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.setting;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.afollestad.materialdialogs.MaterialDialog;

import cn.wildfire.chat.app.AppService;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.net.SimpleCallback;
import cn.wildfire.chat.kit.net.base.StatusResult;
import cn.wildfire.chat.kit.widget.SimpleTextWatcher;
import cn.wildfirechat.chat.R;

public class ChangePasswordActivity extends WfcBaseActivity {
    Button confirmButton;
    EditText oldPasswordEditText;
    EditText newPasswordEditText;
    EditText confirmPasswordEditText;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected void bindEvents() {
        super.bindEvents();
        findViewById(R.id.confirmButton).setOnClickListener(v -> resetPassword());
        oldPasswordEditText.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                oldPassword(s);
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
        oldPasswordEditText = findViewById(R.id.oldPasswordEditText);
        newPasswordEditText = findViewById(R.id.newPasswordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
    }

    @Override
    protected int contentLayout() {
        return R.layout.change_password_activity;
    }

    @Override
    protected void afterViews() {
//        setStatusBarTheme(this, false);
//        setStatusBarColor(R.color.gray14);
    }

    void oldPassword(Editable editable) {
        if (!TextUtils.isEmpty(newPasswordEditText.getText()) && !TextUtils.isEmpty(confirmPasswordEditText.getText()) && !TextUtils.isEmpty(editable)) {
            confirmButton.setEnabled(true);
        } else {
            confirmButton.setEnabled(false);
        }
    }

    void newPassword(Editable editable) {
        if (!TextUtils.isEmpty(oldPasswordEditText.getText()) && !TextUtils.isEmpty(confirmPasswordEditText.getText()) && !TextUtils.isEmpty(editable)) {
            confirmButton.setEnabled(true);
        } else {
            confirmButton.setEnabled(false);
        }
    }

    void confirmPassword(Editable editable) {
        if (!TextUtils.isEmpty(oldPasswordEditText.getText()) && !TextUtils.isEmpty(newPasswordEditText.getText()) && !TextUtils.isEmpty(editable)) {
            confirmButton.setEnabled(true);
        } else {
            confirmButton.setEnabled(false);
        }
    }

    void resetPassword() {

        String oldPassword = oldPasswordEditText.getText().toString().trim();
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

        AppService.Instance().changePassword(oldPassword, newPassword, new SimpleCallback<StatusResult>() {

            @Override
            public void onUiSuccess(StatusResult result) {
                if (isFinishing()) {
                    return;
                }
                Toast.makeText(ChangePasswordActivity.this, "修改密码成功", Toast.LENGTH_SHORT).show();

                dialog.dismiss();
                finish();
            }

            @Override
            public void onUiFailure(int code, String msg) {
                if (isFinishing()) {
                    return;
                }
                dialog.dismiss();

                Toast.makeText(ChangePasswordActivity.this, "修改密码失败:" + code + " " + msg, Toast.LENGTH_SHORT).show();
            }
        });

    }
}
