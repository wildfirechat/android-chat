/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.chatme.remote;

import java.util.List;

public interface GeneralCallback3 {
    void onSuccess(List<String> result);

    void onFail(int errorCode);
}
