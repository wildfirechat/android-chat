package cn.wildfirechat.remote;

// TODO

import cn.wildfirechat.message.Message;

/**
 * /
 * 觉得该提供两套回调接口
 * <p>
 * 默认原始线程(可能是非ui线程)回调
 * <p>
 * Simple系列接口，所有方法都是在ui线程回调
 */

public class SimpleSendMessageListener implements OnSendMessageListener {


    @Override
    public void onSendSuccess(Message message) {
    }

    @Override
    public void onSendFailure(Message message, int errorCode) {

    }

    @Override
    public void onSendPrepared(Message message, long savedTime) {

    }

    @Override
    public void onProgress(Message message, long uploaded, long total) {

    }

    @Override
    public void onMediaUploaded(Message message, String remoteUrl) {

    }
}
