/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.archive.service;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import cn.wildfirechat.message.Message;

/**
 * 归档服务接口
 * <p>
 * 应用层需要实现此接口来提供从归档服务器加载历史消息的功能。
 * </p>
 */
public interface ArchiveService {

    /**
     * 获取归档消息
     *
     * @param conversationType 会话类型
     * @param convTarget       会话目标ID
     * @param startMid         起始消息ID（分页用）
     * @param before           true=获取比startMid旧的消息，false=获取比startMid新的消息
     * @param limit            获取消息数量限制（最大100）
     * @param callback         回调
     */
    void getArchivedMessages(int conversationType, String convTarget, long startMid,
                             boolean before, int limit, OnArchiveCallback<ArchiveMessageResult> callback);

    /**
     * 获取归档消息（带会话线路）
     *
     * @param conversationType 会话类型
     * @param convTarget       会话目标ID
     * @param convLine         会话线路
     * @param startMid         起始消息ID（分页用）
     * @param before           true=获取比startMid旧的消息，false=获取比startMid新的消息
     * @param limit            获取消息数量限制（最大100）
     * @param callback         回调
     */
    void getArchivedMessages(int conversationType, String convTarget, int convLine, long startMid,
                             boolean before, int limit, OnArchiveCallback<ArchiveMessageResult> callback);

    /**
     * 搜索归档消息
     *
     * @param keyword  搜索关键词
     * @param limit    获取消息数量限制（最大100）
     * @param callback 回调
     */
    void searchArchivedMessages(String keyword, int limit, OnArchiveCallback<ArchiveMessageResult> callback);

    /**
     * 搜索归档消息（带过滤条件）
     *
     * @param keyword          搜索关键词
     * @param conversationType 会话类型（可选，null表示不过滤）
     * @param convTarget       会话目标ID（可选，null表示不过滤）
     * @param convLine         会话线路（可选，null表示不过滤）
     * @param startMid         起始消息ID（分页用）
     * @param before           true=获取比startMid旧的消息，false=获取比startMid新的消息
     * @param limit            获取消息数量限制（最大100）
     * @param callback         回调
     */
    void searchArchivedMessages(String keyword, Integer conversationType, String convTarget,
                                Integer convLine, long startMid, boolean before, int limit,
                                OnArchiveCallback<ArchiveMessageResult> callback);

    /**
     * 归档回调接口
     *
     * @param <T> 结果数据类型
     */
    interface OnArchiveCallback<T> {
        /**
         * 成功回调
         *
         * @param result 结果数据
         */
        void onSuccess(T result);

        /**
         * 失败回调
         *
         * @param errorCode 错误码
         * @param message   错误信息
         */
        void onError(int errorCode, String message);
    }

    /**
     * 归档消息结果
     */
    class ArchiveMessageResult {
        /**
         * 消息列表（已转换为SDK的Message对象）
         */
        public List<Message> messages;

        /**
         * 是否还有更多消息
         */
        public boolean hasMore;

        /**
         * 下一页起始消息ID（用于翻页）
         */
        public long nextStartMid;

        public ArchiveMessageResult(List<Message> messages, boolean hasMore, long nextStartMid) {
            this.messages = messages;
            this.hasMore = hasMore;
            this.nextStartMid = nextStartMid;
        }

        /**
         * 从JSON对象创建结果对象
         *
         * @param json JSON对象
         * @return ArchiveMessageResult对象，解析失败返回null
         */
        public static ArchiveMessageResult fromJson(JSONObject json) {
            if (json == null) {
                return null;
            }

            ArchiveMessageResult result = new ArchiveMessageResult(new ArrayList<>(), false, 0);
            result.hasMore = json.optBoolean("hasMore", false);
            // 处理可能为null的情况
            if (json.isNull("nextStartMid")) {
                result.nextStartMid = 0;
            } else {
                result.nextStartMid = json.optLong("nextStartMid", 0);
            }

            // 解析消息列表
            JSONArray messagesArray = json.optJSONArray("messages");
            if (messagesArray != null) {
                for (int i = 0; i < messagesArray.length(); i++) {
                    JSONObject messageJson = messagesArray.optJSONObject(i);
                    if (messageJson != null) {
                        // 使用 ChatManager 解析消息
                        Message message = parseArchivedMessage(messageJson);
                        if (message != null) {
                            result.messages.add(message);
                        }
                    }
                }
            }

            return result;
        }

        /**
         * 解析归档消息JSON为Message对象
         * 注：此方法由实现类提供具体的解析逻辑
         */
        private static Message parseArchivedMessage(JSONObject messageJson) {
            // 这是一个占位方法，实际解析逻辑在实现类中处理
            // 因为解析需要依赖应用层的消息内容解析
            return null;
        }
    }
}
