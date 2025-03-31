/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.remote;

import java.util.List;

public interface GeneralCallback4<T> {
    void onSuccess(List<T> result);

    void onFail(int errorCode);
}
