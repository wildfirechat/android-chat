package cn.wildfire.chat.app;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.wildfire.chat.app.login.model.LoginResult;
import cn.wildfire.chat.app.login.model.PCSession;
import cn.wildfire.chat.kit.AppServiceProvider;
import cn.wildfire.chat.kit.ChatManagerHolder;
import cn.wildfire.chat.kit.group.GroupAnnouncement;
import cn.wildfire.chat.kit.net.OKHttpHelper;
import cn.wildfire.chat.kit.net.SimpleCallback;
import cn.wildfire.chat.kit.net.base.StatusResult;
import cn.wildfirechat.remote.ChatManager;
import okhttp3.MediaType;

public class AppService implements AppServiceProvider {
    private static AppService Instance = new AppService();

    private AppService() {

    }

    public static AppService Instance() {
        return Instance;
    }

    public interface LoginCallback {
        void onUiSuccess(LoginResult loginResult);

        void onUiFailure(int code, String msg);
    }

    @Deprecated //"已经废弃，请使用smsLogin"
    public void namePwdLogin(String account, String password, LoginCallback callback) {

        String url = Config.APP_SERVER_ADDRESS + "/api/login";
        Map<String, Object> params = new HashMap<>();
        params.put("name", account);
        params.put("password", password);

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

    public void smsLogin(String phoneNumber, String authCode, LoginCallback callback) {

        String url = Config.APP_SERVER_ADDRESS + "/login";
        Map<String, Object> params = new HashMap<>();
        params.put("mobile", phoneNumber);
        params.put("code", authCode);


        //Platform_iOS = 1,
        //Platform_Android = 2,
        //Platform_Windows = 3,
        //Platform_OSX = 4,
        //Platform_WEB = 5,
        //Platform_WX = 6,
        params.put("platform", new Integer(2));

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

    public void requestAuthCode(String phoneNumber, SendCodeCallback callback) {

        String url = Config.APP_SERVER_ADDRESS + "/send_code";
        Map<String, Object> params = new HashMap<>();
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

    public void scanPCLogin(String token, ScanPCCallback callback) {
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

    public void confirmPCLogin(String token, String userId, PCLoginCallback callback) {
        String url = Config.APP_SERVER_ADDRESS + "/confirm_pc";

        Map<String, Object> params = new HashMap<>(2);
        params.put("user_id", userId);
        params.put("token", token);
        OKHttpHelper.post(url, params, new SimpleCallback<PCSession>() {
            @Override
            public void onUiSuccess(PCSession pcSession) {
                if (pcSession.getStatus() == 2) {
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


    @Override
    public void getGroupAnnouncement(String groupId, AppServiceProvider.GetGroupAnnouncementCallback callback) {
        //从SP中获取到历史数据callback回去，然后再从网络刷新
        String url = Config.APP_SERVER_ADDRESS + "/get_group_announcement";

        Map<String, Object> params = new HashMap<>(2);
        params.put("groupId", groupId);
        OKHttpHelper.post(url, params, new SimpleCallback<GroupAnnouncement>() {
            @Override
            public void onUiSuccess(GroupAnnouncement announcement) {
                callback.onUiSuccess(announcement);
            }

            @Override
            public void onUiFailure(int code, String msg) {
                callback.onUiFailure(-1, msg);
            }
        });
    }


    @Override
    public void updateGroupAnnouncement(String groupId, String announcement, AppServiceProvider.UpdateGroupAnnouncementCallback callback) {
        //更新到应用服务，再保存到本地SP中
        String url = Config.APP_SERVER_ADDRESS + "/put_group_announcement";

        Map<String, Object> params = new HashMap<>(2);
        params.put("groupId", groupId);
        params.put("author", ChatManagerHolder.gChatManager.getUserId());
        params.put("text", announcement);
        OKHttpHelper.post(url, params, new SimpleCallback<GroupAnnouncement>() {
            @Override
            public void onUiSuccess(GroupAnnouncement announcement) {
                callback.onUiSuccess(announcement);
            }

            @Override
            public void onUiFailure(int code, String msg) {
                callback.onUiFailure(-1, msg);
            }
        });
    }

    @Override
    public void uploadLog(SimpleCallback<String> callback) {
        List<String> filePaths = ChatManager.Instance().getLogFilesPath();
        if (filePaths == null || filePaths.isEmpty()) {
            if (callback != null) {
                callback.onUiFailure(-1, "没有日志文件");
            }
            return;
        }
        Context context = ChatManager.Instance().getApplicationContext();
        if (context == null) {
            if (callback != null) {
                callback.onUiFailure(-1, "not init");
            }
            return;
        }
        SharedPreferences sp = context.getSharedPreferences("log_history", Context.MODE_PRIVATE);

        String userId = ChatManager.Instance().getUserId();
        String url = Config.APP_SERVER_ADDRESS + "/logs/" + userId + "/upload";

        int toUploadCount = 0;
        Collections.sort(filePaths);
        for (int i = 0; i < filePaths.size(); i++) {
            String path = filePaths.get(i);
            // 重复上传最后一个日志文件，因为上传之后，还会追加内容
            if (!sp.contains(path) || i == filePaths.size() - 1) {
                toUploadCount++;
                File file = new File(path);
                if (file.exists()) {
                    OKHttpHelper.upload(url, null, file, MediaType.get("application/octet-stream"), new SimpleCallback<Void>() {
                        @Override
                        public void onUiSuccess(Void aVoid) {
                            if (callback != null) {
                                callback.onSuccess(url);
                            }
                            sp.edit().putBoolean(path, true).commit();
                        }

                        @Override
                        public void onUiFailure(int code, String msg) {
                            if (callback != null) {
                                callback.onUiFailure(code, msg);
                            }
                        }
                    });
                }
            }
        }
        if (toUploadCount == 0) {
            if (callback != null) {
                callback.onUiFailure(-1, "所有日志都已上传");
            }
        }
    }
}
