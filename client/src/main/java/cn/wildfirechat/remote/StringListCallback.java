/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.remote;

import java.util.List;

import cn.wildfirechat.model.GroupInfo;

public interface StringListCallback {

    void onSuccess(List<String> strings);

    void onFail(int errorCode);
}
