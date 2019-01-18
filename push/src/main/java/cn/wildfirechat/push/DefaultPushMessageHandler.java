package cn.wildfirechat.push;

import android.content.Context;

class DefaultPushMessageHandler extends PushMessageHandler {
    @Override
    public void handleIMPushMessage(Context context, AndroidPushMessage pushMessage, PushService.PushServiceType pushServiceType) {
        // do nothing， 透传消息的receiver都在主进程执行，当有透传消息时，会启动主进程，然后主进程主动去拉取消息，
        // 并处理
//        Intent intent = new Intent();
//        intent.setAction(PushConstant.PUSH_MESSAGE_ACTION);
//        intent.putExtra(PushConstant.PUSH_MESSAGE, pushMessage);
//        context.sendBroadcast(intent);
        //ChatManager.Instance().forceConnect();
    }

    //Application push data
    @Override
    public void handlePushMessageData(Context context, String pushData) {

    }
}
