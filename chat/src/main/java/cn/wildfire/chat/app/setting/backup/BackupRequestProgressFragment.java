package cn.wildfire.chat.app.setting.backup;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import java.io.File;
import java.util.List;

import cn.wildfire.chat.kit.ChatManagerHolder;
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
 * 备份请求进度界面 Fragment
 */
public class BackupRequestProgressFragment extends Fragment {

    private ProgressBar progressBar;
    private TextView statusTextView;
    private TextView detailTextView;
    private TextView closeButton;

    private List<ConversationInfo> conversations;
    private boolean includeMedia;
    private boolean isWaitingForResponse = true;
    private Handler timeoutHandler;
    private Runnable timeoutRunnable;

    private String serverIP;
    private int serverPort;

    // 消息监听
    private OnReceiveMessageListener messageListener;
    private PickBackupConversationViewModel pickBackupConversationViewModel;
    private OnBackPressedCallback onBackPressedCallback;
    private boolean isFinished = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_backup_request_progress, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        pickBackupConversationViewModel = new ViewModelProvider(requireActivity()).get(PickBackupConversationViewModel.class);
        conversations = pickBackupConversationViewModel.getSelectedConversations();
        includeMedia = pickBackupConversationViewModel.isIncludeMedia();

        if (conversations == null || conversations.isEmpty()) {
            Toast.makeText(requireContext(), R.string.no_conversations_to_backup, Toast.LENGTH_SHORT).show();
            getParentFragmentManager().popBackStack();
            return;
        }

        initView(view);
        setupBackPressHandler();
        startBackupRequest();
    }

    private void initView(View view) {
        progressBar = view.findViewById(R.id.progressBar);
        statusTextView = view.findViewById(R.id.statusTextView);
        detailTextView = view.findViewById(R.id.detailTextView);
        closeButton = view.findViewById(R.id.closeButton);

        progressBar.setVisibility(View.VISIBLE);
        statusTextView.setText(R.string.waiting_for_pc_response);
        detailTextView.setText(R.string.confirm_backup_on_pc);

        closeButton.setOnClickListener(v -> popToRoot());

        timeoutHandler = new Handler(Looper.getMainLooper());
        timeoutRunnable = this::onTimeout;
    }

    private void setupBackPressHandler() {
        onBackPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isFinished) {
                    popToRoot();
                } else {
                    // Prevent back navigation during backup
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
            getParentFragmentManager().popBackStack();
        }
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
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
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
        if (!isAdded()) return;
        serverIP = response.getServerIP();
        serverPort = response.getServerPort();

        requireActivity().runOnUiThread(() -> {
            statusTextView.setText(R.string.pc_approved_backup);
            detailTextView.setText(getString(R.string.creating_backup_data, serverIP, serverPort));
        });

        // 开始创建备份并上传
        createAndUploadBackup();
    }

    private void onBackupRejected() {
        if (!isAdded()) return;
        isWaitingForResponse = false;
        requireActivity().runOnUiThread(() -> {
            isFinished = true;
            progressBar.setVisibility(View.GONE);
            statusTextView.setText(R.string.backup_request_rejected_title);
            detailTextView.setText(R.string.pc_rejected_request);
            closeButton.setVisibility(View.VISIBLE);
        });
    }

    private void onTimeout() {
        if (!isWaitingForResponse) {
            return;
        }
        if (!isAdded()) return;
        isWaitingForResponse = false;
        requireActivity().runOnUiThread(() -> {
            isFinished = true;
            progressBar.setVisibility(View.GONE);
            statusTextView.setText(R.string.request_timeout_title);
            detailTextView.setText(R.string.pc_not_respond_30s);
            closeButton.setVisibility(View.VISIBLE);
        });
    }

    private void createAndUploadBackup() {
        File tempDir = new File(requireContext().getCacheDir(), "backup_upload");
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
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            int percentage = progress.getPercentage();
                            statusTextView.setText(getString(R.string.backing_up_progress, percentage));
                            detailTextView.setText(getString(R.string.completed_progress,
                                    progress.getCompletedUnitCount(),
                                    progress.getTotalUnitCount()));
                        });
                    }

                    @Override
                    public void onUploadProgress(int uploadedFiles, int totalFiles) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            statusTextView.setText(R.string.uploading_to_pc);
                            int progress = (int) ((uploadedFiles * 100) / totalFiles);
                            statusTextView.setText(getString(R.string.uploading_progress, progress));
                            detailTextView.setText(getString(R.string.file_progress, uploadedFiles, totalFiles));
                        });
                    }

                    @Override
                    public void onSuccess() {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            isFinished = true;
                            statusTextView.setText(R.string.backup_completed);
//                            detailTextView.setText(R.string.notifying_pc);
                            detailTextView.setVisibility(View.GONE);
                            progressBar.setVisibility(View.GONE);
                            closeButton.setVisibility(View.VISIBLE);

                            File tempDir = new File(requireContext().getCacheDir(), "backup_upload");
                            deleteDirectory(tempDir);
                        });
                    }

                    @Override
                    public void onError(int errorCode) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            isFinished = true;
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
        if (!isAdded()) return;
        isWaitingForResponse = false;
        requireActivity().runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
            statusTextView.setText(R.string.operation_failed);
            detailTextView.setText(message);
            closeButton.setVisibility(View.VISIBLE);
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (messageListener != null) {
            ChatManager.Instance().removeOnReceiveMessageListener(messageListener);
        }

        if (timeoutHandler != null && timeoutRunnable != null) {
            timeoutHandler.removeCallbacks(timeoutRunnable);
        }

        File tempDir = new File(requireContext().getCacheDir(), "backup_upload");
        deleteDirectory(tempDir);
    }
}
