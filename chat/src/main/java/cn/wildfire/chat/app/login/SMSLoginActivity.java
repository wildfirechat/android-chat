package cn.wildfire.chat.app.login;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.text.Editable;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import butterknife.BindView;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import cn.wildfire.chat.app.AppService;
import cn.wildfire.chat.app.login.model.LoginResult;
import cn.wildfire.chat.app.main.MainActivity;
import cn.wildfire.chat.kit.ChatManagerHolder;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfirechat.chat.R;

public class SMSLoginActivity extends WfcBaseActivity {
    @BindView(R.id.loginButton)
    Button loginButton;
    @BindView(R.id.phoneNumberEditText)
    EditText phoneNumberEditText;
    @BindView(R.id.authCodeEditText)
    EditText authCodeEditText;
    @BindView(R.id.requestAuthCodeButton)
    Button requestAuthCodeButton;

    private String phoneNumber;

    @Override
    protected int contentLayout() {
        return R.layout.login_activity_sms;
    }

    @OnTextChanged(value = R.id.phoneNumberEditText, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    void inputPhoneNumber(Editable editable) {
        String phone = editable.toString().trim();
        if (phone.length() == 11) {
            requestAuthCodeButton.setEnabled(true);
        } else {
            requestAuthCodeButton.setEnabled(false);
            loginButton.setEnabled(false);
        }
    }

    @OnTextChanged(value = R.id.authCodeEditText, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    void inputAuthCode(Editable editable) {
        if (editable.toString().length() > 2) {
            loginButton.setEnabled(true);
        }
    }

    @Override
    protected boolean showHomeMenuItem() {
        return false;
    }

    @OnClick(R.id.loginButton)
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
                SharedPreferences sp = getSharedPreferences("config", Context.MODE_PRIVATE);
                sp.edit()
                        .putString("id", loginResult.getUserId())
                        .putString("token", loginResult.getToken())
                        .apply();
                Intent intent = new Intent(SMSLoginActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
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
