package cn.wildfirechat.backup;

/**
 * 备份恢复功能常量定义
 */
public class BackupConstants {

    // 版本和格式
    public static final String BACKUP_VERSION = "1";
    public static final String BACKUP_FORMAT = "directory";
    public static final String BACKUP_APP_TYPE = "android-chat";

    // 备份模式
    public static final String BACKUP_MODE_MESSAGE_ONLY = "message_only";
    public static final String BACKUP_MODE_MESSAGE_WITH_MEDIA = "message_with_media";

    // 加密算法
    public static final String ENCRYPTION_ALGORITHM = "AES-256-CBC";
    public static final String KEY_DERIVATION = "PBKDF2-SHA256";
    public static final int PBKDF2_ITERATIONS = 10000; // 降低迭代次数避免Android栈溢出
    public static final int SALT_LENGTH = 16;
    public static final int IV_LENGTH = 16;
    public static final int KEY_LENGTH = 256; // bits

    // 文件名和目录
    public static final String METADATA_FILE_NAME = "metadata.json";
    public static final String MESSAGES_FILE_NAME = "messages.json";
    public static final String MEDIA_DIR_NAME = "media";
    public static final String CONVERSATIONS_DIR_NAME = "conversations";
    public static final String MEDIA_FILE_PREFIX = "media_";

    // 批处理大小
    public static final int DEFAULT_MESSAGE_BATCH_SIZE = 100;

    // 缩略图压缩质量
    public static final float THUMBNAIL_COMPRESSION_QUALITY = 0.45f;

    // 会话目录名格式
    public static final String CONVERSATION_DIR_FORMAT = "conv_type%d_%s_line%d";

    // 时间戳格式
    public static final String TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    public static final String BACKUP_DIR_TIME_FORMAT = "yyyy-MM-dd_HH-mm-ss";

    // 备份目录名前缀
    public static final String BACKUP_DIR_PREFIX = "backup_";

    /**
     * 备份错误码
     */
    public static final int ERROR_NO_ERROR = 0;
    public static final int ERROR_FILE_NOT_FOUND = 1001;
    public static final int ERROR_INVALID_FORMAT = 1002;
    public static final int ERROR_IO_ERROR = 1003;
    public static final int ERROR_OUT_OF_SPACE = 1004;
    public static final int ERROR_CANCELLED = 1005;
    public static final int ERROR_ENCRYPTION_FAILED = 3001;
    public static final int ERROR_DECRYPTION_FAILED = 3002;
    public static final int ERROR_WRONG_PASSWORD = 3003;
    public static final int ERROR_INVALID_PASSWORD = 3004;
    public static final int ERROR_NOT_ENCRYPTED = 3006;
    public static final int ERROR_RESTORE_FAILED = 4001;

    /**
     * 获取错误消息
     */
    public static String getErrorMessage(int errorCode) {
        switch (errorCode) {
            case ERROR_NO_ERROR:
                return "Success";
            case ERROR_FILE_NOT_FOUND:
                return "Backup file not found";
            case ERROR_INVALID_FORMAT:
                return "Invalid backup format";
            case ERROR_IO_ERROR:
                return "IO error occurred";
            case ERROR_OUT_OF_SPACE:
                return "Not enough storage space";
            case ERROR_CANCELLED:
                return "Operation cancelled";
            case ERROR_ENCRYPTION_FAILED:
                return "Encryption failed";
            case ERROR_DECRYPTION_FAILED:
                return "Decryption failed";
            case ERROR_WRONG_PASSWORD:
                return "Wrong password";
            case ERROR_INVALID_PASSWORD:
                return "Invalid password";
            case ERROR_NOT_ENCRYPTED:
                return "Backup is not encrypted";
            case ERROR_RESTORE_FAILED:
                return "Restore failed";
            default:
                return "Unknown error";
        }
    }

    private BackupConstants() {
        // 私有构造函数，防止实例化
    }
}
