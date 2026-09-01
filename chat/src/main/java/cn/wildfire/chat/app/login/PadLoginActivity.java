/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.login;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Handler;
import android.text.Editable;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.king.zxing.util.CodeUtils;

import cn.wildfire.chat.app.AppService;
import cn.wildfire.chat.app.login.model.LoginResult;
import cn.wildfire.chat.app.main.MainActivity;
import cn.wildfire.chat.app.misc.KeyStoreUtil;
import cn.wildfire.chat.app.setting.ResetPasswordActivity;
import cn.wildfire.chat.app.widget.SlideVerifyDialog;
import cn.wildfire.chat.kit.ChatManagerHolder;
import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.WfcBaseNoToolbarActivity;
import cn.wildfire.chat.kit.WfcWebViewActivity;
import cn.wildfire.chat.kit.utils.ViewUtil;
import cn.wildfire.chat.kit.widget.SimpleTextWatcher;
import cn.wildfirechat.chat.R;

/**
 * 平板（sw600dp）卡片式登录页，参考 iOS WFCLoginViewController 的 pad 实现：
 * <p>
 * 登录方式三态（与 iOS/PC 端 loginType 一致）：
 * <ul>
 * <li>0 扫码登录（平板默认）：展示二维码，手机端扫码确认后自动登录；2s 轮询 + 60s 自动刷新；</li>
 * <li>1 密码登录：手机号 + 密码；</li>
 * <li>2 验证码登录：手机号 + 短信验证码（带倒计时）。</li>
 * </ul>
 * <p>
 * 二维码内容为 {@code wildfirechat://pcsession/<token>}，与手机端扫码识别的格式一致；
 * 服务端接口 {@code POST /pc_session} 与 {@code POST /session_login/<token>}（0 成功 / 9 已扫码 / 18 已取消）。
 * <p>
 * 本页仅在平板（{@code sw600dp}，即 {@code WfcDeviceUtils.isTwoPaneLayout}）由启动/协议页路由进入，
 * 手机端登录仍走 {@link LoginActivity} / {@link SMSLoginActivity}，不受影响。
 */
public class PadLoginActivity extends WfcBaseNoToolbarActivity {

    private static final int LOGIN_TYPE_QR = 0;
    private static final int LOGIN_TYPE_PASSWORD = 1;
    private static final int LOGIN_TYPE_SMS = 2;

    private static final long QR_POLL_INTERVAL_MS = 2000;
    private static final long QR_REFRESH_INTERVAL_MS = 60 * 1000;

    private int loginType = LOGIN_TYPE_QR;

    // ==================== 表单 ====================
    private TextView loginTitleTextView;
    private Button loginButton;
    private EditText phoneNumberEditText;
    private EditText passwordEditText;
    private TextView passwordLabel;
    private TextView requestAuthCodeButton;
    private TextView switchButton;
    private TextView registerButton;
    private CheckBox checkBox;

    // ==================== 扫码登录 ====================
    private ImageView qrImageView;
    private ProgressBar qrLoadingView;
    private TextView qrStatusLabel;
    private TextView qrSwitchButton;
    private String pcSessionToken;
    private int qrStatus; // 0 等待扫码；1 已被扫码、等手机端确认

    private final Handler handler = new Handler();
    private Runnable qrPollRunnable;
    private Runnable qrRefreshRunnable;

    // ==================== 验证码登录 ====================
    private boolean hasSlideVerifiedForCode;
    private String cachedSlideVerifyToken;
    private int countdownSeconds = 60;
    private Runnable countdownRunnable;

    @Override
    protected int contentLayout() {
        return R.layout.pad_login_activity;
    }

    @Override
    protected void afterViews() {
        bindViews();
        bindEvents();
        setStatusBarTheme(this, false);
        setStatusBarColor(R.color.gray14);
        if (getIntent().getBooleanExtra("isKickedOff", false)) {
            new MaterialDialog.Builder(this)
                .content(R.string.kicked_off_message)
                .negativeText(R.string.kicked_off_confirm)
                .build()
                .show();
        }
        // 平板默认扫码登录（参考 iOS/PC 端）
        setLoginType(LOGIN_TYPE_QR);
    }

