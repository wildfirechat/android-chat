package cn.wildfire.chat.kit.third.utils;

import androidx.annotation.NonNull;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;

import java.util.Calendar;

/**
 * @创建者 CSDN_LQR
 * @描述 时间工具（需要joda-time）
 */
public class TimeUtils {

    /**
     * 得到仿微信日期格式输出
     *
     * @param msgTimeMillis
     * @return
     */
    public static String getMsgFormatTime(long msgTimeMillis) {
        if (msgTimeMillis == 0) {
            return "";
        }

        DateTime msgTime = new DateTime(msgTimeMillis);
        Calendar nowCalendar = Calendar.getInstance();
        nowCalendar.get(Calendar.DAY_OF_YEAR);
        Calendar msgCalendar = Calendar.getInstance();
        msgCalendar.setTimeInMillis(msgTimeMillis);

        if (nowCalendar.get(Calendar.YEAR) == msgCalendar.get(Calendar.YEAR)) {
            if (nowCalendar.get(Calendar.DAY_OF_YEAR) == msgCalendar.get(Calendar.DAY_OF_YEAR)) {
                //早上、下午、晚上 1:40
                return getTime(msgTime);
            } else if (msgCalendar.get(Calendar.DAY_OF_YEAR) + 1 == nowCalendar.get(Calendar.DAY_OF_YEAR)) {
                //昨天
                return "昨天 " + getTime(msgTime);
            } else if (nowCalendar.get(Calendar.WEEK_OF_YEAR) == msgCalendar.get(Calendar.WEEK_OF_YEAR)) {
                //星期
                switch (msgTime.getDayOfWeek()) {
                    case DateTimeConstants.SUNDAY:
                        return "周日 " + getTime(msgTime);
                    case DateTimeConstants.MONDAY:
                        return "周一 " + getTime(msgTime);
                    case DateTimeConstants.TUESDAY:
                        return "周二 " + getTime(msgTime);
                    case DateTimeConstants.WEDNESDAY:
                        return "周三 " + getTime(msgTime);
                    case DateTimeConstants.THURSDAY:
                        return "周四 " + getTime(msgTime);
                    case DateTimeConstants.FRIDAY:
                        return "周五 " + getTime(msgTime);
                    case DateTimeConstants.SATURDAY:
                        return "周六 " + getTime(msgTime);
                    default:
                        break;
                }
                return "";
            } else {
                //12月22日
                return msgTime.toString("MM月dd日 HH:mm");
            }
        } else {
            return msgTime.toString("yyyy年MM月dd日 HH:mm");
        }
    }

    @NonNull
    private static String getTime(DateTime msgTime) {
        return msgTime.toString("HH:mm");
    }
}
