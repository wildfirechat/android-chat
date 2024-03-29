/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.chatme.client;

/**
 * Created by heavyrainlee on 17/02/2018.
 */

public class NotInitializedExecption extends RuntimeException {
    public NotInitializedExecption() {
        super("Not init!!!");
    }
}
