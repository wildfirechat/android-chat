package cn.wildfire.chat.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.HashMap;
import java.util.Map;

import cn.wildfire.chat.app.login.LoginActivity;
import cn.wildfire.chat.app.login.SMSLoginActivity;
import cn.wildfire.chat.app.login.model.LoginResult;
import cn.wildfire.chat.app.login.model.PCSession;
import cn.wildfire.chat.app.main.MainActivity;
import cn.wildfire.chat.app.main.PCLoginActivity;
import cn.wildfire.chat.kit.ChatManagerHolder;
import cn.wildfire.chat.kit.net.OKHttpHelper;
import cn.wildfire.chat.kit.net.SimpleCallback;
import cn.wildfire.chat.kit.net.base.StatusResult;

public class AppService {
    public interface LoginCallback {
        void onUiSuccess(LoginResult loginResult);
        void onUiFailure(int code, String msg);
    }

    public static void login(String phoneNumber, String authCode, LoginCallback callback) {

        String url = Config.APP_SERVER_ADDRESS + "/login";
        Map<String, String> params = new HashMap<>();
        params.put("mobile", phoneNumber);
        params.put("code", authCode);

        try {
            params.put("clientId", ChatManagerHolder.gChatManager.getClientId());
        } catch (Exception e) {
            e.printStackTrace();
            callback.onUiFailure(-1, "网络出来问题了。。。");
            return;
        }

        OKHttpHelper.post(url, params, new SimpleCallback<LoginResult>() {
            @Override
            public void onUiSuccess(LoginResult loginResult) {
                callback.onUiSuccess(loginResult);
            }

            @Override
            public void onUiFailure(int code, String msg) {
                callback.onUiFailure(code, msg);
            }
        });
    }

    public interface SendCodeCallback {
        void onUiSuccess();
        void onUiFailure(int code, String msg);
    }

    public static void requestAuthCode(String phoneNumber, SendCodeCallback callback) {

        String url = Config.APP_SERVER_ADDRESS + "/send_code";
        Map<String, String> params = new HashMap<>();
        params.put("mobile", phoneNumber);
        OKHttpHelper.post(url, params, new SimpleCallback<StatusResult>() {
            @Override
            public void onUiSuccess(StatusResult statusResult) {
                if (statusResult.getCode() == 0) {
                    callback.onUiSuccess();
                } else {
                    callback.onUiFailure(statusResult.getCode(), "");
                }
            }

            @Override
            public void onUiFailure(int code, String msg) {
                callback.onUiFailure(-1, msg);
            }
        });

    }

    public interface ScanPCCallback {
        void onUiSuccess(PCSession pcSession);
        void onUiFailure(int code, String msg);
    }

    public static void scanPCLogin(String token, ScanPCCallback callback) {
        String url = Config.APP_SERVER_ADDRESS + "/scan_pc";
        url += "/" + token;
        OKHttpHelper.post(url, null, new SimpleCallback<PCSession>() {
            @Override
            public void onUiSuccess(PCSession pcSession) {
                if (pcSession.getStatus() == 1) {
                    callback.onUiSuccess(pcSession);
                } else {
                    callback.onUiFailure(pcSession.getStatus(), "");
                }
            }

            @Override
            public void onUiFailure(int code, String msg) {
                callback.onUiFailure(-1, msg);
            }
        });
    }

    public interface PCLoginCallback {
        void onUiSuccess();
        void onUiFailure(int code, String msg);
    }
    public static void confirmPCLogin(String token, String userId, PCLoginCallback callback) {
        String url = Config.APP_SERVER_ADDRESS + "/confirm_pc";

        Map<String, String> params = new HashMap<>(2);
        params.put("user_id", userId);
        params.put("token", token);
        OKHttpHelper.post(url, params, new SimpleCallback<PCSession>() {
            @Override
            public void onUiSuccess(PCSession pcSession) {
                if (pcSession.getStatus() == 0) {
                    callback.onUiSuccess();
                } else {
                    callback.onUiFailure(pcSession.getStatus(), "");
                }
            }

            @Override
            public void onUiFailure(int code, String msg) {
                callback.onUiFailure(-1, msg);
            }
        });
    }
}
