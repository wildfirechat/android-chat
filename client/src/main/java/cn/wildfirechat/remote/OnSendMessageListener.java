package cn.wildfirechat.remote;

import cn.wildfirechat.message.Message;

/**
 * 目前这些回调在ui thread执行
 * <p>
 * 当消息为{@link cn.wildfirechat.message.core.PersistFlag#No_Persist}, 不进行通知
 */
public interface OnSendMessageListener {
    void onSendSuccess(Message message);

    void onSendFail(Message message, int errorCode);

    /**
     * 消息已插入本地数据库
     *
     * @param message
     * @param savedTime
     */
    void onSendPrepare(Message message, long savedTime);

    /**
     * 发送进度，media类型消息，且媒体大于100k时，才有进度回调
     *
     * @param message
     * @param uploaded
     * @param total
     */
    default void onProgress(Message message, long uploaded, long total) {
    }

    /**
     * media上传之后的url，media类型消息有效
     *
     * @param message
     * @param remoteUrl
     */
    default void onMediaUpload(Message message, String remoteUrl) {
    }
}
