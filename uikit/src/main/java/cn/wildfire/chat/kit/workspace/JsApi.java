/*
 * Copyright (c) 2022 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.workspace;

import android.app.Activity;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import cn.wildfire.chat.kit.WfcWebViewActivity;
import cn.wildfire.chat.kit.contact.pick.PickContactActivity;
import cn.wildfire.chat.kit.group.GroupListActivity;
import cn.wildfire.chat.kit.page.WfcPageCompat;
import cn.wildfire.chat.kit.utils.FileUtils;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GeneralCallback;
import cn.wildfirechat.remote.GeneralCallback2;
import wendu.dsbridge.CompletionHandler;
import wendu.dsbridge.DWebView;

public class JsApi {
    private static final String TAG = "JsApi";
    private Activity activity;
    private Fragment fragment;
    private DWebView webView;
    private String appUrl;
    private String currentUrl;
    private SparseArray<CompletionHandler> jsCallbackHandlers;
    private boolean ready;

    private static final int REQUEST_CODE_PICK_CONTACT = 300;
    private static final int REQUEST_CODE_PICK_GROUP = 301;

    public JsApi(Activity context, DWebView webView, String url) {
        this.activity = context;
        this.webView = webView;
        this.appUrl = url;
        this.currentUrl = url;
        jsCallbackHandlers = new SparseArray<>();
    }

    public JsApi(Fragment fragment, DWebView webView, String url) {
        this(fragment.getActivity(), webView, url);
        this.fragment = fragment;
    }

    public void setCurrentUrl(String currentUrl) {
        this.currentUrl = currentUrl;
    }

    @JavascriptInterface
    public void openUrl(Object url) {
//        if (!Config.WORKSPACE_URL.equals(this.appUrl)) {
//            Log.e(TAG, "only workspace can call openurl " + this.appUrl);
//            return;
//        }
        if (fragment != null) {
            WfcWebViewActivity.loadUrl(fragment, "", url.toString());
        } else {
            WfcWebViewActivity.loadUrl(activity, "", url.toString());
        }
    }

    @JavascriptInterface
    public void close(Object obj, CompletionHandler handler) {
        // 平板右栏里 activity 是双栏主界面，finish 掉就是整个界面退出；
        // 走 WfcPageCompat 才是「关掉网页这一页」。手机端仍然落到 activity.finish()。
        if (fragment != null) {
            WfcPageCompat.finishPage(fragment);
        } else {
            activity.finish();
        }
        JSONObject resultObj = new JSONObject();
        try {
            resultObj.put("code", 0);
            handler.complete(resultObj);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // web 从 native 获取授权码，并进行登录
    @JavascriptInterface
    public void getAuthCode(Object obj, CompletionHandler handler) {
        JSONObject jsonObject = (JSONObject) obj;
        String appId = jsonObject.optString("appId");
        int type = jsonObject.optInt("appType");
        String host = null;
        try {
            host = new URL(appUrl).getHost().toString();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        // 开发调试时，将 host 固定写是为开发平台上该应用的回调地址对应的 host
        Log.d(TAG, "getAuthCode appUrl " + this.appUrl + " appId:" + appId + " type:" + type + " host:" + host);
        ChatManager.Instance().getAuthCode(appId, type, host, new GeneralCallback2() {
            @Override
            public void onSuccess(String result) {
                JSONObject resultObj = new JSONObject();
                try {
                    resultObj.put("code", 0);
                    resultObj.put("data", result);
                    Log.d(TAG, "getAuthCode success, appUrl: " + appUrl + " success: " + result);
                    handler.complete(resultObj);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFail(int errorCode) {
                JSONObject resultObj = new JSONObject();
                try {
                    resultObj.put("code", errorCode);
                    Log.d(TAG, "getAuthCode fail, appUrl: " + appUrl + " errorCode: " + errorCode);
                    Log.d(TAG, "getAuthCode fail, appUrl: " + appUrl + " ,请确认应用 url 对应的 host和应用回调地址的 host 是否一致");
                    handler.complete(resultObj);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // native 认证 web
    // 大概流程是 web应用端从对应 web 应用服务端获取应用验证信息，调用该接口去 im-server 进行认证
    @JavascriptInterface
    public void config(Object obj) {
        JSONObject jsonObject = (JSONObject) obj;
        String appId = jsonObject.optString("appId");
        int type = jsonObject.optInt("appType");
        long timestamp = jsonObject.optLong("timestamp");
        String nonce = jsonObject.optString("nonceStr");
        // signature 是 web 应用服务端使用 nonce | appId | timestamp | appSecret 进行 sha1 加密得到的字符串
        // im-server 根据 appId 拿到 appSecret，然后用相同的算法生成 signature 进行对比
        String signature = jsonObject.optString("signature");
        Log.d(TAG, "config appUrl: "+ this.appUrl + " appId:" + appId + " type:" + type + " timestamp:" + timestamp + " nonce:" + nonce + " signature:" + signature);
        ChatManager.Instance().configApplication(appId, type, timestamp, nonce, signature, new GeneralCallback() {
            @Override
            public void onSuccess() {
                JsApi.this.ready = true;
                Log.d(TAG, "config success, appUrl: " +  appUrl);
                webView.callHandler("ready", (Object[]) null);
            }

            @Override
            public void onFail(int errorCode) {
                Log.d(TAG, "config appUrl: " +  appUrl + " fail:" + errorCode);
                webView.callHandler("error", new String[]{"" + errorCode});
            }
        });
    }

    @JavascriptInterface
    public void toast(Object text) {
        Toast.makeText(activity, text.toString(), Toast.LENGTH_SHORT).show();
    }

    @JavascriptInterface
    public void chooseContacts(Object obj, CompletionHandler handler) {
        if (!preCheck()) {
            callbackJs(handler, -2);
            return;
        }
        JSONObject jsonObject = (JSONObject) obj;
        int max = jsonObject.optInt("max", 0);
        Intent intent = PickContactActivity.buildPickIntent(activity, max, null, null);
        startActivityForResult(intent, REQUEST_CODE_PICK_CONTACT);
        this.jsCallbackHandlers.put(REQUEST_CODE_PICK_CONTACT, handler);
    }

    /**
     * 在线文档页选群。异步回调 JSON 串 [{gid,name,portrait}]。
     */
    @JavascriptInterface
    public void chooseGroup(Object obj, CompletionHandler handler) {
        if (!preCheck()) {
            callbackJs(handler, -2);
            return;
        }
        Intent intent = new Intent(activity, GroupListActivity.class);
        intent.putExtra(GroupListActivity.INTENT_FOR_RESULT, true);
        startActivityForResult(intent, REQUEST_CODE_PICK_GROUP);
        this.jsCallbackHandlers.put(REQUEST_CODE_PICK_GROUP, handler);
    }

    /**
     * 在线文档页的「下载」：H5 用 callSync 调，交给系统打开地址。
     */
    @JavascriptInterface
    public void downloadFile(Object arg) {
        String url = null;
        if (arg instanceof String) {
            url = (String) arg;
        } else if (arg instanceof JSONObject) {
            url = ((JSONObject) arg).optString("url");
        } else if (arg instanceof java.util.Map) {
            Object value = ((java.util.Map<?, ?>) arg).get("url");
            if (value != null) {
                url = value.toString();
            }
        }
        if (TextUtils.isEmpty(url)) {
            Log.w(TAG, "downloadFile ignored, no url: " + arg);
            return;
        }
        FileUtils.openUrlBySystem(activity, url);
    }

    /**
     * 在线文档页把标题交给客户端标题栏渲染。操作按钮（分享等）暂不实现，返回 code 0 让页面不再重画标题。
     */
    @JavascriptInterface
    public void setPageHeader(Object obj, CompletionHandler handler) {
        if (obj instanceof JSONObject) {
            String title = ((JSONObject) obj).optString("title");
            if (!TextUtils.isEmpty(title)) {
                if (fragment != null) {
                    WfcPageCompat.setPageTitle(fragment, title);
                } else if (activity != null) {
                    activity.setTitle(title);
                }
            }
        }
        if (handler != null) {
            JSONObject resultObj = new JSONObject();
            try {
                resultObj.put("code", 0);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            handler.complete(resultObj);
        }
    }

    public boolean onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        boolean handled = true;
        switch (requestCode) {
            case REQUEST_CODE_PICK_CONTACT:
                handlePickContactResult(requestCode, resultCode, data);
                break;
            case REQUEST_CODE_PICK_GROUP:
                handlePickGroupResult(requestCode, resultCode, data);
                break;
            default:
                handled = false;
                break;
        }
        return handled;
    }

    private void handlePickContactResult(int requestCode, int resultCode, Intent data) {
        CompletionHandler handler = this.jsCallbackHandlers.get(requestCode);
        if (resultCode == Activity.RESULT_OK) {
            if (data != null) {
                List<UserInfo> userInfos = data.getParcelableArrayListExtra(PickContactActivity.RESULT_PICKED_USERS);
                callbackJs(handler, 0, userInfos);
            } else {
                callbackJs(handler, -1);
            }
        } else {
            callbackJs(handler, -1);
        }
        this.jsCallbackHandlers.remove(requestCode);
    }

    private void handlePickGroupResult(int requestCode, int resultCode, Intent data) {
        CompletionHandler handler = this.jsCallbackHandlers.get(requestCode);
        if (resultCode == Activity.RESULT_OK && data != null) {
            List<GroupInfo> groupInfos = data.getParcelableArrayListExtra("groupInfos");
            if (groupInfos != null && !groupInfos.isEmpty()) {
                try {
                    JSONArray array = new JSONArray();
                    for (GroupInfo groupInfo : groupInfos) {
                        JSONObject object = new JSONObject();
                        object.put("gid", groupInfo.target);
                        object.put("name", groupInfo.name);
                        object.put("portrait", groupInfo.portrait);
                        array.put(object);
                    }
                    callbackJs(handler, 0, array.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                    callbackJs(handler, -1);
                }
            } else {
                callbackJs(handler, -1);
            }
        } else {
            callbackJs(handler, -1);
        }
        this.jsCallbackHandlers.remove(requestCode);
    }

    private void callbackJs(CompletionHandler handler, int code) {
        callbackJs(handler, code, null);
    }

    private void callbackJs(CompletionHandler handler, int code, Object result) {
        if (handler == null) {
            return;
        }
        JSONObject object = new JSONObject();
        try {
            if (result instanceof JSONObject || result instanceof String) {
                object.put("data", result);
            } else {
                object.put("data", new Gson().toJson(result));
            }
            object.put("code", code);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        handler.complete(object);
    }

    private void startActivityForResult(Intent intent, int requestCode) {
        if (fragment != null) {
            // 走 WfcPageCompat：选联系人在平板上压到网页所在的那条右栏栈上，
            // 结果照样带着原始 requestCode 回到 fragment.onActivityResult
            WfcPageCompat.startPageForResult(fragment, intent, requestCode);
        } else {
            activity.startActivityForResult(intent, requestCode);
        }

    }

    // 在线文档页只调 getAuthCode、不调 config，因此这里不能要求 ready；
    // 只校验页面与初始地址同源，防止被任意网页调用。
    private boolean preCheck() {
        return TextUtils.equals(getHost(this.appUrl), getHost(this.currentUrl));
    }

    private String getHost(String url) {
        try {
            String host = new URL(url).getHost();
            return host;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }

}
