/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.setting;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.afollestad.materialdialogs.MaterialDialog;

import cn.wildfire.chat.app.AppService;
import cn.wildfire.chat.app.widget.SlideVerifyDialog;
import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.net.SimpleCallback;
import cn.wildfire.chat.kit.net.base.StatusResult;
import cn.wildfire.chat.kit.page.WfcPageCompat;
import cn.wildfire.chat.kit.widget.SimpleTextWatcher;
import cn.wildfirechat.chat.R;

/**
 * 修改密码页。手机端装在 {@link ChangePasswordActivity} 这个空壳里，平板上同一份实现进右栏。
 */
public class ChangePasswordFragment extends Fragment {

    private Button confirmButton;
    private EditText oldPasswordEditText;
    private EditText newPasswordEditText;
    private EditText confirmPasswordEditText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.change_password_activity, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        confirmButton = view.findViewById(R.id.confirmButton);
        oldPasswordEditText = view.findViewById(R.id.oldPasswordEditText);
        newPasswordEditText = view.findViewById(R.id.newPasswordEditText);
        confirmPasswordEditText = view.findViewById(R.id.confirmPasswordEditText);

        confirmButton.setOnClickListener(v -> resetPassword());
        SimpleTextWatcher watcher = new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                updateConfirmEnabled();
            }
        };
        oldPasswordEditText.addTextChangedListener(watcher);
        newPasswordEditText.addTextChangedListener(watcher);
        confirmPasswordEditText.addTextChangedListener(watcher);
        updateConfirmEnabled();
    }

    /**
     * 三个输入框改造前各挂一个内容相同的 watcher，判断条件也逐字相同（三个都非空才能点确定）。
     */
    private void updateConfirmEnabled() {
        confirmButton.setEnabled(!TextUtils.isEmpty(oldPasswordEditText.getText())
            && !TextUtils.isEmpty(newPasswordEditText.getText())
            && !TextUtils.isEmpty(confirmPasswordEditText.getText()));
    }

    private void resetPassword() {
        String oldPassword = oldPasswordEditText.getText().toString().trim();
        String newPassword = newPasswordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();
        if (!TextUtils.equals(newPassword, confirmPassword)) {
            Toast.makeText(getActivity(), R.string.password_not_match, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Config.ENABLE_SLIDE_VERIFY) {
            performChangePassword(oldPassword, newPassword, null);
            return;
        }

        // Show slide verify dialog before changing password
        new SlideVerifyDialog(requireContext(), new SlideVerifyDialog.OnVerifySuccessListener() {
            @Override
            public void onVerifySuccess(String token) {
                performChangePassword(oldPassword, newPassword, token);
            }

            @Override
            public void onVerifyFailed() {
                // 验证失败（滑动位置不对），不关闭窗口
                // 这个方法现在不需要做任何事，因为 SlideVerifyDialog 已经处理了提示和重置
            }

            @Override
            public void onLoadFailed() {
                // 加载验证码失败，对话框已经关闭
                // 不需要做任何事，用户可以重新点击按钮
            }
        }).show();
    }

    private void performChangePassword(String oldPassword, String newPassword, String slideVerifyToken) {
        MaterialDialog dialog = new MaterialDialog.Builder(requireContext())
            .content(R.string.password_changing)
            .progress(true, 10)
            .cancelable(false)
            .build();
        dialog.show();

        AppService.Instance().changePassword(oldPassword, newPassword, slideVerifyToken, new SimpleCallback<StatusResult>() {
            @Override
            public void onUiSuccess(StatusResult result) {
                if (!isAdded()) {
                    return;
                }
                Toast.makeText(getActivity(), R.string.password_change_success, Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                WfcPageCompat.finishPage(ChangePasswordFragment.this);
            }

            @Override
            public void onUiFailure(int code, String msg) {
                if (!isAdded()) {
                    return;
                }
                dialog.dismiss();
                Toast.makeText(getActivity(), getString(R.string.password_change_failed, code, msg), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
