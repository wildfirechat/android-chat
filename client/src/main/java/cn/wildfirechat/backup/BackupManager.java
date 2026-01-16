package cn.wildfirechat.backup;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import cn.wildfirechat.message.Message;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.ConversationInfo;
import cn.wildfirechat.remote.ChatManager;

/**
 * 消息备份管理器
 * 实现与 iOS 一致的备份和恢复功能
 */
public class BackupManager {
    private static final String TAG = "BackupManager";
    private static BackupManager instance;

    private AtomicBoolean isCancelled;
    private String currentBackupDirectory;
    private Handler mainHandler;

    private BackupManager() {
        this.isCancelled = new AtomicBoolean(false);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public static synchronized BackupManager getInstance() {
        if (instance == null) {
            instance = new BackupManager();
        }
        return instance;
    }

    /**
     * 备份回调接口
     */
    public interface BackupCallback {
        void onProgress(BackupProgress progress);
        void onSuccess(String backupPath, int msgCount, int mediaCount, long mediaSize);
        void onError(int errorCode);
    }

    /**
     * 恢复回调接口
     */
    public interface RestoreCallback {
        void onProgress(BackupProgress progress);
        void onSuccess(int msgCount, int mediaCount);
        void onError(int errorCode);
    }

    // ============================================
    // 备份功能
    // ============================================

    /**
     * 创建基于目录的备份
     * @param directoryPath 备份根目录路径
     * @param conversations 要备份的会话列表（null 表示备份所有会话）
     * @param password 密码（null 表示不加密）
     * @param passwordHint 密码提示
     * @param callback 回调
     */
    public void createDirectoryBasedBackup(final String directoryPath,
                                          final List<ConversationInfo> conversations,
                                          final String password,
                                          final String passwordHint,
                                          final BackupCallback callback) {
        // 在后台线程执行
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    performBackup(directoryPath, conversations, password, passwordHint, callback);
                } catch (Exception e) {
                    Log.e(TAG, "Backup failed", e);
                    notifyError(callback, BackupConstants.ERROR_IO_ERROR);
                }
            }
        }).start();
    }

    private void performBackup(String directoryPath, List<ConversationInfo> conversations,
                               String password, String passwordHint, BackupCallback callback)
            throws Exception {
        isCancelled.set(false);

        // 1. 创建备份目录
        File backupDir = new File(directoryPath);
        if (backupDir.exists()) {
            deleteDirectory(backupDir);
        }
        if (!backupDir.mkdirs()) {
            notifyError(callback, BackupConstants.ERROR_IO_ERROR);
            return;
        }
        currentBackupDirectory = directoryPath;

        // 2. 创建 conversations 子目录
        File conversationsDir = new File(backupDir, BackupConstants.CONVERSATIONS_DIR_NAME);
        if (!conversationsDir.mkdirs()) {
            notifyError(callback, BackupConstants.ERROR_IO_ERROR);
            return;
        }

        // 3. 获取要备份的会话列表
        List<ConversationInfo> backupConversations = conversations;
        if (backupConversations == null || backupConversations.isEmpty()) {
            // 获取所有会话
            backupConversations = ChatManager.Instance().getConversationList(Arrays.asList(Conversation.ConversationType.Single,
                    Conversation.ConversationType.Group,
                    Conversation.ConversationType.Channel), null);
        }

        if (backupConversations == null || backupConversations.isEmpty()) {
            notifyError(callback, BackupConstants.ERROR_INVALID_FORMAT);
            return;
        }

        // 4. 初始化统计信息
        final int totalConversations = backupConversations.size();
        BackupMetadata.BackupStatistics statistics = new BackupMetadata.BackupStatistics();
        statistics.totalConversations = totalConversations;
        statistics.firstMessageTime = Long.MAX_VALUE;
        statistics.lastMessageTime = 0;

        List<BackupConversationInfo> convMetadataList = new ArrayList<>();
        final BackupProgress progress = new BackupProgress(totalConversations);

        // 5. 遍历每个会话
        for (int i = 0; i < backupConversations.size(); i++) {
            if (isCancelled.get()) {
                cleanupIncompleteBackup();
                notifyError(callback, BackupConstants.ERROR_CANCELLED);
                return;
            }

            ConversationInfo convInfo = backupConversations.get(i);
            Conversation conversation = convInfo.conversation;

            // 5.1 创建会话目录和 media 子目录
            String convDirName = BackupSerializer.getConversationDirectoryName(conversation);
            File convDir = new File(conversationsDir, convDirName);
            File mediaDir = new File(convDir, BackupConstants.MEDIA_DIR_NAME);
            if (!mediaDir.mkdirs()) {
                Log.w(TAG, "Failed to create media directory for " + convDirName);
            }

            // 5.2 备份消息
            BackupConversationInfo backupConvInfo = backupConversation(
                    conversation, convDir, mediaDir, password, statistics);

            convMetadataList.add(backupConvInfo);

            // 5.3 更新进度
            progress.setCurrentPhase("Backing up " + (i + 1) + "/" + totalConversations);
            progress.increment();
            notifyProgress(callback, progress);
        }

        // 6. 创建 metadata.json
        BackupMetadata metadata = new BackupMetadata();
        metadata.setBackupTime(BackupSerializer.generateBackupTimeString());
        metadata.setUserId(ChatManager.Instance().getUserId());
        metadata.setDeviceName(android.os.Build.MODEL); // 设置设备名称
        metadata.setStatistics(statistics);
        metadata.setConversations(convMetadataList);

        if (password != null && !password.isEmpty()) {
            metadata.setEncryption(new BackupEncryptionInfo(true, passwordHint));
        }

        File metadataFile = new File(backupDir, BackupConstants.METADATA_FILE_NAME);
        BackupSerializer.writeJSONFile(metadataFile, metadata.toJSON());

        // 7. 完成
        currentBackupDirectory = null;
        notifySuccess(callback, directoryPath,
                statistics.totalMessages,
                statistics.mediaFileCount,
                statistics.mediaTotalSize);
    }

    /**
     * 备份单个会话
     */
    private BackupConversationInfo backupConversation(Conversation conversation,
                                                      File convDir,
                                                      File mediaDir,
                                                      String password,
                                                      BackupMetadata.BackupStatistics statistics)
            throws Exception {
        String convDirName = convDir.getName();
        BackupConversationInfo backupConvInfo = new BackupConversationInfo(conversation, convDirName);

        List<BackupMessage> messages = new ArrayList<>();
        int messageCount = 0;
        int mediaCount = 0;
        long mediaSize = 0;
        long firstTime = Long.MAX_VALUE;
        long lastTime = 0;

        // 分批获取消息
        long fromIndex = 0;
        while (true) {
            if (isCancelled.get()) {
                throw new Exception("Backup cancelled");
            }

            List<Message> batchMessages = ChatManager.Instance().getMessages(
                    conversation,
                    fromIndex,
                    false,
                    BackupConstants.DEFAULT_MESSAGE_BATCH_SIZE,
                    null
            );

            if (batchMessages == null || batchMessages.isEmpty()) {
                break;
            }

            // 编码每条消息
            for (Message msg : batchMessages) {
                if (isCancelled.get()) {
                    throw new Exception("Backup cancelled");
                }

                try {
                    BackupMessage backupMsg = BackupSerializer.encodeMessage(
                            msg, true, mediaDir);
                    messages.add(backupMsg);
                    messageCount++;

                    if (msg.serverTime > 0) {
                        firstTime = Math.min(firstTime, msg.serverTime);
                        lastTime = Math.max(lastTime, msg.serverTime);
                    }

                    if (backupMsg.getMediaFileSize() > 0) {
                        mediaCount++;
                        mediaSize += backupMsg.getMediaFileSize();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to encode message " + msg.messageUid, e);
                }
            }

            fromIndex = batchMessages.get(batchMessages.size() - 1).messageId;
        }

        // 保存 messages.json
        JSONArray messagesArray = new JSONArray();
        for (BackupMessage msg : messages) {
            messagesArray.put(msg.toJSON());
        }

        JSONObject messagesJson = new JSONObject();
        messagesJson.put("version", BackupConstants.BACKUP_VERSION);
        messagesJson.put("conversation", new JSONObject()
                .put("type", conversation.type.getValue())
                .put("target", conversation.target)
                .put("line", conversation.line));
        messagesJson.put("messages", messagesArray);

        File messagesFile = new File(convDir, BackupConstants.MESSAGES_FILE_NAME);
        BackupSerializer.writeJSONFile(messagesFile, messagesJson);

        // 加密 messages.json
        if (password != null && !password.isEmpty()) {
            byte[] fileData = readFileBytes(messagesFile);
            JSONObject encryptedData = BackupCrypto.encryptData(fileData, password);
            BackupSerializer.writeJSONFile(messagesFile, encryptedData);
        }

        // 更新统计信息
        backupConvInfo.setMessageCount(messageCount);
        backupConvInfo.setMediaCount(mediaCount);
        backupConvInfo.setFirstMessageTime(firstTime);
        backupConvInfo.setLastMessageTime(lastTime);

        statistics.totalMessages += messageCount;
        statistics.mediaFileCount += mediaCount;
        statistics.mediaTotalSize += mediaSize;
        statistics.firstMessageTime = Math.min(statistics.firstMessageTime, firstTime);
        statistics.lastMessageTime = Math.max(statistics.lastMessageTime, lastTime);

        return backupConvInfo;
    }

    // ============================================
    // 恢复功能
    // ============================================

    /**
     * 从备份恢复
     * @param directoryPath 备份目录路径
     * @param password 密码（如果备份已加密）
     * @param overwriteExisting 是否覆盖已存在的消息
     * @param callback 回调
     */
    public void restoreFromBackup(final String directoryPath,
                                  final String password,
                                  final boolean overwriteExisting,
                                  final RestoreCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    performRestore(directoryPath, password, overwriteExisting, callback);
                } catch (Exception e) {
                    Log.e(TAG, "Restore failed", e);
                    notifyError(callback, BackupConstants.ERROR_RESTORE_FAILED);
                }
            }
        }).start();
    }

    private void performRestore(String directoryPath, String password,
                               boolean overwriteExisting, RestoreCallback callback)
            throws Exception {
        isCancelled.set(false);

        // 1. 读取 metadata.json
        File backupDir = new File(directoryPath);
        File metadataFile = new File(backupDir, BackupConstants.METADATA_FILE_NAME);
        if (!metadataFile.exists()) {
            notifyError(callback, BackupConstants.ERROR_FILE_NOT_FOUND);
            return;
        }

        JSONObject metadataJson = BackupSerializer.readJSONFile(metadataFile);
        BackupMetadata metadata = BackupMetadata.fromJSON(metadataJson);

        // 2. 验证加密
        boolean isEncrypted = metadata.getEncryption() != null && metadata.getEncryption().isEnabled();
        if (isEncrypted && (password == null || password.isEmpty())) {
            notifyError(callback, BackupConstants.ERROR_INVALID_PASSWORD);
            return;
        }

        // 3. 获取会话列表
        List<BackupConversationInfo> conversations = metadata.getConversations();
        if (conversations == null || conversations.isEmpty()) {
            notifyError(callback, BackupConstants.ERROR_INVALID_FORMAT);
            return;
        }

        // 4. 恢复会话
        final BackupProgress progress = new BackupProgress(conversations.size());
        int totalMessages = 0;
        int totalMedia = 0;

        for (int i = 0; i < conversations.size(); i++) {
            if (isCancelled.get()) {
                notifyError(callback, BackupConstants.ERROR_CANCELLED);
                return;
            }

            BackupConversationInfo convInfo = conversations.get(i);
            File convDir = new File(backupDir,
                    BackupConstants.CONVERSATIONS_DIR_NAME + "/" + convInfo.getDirectory());

            if (!convDir.exists()) {
                Log.w(TAG, "Conversation directory not found: " + convInfo.getDirectory());
                continue;
            }

            // 4.1 读取并恢复消息
            int[] counts = restoreConversation(convDir, password, overwriteExisting, convInfo);
            totalMessages += counts[0];
            totalMedia += counts[1];

            // 4.2 更新进度
            progress.setCurrentPhase("Restoring " + (i + 1) + "/" + conversations.size());
            progress.increment();
            notifyProgress(callback, progress);
        }

        // 5. 完成
        notifySuccess(callback, totalMessages, totalMedia);
    }

    /**
     * 恢复单个会话
     */
    private int[] restoreConversation(File convDir, String password,
                                     boolean overwriteExisting,
                                     BackupConversationInfo convInfo)
            throws Exception {
        File messagesFile = new File(convDir, BackupConstants.MESSAGES_FILE_NAME);
        File mediaDir = new File(convDir, BackupConstants.MEDIA_DIR_NAME);

        if (!messagesFile.exists()) {
            return new int[]{0, 0};
        }

        // 读取 messages.json
        JSONObject messagesJson = BackupSerializer.readJSONFile(messagesFile);

        // 检查是否加密
        if (messagesJson.has("salt") && messagesJson.has("iv") && messagesJson.has("data")) {
            // 文件已加密，需要解密
            if (password == null || password.isEmpty()) {
                throw new Exception("Backup is encrypted but no password provided");
            }

            byte[] decryptedData = BackupCrypto.decryptData(messagesJson, password);
            String decryptedJson = new String(decryptedData);
            messagesJson = new JSONObject(decryptedJson);
        }

        JSONArray messagesArray = messagesJson.getJSONArray("messages");

        int messageCount = 0;
        int mediaCount = 0;

        // 构造会话对象
        Conversation conversation = new Conversation();
        conversation.type = convInfo.getType();
        conversation.target = convInfo.getTarget();
        conversation.line = convInfo.getLine();

        for (int i = 0; i < messagesArray.length(); i++) {
            if (isCancelled.get()) {
                throw new Exception("Restore cancelled");
            }

            try {
                JSONObject msgJson = messagesArray.getJSONObject(i);
                BackupMessage backupMsg = BackupMessage.fromJSON(msgJson);

                // 检查消息是否已存在
                if (!overwriteExisting) {
                    Message existing = ChatManager.Instance().getMessage(backupMsg.getMessageUid());
                    if (existing != null) {
                        continue;
                    }
                }

                // 解码并插入消息
                Message message = BackupSerializer.decodeMessage(backupMsg, conversation, mediaDir);
                ChatManager.Instance().insertMessage(message);

                messageCount++;
                if (backupMsg.getMediaFileSize() > 0) {
                    mediaCount++;
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to restore message", e);
            }
        }

        return new int[]{messageCount, mediaCount};
    }

    // ============================================
    // 工具方法
    // ============================================

    /**
     * 获取备份列表
     */
    public List<BackupMetadata> getBackupList(String parentDirectory) {
        List<BackupMetadata> backups = new ArrayList<>();
        File parentDir = new File(parentDirectory);

        if (!parentDir.exists() || !parentDir.isDirectory()) {
            return backups;
        }

        File[] dirs = parentDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith(BackupConstants.BACKUP_DIR_PREFIX);
            }
        });

        if (dirs == null) {
            return backups;
        }

        for (File dir : dirs) {
            try {
                File metadataFile = new File(dir, BackupConstants.METADATA_FILE_NAME);
                if (metadataFile.exists()) {
                    JSONObject json = BackupSerializer.readJSONFile(metadataFile);
                    BackupMetadata metadata = BackupMetadata.fromJSON(json);
                    metadata.setBackupDir(dir.getName());
                    backups.add(metadata);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to read backup metadata", e);
            }
        }

        // 按时间排序
        Collections.sort(backups, new Comparator<BackupMetadata>() {
            @Override
            public int compare(BackupMetadata o1, BackupMetadata o2) {
                return o2.getBackupTime().compareTo(o1.getBackupTime());
            }
        });

        return backups;
    }

    /**
     * 删除备份
     */
    public boolean deleteBackup(String backupPath) {
        File backupDir = new File(backupPath);
        return deleteDirectory(backupDir);
    }

    /**
     * 取消当前操作
     */
    public void cancelCurrentOperation() {
        isCancelled.set(true);
    }

    /**
     * 清理不完整的备份
     */
    private void cleanupIncompleteBackup() {
        if (currentBackupDirectory != null) {
            deleteDirectory(new File(currentBackupDirectory));
            currentBackupDirectory = null;
        }
    }

    /**
     * 递归删除目录
     */
    private boolean deleteDirectory(File dir) {
        if (dir == null || !dir.exists()) {
            return false;
        }

        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        return dir.delete();
    }

    /**
     * 读取文件字节数组
     */
    private byte[] readFileBytes(File file) throws Exception {
        java.io.FileInputStream fis = new java.io.FileInputStream(file);
        byte[] data = new byte[(int) file.length()];
        fis.read(data);
        fis.close();
        return data;
    }

    // ============================================
    // 回调通知方法
    // ============================================

    private void notifyProgress(final BackupCallback callback, final BackupProgress progress) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onProgress(progress);
            }
        });
    }

    private void notifyProgress(final RestoreCallback callback, final BackupProgress progress) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onProgress(progress);
            }
        });
    }

    private void notifySuccess(final BackupCallback callback, final String backupPath,
                              final int msgCount, final int mediaCount, final long mediaSize) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onSuccess(backupPath, msgCount, mediaCount, mediaSize);
            }
        });
    }

    private void notifySuccess(final RestoreCallback callback, final int msgCount, final int mediaCount) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onSuccess(msgCount, mediaCount);
            }
        });
    }

    private void notifyError(final BackupCallback callback, final int errorCode) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onError(errorCode);
            }
        });
    }

    private void notifyError(final RestoreCallback callback, final int errorCode) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onError(errorCode);
            }
        });
    }
}
