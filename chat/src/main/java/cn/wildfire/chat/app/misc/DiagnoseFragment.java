/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.misc;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.Socket;
import java.util.Date;

import cn.wildfire.chat.app.AppService;
import cn.wildfire.chat.app.MyApp;
import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.WfcWebViewActivity;
import cn.wildfire.chat.kit.net.OKHttpHelper;
import cn.wildfire.chat.kit.net.SimpleCallback;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.remote.ChatManager;

/**
 * 连接诊断页。手机端装在 {@link DiagnoseActivity} 这个空壳里，平板上同一份实现进右栏。
 */
public class DiagnoseFragment extends Fragment {

    private TextView configInfoTextView;
    private TextView diagnoseResultTextView;

    private StringBuffer diagnoseResultSB;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_diagnose, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        configInfoTextView = view.findViewById(R.id.configInfoTextView);
        diagnoseResultTextView = view.findViewById(R.id.resultTextView);
        view.findViewById(R.id.startDiagnoseButton).setOnClickListener(v -> diagnose());
        view.findViewById(R.id.startTurnDiagnoseButton).setOnClickListener(v -> webrtcDiagnose());
        updateConfigInfo();
    }

    private void diagnose() {
        Toast.makeText(getActivity(), R.string.diagnose_start, Toast.LENGTH_SHORT).show();
        diagnoseResultSB = new StringBuffer();
        checkAppServer();
        checkApiVersion();
        tcping();
    }

    private void webrtcDiagnose() {
        String url = "https://static.wildfirechat.cn/webrtc/index.html";
        if (Config.ICE_SERVERS.length > 0 && Config.ICE_SERVERS[0].length > 0) {
            url += "?host=" + Config.ICE_SERVERS[0][0].replace("turn:", "");
            url += "&username=" + Config.ICE_SERVERS[0][1];
            url += "&secret=" + Config.ICE_SERVERS[0][2];
            WfcWebViewActivity.loadUrl(this, "WEBRTC 测试", url);
        } else {
            Toast.makeText(getActivity(), "本功能用于测试 TURN Server，但项目未配置", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateConfigInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append(getString(R.string.diagnose_current_time, new Date().toString())).append("\n");
        sb.append(getString(R.string.diagnose_app_server, AppService.Instance().appServerAddress())).append("\n");
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
        // 三项检查都是异步回的，页面可能已经关掉了
        if (getView() == null) {
            return;
        }
        diagnoseResultTextView.setText(diagnoseResultSB.toString());
    }

    private void checkAppServer() {
        OKHttpHelper.get(AppService.Instance().appServerAddress(), null, new SimpleCallback<String>() {
            @Override
            public void onUiSuccess(String s) {
                if (!isAdded()) {
                    return;
                }
                if ("Ok".equals(s)) {
                    diagnoseResultSB.append(getString(R.string.diagnose_app_server_ok)).append("\n\n");
                    updateDiagnoseResult();
                } else {
                    diagnoseResultSB.append(getString(R.string.diagnose_app_server_error, s)).append("\n\n");
                }
            }

            @Override
            public void onUiFailure(int code, String msg) {
                if (!isAdded()) {
                    return;
                }
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
                if (!isAdded()) {
                    return;
                }
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
                if (!isAdded()) {
                    return;
                }
                diagnoseResultSB.append(getString(R.string.diagnose_im_server_error, code, msg)).append("\n");
                updateDiagnoseResult();
            }
        });
    }

    private void tcping() {
        if (TextUtils.isEmpty(MyApp.longLinkHost)) {
            Toast.makeText(getActivity(), R.string.diagnose_longlink_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        String host = MyApp.longLinkHost;
        int port = ChatManager.Instance().getLongLinkPort();
        ChatManager.Instance().getWorkHandler().post(() -> {
            if (!isAdded()) {
                return;
            }
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
