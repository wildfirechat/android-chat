/*
 * Copyright (c) 2023 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.misc;

import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.Socket;
import java.util.Date;

import cn.wildfire.chat.app.AppService;
import cn.wildfire.chat.app.MyApp;
import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.WfcWebViewActivity;
import cn.wildfire.chat.kit.net.OKHttpHelper;
import cn.wildfire.chat.kit.net.SimpleCallback;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.remote.ChatManager;

public class DiagnoseActivity extends WfcBaseActivity {

    private TextView configInfoTextView;
    private TextView diagnoseResultTextView;

    private StringBuffer diagnoseResultSB;

    @Override
    protected int contentLayout() {
        return R.layout.activity_diagnose;
    }

    protected void bindEvents() {
        super.bindEvents();
        findViewById(R.id.startDiagnoseButton).setOnClickListener(v -> diagnose());
        findViewById(R.id.startTurnDiagnoseButton).setOnClickListener(v -> webrtcDiagnose());
    }

    protected void bindViews() {
        super.bindViews();
        configInfoTextView = findViewById(R.id.configInfoTextView);
        diagnoseResultTextView = findViewById(R.id.resultTextView);
    }

    @Override
    protected void afterViews() {
        super.afterViews();
        updateConfigInfo();
    }

    private void diagnose() {
        Toast.makeText(this, R.string.diagnose_start, Toast.LENGTH_SHORT).show();
        diagnoseResultSB = new StringBuffer();
        checkAppServer();
        checkApiVersion();
        tcping();
    }

    private void webrtcDiagnose() {
        String url = "https://static.wildfirechat.cn/webrtc/index.html";
        if(Config.ICE_SERVERS.length > 0 && Config.ICE_SERVERS[0].length > 0){
            url += "?host=" + Config.ICE_SERVERS[0][0].replace("turn:", "");
            url += "&username=" + Config.ICE_SERVERS[0][1];
            url += "&secret=" + Config.ICE_SERVERS[0][2];
        }
        WfcWebViewActivity.loadUrl(this, "Webrt测试", url);
    }

    private void updateConfigInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append(getString(R.string.diagnose_current_time, new Date().toString())).append("\n");
        sb.append(getString(R.string.diagnose_app_server, AppService.APP_SERVER_ADDRESS)).append("\n");
        sb.append(getString(R.string.diagnose_route_host, Config.IM_SERVER_HOST)).append("\n");
        sb.append(getString(R.string.diagnose_route_port, MyApp.routePort)).append("\n");
        sb.append(getString(R.string.diagnose_longlink_host, MyApp.longLinkHost)).append("\n");
        sb.append(getString(R.string.diagnose_longlink_port, ChatManager.Instance().getLongLinkPort())).append("\n");
        sb.append(getString(R.string.diagnose_av_sdk,
            AVEngineKit.isSupportConference() ?
            getString(R.string.diagnose_av_sdk_pro) :
            getString(R.string.diagnose_av_sdk_basic))).append("\n");

        String ices = "";
        for (String[] ice : Config.ICE_SERVERS) {
            ices += ice[0] + " " + ice[1] + " " + ice[2] + "\n";
        }
        sb.append(getString(R.string.diagnose_turnserver, ices));
        sb.append(getString(R.string.diagnose_proto_version, ChatManager.Instance().getProtoRevision())).append("\n");

        configInfoTextView.setText(sb.toString());
    }

    private void updateDiagnoseResult() {
        diagnoseResultTextView.setText(diagnoseResultSB.toString());
    }

    private void checkAppServer() {
        OKHttpHelper.get(AppService.APP_SERVER_ADDRESS, null, new SimpleCallback<String>() {
            @Override
            public void onUiSuccess(String s) {
                if ("Ok".equals(s)) {
                    diagnoseResultSB.append(getString(R.string.diagnose_app_server_ok)).append("\n\n");
                    updateDiagnoseResult();
                } else {
                    diagnoseResultSB.append(getString(R.string.diagnose_app_server_error, s)).append("\n\n");
                }
            }

            @Override
            public void onUiFailure(int code, String msg) {
                diagnoseResultSB.append(getString(R.string.diagnose_app_server_error, code + " " + msg)).append("\n\n");
                updateDiagnoseResult();
            }
        });
    }

    private void checkApiVersion() {
        String url = "http://" + Config.IM_SERVER_HOST + ":" + MyApp.routePort + "/api/version";
        OKHttpHelper.get(url, null, new SimpleCallback<String>() {
            @Override
            public void onUiSuccess(String s) {
                try {
                    JSONObject json = new JSONObject(s);
                    diagnoseResultSB.append(getString(R.string.diagnose_im_server_ok));
                    diagnoseResultSB.append(getString(R.string.diagnose_remote_origin, json.getString("remoteOriginUrl"))).append("\n");
                    diagnoseResultSB.append(getString(R.string.diagnose_commit_message, json.getString("commitMessageShort"))).append("\n");
                    diagnoseResultSB.append(getString(R.string.diagnose_commit_time, json.getString("commitTime"))).append("\n\n");
                    updateDiagnoseResult();
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onUiFailure(int code, String msg) {
                diagnoseResultSB.append(getString(R.string.diagnose_im_server_error, code, msg)).append("\n");
                updateDiagnoseResult();
            }
        });
    }

    private void tcping() {
        if (TextUtils.isEmpty(MyApp.longLinkHost)) {
            Toast.makeText(this, R.string.diagnose_longlink_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        String host = MyApp.longLinkHost;
        int port = ChatManager.Instance().getLongLinkPort();
        ChatManager.Instance().getWorkHandler().post(() -> {
            try (Socket socket = new Socket(host, port)) {
                diagnoseResultSB.append(getString(R.string.diagnose_tcp_ping_ok)).append("\n\n");
                ChatManager.Instance().getMainHandler().post(this::updateDiagnoseResult);
            } catch (IOException e) {
                diagnoseResultSB.append(getString(R.string.diagnose_tcp_ping_error, e.getMessage())).append("\n\n");
                ChatManager.Instance().getMainHandler().post(this::updateDiagnoseResult);
            }
        });
    }
}