    private void bindViews() {
        loginTitleTextView = findViewById(R.id.loginTitleTextView);
        loginButton = findViewById(R.id.loginButton);
        phoneNumberEditText = findViewById(R.id.phoneNumberEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        passwordLabel = findViewById(R.id.passwordLabel);
        requestAuthCodeButton = findViewById(R.id.requestAuthCodeButton);
        switchButton = findViewById(R.id.switchButton);
        registerButton = findViewById(R.id.registerButton);
        checkBox = findViewById(R.id.agreementCheckBox);
        qrImageView = findViewById(R.id.qrImageView);
        qrLoadingView = findViewById(R.id.qrLoadingView);
        qrStatusLabel = findViewById(R.id.qrStatusLabel);
        qrSwitchButton = findViewById(R.id.qrSwitchButton);

        TextView agreementTextView = findViewById(R.id.agreementTextView);
        CharSequence text = Html.fromHtml(getString(R.string.privacy_agreement_tip_and_links));
        SpannableString spannableString = new SpannableString(text);
        URLSpan[] urlSpans = spannableString.getSpans(0, spannableString.length(), URLSpan.class);
        for (URLSpan urlSpan : urlSpans) {
            int start = spannableString.getSpanStart(urlSpan);
            int end = spannableString.getSpanEnd(urlSpan);
            final String url = urlSpan.getURL();
            ClickableSpan clickableSpan = new ClickableSpan() {
                @Override
                public void onClick(View widget) {
                    handleAgreementClick(url);
                }
            };
            spannableString.removeSpan(urlSpan);
            spannableString.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        agreementTextView.setText(spannableString);
        agreementTextView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void bindEvents() {
        qrImageView.setOnClickListener(v -> refreshQRCode());
        qrSwitchButton.setOnClickListener(v -> onSwitchQRLogin());
        switchButton.setOnClickListener(v -> setLoginType(loginType == LOGIN_TYPE_PASSWORD ? LOGIN_TYPE_SMS : LOGIN_TYPE_PASSWORD));
        registerButton.setOnClickListener(v -> onRegister());
        requestAuthCodeButton.setOnClickListener(v -> requestAuthCode());
        loginButton.setOnClickListener(v -> {
            if (checkBox.isChecked()) {
                login();
            } else {
                ViewUtil.hideKeyboard(this, passwordEditText);
                Toast.makeText(this, R.string.check_agreement_tip, Toast.LENGTH_SHORT).show();
            }
        });
        phoneNumberEditText.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                updateButtons();
            }
        });
        passwordEditText.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                updateButtons();
            }
        });
    }

    private void handleAgreementClick(String url) {
        if (url.contains("privacy://")) {
            if (TextUtils.isEmpty(Config.PRIVACY_AGREEMENT_URL) || Config.PRIVACY_AGREEMENT_URL.indexOf("https://example.com") >= 0) {
                Toast.makeText(this, R.string.no_privacy_agreement_url_tip, Toast.LENGTH_SHORT).show();
                return;
            }
            WfcWebViewActivity.loadUrl(this, getString(R.string.privacy_agreement), Config.PRIVACY_AGREEMENT_URL);
        } else if (url.contains("user://")) {
            if (TextUtils.isEmpty(Config.USER_AGREEMENT_URL) || Config.USER_AGREEMENT_URL.indexOf("https://example.com") >= 0) {
                Toast.makeText(this, R.string.no_user_agreement_url_tip, Toast.LENGTH_SHORT).show();
                return;
            }
            WfcWebViewActivity.loadUrl(this, getString(R.string.user_agreement), Config.USER_AGREEMENT_URL);
        }
    }

    // ==================== 登录方式切换 ====================

    private void setLoginType(int type) {
        loginType = type;
        if (type == LOGIN_TYPE_QR) {
            findViewById(R.id.qrContainer).setVisibility(View.VISIBLE);
            findViewById(R.id.formContainer).setVisibility(View.GONE);
            loginTitleTextView.setVisibility(View.GONE);
            qrSwitchButton.setText(R.string.use_password_or_code_login);
            showQRLogin();
        } else {
            stopQRPolling();
            findViewById(R.id.qrContainer).setVisibility(View.GONE);
            findViewById(R.id.formContainer).setVisibility(View.VISIBLE);
            loginTitleTextView.setVisibility(View.VISIBLE);
            qrSwitchButton.setText(R.string.scan_code_login);
            configureFormForLoginType(type);
        }
        // 切换登录方式后，重置滑动验证标志并更新按钮状态
        hasSlideVerifiedForCode = false;
        cachedSlideVerifyToken = null;
        updateButtons();
    }

    private void configureFormForLoginType(int type) {
        boolean isPwdLogin = type == LOGIN_TYPE_PASSWORD;
        if (isPwdLogin) {
            loginTitleTextView.setText(R.string.login_password_title);
            passwordLabel.setText(R.string.login_password_label);
            requestAuthCodeButton.setVisibility(View.GONE);
            passwordEditText.setHint(R.string.login_password_hint);
            passwordEditText.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
            switchButton.setText(R.string.login_use_auth_code);
        } else {
            loginTitleTextView.setText(R.string.sms_login_title);
            passwordLabel.setText(R.string.verify_code);
            requestAuthCodeButton.setVisibility(View.VISIBLE);
            passwordEditText.setHint(R.string.verify_code_hint);
            passwordEditText.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
            switchButton.setText(R.string.login_use_password);
        }
        passwordEditText.setText("");
    }

    private void onSwitchQRLogin() {
        // 扫码 ⇄ 验证码登录（参考 iOS：扫码切换回验证码）
        setLoginType(loginType == LOGIN_TYPE_QR ? LOGIN_TYPE_SMS : LOGIN_TYPE_QR);
    }

    private void onRegister() {
        MaterialDialog dialog = new MaterialDialog.Builder(this)
            .title(R.string.register_tip_title)
            .content(R.string.register_tip_message)
            .cancelable(true)
            .positiveText(R.string.confirm)
            .negativeText(R.string.cancel)
            .onPositive((dialog1, which) -> setLoginType(LOGIN_TYPE_SMS))
            .build();
        dialog.show();
    }

    private void updateButtons() {
        String phone = phoneNumberEditText.getText().toString().trim();
        boolean phoneValid = isValidNumber(phone);
        if (loginType == LOGIN_TYPE_SMS) {
            // 验证码发送按钮：只依赖手机号
            requestAuthCodeButton.setEnabled(phoneValid && countdownRunnable == null);
        }
        // 登录按钮：手机号与密码/验证码都不为空（密码登录沿用手机版不校验手机号格式的行为）
        boolean loginEnabled = !TextUtils.isEmpty(phone)
            && !TextUtils.isEmpty(passwordEditText.getText())
            && (loginType != LOGIN_TYPE_SMS || phoneValid);
        loginButton.setEnabled(loginEnabled);
    }

    private boolean isValidNumber(String phone) {
        if (TextUtils.isEmpty(phone)) {
            return false;
        }
        String regex = "^((1[23456789]))\\d{9}$";
        return phone.length() == 11 && phone.matches(regex);
    }

    // ==================== 密码 / 验证码登录 ====================

    private void login() {
        String account = phoneNumberEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        loginButton.setEnabled(false);

        if (loginType == LOGIN_TYPE_PASSWORD) {
            if (!Config.ENABLE_SLIDE_VERIFY) {
                performPasswordLogin(account, password, null);
                return;
            }
            SlideVerifyDialog verifyDialog = new SlideVerifyDialog(this, new SlideVerifyDialog.OnVerifySuccessListener() {
                @Override
                public void onVerifySuccess(String token) {
                    performPasswordLogin(account, password, token);
                }

                @Override
                public void onVerifyFailed() {
                }

                @Override
                public void onLoadFailed() {
                    loginButton.setEnabled(true);
                }
            });
            verifyDialog.show();
        } else {
            if (!Config.ENABLE_SLIDE_VERIFY) {
                performSMSLogin(account, password, null);
                return;
            }
            // 已经通过滑动验证（发送验证码时已验证），直接登录
            if (hasSlideVerifiedForCode && cachedSlideVerifyToken != null) {
                performSMSLogin(account, password, null);
                return;
            }
            SlideVerifyDialog verifyDialog = new SlideVerifyDialog(this, new SlideVerifyDialog.OnVerifySuccessListener() {
                @Override
                public void onVerifySuccess(String token) {
                    performSMSLogin(account, password, token);
                }

                @Override
                public void onVerifyFailed() {
                }

                @Override
                public void onLoadFailed() {
                    loginButton.setEnabled(true);
                }
            });
            verifyDialog.show();
        }
    }

    private void performPasswordLogin(String account, String password, String slideVerifyToken) {
        MaterialDialog dialog = new MaterialDialog.Builder(this)
            .content(R.string.login_progress)
            .progress(true, 10)
            .cancelable(false)
            .build();
        dialog.show();

        AppService.Instance().passwordLogin(account, password, slideVerifyToken, new AppService.LoginCallback() {
            @Override
            public void onUiSuccess(LoginResult loginResult) {
                if (isFinishing()) {
                    return;
                }
                dialog.dismiss();
                loginSuccess(loginResult);
            }

            @Override
            public void onUiFailure(int code, String msg) {
                if (isFinishing()) {
                    return;
                }
                dialog.dismiss();
                loginButton.setEnabled(true);
                Toast.makeText(PadLoginActivity.this, getString(R.string.login_error_hint, code, msg), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void performSMSLogin(String phoneNumber, String authCode, String slideVerifyToken) {
        MaterialDialog dialog = new MaterialDialog.Builder(this)
            .content(R.string.login_progress)
            .progress(true, 100)
            .cancelable(false)
            .build();
        dialog.show();

        AppService.Instance().smsLogin(phoneNumber, authCode, slideVerifyToken, new AppService.LoginCallback() {
            @Override
            public void onUiSuccess(LoginResult loginResult) {
                if (isFinishing()) {
                    return;
                }
                dialog.dismiss();
                loginSuccess(loginResult);
            }

            @Override
            public void onUiFailure(int code, String msg) {
                if (isFinishing()) {
                    return;
                }
                dialog.dismiss();
                loginButton.setEnabled(true);
                hasSlideVerifiedForCode = false;
                cachedSlideVerifyToken = null;
                Toast.makeText(PadLoginActivity.this, getString(R.string.sms_login_failure, code, msg), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 登录成功：保存凭证、连接 IM、进入主界面（扫码 / 密码 / 验证码三种方式共用）。
     */
    private void loginSuccess(LoginResult loginResult) {
        loginSuccess(loginResult.getUserId(), loginResult.getToken(), loginResult.getResetCode());
    }

    private void loginSuccess(String userId, String token, String resetCode) {
        //需要注意token跟clientId是强依赖的，一定要调用getClientId获取到clientId，然后用这个clientId获取token，这样connect才能成功，如果随便使用一个clientId获取到的token将无法链接成功。
        ChatManagerHolder.gChatManager.connect(userId, token);
        try {
            KeyStoreUtil.saveData(PadLoginActivity.this, "wf_userId", userId);
            KeyStoreUtil.saveData(PadLoginActivity.this, "wf_token", token);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Intent intent = new Intent(PadLoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        // 初始密码（需要 app-server 开启，开启后，默认是手机号后 6 位）登录后，重置密码
        if (!TextUtils.isEmpty(resetCode)) {
            Intent resetPasswordIntent = new Intent(PadLoginActivity.this, ResetPasswordActivity.class);
            resetPasswordIntent.putExtra("resetCode", resetCode);
            startActivity(resetPasswordIntent);
        }
        finish();
    }

    private void requestAuthCode() {
        String phoneNumber = phoneNumberEditText.getText().toString().trim();
        if (!Config.ENABLE_SLIDE_VERIFY) {
            performRequestAuthCode(phoneNumber, null);
            return;
        }
        SlideVerifyDialog verifyDialog = new SlideVerifyDialog(this, new SlideVerifyDialog.OnVerifySuccessListener() {
            @Override
            public void onVerifySuccess(String token) {
                performRequestAuthCode(phoneNumber, token);
            }

            @Override
            public void onVerifyFailed() {
            }

            @Override
            public void onLoadFailed() {
                requestAuthCodeButton.setEnabled(true);
            }
        });
        verifyDialog.show();
    }

    private void performRequestAuthCode(String phoneNumber, String slideVerifyToken) {
        requestAuthCodeButton.setEnabled(false);
        countdownSeconds = 60;
        updateCountdownText();
        if (countdownRunnable == null) {
            countdownRunnable = new Runnable() {
                @Override
                public void run() {
                    if (isFinishing()) {
                        return;
                    }
                    countdownSeconds--;
                    updateCountdownText();
                    if (countdownSeconds > 0) {
                        handler.postDelayed(this, 1000);
                    } else {
                        requestAuthCodeButton.setText(R.string.get_verify_code);
                        requestAuthCodeButton.setEnabled(true);
                        countdownRunnable = null;
                    }
                }
            };
        }
        handler.postDelayed(countdownRunnable, 1000);

        AppService.Instance().requestAuthCode(phoneNumber, slideVerifyToken, new AppService.SendCodeCallback() {
            @Override
            public void onUiSuccess() {
                Toast.makeText(PadLoginActivity.this, R.string.auth_code_request_success, Toast.LENGTH_SHORT).show();
                hasSlideVerifiedForCode = true;
                cachedSlideVerifyToken = slideVerifyToken;
            }

            @Override
            public void onUiFailure(int code, String msg) {
                Toast.makeText(PadLoginActivity.this, getString(R.string.auth_code_request_failure, code, msg), Toast.LENGTH_SHORT).show();
                hasSlideVerifiedForCode = false;
                cachedSlideVerifyToken = null;
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
        if (countdownRunnable != null) {
            handler.removeCallbacks(countdownRunnable);
        }
        requestAuthCodeButton.setText(R.string.get_verify_code);
        requestAuthCodeButton.setEnabled(true);
        countdownSeconds = 60;
        countdownRunnable = null;
    }

    // ==================== 扫码登录（参考 iOS WFCLoginViewController） ====================

    private void showQRLogin() {
        qrStatus = 0;
        qrStatusLabel.setText(R.string.qr_login_waiting);
        refreshQRCode();
    }

    private void refreshQRCode() {
        stopQRPolling();
        pcSessionToken = null;
        qrImageView.setImageDrawable(null);
        qrImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        qrLoadingView.setVisibility(View.VISIBLE);
        qrStatus = 0;
        qrStatusLabel.setText(R.string.qr_login_waiting);

        AppService.Instance().createPCLoginSession(null, new AppService.PCLoginSessionCallback() {
            @Override
            public void onSuccess(@NonNull String token) {
                if (isFinishing() || loginType != LOGIN_TYPE_QR) {
                    return;
                }
                pcSessionToken = token;
                qrLoadingView.setVisibility(View.GONE);
                // 二维码内容与手机端扫码识别的格式一致：wildfirechat://pcsession/<token>
                // CodeUtils 来自 zxing-lite（编译期 compileOnly），运行期由 uikit 的 zxing core 提供
                Bitmap bitmap = CodeUtils.createQRCode("wildfirechat://pcsession/" + token, 480);
                if (bitmap != null) {
                    qrImageView.setImageBitmap(bitmap);
                } else {
                    qrStatusLabel.setText(R.string.qr_code_generate_failed);
                    return;
                }
                startQRPolling();
            }

            @Override
            public void onFailure(int code, String msg) {
                if (isFinishing() || loginType != LOGIN_TYPE_QR) {
                    return;
                }
                qrLoadingView.setVisibility(View.GONE);
                qrStatusLabel.setText(R.string.qr_code_generate_failed);
            }
        });
    }

    private void startQRPolling() {
        if (qrPollRunnable == null) {
            qrPollRunnable = this::pollQRStatus;
            // 立即轮询一次，之后由 pollQRStatus 自身每 2s 重新排队（与 iOS NSTimer fire + 2s 定时一致）
            handler.post(qrPollRunnable);
            // 60 秒后自动刷新二维码（与 PC 端一致）
            qrRefreshRunnable = this::refreshQRCode;
            handler.postDelayed(qrRefreshRunnable, QR_REFRESH_INTERVAL_MS);
        }
    }

    private void stopQRPolling() {
        if (qrPollRunnable != null) {
            handler.removeCallbacks(qrPollRunnable);
            qrPollRunnable = null;
        }
        if (qrRefreshRunnable != null) {
            handler.removeCallbacks(qrRefreshRunnable);
            qrRefreshRunnable = null;
        }
    }

    private void pollQRStatus() {
        // qrPollRunnable 被 stopQRPolling 置空后，不再重新排队，轮询就此停止
        if (qrPollRunnable == null || TextUtils.isEmpty(pcSessionToken) || loginType != LOGIN_TYPE_QR) {
            return;
        }
        String token = pcSessionToken;
        AppService.Instance().loginWithPCLoginSession(token, new AppService.PCLoginPollCallback() {
            @Override
            public void onSuccess(@NonNull String userId, @NonNull String imToken) {
                if (isFinishing() || loginType != LOGIN_TYPE_QR || !token.equals(pcSessionToken)) {
                    // 二维码已刷新，丢弃过期结果
                    return;
                }
                // 登录等于同意用户协议：未勾选时提示并刷新回新的二维码，不登录
                if (!checkBox.isChecked()) {
                    Toast.makeText(PadLoginActivity.this, R.string.check_agreement_tip, Toast.LENGTH_SHORT).show();
                    refreshQRCode();
                    return;
                }
                stopQRPolling();
                loginSuccess(userId, imToken, null);
            }

            @Override
            public void onScanned(String userName, String portrait) {
                if (isFinishing() || loginType != LOGIN_TYPE_QR || !token.equals(pcSessionToken)) {
                    return;
                }
                if (qrStatus != 1) {
                    qrStatus = 1;
                    // 扫码后、手机端确认前：二维码区域换成扫码用户的头像（参考 iOS/PC 端）
                    qrImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    if (!TextUtils.isEmpty(portrait)) {
                        Glide.with(PadLoginActivity.this)
                            .load(portrait)
                            .placeholder(R.mipmap.default_header)
                            .error(R.mipmap.default_header)
                            .into(qrImageView);
                    } else {
                        qrImageView.setImageResource(R.mipmap.default_header);
                    }
                    qrStatusLabel.setText(getString(R.string.qr_login_scanned, userName == null ? "" : userName));
                }
            }

            @Override
            public void onCanceled() {
                if (isFinishing() || loginType != LOGIN_TYPE_QR || !token.equals(pcSessionToken)) {
                    return;
                }
                // 手机端取消/拒绝登录，重新生成二维码
                refreshQRCode();
            }

            @Override
            public void onFailure(int code, String msg) {
                // 网络波动等暂时性错误：继续轮询即可
            }
        });
        // 安排下一次轮询（stopQRPolling 置空 qrPollRunnable 后，下次触发直接退出）
        if (qrPollRunnable != null && loginType == LOGIN_TYPE_QR && !TextUtils.isEmpty(pcSessionToken)) {
            handler.postDelayed(qrPollRunnable, QR_POLL_INTERVAL_MS);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 回到前台时若仍在扫码模式，恢复轮询（onPause 时已停掉）
        if (loginType == LOGIN_TYPE_QR && !TextUtils.isEmpty(pcSessionToken)) {
            startQRPolling();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 离开页面时停掉扫码轮询
        stopQRPolling();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopQRPolling();
        if (countdownRunnable != null) {
            handler.removeCallbacks(countdownRunnable);
        }
    }
}
