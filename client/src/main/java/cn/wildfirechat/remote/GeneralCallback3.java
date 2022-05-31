/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.remote;

import java.util.List;

public interface GeneralCallback3 {
    void onSuccess(List<String> result);

    void onFail(int errorCode);
}
