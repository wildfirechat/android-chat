/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.chatme.remote;

import java.util.List;

public interface StringListCallback {

    void onSuccess(List<String> strings);

    void onFail(int errorCode);
}
