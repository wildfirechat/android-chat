/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.message.core;

/**
 * Created by heavyrain lee on 2017/12/6.
 */

public enum PersistFlag {
    /*
     * 消息在本地不存储
     */
    No_Persist(0),

    /*
     * 消息在本地存储
     */
    Persist(1),

    /*
     * 消息在本地存储且计入未读数
     */
    Persist_And_Count(3),

    /*
     * 消息在服务器透传，如果对方不在线会丢弃，不会同步到发送方的多端，不支持聊天室。
     */
    Transparent(4);

    private int value;

    PersistFlag(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
    public static PersistFlag flag(int flag) {
        PersistFlag pflag = null;
        switch (flag) {
            case 0:
                pflag = No_Persist;
                break;
            case 1:
                pflag = Persist;
                break;
            case 3:
                pflag = Persist_And_Count;
                break;
            case 4:
                pflag = Transparent;
                break;
            default:
                throw new IllegalArgumentException("flag" + flag + " is invalid");
        }
        return pflag;
    }
}
