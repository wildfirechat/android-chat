/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.iot;

import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.afollestad.materialdialogs.MaterialDialog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfirechat.chat.R;

/**
 * 扫码配网：把一台还不知道 Wi-Fi 的 ESP32 设备接进网络。
 * <p>
 * 设备开机发现自己没有 Wi-Fi 配置时，会开一个自己的热点，并在屏幕上显示
 * {@code wildfirechat://espwifi/<热点名>?pwd=<密码>}。手机扫到它就进到这一页，
 * 剩下的事情都在这一页里做完：
 * <ol>
 * <li>连上设备热点（Android 10 以上由系统弹框确认，10 以下引导去系统设置手连）；</li>
 * <li>{@code GET http://192.168.4.1/wifi/scan} 取设备自己扫到的网络列表 ——
 * 列表是<b>设备</b>看到的，不是手机看到的：ESP32 只有 2.4G，手机能看到的 5G
 * 网络它连不上，配网最常见的失败就是给设备填了一个 5G 的密码；</li>
 * <li>{@code POST http://192.168.4.1/wifi/config} 把选中的网络发过去；</li>
 * <li>{@code GET http://192.168.4.1/wifi/status} 轮询到 connected 或 failed。
 * 设备这期间是 AP+STA 双模，所以失败了还能把原因告诉手机，而不是一试就失联。</li>
 * </ol>
 * <p>
 * 协议的另一半在 esp 工程的 {@code main/app_net.c}。
 * <p>
 * 请求都走 {@link Network#openConnection(URL)}，绑在设备热点这条网络上，
 * 不动进程默认网络——否则 IM 长连接会跟着一起被切到一个没有外网的热点上去。
 */
public class DeviceProvisionActivity extends WfcBaseActivity {
    private static final String DEVICE_BASE_URL = "http://192.168.4.1";

    private static final int JOIN_TIMEOUT_MS = 30 * 1000;
    private static final int HTTP_TIMEOUT_MS = 10 * 1000;
    /** 设备连一个新网络，DHCP 拿到地址通常几秒；给到一分钟是留给要重试的路由器。 */
    private static final int PROVISION_TIMEOUT_MS = 60 * 1000;
    private static final int POLL_INTERVAL_MS = 2000;

    private String apSsid;
    private String apPassword;

    private TextView statusTextView;
    private ProgressBar progressBar;
    private LinearLayout networkListLayout;
    private Button actionButton;

    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    /** Android 10 以上拿得到；10 以下是 null，走手机默认网络（那时默认网络就是设备热点）。 */
    private Network deviceNetwork;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private boolean destroyed;

    @Override
    protected int contentLayout() {
        return R.layout.device_provision_activity;
    }

    @Override
    protected void beforeViews() {
        apSsid = getIntent().getStringExtra("ssid");
        apPassword = getIntent().getStringExtra("pwd");
        if (TextUtils.isEmpty(apSsid)) {
            finish();
        }
    }

    @Override
    protected void bindViews() {
        super.bindViews();
        statusTextView = findViewById(R.id.statusTextView);
        progressBar = findViewById(R.id.progressBar);
        networkListLayout = findViewById(R.id.networkListLayout);
        actionButton = findViewById(R.id.actionButton);
    }

    @Override
    protected void afterViews() {
        setTitle(R.string.esp_provision_title);
        statusTextView.setText(getString(R.string.esp_provision_intro, apSsid));
        actionButton.setText(R.string.esp_provision_connect);
        actionButton.setOnClickListener(v -> joinDeviceHotspot());
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
        releaseDeviceNetwork();
        executor.shutdownNow();
        super.onDestroy();
    }

    // ==================== 第一步：连上设备热点 ====================

