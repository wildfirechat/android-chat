package cn.wildfire.chat.app.setting.backup;

import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.activity.OnBackPressedCallback;
import androidx.lifecycle.ViewModelProvider;

import java.io.File;
import java.util.ArrayList;

import cn.wildfirechat.chat.R;
import cn.wildfirechat.backup.BackupConstants;
import cn.wildfirechat.backup.BackupManager;
import cn.wildfirechat.backup.BackupProgress;
import cn.wildfirechat.model.ConversationInfo;

/**
 * 备份进度显示界面 Fragment
 */
public class BackupProgressFragment extends Fragment {

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

    private PickBackupConversationViewModel pickBackupConversationViewModel;
    private OnBackPressedCallback onBackPressedCallback;
    private boolean isFinished = false;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_backup_progress, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        pickBackupConversationViewModel = new ViewModelProvider(requireActivity()).get(PickBackupConversationViewModel.class);

        conversations = (ArrayList<ConversationInfo>) pickBackupConversationViewModel.getSelectedConversations();
        includeMedia = pickBackupConversationViewModel.isIncludeMedia();
        // 密码逻辑可能需要调整，这里简化处理
        password = null;
        passwordHint = null;

        initView(view);
        setupBackPressHandler();
        startOperation();
    }

    private void initView(View view) {
        progressBar = view.findViewById(R.id.progressBar);
        percentageTextView = view.findViewById(R.id.percentageTextView);
        statusTextView = view.findViewById(R.id.statusTextView);
        detailTextView = view.findViewById(R.id.detailTextView);
        cancelButton = view.findViewById(R.id.cancelButton);
        closeButton = view.findViewById(R.id.closeButton);

        statusTextView.setText(R.string.preparing_backup);

        cancelButton.setOnClickListener(v -> cancel());
        closeButton.setOnClickListener(v -> popToRoot());
        closeButton.setVisibility(View.GONE);
    }

    private void setupBackPressHandler() {
        onBackPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isFinished) {
                    popToRoot();
                } else {
                    // Do nothing or show toast "Backing up..."
                }
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), onBackPressedCallback);
    }

    private void popToRoot() {
        if (getParentFragmentManager().getBackStackEntryCount() > 0) {
            int firstId = getParentFragmentManager().getBackStackEntryAt(0).getId();
            getParentFragmentManager().popBackStack(firstId, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        } else {
            // Fallback
            getParentFragmentManager().popBackStack();
        }
    }

    private void startOperation() {
        startBackup();
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
                        BackupProgressFragment.this.onError(errorCode);
                    }
                }
        );
    }

    private void updateProgress(BackupProgress progress) {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(() -> {
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
        if (!isAdded()) return;
        requireActivity().runOnUiThread(() -> {
            isFinished = true;
            statusTextView.setText(R.string.backup_completed);
            progressBar.setProgress(100);
            percentageTextView.setText("100%");

            String mediaInfo = includeMedia ? getString(R.string.media_files_info, mediaCount, mediaSize / (1024.0 * 1024.0)) : "";

            detailTextView.setText(getString(R.string.backed_up_messages, msgCount, mediaInfo));

            cancelButton.setVisibility(View.GONE);
            closeButton.setVisibility(View.VISIBLE);

            Toast.makeText(requireContext(),
                    R.string.backup_completed_successfully, Toast.LENGTH_SHORT).show();
        });
    }

    private void onError(int errorCode) {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(() -> {
            isFinished = true;
            String errorMsg = BackupConstants.getErrorMessage(errorCode);
            String operation = getString(R.string.backing_up) ;
            statusTextView.setText(operation + " " + getString(R.string.backup_failed));
            detailTextView.setText(getString(R.string.error_prefix) + errorMsg);

            cancelButton.setVisibility(View.GONE);
            closeButton.setVisibility(View.VISIBLE);

            Toast.makeText(requireContext(),
                    operation + " " + getString(R.string.backup_failed) + ": " + errorMsg, Toast.LENGTH_LONG).show();
        });
    }

    private void cancel() {
        if (backupManager != null) {
            backupManager.cancelCurrentOperation();
        }
        getParentFragmentManager().popBackStack();
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
