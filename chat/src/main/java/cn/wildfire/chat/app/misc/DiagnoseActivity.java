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
        // 1. app-server
        // 2. api/version
        // 3. tcping

        Toast.makeText(this, "开始进行网络诊断，请稍后", Toast.LENGTH_SHORT).show();

        diagnoseResultSB = new StringBuffer();
        checkAppServer();
        checkApiVersion();
        tcping();
    }

    private void updateConfigInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("当前时间：" + new Date().toString());
        sb.append("\n");
        sb.append("APP-Sever: " + AppService.APP_SERVER_ADDRESS);
        sb.append("\n");
        sb.append("Route-Host: " + Config.IM_SERVER_HOST);
        sb.append("\n");
        sb.append("Route-Port: " + MyApp.routePort);
        sb.append("\n");
        sb.append("Long-Link-Host: " + MyApp.longLinkHost);
        sb.append("\n");
        sb.append("Long-Link-Port: " + ChatManager.Instance().getLongLinkPort());
        sb.append("\n");
        sb.append("音视频 SDK: " + (AVEngineKit.isSupportConference() ? "高级版" : "多人版") + "\n");
        String ices = "";
        for (String[] ice : Config.ICE_SERVERS) {
            ices += ice[0] + " " + ice[1] + " " + ice[2] + "\n";
        }
        sb.append("Turn-Server: " + ices);
        sb.append("协议栈版本：" + ChatManager.Instance().getProtoRevision());
        sb.append("\n");

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
                    diagnoseResultSB.append("APP-Server 正常");
                    diagnoseResultSB.append("\n\n");

                    updateDiagnoseResult();
                } else {
                    diagnoseResultSB.append("APP-Server 异常: " + s);
                    diagnoseResultSB.append("\n\n");
                }
            }

            @Override
            public void onUiFailure(int code, String msg) {
                diagnoseResultSB.append("APP-Server 异常: " + code + " " + msg);
                diagnoseResultSB.append("\n\n");

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
                    diagnoseResultSB.append("IM-Server api/version 正常\n");
                    diagnoseResultSB.append("remoteOriginUrl: " + json.getString("remoteOriginUrl"));
                    diagnoseResultSB.append("\n");
                    diagnoseResultSB.append("commitMessageShort: " + json.getString("commitMessageShort"));
                    diagnoseResultSB.append("\n");
                    diagnoseResultSB.append("commitTime: " + json.getString("commitTime"));
                    diagnoseResultSB.append("\n\n");
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }

                updateDiagnoseResult();
            }

            @Override
            public void onUiFailure(int code, String msg) {
                diagnoseResultSB.append("IM-Server api/version 异常: " + code + " " + msg);
                diagnoseResultSB.append("\n");

                updateDiagnoseResult();
            }
        });
    }

    private void tcping() {
        if (TextUtils.isEmpty(MyApp.longLinkHost)) {
            Toast.makeText(this, "长连接地址为空，route 请求可能已异常", Toast.LENGTH_SHORT).show();
            return;
        }
        String host = MyApp.longLinkHost;
        int port = ChatManager.Instance().getLongLinkPort();
        ChatManager.Instance().getWorkHandler().post(() -> {
            try (Socket socket = new Socket(host, port)) {
                diagnoseResultSB.append("IM-Server 长连接 tcp ping 正常");
                diagnoseResultSB.append("\n\n");

                ChatManager.Instance().getMainHandler().post(this::updateDiagnoseResult);
            } catch (IOException e) {
                diagnoseResultSB.append("IM-Server 长连接 tcp ping 异常: " + e.getMessage());
                diagnoseResultSB.append("\n\n");
                ChatManager.Instance().getMainHandler().post(this::updateDiagnoseResult);
            }
        });
    }
}