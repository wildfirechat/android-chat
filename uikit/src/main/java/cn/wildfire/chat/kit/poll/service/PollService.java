/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.poll.service;

import java.util.List;

import cn.wildfire.chat.kit.poll.model.Poll;
import cn.wildfire.chat.kit.poll.model.PollVoterDetail;

/**
 * 投票服务接口
 * <p>
 * 应用层需要实现此接口来提供投票功能的网络请求。
 * </p>
 */
public interface PollService {

    /**
     * 创建投票
     *
     * @param groupId     群ID
     * @param title       标题
     * @param description 描述
     * @param options     选项列表
     * @param visibility  可见性: 1=仅群内, 2=公开
     * @param type        类型: 1=单选, 2=多选
     * @param maxSelect   多选时最多选几项
     * @param anonymous   是否匿名: 0=实名, 1=匿名
     * @param endTime     截止时间（毫秒时间戳，0表示无截止时间）
     * @param showResult  是否始终显示结果: 0=投票前隐藏, 1=始终显示
     * @param callback    回调
     */
    void createPoll(String groupId, String title, String description,
                   List<String> options, int visibility, int type,
                   int maxSelect, int anonymous, long endTime, int showResult,
                   OnPollCallback<Poll> callback);

    /**
     * 获取投票详情
     *
     * @param pollId   投票ID
     * @param callback 回调
     */
    void getPoll(long pollId, OnPollCallback<Poll> callback);

    /**
     * 参与投票
     *
     * @param pollId    投票ID
     * @param optionIds 选项ID列表
     * @param callback  回调
     */
    void vote(long pollId, List<Long> optionIds, OnPollCallback<Void> callback);

    /**
     * 结束投票（仅创建者）
     *
     * @param pollId   投票ID
     * @param callback 回调
     */
    void closePoll(long pollId, OnPollCallback<Void> callback);

    /**
     * 删除投票（仅创建者）
     *
     * @param pollId   投票ID
     * @param callback 回调
     */
    void deletePoll(long pollId, OnPollCallback<Void> callback);

    /**
     * 导出投票明细（仅实名投票创建者）
     *
     * @param pollId   投票ID
     * @param callback 回调
     */
    void exportPollDetails(long pollId, OnPollCallback<List<PollVoterDetail>> callback);

    /**
     * 获取我的投票列表
     *
     * @param callback 回调
     */
    void getMyPolls(OnPollCallback<List<Poll>> callback);

    /**
     * 投票回调接口
     */
    interface OnPollCallback<T> {
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
}
