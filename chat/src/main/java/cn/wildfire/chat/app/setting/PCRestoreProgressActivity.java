/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.setting;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.ChatManagerHolder;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfirechat.backup.BackupManager;
import cn.wildfirechat.backup.BackupProgress;
import cn.wildfirechat.chat.R;

/**
 * PC端恢复进度界面
 */
public class PCRestoreProgressActivity extends WfcBaseActivity {

    private static final String EXTRA_BACKUP_PATH = "backup_path";
    private static final String EXTRA_SERVER_IP = "server_ip";
    private static final String EXTRA_SERVER_PORT = "server_port";

    private String backupPath;
    private String serverIP;
    private int serverPort;

    private ProgressBar progressBar;
    private TextView statusTextView;
    private TextView detailTextView;

    private File tempRestoreDir;
    private int downloadedFiles = 0;
    private int totalFiles = 0;

    public static void start(Context context, String backupPath, String serverIP, int serverPort) {
        Intent intent = new Intent(context, PCRestoreProgressActivity.class);
        intent.putExtra(EXTRA_BACKUP_PATH, backupPath);
        intent.putExtra(EXTRA_SERVER_IP, serverIP);
        intent.putExtra(EXTRA_SERVER_PORT, serverPort);
        context.startActivity(intent);
    }

    @Override
    protected int contentLayout() {
        return R.layout.activity_pc_restore_progress;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        backupPath = getIntent().getStringExtra(EXTRA_BACKUP_PATH);
        serverIP = getIntent().getStringExtra(EXTRA_SERVER_IP);
        serverPort = getIntent().getIntExtra(EXTRA_SERVER_PORT, 0);

        initViews();
        startRestore();
    }

