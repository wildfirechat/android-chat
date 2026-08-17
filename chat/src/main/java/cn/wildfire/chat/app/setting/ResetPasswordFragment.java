/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.setting;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
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
 * 重置密码页。手机端装在 {@link ResetPasswordActivity} 这个空壳里，平板上同一份实现进右栏。
 * <p>
 * 两个入口：登录页「忘记密码」（带 resetCode，跳过验证码那一栏），和「账号与安全 → 重置密码」。
 */
public class ResetPasswordFragment extends Fragment {

    private static final String ARG_RESET_CODE = "resetCode";

    private Button confirmButton;
    private EditText authCodeEditText;
    private EditText newPasswordEditText;
    private EditText confirmPasswordEditText;
    private TextView requestAuthCodeButton;
    private FrameLayout authCodeFrameLayout;

    private String resetCode;
    private final Handler handler = new Handler(Looper.getMainLooper());

    /**
     * 由启动 intent 造页面，供 {@link ResetPasswordActivity} 与 {@code PaneRegistry} 共用。
     */
    public static ResetPasswordFragment fromIntent(@Nullable Intent intent) {
        ResetPasswordFragment fragment = new ResetPasswordFragment();
        Bundle args = new Bundle();
        args.putString(ARG_RESET_CODE, intent == null ? null : intent.getStringExtra(ARG_RESET_CODE));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        resetCode = getArguments() == null ? null : getArguments().getString(ARG_RESET_CODE);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.reset_password_activity, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        confirmButton = view.findViewById(R.id.confirmButton);
        authCodeEditText = view.findViewById(R.id.authCodeEditText);
        newPasswordEditText = view.findViewById(R.id.newPasswordEditText);
        confirmPasswordEditText = view.findViewById(R.id.confirmPasswordEditText);
        requestAuthCodeButton = view.findViewById(R.id.requestAuthCodeButton);
        authCodeFrameLayout = view.findViewById(R.id.authCodeFrameLayout);

        requestAuthCodeButton.setOnClickListener(v -> requestAuthCode());
        confirmButton.setOnClickListener(v -> resetPassword());
        SimpleTextWatcher watcher = new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                updateConfirmEnabled();
            }
        };
        authCodeEditText.addTextChangedListener(watcher);
        newPasswordEditText.addTextChangedListener(watcher);
        confirmPasswordEditText.addTextChangedListener(watcher);

        if (!TextUtils.isEmpty(resetCode)) {
            authCodeFrameLayout.setVisibility(View.GONE);
        }
        updateConfirmEnabled();
    }

    @Override
    public void onDestroyView() {
        // 请求验证码后有一个 60 秒的解禁定时器，页面提前关掉的话它还攥着已经销毁的视图
        handler.removeCallbacksAndMessages(null);
        super.onDestroyView();
    }

    /**
     * 三个输入框改造前各挂一个内容相同的 watcher：验证码（或外部带进来的 resetCode）、
     * 新密码、确认密码都非空才能点确定。
     */
    private void updateConfirmEnabled() {
        boolean hasCode = !TextUtils.isEmpty(authCodeEditText.getText()) || !TextUtils.isEmpty(resetCode);
        confirmButton.setEnabled(hasCode
            && !TextUtils.isEmpty(newPasswordEditText.getText())
            && !TextUtils.isEmpty(confirmPasswordEditText.getText()));
    }

    private void requestAuthCode() {
        if (!Config.ENABLE_SLIDE_VERIFY) {
            performRequestResetCode(null);
            return;
        }

        // Show slide verify dialog before sending reset code
        new SlideVerifyDialog(requireContext(), new SlideVerifyDialog.OnVerifySuccessListener() {
            @Override
            public void onVerifySuccess(String token) {
                performRequestResetCode(token);
            }

            @Override
            public void onVerifyFailed() {
                // 验证失败（滑动位置不对），不关闭窗口
                // 这个方法现在不需要做任何事，因为 SlideVerifyDialog 已经处理了提示和重置
            }

            @Override
            public void onLoadFailed() {
                // 加载验证码失败，对话框已经关闭，只需要启用按钮
                requestAuthCodeButton.setEnabled(true);
            }
        }).show();
    }

    private void performRequestResetCode(String slideVerifyToken) {
        requestAuthCodeButton.setEnabled(false);
        handler.postDelayed(() -> {
            if (isAdded()) {
                requestAuthCodeButton.setEnabled(true);
            }
        }, 60 * 1000);

        Toast.makeText(getActivity(), R.string.requesting_reset_code, Toast.LENGTH_SHORT).show();

        AppService.Instance().requestResetAuthCode(null, slideVerifyToken, new AppService.SendCodeCallback() {
            @Override
            public void onUiSuccess() {
                if (isAdded()) {
                    Toast.makeText(getActivity(), R.string.reset_code_send_success, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onUiFailure(int code, String msg) {
                if (isAdded()) {
                    Toast.makeText(getActivity(), getString(R.string.reset_code_send_failure, code, msg), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void resetPassword() {
        String newPassword = newPasswordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();
        if (!TextUtils.equals(newPassword, confirmPassword)) {
            Toast.makeText(getActivity(), R.string.password_not_match, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Config.ENABLE_SLIDE_VERIFY) {
            performResetPassword(newPassword, null);
            return;
        }

        // Show slide verify dialog before resetting password
        new SlideVerifyDialog(requireContext(), new SlideVerifyDialog.OnVerifySuccessListener() {
            @Override
            public void onVerifySuccess(String token) {
                performResetPassword(newPassword, token);
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

    private void performResetPassword(String newPassword, String slideVerifyToken) {
        MaterialDialog dialog = new MaterialDialog.Builder(requireContext())
            .content(R.string.reset_password_progress)
            .progress(true, 10)
            .cancelable(false)
            .build();
        dialog.show();

        String code = TextUtils.isEmpty(resetCode) ? authCodeEditText.getText().toString() : resetCode;

        AppService.Instance().resetPassword(null, code, newPassword, new SimpleCallback<StatusResult>() {
            @Override
            public void onUiSuccess(StatusResult result) {
                if (!isAdded()) {
                    return;
                }
                Toast.makeText(getActivity(), R.string.reset_password_success, Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                WfcPageCompat.finishPage(ResetPasswordFragment.this);
            }

            @Override
            public void onUiFailure(int code, String msg) {
                if (!isAdded()) {
                    return;
                }
                dialog.dismiss();
                Toast.makeText(getActivity(), getString(R.string.reset_password_failure, code, msg), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
