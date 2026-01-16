/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.setting;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;

import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.backup.BackupConstants;
import cn.wildfirechat.backup.BackupManager;
import cn.wildfirechat.backup.BackupProgress;
import cn.wildfirechat.model.ConversationInfo;

/**
 * 备份进度显示界面
 */
public class BackupProgressActivity extends WfcBaseActivity {

    private ProgressBar progressBar;
    private TextView percentageTextView;
    private TextView statusTextView;
    private TextView detailTextView;
    private TextView cancelButton;
    private TextView closeButton;

    private BackupManager backupManager;
    private ArrayList<ConversationInfo> conversations;
    private boolean includeMedia;
    private String password;
    private String passwordHint;
    private boolean isBackupMode = true; // true=备份, false=恢复
    private String backupPath; // For restore mode

    public static void startForBackup(Context context,
                                      ArrayList<ConversationInfo> conversations,
                                      boolean includeMedia,
                                      String password,
                                      String passwordHint) {
        Intent intent = new Intent(context, BackupProgressActivity.class);
        intent.putParcelableArrayListExtra("conversations", conversations);
        intent.putExtra("includeMedia", includeMedia);
        intent.putExtra("password", password);
        intent.putExtra("passwordHint", passwordHint);
        intent.putExtra("isBackup", true);
        context.startActivity(intent);
    }

    public static void startForRestore(Context context, String backupPath, boolean includeMedia, String password) {
        Intent intent = new Intent(context, BackupProgressActivity.class);
        intent.putExtra("backupPath", backupPath);
        intent.putExtra("includeMedia", includeMedia);
        intent.putExtra("password", password);
        intent.putExtra("isBackup", false);
        context.startActivity(intent);
    }

    @Override
    protected int contentLayout() {
        return R.layout.activity_backup_progress;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        isBackupMode = intent.getBooleanExtra("isBackup", true);
        includeMedia = intent.getBooleanExtra("includeMedia", true);
        password = intent.getStringExtra("password");

        if (isBackupMode) {
            conversations = intent.getParcelableArrayListExtra("conversations");
            passwordHint = intent.getStringExtra("passwordHint");
        } else {
            backupPath = intent.getStringExtra("backupPath");
        }

        initView();
        startOperation();
    }

    private void initView() {
        progressBar = findViewById(R.id.progressBar);
        percentageTextView = findViewById(R.id.percentageTextView);
        statusTextView = findViewById(R.id.statusTextView);
        detailTextView = findViewById(R.id.detailTextView);
        cancelButton = findViewById(R.id.cancelButton);
        closeButton = findViewById(R.id.closeButton);

        if (isBackupMode) {
            statusTextView.setText(R.string.preparing_backup);
        } else {
            statusTextView.setText(R.string.preparing_restore);
        }

        cancelButton.setOnClickListener(v -> cancel());
        closeButton.setOnClickListener(v -> finish());
        closeButton.setVisibility(View.GONE);
    }

    private void startOperation() {
        if (isBackupMode) {
            startBackup();
        } else {
            startRestore();
        }
    }

    private void startBackup() {
        // 获取备份目录
        File backupDir = getBackupDirectory();
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }

