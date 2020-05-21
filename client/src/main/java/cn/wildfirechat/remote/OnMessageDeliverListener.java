package cn.wildfirechat.remote;

import java.util.Map;

/**
 * 消息已送达
 */
public interface OnMessageDeliverListener {
    /**
     * @param deliveries 消息送达情况，key表示用户，value表示该用户已收到那个时间点的消息，当消息的serverTime小于这个值时，表示消息已收到
     */
    void onMessageDelivered(Map<String, Long> deliveries);
}
