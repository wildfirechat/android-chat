package cn.wildfire.chat.kit.third.utils;

import android.annotation.SuppressLint;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 时间展示样式转换工具类
 * @author dhl
 *
 */
@SuppressLint("SimpleDateFormat")
public class TimeConvertUtils {

    private static final String NORMAL_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String NORMAL_DATE_FORMAT1 = "yyyy-MM-dd-HH:mm:ss";
    private static final String SPECIFIC_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss:SSS";

    /**
     * 日期转换格式：yyyy-MM-dd HH:mm:ss
     *
     * @param t long型时间
     * @return 2014-03-10 09:01:01
     */
    public static String formatDate4(long t) {
        SimpleDateFormat sdf = new SimpleDateFormat(NORMAL_DATE_FORMAT1);
        return sdf.format(t);
    }

    /**
     * 日期转换格式：yyyy-MM-dd HH:mm:ss
     *
     * @param time long型时间
     * @return 2014-03-10 09:01:01
     */
    public static String formatDate6(String time) {
        if (time.equals("null") || time.equals("")) {
            time = Long.toString(new Date().getTime());
        } else {
            time = time + "000";
        }
        long t = Long.parseLong(time);
        SimpleDateFormat sdf = new SimpleDateFormat(NORMAL_DATE_FORMAT);
        return sdf.format(t);
    }

    /**
     * 日期转换格式：yyyy-MM-dd HH:mm:ss:SSS
     *
     * @param time long型时间
     * @return
     */
    public static String formatDate7(long time) {
        SimpleDateFormat sdf = new SimpleDateFormat(SPECIFIC_DATE_FORMAT);
        return sdf.format(time);
    }

    /**
     * 时间转换 小视频转 微信那种时间格式 mm:ss
     * @param mss
     * @return
     */
    public static String formatLongTime(long mss) {
        String DateTimes = null;
       // long hours = (mss % ( 60 * 60 * 24)) / (60 * 60);
        long minutes = (mss % ( 60 * 60)) /60;
        long seconds = mss % 60;

        DateTimes = String.format("%02d:", minutes) + String.format("%02d", seconds);
        //String.format("%2d:", hours);
        return DateTimes;
    }
}
