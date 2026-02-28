/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.callback;

import androidx.annotation.NonNull;

import cn.wildfire.chat.app.model.SlideVerifyInfo;

/**
 * 滑动验证码回调接口
 * 用于处理验证码加载和验证结果
 */
public interface SlideVerifyCallback {

    /**
     * 验证码加载成功
     *
     * @param verifyInfo 验证码信息
     */
    void onLoadSuccess(@NonNull SlideVerifyInfo verifyInfo);

    /**
     * 验证码加载失败
     *
     * @param errorCode 错误码
     * @param errorMsg  错误信息
     */
    void onLoadFailure(int errorCode, @NonNull String errorMsg);

    /**
     * 验证成功
     *
     * @param token 验证令牌
     */
    void onVerifySuccess(@NonNull String token);

    /**
     * 验证失败（滑动位置不正确）
     *
     * @param errorCode 错误码
     * @param errorMsg  错误信息
     */
    void onVerifyFailure(int errorCode, @NonNull String errorMsg);
}
