/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.setting.backup;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.ChatManagerHolder;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfirechat.backup.BackupManager;
import cn.wildfirechat.backup.BackupProgress;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.notification.BackupRequestNotificationContent;
import cn.wildfirechat.message.notification.BackupResponseNotificationContent;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.ConversationInfo;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.OnReceiveMessageListener;
import cn.wildfirechat.remote.SendMessageCallback;

/**
 * 备份请求进度界面
 * 等待PC端响应备份请求
 */
public class BackupRequestProgressActivity extends WfcBaseActivity {

    private ProgressBar progressBar;
    private TextView statusTextView;
    private TextView detailTextView;

    private ArrayList<ConversationInfo> conversations;
    private boolean includeMedia;
    private boolean isWaitingForResponse = true;
    private Handler timeoutHandler;
    private Runnable timeoutRunnable;

    private String serverIP;
    private int serverPort;

    // 消息监听
    private OnReceiveMessageListener messageListener;

    public static void start(Context context, ArrayList<ConversationInfo> conversations, boolean includeMedia) {
        Intent intent = new Intent(context, BackupRequestProgressActivity.class);
        intent.putParcelableArrayListExtra("conversations", conversations);
        intent.putExtra("includeMedia", includeMedia);
        context.startActivity(intent);
    }

    @Override
    protected int contentLayout() {
        return R.layout.activity_backup_request_progress;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        conversations = getIntent().getParcelableArrayListExtra("conversations");
        includeMedia = getIntent().getBooleanExtra("includeMedia", true);

        if (conversations == null || conversations.isEmpty()) {
            Toast.makeText(this, R.string.no_conversations_to_backup, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initView();
        startBackupRequest();
    }

    private void initView() {
        progressBar = findViewById(R.id.progressBar);
        statusTextView = findViewById(R.id.statusTextView);
        detailTextView = findViewById(R.id.detailTextView);

        progressBar.setVisibility(View.VISIBLE);
        statusTextView.setText(R.string.waiting_for_pc_response);
        detailTextView.setText(R.string.confirm_backup_on_pc);

        timeoutHandler = new Handler(Looper.getMainLooper());
        timeoutRunnable = new Runnable() {
            @Override
            public void run() {
                onTimeout();
            }
        };
    }

    private void startBackupRequest() {
        isWaitingForResponse = true;

        // 启动超时计时器（30秒）
        timeoutHandler.postDelayed(timeoutRunnable, 30000);

        int totalMessageCount = 0;
        for (ConversationInfo convInfo : conversations) {
            Conversation conversation = convInfo.conversation;
            int messageCount = ChatManager.Instance().getMessageCount(conversation);

            totalMessageCount += messageCount;
        }

        // 创建备份请求通知消息
        BackupRequestNotificationContent content = new BackupRequestNotificationContent(
                conversations.size(),
                totalMessageCount,
                includeMedia,
                System.currentTimeMillis()
        );

        // 创建一个给自己（PC端）的通知消息
        String currentUserId = ChatManagerHolder.gChatManager.getUserId();
        Conversation conversation = new Conversation();
        conversation.type = Conversation.ConversationType.Single;
        conversation.target = currentUserId;
        conversation.line = 0;

        // 创建Message对象
        Message msg = new Message();
        msg.conversation = conversation;
        msg.content = content;

        ChatManager.Instance().sendMessage(msg, 0, new SendMessageCallback() {
            @Override
            public void onSuccess(long messageUid, long timestamp) {
                // 备份请求已发送
            }

            @Override
            public void onFail(int errorCode) {
                runOnUiThread(() -> {
                    showErrorMessage(getString(R.string.failed_to_send_request, errorCode));
                });
            }

            @Override
            public void onPrepare(long messageId, long savedTime) {
            }
        });

        // 注册消息监听
        messageListener = new OnReceiveMessageListener() {
            @Override
            public void onReceiveMessage(List<Message> messages, boolean hasMore) {
                if (!isWaitingForResponse) {
                    return;
                }

                for (Message msg : messages) {
                    if (msg.content instanceof BackupResponseNotificationContent) {
                        BackupResponseNotificationContent response = (BackupResponseNotificationContent) msg.content;

                        timeoutHandler.removeCallbacks(timeoutRunnable);
                        isWaitingForResponse = false;

                        if (response.isApproved()) {
                            onBackupApproved(response);
                        } else {
                            onBackupRejected();
                        }
                        break;
                    }
                }
            }
        };

        ChatManager.Instance().addOnReceiveMessageListener(messageListener);
    }

    private void onBackupApproved(BackupResponseNotificationContent response) {
        serverIP = response.getServerIP();
        serverPort = response.getServerPort();

        statusTextView.setText(R.string.pc_approved_backup);
        detailTextView.setText(getString(R.string.creating_backup_data, serverIP, serverPort));

        // 开始创建备份并上传
        createAndUploadBackup();
    }

    private void onBackupRejected() {
        isWaitingForResponse = false;
        progressBar.setVisibility(View.GONE);
        statusTextView.setText(R.string.backup_request_rejected_title);
        detailTextView.setText(R.string.pc_rejected_request);

        ChatManager.Instance().getMainHandler().postDelayed(this::finish, 2000);
    }

    private void onTimeout() {
        if (!isWaitingForResponse) {
            return;
        }
        isWaitingForResponse = false;
        progressBar.setVisibility(View.GONE);
        statusTextView.setText(R.string.request_timeout_title);
        detailTextView.setText(R.string.pc_not_respond_30s);

        ChatManager.Instance().getMainHandler().postDelayed(this::finish, 2000);
    }

    private void createAndUploadBackup() {
        File tempDir = new File(getCacheDir(), "backup_upload");
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }

        // Use user ID as backup password
        String userId = ChatManagerHolder.gChatManager.getUserId();

        BackupManager.getInstance().createAndUploadBackup(
                tempDir.getAbsolutePath(),
                conversations,
                userId,
                null,
                serverIP,
                serverPort,
                new BackupManager.BackupAndUploadCallback() {
                    @Override
                    public void onBackupProgress(BackupProgress progress) {
                        runOnUiThread(() -> {
                            int percentage = progress.getPercentage();
                            statusTextView.setText(getString(R.string.backing_up_progress, percentage));
                            detailTextView.setText(getString(R.string.completed_progress,
                                    progress.getCompletedUnitCount(),
                                    progress.getTotalUnitCount()));
                        });
                    }

                    @Override
                    public void onUploadProgress(int uploadedFiles, int totalFiles) {
                        runOnUiThread(() -> {
                            statusTextView.setText(R.string.uploading_to_pc);
                            int progress = (int) ((uploadedFiles * 100) / totalFiles);
                            statusTextView.setText(getString(R.string.uploading_progress, progress));
                            detailTextView.setText(getString(R.string.file_progress, uploadedFiles, totalFiles));
                        });
                    }

                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> {
                            statusTextView.setText(R.string.backup_completed);
                            detailTextView.setText(R.string.notifying_pc);

                            File tempDir = new File(getCacheDir(), "backup_upload");
                            deleteDirectory(tempDir);

                            new Handler(Looper.getMainLooper()).postDelayed(() -> finish(), 2000);
                        });
                    }

                    @Override
                    public void onError(int errorCode) {
                        runOnUiThread(() -> {
                            showErrorMessage(getString(R.string.failed_to_create_backup, errorCode));
                        });
                    }
                }
        );
    }

    private void deleteDirectory(File directory) {
        if (directory != null && directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
    }

    private void showErrorMessage(String message) {
        isWaitingForResponse = false;
        progressBar.setVisibility(View.GONE);
        statusTextView.setText(R.string.operation_failed);
        detailTextView.setText(message);

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        }, 2000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (messageListener != null) {
            ChatManager.Instance().removeOnReceiveMessageListener(messageListener);
        }

        if (timeoutHandler != null && timeoutRunnable != null) {
            timeoutHandler.removeCallbacks(timeoutRunnable);
        }

        File tempDir = new File(getCacheDir(), "backup_upload");
        deleteDirectory(tempDir);
    }
}
