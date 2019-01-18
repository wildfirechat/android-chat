package com.lqr.imagepicker;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.DisplayMetrics;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Utils {

    /**
     * 获得状态栏的高度
     */
    public static int getStatusHeight(Context context) {
        int statusHeight = -1;
        try {
            Class<?> clazz = Class.forName("com.android.internal.R$dimen");
            Object object = clazz.newInstance();
            int height = Integer.parseInt(clazz.getField("status_bar_height").get(object).toString());
            statusHeight = context.getResources().getDimensionPixelSize(height);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return statusHeight;
    }

    /**
     * 根据屏幕宽度与密度计算GridView显示的列数， 最少为三列，并获取Item宽度
     */
    public static int getImageItemWidth(Activity activity) {
        int screenWidth = activity.getResources().getDisplayMetrics().widthPixels;
        int densityDpi = activity.getResources().getDisplayMetrics().densityDpi;
        int cols = screenWidth / densityDpi;
        cols = cols < 3 ? 3 : cols;
        int columnSpace = (int) (2 * activity.getResources().getDisplayMetrics().density);
        return (screenWidth - columnSpace * (cols - 1)) / cols;
    }

    /**
     * 判断SDCard是否可用
     */
    public static boolean existSDCard() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    /**
     * 获取手机大小（分辨率）
     */
    public static DisplayMetrics getScreenPix(Activity activity) {
        DisplayMetrics displaysMetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(displaysMetrics);
        return displaysMetrics;
    }

    /**
     * 扫描图片
     */
    public static void notifyToScanMedia(Context context, String path) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri contentUri = Uri.parse("file://" + path);
        mediaScanIntent.setData(contentUri);
        context.sendBroadcast(mediaScanIntent);
    }

    /**
     * 拍照的方法
     */
    public static void takePhoto(Activity activity, String outputPath, int requestCode) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        takePictureIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (takePictureIntent.resolveActivity(activity.getPackageManager()) != null) {
            // 默认情况下，即不需要指定intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
            // 照相机有自己默认的存储路径，拍摄的照片将返回一个缩略图。如果想访问原始图片，
            // 可以通过dat extra能够得到原始图片位置。即，如果指定了目标uri，data就没有数据，
            // 如果没有指定uri，则data就返回有数据！
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.parse("file://" + outputPath));
        }
        activity.startActivityForResult(takePictureIntent, requestCode);
    }

    /**
     * @return 绝对路径
     */
    public static String genTakePhotoOutputPath() {
        File takeImageFile;
        if (existSDCard()) {
            takeImageFile = new File(Environment.getExternalStorageDirectory(), "/DCIM/camera/");
        } else {
            takeImageFile = Environment.getDataDirectory();
        }
        takeImageFile = createFile(takeImageFile, "IMG_", ".jpg");
        return takeImageFile.getAbsolutePath();
    }

    /**
     * 根据系统时间、前缀、后缀产生一个文件
     */
    private static File createFile(File folder, String prefix, String suffix) {
        if (!folder.exists() || !folder.isDirectory()) {
            folder.mkdirs();
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA);
        String filename = prefix + dateFormat.format(new Date(System.currentTimeMillis())) + suffix;
        return new File(folder, filename);
    }
}