    private void joinDeviceHotspot() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            // Android 10 以下没有 WifiNetworkSpecifier，addNetwork 又已经废弃且要定位
            // 权限；与其为一个越来越少见的版本写一条会被系统悄悄拒绝的路径，不如把
            // 热点名和密码摆出来，让用户在系统设置里连——连完这一页的请求走默认网络，
            // 后面的步骤完全一样。
            showManualJoin();
            return;
        }

        busy(getString(R.string.esp_provision_joining));

        WifiNetworkSpecifier.Builder specifier = new WifiNetworkSpecifier.Builder().setSsid(apSsid);
        if (!TextUtils.isEmpty(apPassword)) {
            specifier.setWpa2Passphrase(apPassword);
        }
        NetworkRequest request = new NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            // 设备热点没有外网，不去掉这个能力，系统会认为这条网络没通过校验。
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .setNetworkSpecifier(specifier.build())
            .build();

        connectivityManager = getSystemService(ConnectivityManager.class);
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                deviceNetwork = network;
                handler.post(() -> loadNetworkList());
            }

            @Override
            public void onUnavailable() {
                handler.post(() -> failed(getString(R.string.esp_provision_unreachable, apSsid),
                    R.string.esp_provision_retry, v -> joinDeviceHotspot()));
            }

            @Override
            public void onLost(@NonNull Network network) {
                deviceNetwork = null;
            }
        };
        connectivityManager.requestNetwork(request, networkCallback, JOIN_TIMEOUT_MS);
    }

    private void showManualJoin() {
        new MaterialDialog.Builder(this)
            .title(R.string.esp_provision_title)
            .content(getString(R.string.esp_provision_join_manually, apSsid,
                TextUtils.isEmpty(apPassword) ? "-" : apPassword))
            .positiveText(R.string.esp_provision_open_settings)
            .onPositive((dialog, which) -> startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)))
            .negativeText(R.string.esp_provision_continue)
            .onNegative((dialog, which) -> loadNetworkList())
            .show();
    }

    private void releaseDeviceNetwork() {
        if (connectivityManager != null && networkCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (IllegalArgumentException ignored) {
                // 已经注销过
            }
            networkCallback = null;
        }
        deviceNetwork = null;
    }

    // ==================== 第二步：设备扫到的网络 ====================

    private void loadNetworkList() {
        busy(getString(R.string.esp_provision_joined));
        executor.execute(() -> {
            try {
                JSONObject result = new JSONObject(get("/wifi/scan"));
                JSONArray aps = result.optJSONArray("aps");
                List<Ap> list = new ArrayList<>();
                for (int i = 0; aps != null && i < aps.length(); i++) {
                    JSONObject ap = aps.getJSONObject(i);
                    list.add(new Ap(ap.optString("ssid"), ap.optInt("rssi"), ap.optInt("auth")));
                }
                handler.post(() -> showNetworkList(list));
            } catch (Exception e) {
                e.printStackTrace();
                handler.post(() -> failed(getString(R.string.esp_provision_unreachable, apSsid),
                    R.string.esp_provision_retry, v -> loadNetworkList()));
            }
        });
    }

    private void showNetworkList(List<Ap> aps) {
        if (destroyed) {
            return;
        }
        progressBar.setVisibility(View.GONE);
        statusTextView.setText(R.string.esp_provision_pick);
        networkListLayout.removeAllViews();
        networkListLayout.setVisibility(View.VISIBLE);

        for (Ap ap : aps) {
            // 带上信号强度：同名网络在办公室里不止一个，设备离哪个近是它自己才知道的事。
            addRow(getString(R.string.esp_provision_ap_row, ap.ssid, ap.rssi),
                v -> askPassword(ap.ssid, ap.open));
        }
        addRow(getString(R.string.esp_provision_manual), v -> askSsid());

        actionButton.setVisibility(View.VISIBLE);
        actionButton.setText(R.string.esp_provision_rescan);
        actionButton.setOnClickListener(v -> loadNetworkList());
    }

    private void addRow(String text, View.OnClickListener listener) {
        TextView row = new TextView(this);
        row.setText(text);
        row.setTextSize(16);
        row.setPadding(0, dp(14), 0, dp(14));
        row.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT));
        row.setOnClickListener(listener);
        networkListLayout.addView(row);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    private void askSsid() {
        new MaterialDialog.Builder(this)
            .title(R.string.esp_provision_manual)
            .input(getString(R.string.esp_provision_ssid_hint), "", false,
                (dialog, input) -> askPassword(input.toString().trim(), false))
            .show();
    }

    private void askPassword(String ssid, boolean open) {
        if (TextUtils.isEmpty(ssid)) {
            return;
        }
        if (open) {
            sendNetwork(ssid, "");
            return;
        }
        new MaterialDialog.Builder(this)
            .title(ssid)
            .input(getString(R.string.esp_provision_password_hint), "", true,
                (dialog, input) -> sendNetwork(ssid, input.toString()))
            .show();
    }

    // ==================== 第三步：发给设备，然后等它连上 ====================

    private void sendNetwork(String ssid, String password) {
        busy(getString(R.string.esp_provision_sending));
        networkListLayout.setVisibility(View.GONE);
        actionButton.setVisibility(View.GONE);

        executor.execute(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("ssid", ssid);
                body.put("password", password);
                post("/wifi/config", body.toString());
                handler.post(() -> {
                    busy(getString(R.string.esp_provision_waiting, ssid));
                    pollStatus(ssid, System.currentTimeMillis() + PROVISION_TIMEOUT_MS);
                });
            } catch (Exception e) {
                e.printStackTrace();
                handler.post(() -> failed(getString(R.string.esp_provision_unreachable, apSsid),
                    R.string.esp_provision_retry, v -> loadNetworkList()));
            }
        });
    }

    private void pollStatus(String ssid, long deadline) {
        if (destroyed) {
            return;
        }
        executor.execute(() -> {
            String state = null;
            int reason = 0;
            try {
                JSONObject result = new JSONObject(get("/wifi/status"));
                state = result.optString("state");
                reason = result.optInt("reason");
            } catch (Exception e) {
                // 设备切到目标网络的信道时，热点这一侧会短暂断一下，读不到状态是正常的：
                // 继续轮询，由 deadline 决定什么时候放弃。
                e.printStackTrace();
            }

            final String currentState = state;
            final int currentReason = reason;
            handler.post(() -> {
                if (destroyed) {
                    return;
                }
                if ("connected".equals(currentState)) {
                    done();
                } else if ("failed".equals(currentState)) {
                    failed(getString(R.string.esp_provision_failed, reasonText(currentReason)),
                        R.string.esp_provision_retry, v -> loadNetworkList());
                } else if (System.currentTimeMillis() > deadline) {
                    failed(getString(R.string.esp_provision_timeout),
                        R.string.esp_provision_retry, v -> loadNetworkList());
                } else {
                    handler.postDelayed(() -> pollStatus(ssid, deadline), POLL_INTERVAL_MS);
                }
            });
        });
    }

    /** esp_wifi_types.h 里的 disconnect reason，只翻译用户能据此行动的那几个。 */
    private String reasonText(int reason) {
        switch (reason) {
            case 201:
                return getString(R.string.esp_provision_reason_no_ap);
            case 2:
            case 15:
            case 202:
            case 204:
                return getString(R.string.esp_provision_reason_password);
            case 210:
            case 211:
                return getString(R.string.esp_provision_reason_auth);
            case 212:
                return getString(R.string.esp_provision_reason_weak);
            default:
                return getString(R.string.esp_provision_reason_other, reason);
        }
    }

    private void done() {
        progressBar.setVisibility(View.GONE);
        networkListLayout.setVisibility(View.GONE);
        statusTextView.setText(R.string.esp_provision_done);
        actionButton.setVisibility(View.VISIBLE);
        actionButton.setText(R.string.esp_provision_finish);
        actionButton.setOnClickListener(v -> finish());
        // 设备已经在目标网络上了，手机没有理由继续待在一个没有外网的热点上。
        releaseDeviceNetwork();
        Toast.makeText(this, R.string.esp_provision_done, Toast.LENGTH_LONG).show();
    }

    // ==================== HTTP ====================

    private void busy(String text) {
        if (destroyed) {
            return;
        }
        statusTextView.setText(text);
        progressBar.setVisibility(View.VISIBLE);
        networkListLayout.setVisibility(View.GONE);
        actionButton.setVisibility(View.GONE);
    }

    private void failed(String text, int buttonText, View.OnClickListener listener) {
        if (destroyed) {
            return;
        }
        progressBar.setVisibility(View.GONE);
        networkListLayout.setVisibility(View.GONE);
        statusTextView.setText(text);
        actionButton.setVisibility(View.VISIBLE);
        actionButton.setText(buttonText);
        actionButton.setOnClickListener(listener);
    }

    private HttpURLConnection open(String path) throws Exception {
        URL url = new URL(DEVICE_BASE_URL + path);
        HttpURLConnection connection = (HttpURLConnection)
            (deviceNetwork != null ? deviceNetwork.openConnection(url) : url.openConnection());
        connection.setConnectTimeout(HTTP_TIMEOUT_MS);
        connection.setReadTimeout(HTTP_TIMEOUT_MS);
        return connection;
    }

    private String get(String path) throws Exception {
        HttpURLConnection connection = open(path);
        try {
            return readBody(connection);
        } finally {
            connection.disconnect();
        }
    }

    private String post(String path, String json) throws Exception {
        HttpURLConnection connection = open(path);
        try {
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            byte[] body = json.getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(body.length);
            try (OutputStream out = connection.getOutputStream()) {
                out.write(body);
            }
            return readBody(connection);
        } finally {
            connection.disconnect();
        }
    }

    private String readBody(HttpURLConnection connection) throws Exception {
        int status = connection.getResponseCode();
        if (status != 200) {
            throw new IllegalStateException("HTTP " + status);
        }
        try (InputStream in = connection.getInputStream()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] chunk = new byte[1024];
            int read;
            while ((read = in.read(chunk)) > 0) {
                out.write(chunk, 0, read);
            }
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }

    private static class Ap {
        final String ssid;
        final int rssi;
        final boolean open;

        Ap(String ssid, int rssi, int auth) {
            this.ssid = ssid;
            this.rssi = rssi;
            // wifi_auth_mode_t: 0 是 WIFI_AUTH_OPEN，其它都要密码。
            this.open = auth == 0;
        }
    }

    /** 扫码入口：{@code wildfirechat://espwifi/<ssid>?pwd=<password>}。 */
    public static Intent buildIntent(@NonNull android.content.Context context, @NonNull String ssid,
                                     @Nullable String password) {
        return new Intent(context, DeviceProvisionActivity.class)
            .putExtra("ssid", ssid)
            .putExtra("pwd", password);
    }
}
