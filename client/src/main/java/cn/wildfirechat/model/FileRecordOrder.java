package cn.wildfirechat.model;

public enum FileRecordOrder {

    By_Time_Desc(0),
    // 群聊
    By_Time_Asc(1),
    // 聊天室
    By_Size_Desc(2),
    //频道
    By_Size_Asc(3);

    public int value;

    FileRecordOrder(int value) {
        this.value = value;
    }
}
