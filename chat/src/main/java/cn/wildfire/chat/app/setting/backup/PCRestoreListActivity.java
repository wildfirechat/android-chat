/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.setting.backup;

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

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.ChatManagerHolder;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfirechat.backup.BackupManager;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.notification.RestoreRequestNotificationContent;
import cn.wildfirechat.message.notification.RestoreResponseNotificationContent;
import cn.wildfirechat.model.Conversation;
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
    private List<BackupManager.PCBackupInfo> backupList;
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

        BackupManager.getInstance().fetchBackupListFromPC(serverIP, serverPort, new BackupManager.BackupListCallback() {
            @Override
            public void onSuccess(List<BackupManager.PCBackupInfo> backups) {
                runOnUiThread(() -> {
                    if (backups == null || backups.isEmpty()) {
                        showError(getString(R.string.no_backups_on_pc));
                        return;
                    }

                    backupList = backups;
                    adapter.setBackupList(backupList);
                    progressBar.setVisibility(View.GONE);
                    statusTextView.setVisibility(View.GONE);
                    backupListRecyclerView.setVisibility(View.VISIBLE);
                });
            }

            @Override
            public void onError(int errorCode, String message) {
                runOnUiThread(() -> showError(getString(R.string.failed_to_fetch_list, message)));
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
        private List<BackupManager.PCBackupInfo> backupList;

        public void setBackupList(List<BackupManager.PCBackupInfo> backupList) {
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
                BackupManager.PCBackupInfo info = backupList.get(position);
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

            void bind(BackupManager.PCBackupInfo info) {
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
}
