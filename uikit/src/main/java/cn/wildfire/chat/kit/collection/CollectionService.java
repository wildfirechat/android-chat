/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.collection;

import cn.wildfire.chat.kit.collection.model.Collection;

/**
 * 接龙服务接口
 * <p>
 * 定义接龙相关的网络服务操作，包括创建、获取、参与、删除、关闭接龙等功能。
 * 实现类需要处理具体的HTTP请求和认证逻辑。
 * </p>
 *
 * @author WildFireChat
 * @since 2020
 */
public interface CollectionService {

    /**
     * 创建接龙回调
     */
    interface CreateCollectionCallback {
        void onSuccess(Collection collection);
        void onError(int errorCode, String message);
    }

    /**
     * 获取接龙详情回调
     */
    interface GetCollectionCallback {
        void onSuccess(Collection collection);
        void onError(int errorCode, String message);
    }

    /**
     * 通用操作回调
     */
    interface OperationCallback {
        void onSuccess();
        void onError(int errorCode, String message);
    }

    /**
     * 创建接龙
     * <p>
     * POST /api/collections
     * </p>
     *
     * @param groupId 群ID
     * @param title 接龙标题（必填）
     * @param desc 接龙描述（可选）
     * @param template 参与模板（可选，如：姓名-电话）
     * @param expireType 过期类型：0=无限期，1=有限期
     * @param expireAt 过期时间（毫秒时间戳，当expireType=1时有效）
     * @param maxParticipants 最大参与人数（0表示无限制）
     * @param callback 回调
     */
    void createCollection(String groupId, String title, String desc, String template,
                         int expireType, long expireAt, int maxParticipants,
                         CreateCollectionCallback callback);

    /**
     * 获取接龙详情
     * <p>
     * POST /api/collections/{collectionId}/detail
     * </p>
     *
     * @param collectionId 接龙ID
     * @param groupId 群ID
     * @param callback 回调
     */
    void getCollection(long collectionId, String groupId, GetCollectionCallback callback);

    /**
     * 参与接龙或更新参与内容
     * <p>
     * POST /api/collections/{collectionId}/join
     * </p>
     *
     * @param collectionId 接龙ID
     * @param groupId 群ID
     * @param content 参与内容
     * @param callback 回调
     */
    void joinCollection(long collectionId, String groupId, String content, OperationCallback callback);

    /**
     * 删除自己的参与记录
     * <p>
     * POST /api/collections/{collectionId}/delete
     * </p>
     *
     * @param collectionId 接龙ID
     * @param groupId 群ID
     * @param callback 回调
     */
    void deleteCollectionEntry(long collectionId, String groupId, OperationCallback callback);

    /**
     * 关闭接龙（仅创建者可操作）
     * <p>
     * POST /api/collections/{collectionId}/close
     * </p>
     *
     * @param collectionId 接龙ID
     * @param groupId 群ID
     * @param callback 回调
     */
    void closeCollection(long collectionId, String groupId, OperationCallback callback);
}
