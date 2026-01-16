package cn.wildfirechat.backup;

import android.graphics.Bitmap;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cn.wildfirechat.message.MessageContent;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.remote.ChatManager;

/**
 * 备份序列化工具类
 * 用于序列化和反序列化消息数据
 */
public class BackupSerializer {

    /**
     * 生成会话目录名
     * 格式：conv_type{type}_{target}_{line}
     */
    public static String getConversationDirectoryName(Conversation conversation) {
        return String.format(BackupConstants.CONVERSATION_DIR_FORMAT,
                conversation.type.getValue(),
                conversation.target,
                conversation.line);
    }

    /**
     * 从目录名解析会话信息
     */
    public static ConversationInfo parseConversationDirectoryName(String dirName) {
        // 格式：conv_type{type}_{target}_{line}
        if (!dirName.startsWith("conv_type")) {
            return null;
        }

        try {
            String[] parts = dirName.split("_");
            if (parts.length < 4) {
                return null;
            }

            int type = Integer.parseInt(parts[1].replace("type", ""));
            String target = URLDecoder.decode(parts[2], StandardCharsets.UTF_8);
            int line = Integer.parseInt(parts[3].replace("line", ""));

            return new ConversationInfo(type, target, line);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 生成备份目录名（基于时间戳）
     */
    public static String generateBackupDirectoryName() {
        String timestamp = new java.text.SimpleDateFormat(BackupConstants.BACKUP_DIR_TIME_FORMAT)
                .format(new java.util.Date());
        return BackupConstants.BACKUP_DIR_PREFIX + timestamp;
    }

    /**
     * 生成备份时间字符串（ISO 8601 格式）
     */
    public static String generateBackupTimeString() {
        return new java.text.SimpleDateFormat(BackupConstants.TIMESTAMP_FORMAT)
                .format(new java.util.Date());
    }

    /**
     * 读取 JSON 文件
     */
    public static JSONObject readJSONFile(File file) throws IOException, JSONException {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(file), StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
        return new JSONObject(sb.toString());
    }

    /**
     * 写入 JSON 文件
     */
    public static void writeJSONFile(File file, JSONObject json) throws IOException, JSONException {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(file), StandardCharsets.UTF_8));
            writer.write(json.toString(2)); // 缩进 2 个空格
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }

    /**
     * 编码消息为 BackupMessage
     */
    public static BackupMessage encodeMessage(cn.wildfirechat.message.Message message,
                                              boolean includeMedia,
                                              File mediaDir) throws Exception {
        BackupMessage backupMessage = new BackupMessage();

        // 基本信息
        backupMessage.setMessageUid(message.messageUid);
        backupMessage.setFromUser(message.sender);
        backupMessage.setToUsers(message.toUsers);
        backupMessage.setDirection(message.direction != null ? message.direction.value() : 0);
        backupMessage.setStatus(message.status != null ? message.status.value() : 0);
        backupMessage.setTimestamp(message.serverTime);
        backupMessage.setLocalExtra(message.localExtra != null ? message.localExtra : "");

        // 编码内容
        BackupMessage.BackupMessagePayload payload = BackupMessage.BackupMessagePayload.fromMessagePayload(message.content.encode());
        MessageContent content = message.content;

        // 处理媒体文件
        // TODO: 需要根据实际的 MediaMessageContent 实现来获取本地路径
        if (includeMedia && content instanceof cn.wildfirechat.message.MediaMessageContent) {
            cn.wildfirechat.message.MediaMessageContent mediaMessage =
                    (cn.wildfirechat.message.MediaMessageContent) content;
            String localPath = mediaMessage.localPath;

            if (localPath != null && !localPath.isEmpty()) {
                File localFile = new File(localPath);
                if (localFile.exists()) {
                    // 计算 MD5 并生成文件名
                    String md5 = BackupCrypto.calculateMD5ForFile(localFile);
                    String fileId = md5.substring(0, Math.min(16, md5.length()));
                    String extension = getFileExtension(localPath);
                    String fileName = String.format("%s%s.%s",
                            BackupConstants.MEDIA_FILE_PREFIX, fileId, extension);
                    long fileSize = localFile.length();

                    // 复制文件到 media 目录
                    if (mediaDir != null && mediaDir.exists()) {
                        File targetFile = new File(mediaDir, fileName);
                        if (!targetFile.exists()) {
                            copyFile(localFile, targetFile);
                        }

                        // 保存媒体信息
                        BackupMediaInfo mediaInfo = new BackupMediaInfo(fileName, fileId, fileSize, md5);
                        payload.setLocalMediaInfo(mediaInfo);
                        backupMessage.setMediaFileSize(fileSize);
                    }
                }
            }
        }

        backupMessage.setPayload(payload);
        return backupMessage;
    }

