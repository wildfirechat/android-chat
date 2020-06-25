package cn.wildfirechat;

public class ErrorCode {
    //AIDL error
    public static final int SERVICE_EXCEPTION = -1000;
    public static final int SERVICE_DIED = -1001;
    public static final int FILE_NOT_EXIST = -1002;
    public static final int FILE_TOO_LARGE = -1003;
    public static final int INVALID_PARAMETER = -1004;


    //0~255 server error
    public static final int SUCCESS = 0;  // //"success");
    public static final int SECRECT_KEY_MISMATCH = 1;  //"secrect key mismatch");
    public static final int INVALID_DATA = 2;  //"invalid data");
    public static final int NODE_NOT_EXIST = 3;  //"node not exist");
    public static final int SERVER_ERROR = 4;  //"server error");
    public static final int NOT_MODIFIED = 5;  //"not modified");

    //Auth error
    public static final int TOKEN_ERROR = 6;  //"token error");
    public static final int USER_FORBIDDEN = 8;  //"user forbidden");

    //Message error
    public static final int NOT_IN_GROUP = 9;  //"not in group");
    public static final int INVALID_MESSAGE = 10;  //"invalid message");

    //Group error
    public static final int GROUP_ALREADY_EXIST = 11;  //"group aleady exist");


    //user error
    public static final int PASSWORD_INCORRECT = 15;  //"password incorrect");

    //user error
    public static final int FRIEND_ALREADY_REQUEST = 16;  //"already send request");
    public static final int FRIEND_REQUEST_BLOCKED = 18;  //"friend request blocked");
    public static final int FRIEND_REQUEST_EXPIRED = 19;  //"friend request expired");

    public static final int NOT_IN_CHATROOM = 20;  //"not in chatroom");

    public static final int NOT_IN_CHANNEL = 21;  //"not in channel");

    public static final int NOT_LICENSED = 22;  //"not licensed");
    public static final int ALREADY_FRIENDS = 23;  //"already friends");

    public static final int GROUP_EXCEED_MAX_MEMBER_COUNT = 240;  //"group exceed max member count");
    public static final int GROUP_MUTED = 241;  //"group is muted");
    public static final int SENSITIVE_MATCHED = 242;  //"sensitive matched");
    public static final int SIGN_EXPIRED = 243;  //"sign expired");
    public static final int AUTH_FAILURE = 244;  //"auth failure");
    public static final int CLIENT_COUNT_OUT_OF_LIMIT = 245;  //"client count out of limit");
    public static final int IN_BLACK_LIST = 246;  //"user in balck list");
    public static final int FORBIDDEN_SEND_MSG = 247;  //"forbidden send msg globally");
    public static final int NOT_RIGHT = 248;  //"no right to operate");
    public static final int TIMEOUT = 249;  //"timeout");
    public static final int OVER_FREQUENCY = 250;  //"over frequency");
    public static final int SERVER_INVALID_PARAMETER = 251;  //"Invalid parameter");
    public static final int NOT_EXIST = 253;  //"not exist");
    public static final int NOT_IMPLEMENT = 254;  //"not implement");

    //负值为mars返回错误
    public static final int Local_TaskTimeout = -1;
    public static final int Local_TaskRetry = -2;
    public static final int Local_StartTaskFail = -3;
    public static final int Local_AntiAvalanche = -4;
    public static final int Local_ChannelSelect = -5;
    public static final int Local_NoNet = -6;
    public static final int Local_Cancel = -7;
    public static final int Local_Clear = -8;
    public static final int Local_Reset = -9;
    public static final int Local_TaskParam = -12;
    public static final int Local_CgiFrequcencyLimit = -13;
    public static final int Local_ChannelID = -14;

    public static final int Long_FirstPkgTimeout = -500;
    public static final int Long_PkgPkgTimeout = -501;
    public static final int Long_ReadWriteTimeout = -502;
    public static final int Long_TaskTimeout = -503;

    public static final int Socket_NetworkChange = -10086;
    public static final int Socket_MakeSocketPrepared = -10087;
    public static final int Socket_WritenWithNonBlock = -10088;
    public static final int Socket_ReadOnce = -10089;
    public static final int Socket_RecvErr = -10091;
    public static final int Socket_SendErr = -10092;
    public static final int Socket_NoopTimeout = -10093;
    public static final int Socket_NoopAlarmTooLate = -10094;
    public static final int Http_SplitHttpHeadAndBody = -10194;
    public static final int Http_ParseStatusLine = -10195;
    public static final int Net_MsgXPHandleBufferErr = -10504;
    public static final int Dns_MakeSocketPrepared = -10606;

    //proto error code
    public static final int Proto_CorruptData = -100001;
    public static final int Proto_InvalideParameter = -100002;
}
