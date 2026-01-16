/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.setting;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.ChatManagerHolder;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.MessageContent;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.message.notification.RestoreRequestNotificationContent;
import cn.wildfirechat.message.notification.RestoreResponseNotificationContent;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.OnReceiveMessageListener;
import cn.wildfirechat.remote.SendMessageCallback;

/**
 * PC端恢复列表界面
 */
public class PCRestoreListActivity extends WfcBaseActivity {

    private RecyclerView backupListRecyclerView;
    private TextView statusTextView;
    private ProgressBar progressBar;
    private PCBackupListAdapter adapter;
    private List<PCBackupInfo> backupList;
    private String serverIP;
    private int serverPort;
    private boolean isWaitingForResponse = true;
    private Handler timeoutHandler;

    public static void start(Context context) {
        Intent intent = new Intent(context, PCRestoreListActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected int contentLayout() {
        return R.layout.activity_pc_restore_list;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initViews();
        sendRestoreRequest();
    }

    private void initViews() {
        backupListRecyclerView = findViewById(R.id.backupListRecyclerView);
        statusTextView = findViewById(R.id.statusTextView);
        progressBar = findViewById(R.id.progressBar);

        backupListRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PCBackupListAdapter();
        backupListRecyclerView.setAdapter(adapter);

        backupList = new ArrayList<>();

        timeoutHandler = new Handler(Looper.getMainLooper());

        // 注册消息监听
        ChatManager.Instance().addOnReceiveMessageListener(messageListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ChatManager.Instance().removeOnReceiveMessageListener(messageListener);
        if (timeoutHandler != null) {
            timeoutHandler.removeCallbacksAndMessages(null);
        }
    }

    private final OnReceiveMessageListener messageListener = new OnReceiveMessageListener() {
        @Override
        public void onReceiveMessage(List<Message> messages, boolean hasMore) {
            for (Message msg : messages) {
                if (msg.content instanceof RestoreResponseNotificationContent) {
                    handleRestoreResponse((RestoreResponseNotificationContent) msg.content);
                }
            }
        }
    };

    private void sendRestoreRequest() {
        // 30秒超时
        timeoutHandler.postDelayed(this::onTimeout, 30000);

        // 发送恢复请求消息给自己（PC端会收到）
        String currentUserId = ChatManagerHolder.gChatManager.getUserId();
        Conversation conversation = new Conversation();
        conversation.type = Conversation.ConversationType.Single;
        conversation.target = currentUserId;
        conversation.line = 0;

        RestoreRequestNotificationContent content = new RestoreRequestNotificationContent();
        content.setTimestamp(System.currentTimeMillis());

        Message msg = new Message();
        msg.conversation = conversation;
        msg.content = content;

        ChatManager.Instance().sendMessage(msg, 0, new SendMessageCallback() {
            @Override
            public void onPrepare(long messageUid, long timestamp) {
                // 消息准备发送
            }

            @Override
            public void onSuccess(long messageUid, long timestamp) {
                // 请求已发送，等待响应
            }

            @Override
            public void onFail(int errorCode) {
                runOnUiThread(() -> showError(getString(R.string.failed_to_send_restore_request, errorCode)));
            }
        });
    }

    private void handleRestoreResponse(RestoreResponseNotificationContent response) {
        if (!isWaitingForResponse) {
            return;
        }

        timeoutHandler.removeCallbacksAndMessages(null);

        runOnUiThread(() -> {
            if (response.isApproved()) {
                serverIP = response.getServerIP();
                serverPort = response.getServerPort();
                fetchBackupList();
            } else {
                showError(getString(R.string.pc_rejected_restore_request));
            }
        });
    }

    private void fetchBackupList() {
        statusTextView.setText(R.string.fetching_backup_list);

        new Thread(() -> {
            try {
                String urlString = String.format("http://%s:%d/restore_list", serverIP, serverPort);
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(30000);

                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    parseBackupList(response.toString());
                } else {
                    showError(getString(R.string.failed_to_fetch_list_status, responseCode));
                }

                connection.disconnect();
            } catch (Exception e) {
                showError(getString(R.string.failed_to_fetch_list, e.getMessage()));
            }
        }).start();
    }

    private void parseBackupList(String jsonString) {
        runOnUiThread(() -> {
            try {
                JSONArray jsonArray = new JSONArray(jsonString);
                backupList = new ArrayList<>();

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject backupJson = jsonArray.getJSONObject(i);
                    PCBackupInfo info = new PCBackupInfo();
                    info.name = backupJson.optString("name", getString(R.string.unknown_backup));
                    info.time = backupJson.optString("time", "");
                    info.path = backupJson.optString("path", "");
                    info.fileCount = backupJson.optInt("fileCount", 0);
                    info.conversationCount = backupJson.optInt("conversationCount", 0);
                    info.messageCount = backupJson.optInt("messageCount", 0);
                    info.mediaFileCount = backupJson.optInt("mediaFileCount", 0);
                    backupList.add(info);
                }

                if (backupList.isEmpty()) {
                    showError(getString(R.string.no_backups_on_pc));
                    return;
                }

                // 显示列表
                adapter.setBackupList(backupList);
                progressBar.setVisibility(View.GONE);
                statusTextView.setVisibility(View.GONE);
                backupListRecyclerView.setVisibility(View.VISIBLE);

            } catch (JSONException e) {
                showError(getString(R.string.failed_to_parse_list));
            }
        });
    }

    private void showError(String message) {
        isWaitingForResponse = false;
        progressBar.setVisibility(View.GONE);
        statusTextView.setText(message);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

        // 2秒后返回
        timeoutHandler.postDelayed(() -> finish(), 2000);
    }

    private void onTimeout() {
        if (isWaitingForResponse) {
            showError(getString(R.string.pc_not_respond_30s));
        }
    }

    // Adapter for PC backup list
    private class PCBackupListAdapter extends RecyclerView.Adapter<PCBackupListAdapter.ViewHolder> {
        private List<PCBackupInfo> backupList;

        public void setBackupList(List<PCBackupInfo> backupList) {
            this.backupList = backupList;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_pc_backup, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            if (backupList != null && position < backupList.size()) {
                PCBackupInfo info = backupList.get(position);
                holder.bind(info);
            }
        }

        @Override
        public int getItemCount() {
            return backupList != null ? backupList.size() : 0;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            private TextView backupNameTextView;
            private TextView backupInfoTextView;

            ViewHolder(View itemView) {
                super(itemView);
                backupNameTextView = itemView.findViewById(R.id.backupNameTextView);
                backupInfoTextView = itemView.findViewById(R.id.backupInfoTextView);
            }

            void bind(PCBackupInfo info) {
                backupNameTextView.setText(info.name);

                StringBuilder infoText = new StringBuilder();
                infoText.append(info.time);

                if (info.conversationCount > 0) {
                    infoText.append(" • ").append(info.conversationCount).append(" ").append(itemView.getContext().getString(R.string.conversations_suffix));
                }
                if (info.messageCount > 0) {
                    infoText.append(", ").append(info.messageCount).append(" ").append(itemView.getContext().getString(R.string.messages_suffix_short));
                }
                if (info.mediaFileCount > 0) {
                    infoText.append(", ").append(info.mediaFileCount).append(" ").append(itemView.getContext().getString(R.string.media_files_suffix));
                }
                if (info.conversationCount == 0 && info.messageCount == 0 && info.mediaFileCount == 0 && info.fileCount > 0) {
                    infoText.append(" • ").append(info.fileCount).append(" ").append(itemView.getContext().getString(R.string.files_suffix));
                }

                backupInfoTextView.setText(infoText.toString());

                itemView.setOnClickListener(v -> {
                    PCRestoreProgressActivity.start(PCRestoreListActivity.this,
                            info.path, serverIP, serverPort);
                });
            }
        }
    }

    // PC backup info model
    private static class PCBackupInfo {
        String name;
        String time;
        String path;
        int fileCount;
        int conversationCount;
        int messageCount;
        int mediaFileCount;
    }
}
