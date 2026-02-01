package cn.wildfirechat.model;

/**
 * 文件记录排序枚举
 * <p>
 * 定义文件记录的排序方式。
 * 支持按时间或大小排序，可升序或降序。
 * </p>
 *
 * @author WildFireChat
 * @since 2020
 */
public enum FileRecordOrder {

    /**
     * 按时间降序
     */
    By_Time_Desc(0),

    /**
     * 按时间升序
     */
    By_Time_Asc(1),

    /**
     * 按大小降序
     */
    By_Size_Desc(2),

    /**
     * 按大小升序
     */
    By_Size_Asc(3);

    /**
     * 排序类型的值
     */
    public int value;

    FileRecordOrder(int value) {
        this.value = value;
    }

    public static FileRecordOrder type(int type) {
        FileRecordOrder out = null;
        if (type >= 0 && type < FileRecordOrder.values().length) {
            return FileRecordOrder.values()[type];
        }

        throw new IllegalArgumentException("FileRecordOrder " + type + " is invalid");

    }
}