        // 生成备份目录名
        String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new java.util.Date());
        File targetBackupDir = new File(backupDir, "backup_" + timestamp);

        backupManager = BackupManager.getInstance();
        backupManager.createDirectoryBasedBackup(
                targetBackupDir.getAbsolutePath(),
                conversations,
                TextUtils.isEmpty(password) ? null : password,
                passwordHint,
                new BackupManager.BackupCallback() {
                    @Override
                    public void onProgress(BackupProgress progress) {
                        updateProgress(progress);
                    }

                    @Override
                    public void onSuccess(String backupPath, int msgCount, int mediaCount, long mediaSize) {
                        onBackupSuccess(backupPath, msgCount, mediaCount, mediaSize);
                    }

                    @Override
                    public void onError(int errorCode) {
                        BackupProgressActivity.this.onError(errorCode);
                    }
                }
        );
    }

    private void startRestore() {
        if (backupPath == null) {
            Toast.makeText(this, R.string.backup_path_required, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        backupManager = BackupManager.getInstance();
        backupManager.restoreFromBackup(
                backupPath,
                password, // Use user ID as password (same as backup)
                true, // overwriteExisting (merge with existing data)
                new BackupManager.RestoreCallback() {
                    @Override
                    public void onProgress(BackupProgress progress) {
                        updateProgress(progress);
                    }

                    @Override
                    public void onSuccess(int msgCount, int mediaCount) {
                        onRestoreSuccess(msgCount, mediaCount);
                    }

                    @Override
                    public void onError(int errorCode) {
                        BackupProgressActivity.this.onError(errorCode);
                    }
                }
        );
    }

    private void updateProgress(BackupProgress progress) {
        runOnUiThread(() -> {
            int percentage = progress.getPercentage();
            progressBar.setProgress(percentage);
            percentageTextView.setText(percentage + "%");

            String phase = progress.getCurrentPhase();
            if (phase != null) {
                detailTextView.setText(phase);
            }

            long completed = progress.getCompletedUnitCount();
            long total = progress.getTotalUnitCount();
            if (total > 0) {
                detailTextView.setText(getString(R.string.conversations_progress, completed, total));
            }
        });
    }

    private void onBackupSuccess(String backupPath, int msgCount, int mediaCount, long mediaSize) {
        runOnUiThread(() -> {
            statusTextView.setText(R.string.backup_completed);
            progressBar.setProgress(100);
            percentageTextView.setText("100%");

            String mediaInfo = includeMedia ? getString(R.string.media_files_info, mediaCount, mediaSize / (1024.0 * 1024.0)) : "";

            detailTextView.setText(getString(R.string.backed_up_messages, msgCount, mediaInfo));

            cancelButton.setVisibility(View.GONE);
            closeButton.setVisibility(View.VISIBLE);

            Toast.makeText(BackupProgressActivity.this,
                    R.string.backup_completed_successfully, Toast.LENGTH_SHORT).show();
        });
    }

    private void onRestoreSuccess(int msgCount, int mediaCount) {
        runOnUiThread(() -> {
            statusTextView.setText(R.string.restore_completed);
            progressBar.setProgress(100);
            percentageTextView.setText("100%");

            String mediaInfo = includeMedia ? getString(R.string.media_files_count_only, mediaCount) : "";
            detailTextView.setText(getString(R.string.restored_messages, msgCount, mediaInfo));

            cancelButton.setVisibility(View.GONE);
            closeButton.setVisibility(View.VISIBLE);

            Toast.makeText(BackupProgressActivity.this,
                    R.string.restore_completed_successfully, Toast.LENGTH_SHORT).show();
        });
    }

    private void onError(int errorCode) {
        runOnUiThread(() -> {
            String errorMsg = BackupConstants.getErrorMessage(errorCode);
            String operation = isBackupMode ? getString(R.string.backing_up) : getString(R.string.restoring);
            statusTextView.setText(operation + " " + getString(R.string.backup_failed));
            detailTextView.setText(getString(R.string.error_prefix) + errorMsg);

            cancelButton.setVisibility(View.GONE);
            closeButton.setVisibility(View.VISIBLE);

            Toast.makeText(BackupProgressActivity.this,
                    operation + " " + getString(R.string.backup_failed) + ": " + errorMsg, Toast.LENGTH_LONG).show();
        });
    }

    private void cancel() {
        if (backupManager != null) {
            backupManager.cancelCurrentOperation();
        }
        finish();
    }

    private File getBackupDirectory() {
        // 使用公共Documents目录（用户可以访问，应用卸载后保留）
        File documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        File dir = new File(documentsDir, "WildFireChatBackup");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }
}
