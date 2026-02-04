/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.setting.backup;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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
        // 创建临时恢复目录
        if (!tempRestoreDir.exists()) {
            tempRestoreDir.mkdirs();
        }

        updateStatus(getString(R.string.downloading_backup_info), getString(R.string.connecting_to_pc_status));

        // 使用用户ID作为密码
        String userId = ChatManagerHolder.gChatManager.getUserId();

        BackupManager.getInstance().downloadAndRestoreBackup(
                backupPath,
                serverIP,
                serverPort,
                tempRestoreDir,
                userId,
                true,
                new BackupManager.DownloadAndRestoreCallback() {
                    @Override
                    public void onDownloadProgress(int downloadedFiles, int totalFiles) {
                        runOnUiThread(() -> {
                            int progress = (int) ((downloadedFiles * 100.0) / totalFiles);
                            updateProgress(progress, totalFiles, getString(R.string.downloading_file_progress, downloadedFiles, totalFiles));
                        });
                    }

                    @Override
                    public void onRestoreProgress(BackupProgress progress) {
                        runOnUiThread(() -> {
                            updateStatus(getString(R.string.restoring_messages), getString(R.string.please_wait));
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
                    public void onError(int errorCode, String message) {
                        runOnUiThread(() -> {
                            if (message != null) {
                                showError(getString(R.string.failed_to_download_metadata, message));
                            } else {
                                String errorMsg = cn.wildfirechat.backup.BackupConstants.getErrorMessage(errorCode);
                                showError(getString(R.string.restore_failed_prefix, errorMsg));
                            }
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