    private void initViews() {
        progressBar = findViewById(R.id.progressBar);
        statusTextView = findViewById(R.id.statusTextView);
        detailTextView = findViewById(R.id.detailTextView);

        // 创建临时恢复目录
        File backupDir = new File(getExternalFilesDir(null), "WildFireChatBackup");
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }
        tempRestoreDir = new File(backupDir, "temp_restore");
        if (!tempRestoreDir.exists()) {
            tempRestoreDir.mkdirs();
        }
    }

    private void startRestore() {
        new Thread(this::downloadMetadata).start();
    }

    private void downloadMetadata() {
        updateStatus(getString(R.string.downloading_backup_info), getString(R.string.connecting_to_pc_status));

        new Thread(() -> {
            try {
                String encodedPath = java.net.URLEncoder.encode(backupPath, "UTF-8");
                String urlString = String.format("http://%s:%d/restore_metadata?path=%s",
                        serverIP, serverPort, encodedPath);
                android.util.Log.d("PCRestore", "Downloading metadata from: " + urlString);
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(30000);

                int responseCode = connection.getResponseCode();
                android.util.Log.d("PCRestore", "Response code: " + responseCode);

                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    parseMetadataAndDownloadFiles(response.toString());
                } else {
                    String errorMsg = getString(R.string.failed_to_download_metadata_status, responseCode);
                    android.util.Log.e("PCRestore", errorMsg);
                    showError(errorMsg);
                }

                connection.disconnect();
            } catch (Exception e) {
                String errorMsg = getString(R.string.failed_to_download_metadata, e.getMessage());
                android.util.Log.e("PCRestore", errorMsg, e);
                showError(errorMsg);
            }
        }).start();
    }

    private void parseMetadataAndDownloadFiles(String jsonString) {
        try {
            JSONObject metadataJson = new JSONObject(jsonString);

            // 保存metadata.json
            File metadataFile = new File(tempRestoreDir, "metadata.json");
            FileOutputStream fos = new FileOutputStream(metadataFile);
            fos.write(jsonString.getBytes());
            fos.close();

            // 显示备份信息
            JSONObject statistics = metadataJson.optJSONObject("statistics");
            int totalConversations = statistics != null ? statistics.optInt("totalConversations", 0) : 0;
            int totalMessages = statistics != null ? statistics.optInt("totalMessages", 0) : 0;
            int totalMediaFiles = statistics != null ? statistics.optInt("mediaFileCount", 0) : 0;

            runOnUiThread(() -> {
                detailTextView.setText(getString(R.string.backup_info_format,
                        totalConversations, totalMessages, totalMediaFiles));
            });

            // 获取文件列表
            List<String> filePaths = new ArrayList<>();
            JSONArray conversations = metadataJson.optJSONArray("conversations");
            if (conversations != null) {
                for (int i = 0; i < conversations.length(); i++) {
                    JSONObject conv = conversations.getJSONObject(i);
                    String convDir = conv.optString("directory");
                    filePaths.add("conversations/" + convDir + "/messages.json");
                }
            }

            totalFiles = filePaths.size();
            updateProgress(0, totalFiles, getString(R.string.downloading_backup_files));

            // 下载所有文件
            downloadFiles(filePaths, 0);

        } catch (Exception e) {
            showError(getString(R.string.failed_to_parse_list, e.getMessage()));
        }
    }

    private void downloadFiles(List<String> filePaths, int index) {
        if (index >= filePaths.size()) {
            // 所有文件下载完成，开始恢复
            startLocalRestore();
            return;
        }

        String filePath = filePaths.get(index);
        downloadFile(filePath, () -> {
            downloadedFiles++;
            int progress = (int) ((downloadedFiles * 100.0) / totalFiles);
            updateProgress(progress, totalFiles,
                    getString(R.string.downloading_file_progress, downloadedFiles, totalFiles));

            downloadFiles(filePaths, index + 1);
        });
    }

    private void downloadFile(String relativePath, Runnable onComplete) {
        new Thread(() -> {
            try {
                // 构造完整路径：backupPath/relativePath
                String fullPath = backupPath + "/" + relativePath;
                String encodedPath = java.net.URLEncoder.encode(fullPath, "UTF-8");
                String urlString = String.format("http://%s:%d/restore_file?path=%s",
                        serverIP, serverPort, encodedPath);
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(60000);
                connection.setReadTimeout(60000);

                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    // 保存文件
                    File destFile = new File(tempRestoreDir, relativePath);
                    destFile.getParentFile().mkdirs();

                    FileOutputStream fos = new FileOutputStream(destFile);
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = connection.getInputStream().read(buffer)) != -1) {
                        fos.write(buffer, 0, len);
                    }
                    fos.close();

                    onComplete.run();
                } else {
                    String errorMsg = getString(R.string.failed_to_download_file_status, relativePath, responseCode);
                    android.util.Log.e("PCRestore", errorMsg);
                    showError(errorMsg);
                }

                connection.disconnect();
            } catch (Exception e) {
                String errorMsg = getString(R.string.failed_to_download_file, relativePath, e.getMessage());
                android.util.Log.e("PCRestore", errorMsg, e);
                showError(errorMsg);
            }
        }).start();
    }

    private void startLocalRestore() {
        runOnUiThread(() -> {
            updateStatus(getString(R.string.restoring_messages), getString(R.string.please_wait));
        });

        // 使用用户ID作为密码
        String userId = ChatManagerHolder.gChatManager.getUserId();

        BackupManager.getInstance().restoreFromBackup(
                tempRestoreDir.getAbsolutePath(),
                userId,
                true,
                new BackupManager.RestoreCallback() {
                    @Override
                    public void onProgress(BackupProgress progress) {
                        runOnUiThread(() -> {
                            int percentage = progress.getPercentage();
                            progressBar.setProgress(percentage);
                            statusTextView.setText(getString(R.string.restoring_progress, percentage));
                            detailTextView.setText(progress.getCurrentPhase());
                        });
                    }

                    @Override
                    public void onSuccess(int msgCount, int mediaCount) {
                        runOnUiThread(() -> {
                            updateStatus(getString(R.string.restore_completed_status),
                                    getString(R.string.restored_messages_media, msgCount, mediaCount));
                            progressBar.setProgress(100);

                            Toast.makeText(PCRestoreProgressActivity.this,
                                    R.string.restore_completed_successfully, Toast.LENGTH_SHORT).show();

                            // 清理临时目录
                            deleteDirectory(tempRestoreDir);

                            // 延迟后返回
                            android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
                            handler.postDelayed(() -> finish(), 2000);
                        });
                    }

                    @Override
                    public void onError(int errorCode) {
                        runOnUiThread(() -> {
                            String errorMsg = cn.wildfirechat.backup.BackupConstants.getErrorMessage(errorCode);
                            showError(getString(R.string.restore_failed_prefix, errorMsg));
                        });
                    }
                }
        );
    }

    private void updateStatus(String status, String detail) {
        runOnUiThread(() -> {
            statusTextView.setText(status);
            detailTextView.setText(detail);
        });
    }

    private void updateProgress(int progress, int total, String detail) {
        runOnUiThread(() -> {
            progressBar.setProgress(progress);
            detailTextView.setText(detail);
        });
    }

    private void showError(String message) {
        runOnUiThread(() -> {
            statusTextView.setText(R.string.restore_failed_status);
            detailTextView.setText(message);
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();

            // 清理临时目录
            deleteDirectory(tempRestoreDir);

            // 延迟后返回
            android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
            handler.postDelayed(() -> finish(), 3000);
        });
    }

    private boolean deleteDirectory(File directory) {
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
            return directory.delete();
        }
        return false;
    }
}
