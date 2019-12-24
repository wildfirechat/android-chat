package cn.wildfire.chat.app.login;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.gson.Gson;

import java.io.IOException;

import butterknife.BindView;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import butterknife.OnTouch;
import cn.wildfire.chat.app.AppService;
import cn.wildfire.chat.app.Config;
import cn.wildfire.chat.app.login.model.LoginResult;
import cn.wildfire.chat.app.login.model.RegResult;
import cn.wildfire.chat.app.main.MainActivity;
import cn.wildfire.chat.app.main.model.MainModel;
import cn.wildfire.chat.kit.ChatManagerHolder;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.net.OKHttpHelper;
import cn.wildfire.chat.kit.net.SimpleCallback;
import cn.wildfire.chat.kit.third.utils.IOUtils;
import cn.wildfirechat.chat.R;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SMSLoginActivity extends WfcBaseActivity {
    @BindView(R.id.loginButton)
    Button loginButton;
    @BindView(R.id.phoneNumberEditText)
    EditText phoneNumberEditText;
    @BindView(R.id.authCodeEditText)
    EditText authCodeEditText;
    @BindView(R.id.requestAuthCodeButton)
    Button requestAuthCodeButton;


    @BindView(R.id.regexp_txt)
    TextView regexp_txt;

    @BindView(R.id.reg_content)
    LinearLayout reg_content;

    @BindView(R.id.regButton)
    Button regButton;
    @BindView(R.id.reg_user_txt)
    EditText reg_user_txt;
    @BindView(R.id.reg_passwd_txt)
    EditText reg_passwd_txt;
    @BindView(R.id.reg_repasswd_txt)
    EditText reg_repasswd_txt;

    private String phoneNumber;

    @Override
    protected int contentLayout() {
        return R.layout.login_activity_sms;
    }


    @OnTextChanged(value = R.id.phoneNumberEditText, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    void inputPhoneNumber(Editable editable) {
        /*String phone = editable.toString().trim();
        if (phone.length() == 11) {
            requestAuthCodeButton.setEnabled(true);
        } else {
            requestAuthCodeButton.setEnabled(false);
            loginButton.setEnabled(false);
        }*/
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
            }
        }, this, dialog);
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




    @OnClick(R.id.regexp_txt)
    void onRegExp(){
        int _visi = reg_content.getVisibility();
        if(_visi == View.GONE) {
            reg_content.setVisibility(View.VISIBLE);
        }else if(_visi == View.VISIBLE){
            reg_content.setVisibility(View.GONE);
        }
    }

    //@OnTouch(R.id.regButton)
    @OnClick(R.id.regButton)
    void onRegacc() {
/*
        String json_str = "{'code':0,'msg':'success','result':{'userId':'cic6c6EE','name':'13774513094'}}";
        Gson gson = new Gson();
        RegResult result = gson.fromJson(json_str, RegResult.class);
        Log.e("AA:", result.code);


    }

void onRegaccTest(){

*/


        String _user = reg_user_txt.getText().toString().trim();
        String _pass = reg_passwd_txt.getText().toString().trim();
        String _repass = reg_repasswd_txt.getText().toString().trim();
        if(_user.length()<=0){
            Toast.makeText(SMSLoginActivity.this, "用户名不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        if(_user.length()<=5){
            Toast.makeText(SMSLoginActivity.this, "用户名长度要5位以上", Toast.LENGTH_SHORT).show();
            return;
        }
        if(_pass.length()<=0 || _repass.length()<=0){
            Toast.makeText(SMSLoginActivity.this, "密码或重复密码不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        if(_pass.length()<=5 || _repass.length()<=5){
            Toast.makeText(SMSLoginActivity.this, "密码要5位以上", Toast.LENGTH_SHORT).show();
            return;
        }
        if(!_pass.equals(_repass)){
            Toast.makeText(SMSLoginActivity.this, "密码和重复密码要相同", Toast.LENGTH_SHORT).show();
            return;
        }

        MaterialDialog dialog = new MaterialDialog.Builder(this)
                .content("注册中...")
                .progress(true, 100)
                .cancelable(false)
                .build();
        dialog.show();

        String php_url = Config.APP_SERVER_PHP + "/yh/apireg.php";
        OkHttpClient client = new OkHttpClient();
        RequestBody formBody = new FormBody.Builder()
                .add("mobile", _user)
                .add("passwd", _pass).build();
        Request request = new Request.Builder().url(php_url)
                .post(formBody)
                .build();

        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(SMSLoginActivity.this, "POST注册出错，请检测网络", Toast.LENGTH_SHORT).show();
                    }
                });
                dialog.dismiss();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseStr = response.body().string();
                dialog.dismiss();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        Log.e("D:", responseStr);
                        //Toast.makeText(SMSLoginActivity.this, responseStr, Toast.LENGTH_SHORT).show();

                        Gson gson = new Gson();
                        RegResult result = gson.fromJson(responseStr, RegResult.class);

                        if(result.code.equals("-11023")) {
                            Toast.makeText(SMSLoginActivity.this, "用户名已存在", Toast.LENGTH_SHORT).show();
                        }else
                        if(result.code.equals("0")) {
                            phoneNumberEditText.setText(_user);
                            authCodeEditText.setText(_pass);
                            login();
                        }

                    }
                });


            }
        });


    }


}
