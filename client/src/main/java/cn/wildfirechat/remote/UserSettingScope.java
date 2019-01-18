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

    int kUserSettingCustomBegin = 1000;
}