    /**
     * 解码 BackupMessage 为 Message
     */
    public static cn.wildfirechat.message.Message decodeMessage(BackupMessage backupMessage,
                                                                 Conversation conversation,
                                                                 File mediaDir) throws Exception {
        cn.wildfirechat.message.Message message = new cn.wildfirechat.message.Message();

        message.messageUid = backupMessage.getMessageUid();
        message.sender = backupMessage.getFromUser();
        message.toUsers = backupMessage.getToUsers();
        message.direction = cn.wildfirechat.message.core.MessageDirection.direction(backupMessage.getDirection());
        message.status = cn.wildfirechat.message.core.MessageStatus.status(backupMessage.getStatus());
        message.serverTime = backupMessage.getTimestamp();
        message.localExtra = backupMessage.getLocalExtra();
        message.conversation = conversation;

        // 解码内容
        MessagePayload payload = backupMessage.getPayload().toMessagePayload();
        // TODO: 需要根据实际项目实现消息内容的解码
        MessageContent content = ChatManager.Instance().messageContentFromPayload(payload, message.sender);

        if (content != null) {
            // 恢复媒体文件
            if (content instanceof cn.wildfirechat.message.MediaMessageContent &&
                    backupMessage.getPayload().getLocalMediaInfo() != null && mediaDir != null) {
                BackupMediaInfo mediaInfo = backupMessage.getPayload().getLocalMediaInfo();
                File backupMediaFile = new File(mediaDir, mediaInfo.getRelativePath());

                if (backupMediaFile.exists()) {
                    // 复制到应用的媒体目录
                    String appMediaDir = getSendboxDirectory();
                    if (appMediaDir != null) {
                        File appMediaFile = new File(appMediaDir,
                                mediaInfo.getFileId() + "." + getFileExtension(mediaInfo.getRelativePath()));
                        copyFile(backupMediaFile, appMediaFile);

                        // 设置 localPath
                        ((cn.wildfirechat.message.MediaMessageContent) content).localPath = appMediaFile.getAbsolutePath();
                    }
                }
            }

            message.content = content;
        }

        return message;
    }

    /**
     * 获取文件扩展名
     */
    private static String getFileExtension(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return "";
        }
        int lastDot = filePath.lastIndexOf('.');
        if (lastDot > 0 && lastDot < filePath.length() - 1) {
            return filePath.substring(lastDot + 1).toLowerCase();
        }
        return "";
    }

    /**
     * 复制文件
     */
    private static void copyFile(File source, File target) throws IOException {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            fis = new FileInputStream(source);
            fos = new FileOutputStream(target);
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }

    /**
     * 获取应用的 sendbox 目录
     */
    private static String getSendboxDirectory() {
        // 这里需要根据实际的文件路径配置
        // 通常在 ChatManager 或其他配置类中定义
        return null; // 需要根据实际项目配置
    }

    /**
     * 会话信息
     */
    public static class ConversationInfo {
        public int type;
        public String target;
        public int line;

        public ConversationInfo(int type, String target, int line) {
            this.type = type;
            this.target = target;
            this.line = line;
        }
    }

    private BackupSerializer() {
        // 私有构造函数，防止实例化
    }
}
