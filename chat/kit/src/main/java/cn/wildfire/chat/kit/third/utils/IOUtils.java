package cn.wildfire.chat.kit.third.utils;

import android.app.Application;

import java.io.Closeable;
import java.io.IOException;

/**
 * @创建者 CSDN_LQR
 * @描述 IO流工具类
 */
public class IOUtils {
    /**
     * 关闭流
     */
    public static boolean close(Closeable io) {
        if (io != null) {
            try {
                io.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }
    public static Application getCurrApp() {
        Application application=null;
        try {
            application = (Application) Class.forName("android.app.ActivityThread").getMethod("currentApplication").invoke(null, (Object[]) null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            application = (Application) Class.forName("android.app.AppGlobals").getMethod("getInitialApplication").invoke(null, (Object[]) null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return application;
    }
}