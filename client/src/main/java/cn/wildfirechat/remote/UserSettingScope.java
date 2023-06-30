/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.remote;

public interface UserSettingScope {
    //不能直接使用，调用setConversation:silent:方法会使用到此值。
    int ConversationSilent = 1;
    int GlobalSilent = 2;
    //不能直接使用，调用setConversation:top:方法会使用到此值。
    int ConversationTop = 3;
    int HiddenNotificationDetail = 4;
    int GroupHideNickname = 5;
    int FavoriteGroup = 6;
    //不能直接使用，协议栈内会使用此值
    int Conversation_Sync = 7;
    //不能直接使用，协议栈内会使用此值
    int My_Channel = 8;
    //不能直接使用，协议栈内会使用此值
    int Listened_Channel = 9;
    int PCOnline = 10;
    //不能直接使用，协议栈内会使用此值
    int ConversationReaded = 11;
    int WebOnline = 12;
    int DisableReceipt = 13;
    int FavoriteUser = 14;
    //不能直接使用，协议栈内会使用此值
    int MuteWhenPcOnline = 15;
    //不能直接使用，协议栈内会使用此值
    int NoDisturbing = 17;
    //不能直接使用，协议栈内会使用此值
    int ConversationClearMessage = 18;
    //不能直接使用，协议栈内会使用此值
    int ConversationDraft = 19;
    //不能直接使用，协议栈内会使用此值
    int DisableSyncDraft = 20;
    //不能直接使用，协议栈内会使用此值
    int VoipSilent = 21;
    //不能直接使用，协议栈内会使用此值
    int PTTReserved = 22;
    //不能直接使用，协议栈内会使用此值
    int CustomState = 23;
    // 不能直接使用，协议栈内部会使用此致
    int DisableSecretChat = 24;
    // 不能直接使用，协议栈内部会使用此致
    int Conversation_PTT_Silent = 25;
    // 不能直接使用，协议栈内部会使用此致
    int Conversation_GroupRemark = 26;

    // 不能直接使用，协议栈内部会使用此致
    int Privacy_Searchable = 27;

    //自定义用户设置，请使用1000以上的key
    int kUserSettingCustomBegin = 1000;
}
