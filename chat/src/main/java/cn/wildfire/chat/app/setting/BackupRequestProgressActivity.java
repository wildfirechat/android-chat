/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.setting;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
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
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;

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
    private int uploadedFileCount = 0;
    private OkHttpClient uploadHttpClient;

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

        BackupManager backupManager = BackupManager.getInstance();
        backupManager.createDirectoryBasedBackup(
                tempDir.getAbsolutePath(),
                conversations,
                userId, // Use user ID as password
                null,
                new BackupManager.BackupCallback() {
                    @Override
                    public void onProgress(BackupProgress progress) {
                        updateProgress(progress);
                    }

                    @Override
                    public void onSuccess(String backupPath, int msgCount, int mediaCount, long mediaSize) {
                        uploadBackupToPC(new File(backupPath));
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

    private void updateProgress(BackupProgress progress) {
        runOnUiThread(() -> {
            int percentage = progress.getPercentage();
            statusTextView.setText(getString(R.string.backing_up_progress, percentage));
            detailTextView.setText(getString(R.string.completed_progress,
                    progress.getCompletedUnitCount(),
                    progress.getTotalUnitCount()));
        });
    }

    private void uploadBackupToPC(File backupPath) {
        runOnUiThread(() -> {
            statusTextView.setText(R.string.uploading_to_pc);
            detailTextView.setText(R.string.preparing_file_list);
        });

        List<File> files = getAllFiles(backupPath);

        if (files.isEmpty()) {
            showErrorMessage(getString(R.string.no_backup_files));
            return;
        }

        uploadedFileCount = files.size();

        runOnUiThread(() -> {
            detailTextView.setText(getString(R.string.files_waiting_upload, files.size()));
        });

        uploadFilesSequentially(files, 0, backupPath);
    }

    private void uploadFilesSequentially(List<File> files, int index, File basePath) {
        if (index >= files.size()) {
            onUploadSuccess();
            return;
        }

        File file = files.get(index);

        runOnUiThread(() -> {
            int progress = (int) ((index * 100) / files.size());
            statusTextView.setText(getString(R.string.uploading_progress, progress));
            detailTextView.setText(getString(R.string.file_progress, index + 1, files.size()));
        });

        String relativePath = file.getAbsolutePath().substring(basePath.getAbsolutePath().length());
        if (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }

        uploadFile(file, relativePath, new FileUploadCallback() {
            @Override
            public void onResult(boolean success) {
                if (success) {
                    uploadFilesSequentially(files, index + 1, basePath);
                } else {
                    runOnUiThread(() -> {
                        showErrorMessage(getString(R.string.failed_to_upload_file));
                    });
                }
            }
        });
    }

    private interface FileUploadCallback {
        void onResult(boolean success);
    }

    private void uploadFile(File file, String relativePath, FileUploadCallback callback) {
        new Thread(() -> {
            try {
                byte[] pathBytes = relativePath.getBytes(StandardCharsets.UTF_8);
                ByteBuffer buffer = ByteBuffer.allocate(4 + pathBytes.length + 8);
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                buffer.putInt(pathBytes.length);
                buffer.put(pathBytes);
                buffer.putLong(file.length());
                byte[] headerData = buffer.array();

                if (uploadHttpClient == null) {
                    uploadHttpClient = new OkHttpClient.Builder()
                            .retryOnConnectionFailure(true)
                            .build();
                }

                String urlString = String.format("http://%s:%d/backup", serverIP, serverPort);
                RequestBody requestBody = buildRequestBody(file, headerData);

                Request request = new Request.Builder()
                        .url(urlString)
                        .post(requestBody)
//                        .header("Connection", "close")
                        .build();

                Call call = uploadHttpClient.newCall(request);
                try (Response response = call.execute()) {
                    callback.onResult(response.isSuccessful());
                }
            } catch (Exception e) {
                e.printStackTrace();
                callback.onResult(false);
            }
        }).start();
    }

    @NonNull
    private static RequestBody buildRequestBody(File file, byte[] headerData) {
        long totalLength = headerData.length + file.length();

        RequestBody requestBody = new RequestBody() {
            @Override
            public MediaType contentType() {
                return MediaType.get("application/octet-stream");
            }

            @Override
            public long contentLength() {
                return totalLength;
            }

            @Override
            public void writeTo(BufferedSink sink) throws java.io.IOException {
                sink.write(headerData);
                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] bufferBytes = new byte[8192];
                    int read;
                    while ((read = fis.read(bufferBytes)) != -1) {
                        sink.write(bufferBytes, 0, read);
                    }
                }
            }
        };
        return requestBody;
    }

    private List<File> getAllFiles(File directory) {
        List<File> files = new ArrayList<>();
        if (directory != null && directory.exists() && directory.isDirectory()) {
            File[] fileArray = directory.listFiles();
            if (fileArray != null) {
                for (File file : fileArray) {
                    if (file.isFile()) {
                        files.add(file);
                    } else if (file.isDirectory()) {
                        files.addAll(getAllFiles(file));
                    }
                }
            }
        }
        return files;
    }

    private void onUploadSuccess() {
        runOnUiThread(() -> {
            statusTextView.setText(R.string.backup_completed);
            detailTextView.setText(R.string.notifying_pc);

            sendCompletionRequest();

            File tempDir = new File(getCacheDir(), "backup_upload");
            deleteDirectory(tempDir);

            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    finish();
                }
            }, 2000);
        });
    }

    private void sendCompletionRequest() {
        new Thread(() -> {
            try {
                ByteBuffer buffer = ByteBuffer.allocate(4);
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                buffer.putInt(uploadedFileCount);
                byte[] bodyData = buffer.array();

                String urlString = String.format("http://%s:%d/backup_complete", serverIP, serverPort);
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                connection.setRequestProperty("Content-Type", "application/octet-stream");
                connection.setRequestProperty("Content-Length", String.valueOf(bodyData.length));

                // 发送数据
                connection.getOutputStream().write(bodyData);
                connection.getOutputStream().flush();
                connection.getOutputStream().close();

                // 获取响应码（这一步会触发实际请求）
                int responseCode = connection.getResponseCode();

                if (responseCode == 200) {
                    android.util.Log.d("BackupRequest", "Successfully notified PC of completion");
                } else {
                    android.util.Log.e("BackupRequest", "Failed to notify PC, response code: " + responseCode);
                }

                connection.disconnect();
            } catch (Exception e) {
                android.util.Log.e("BackupRequest", "Failed to send completion request", e);
            }
        }).start();
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
