/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.remote;



public interface GeneralCallback5<T> {
    void onSuccess(T result);

    void onFail(int errorCode);
}
