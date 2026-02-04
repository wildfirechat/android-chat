/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.setting.backup;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.util.ArrayList;

import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfirechat.backup.BackupConstants;
import cn.wildfirechat.backup.BackupManager;
import cn.wildfirechat.backup.BackupProgress;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.model.ConversationInfo;

/**
 * 备份进度显示界面
 */
public class RestoreProgressActivity extends WfcBaseActivity {

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
    private boolean isBackupMode = false; // true=备份, false=恢复
    private String backupPath; // For restore mode

    public static void startForRestore(Context context, String backupPath, boolean includeMedia, String password) {
        Intent intent = new Intent(context, RestoreProgressActivity.class);
        intent.putExtra("backupPath", backupPath);
        intent.putExtra("includeMedia", includeMedia);
        intent.putExtra("password", password);
        intent.putExtra("isBackup", false);
        context.startActivity(intent);
    }

    @Override
    protected int contentLayout() {
        return R.layout.activity_restore_progress;
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
        startRestore();
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
                        RestoreProgressActivity.this.onError(errorCode);
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

    private void onRestoreSuccess(int msgCount, int mediaCount) {
        runOnUiThread(() -> {
            statusTextView.setText(R.string.restore_completed);
            progressBar.setProgress(100);
            percentageTextView.setText("100%");

            String mediaInfo = includeMedia ? getString(R.string.media_files_count_only, mediaCount) : "";
            detailTextView.setText(getString(R.string.restored_messages, msgCount, mediaInfo));

            cancelButton.setVisibility(View.GONE);
            closeButton.setVisibility(View.VISIBLE);

            Toast.makeText(RestoreProgressActivity.this,
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

            Toast.makeText(RestoreProgressActivity.this,
                    operation + " " + getString(R.string.backup_failed) + ": " + errorMsg, Toast.LENGTH_LONG).show();
        });
    }

    private void cancel() {
        if (backupManager != null) {
            backupManager.cancelCurrentOperation();
        }
        finish();
    }
}
