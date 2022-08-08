/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.setting;

import android.text.Editable;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
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

public class ChangePasswordActivity extends WfcBaseActivity {
    @BindView(R.id.confirmButton)
    Button confirmButton;
    @BindView(R.id.oldPasswordEditText)
    EditText oldPasswordEditText;
    @BindView(R.id.newPasswordEditText)
    EditText newPasswordEditText;
    @BindView(R.id.confirmPasswordEditText)
    EditText confirmPasswordEditText;

    @Override
    protected int contentLayout() {
        return R.layout.change_password_activity;
    }

//    @Override
//    protected void afterViews() {
//        setStatusBarTheme(this, false);
//        setStatusBarColor(R.color.gray14);
//    }

    @OnTextChanged(value = R.id.oldPasswordEditText, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    void oldPassword(Editable editable) {
        if (!TextUtils.isEmpty(newPasswordEditText.getText()) && !TextUtils.isEmpty(confirmPasswordEditText.getText()) && !TextUtils.isEmpty(editable)) {
            confirmButton.setEnabled(true);
        } else {
            confirmButton.setEnabled(false);
        }
    }

    @OnTextChanged(value = R.id.newPasswordEditText, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    void newPassword(Editable editable) {
        if (!TextUtils.isEmpty(oldPasswordEditText.getText()) && !TextUtils.isEmpty(confirmPasswordEditText.getText()) && !TextUtils.isEmpty(editable)) {
            confirmButton.setEnabled(true);
        } else {
            confirmButton.setEnabled(false);
        }
    }

    @OnTextChanged(value = R.id.confirmPasswordEditText, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    void confirmPassword(Editable editable) {
        if (!TextUtils.isEmpty(oldPasswordEditText.getText()) && !TextUtils.isEmpty(newPasswordEditText.getText()) && !TextUtils.isEmpty(editable)) {
            confirmButton.setEnabled(true);
        } else {
            confirmButton.setEnabled(false);
        }
    }

    @OnClick(R.id.confirmButton)
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
