package cn.wildfire.chat.kit.third.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.ThumbnailUtils;
import android.os.SystemClock;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @创建者 CSDN_LQR
 * @描述 图像处理工具类
 */
public class ImageUtils {
    private static final String THUMB_IMG_DIR_PATH = UIUtils.getContext().getCacheDir().getAbsolutePath();
    private static final int IMG_WIDTH = 480; //超過此寬、高則會 resize圖片
    private static final int IMG_HIGHT = 800;
    private static final int COMPRESS_QUALITY = 70; //壓縮 JPEG使用的品質(70代表壓縮率為 30%)


    public static File genThumbImgFile(String srcImgPath) {
        File thumbImgDir = new File(THUMB_IMG_DIR_PATH);
        if (!thumbImgDir.exists()) {
            thumbImgDir.mkdirs();
        }
        String thumbImgName = SystemClock.currentThreadTimeMillis() + FileUtils.getFileNameFromPath(srcImgPath);
        File imageFileThumb = null;

        try {
            InputStream is = new FileInputStream(srcImgPath);
            Bitmap bmpSource = BitmapFactory.decodeStream(is);
            Bitmap bmpTarget = ThumbnailUtils.extractThumbnail(bmpSource, 200, 200, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
            if (bmpTarget == null) {
                return null;
            }
            imageFileThumb = new File(thumbImgDir, thumbImgName);
            imageFileThumb.createNewFile();

            FileOutputStream fosThumb = new FileOutputStream(imageFileThumb);

            bmpTarget.compress(Bitmap.CompressFormat.JPEG, 100, fosThumb);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return imageFileThumb;
    }

    public static File compressImage(String srcImgPath) {
        //先取得原始照片的旋轉角度
        int rotate = 0;
        try {
            ExifInterface exif = new ExifInterface(srcImgPath);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotate = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotate = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotate = 270;
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        //計算取 Bitmap時的參數"inSampleSize"
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(srcImgPath, options);

        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > IMG_HIGHT || width > IMG_WIDTH) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= IMG_HIGHT
                    && (halfWidth / inSampleSize) >= IMG_WIDTH) {
                inSampleSize *= 2;
            }
        }

        options.inJustDecodeBounds = false;
        options.inSampleSize = inSampleSize;

        //取出原檔的 Bitmap(若寬高超過會 resize)並設定原始的旋轉角度
        Bitmap srcBitmap = BitmapFactory.decodeFile(srcImgPath, options);
        Matrix matrix = new Matrix();
        matrix.postRotate(rotate);
        Bitmap outBitmap = Bitmap.createBitmap(srcBitmap, 0, 0, srcBitmap.getWidth(), srcBitmap.getHeight(), matrix, false);

        //壓縮並存檔至 cache路徑下的 File
        File tempImgDir = new File(THUMB_IMG_DIR_PATH);
        if (!tempImgDir.exists()) {
            tempImgDir.mkdirs();
        }
        String compressedImgName = SystemClock.currentThreadTimeMillis() + FileUtils.getFileNameFromPath(srcImgPath);
        File compressedImgFile = new File(tempImgDir, compressedImgName);
        FileOutputStream fos = null;
        try {
            compressedImgFile.createNewFile();
            fos = new FileOutputStream(compressedImgFile);
            outBitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESS_QUALITY, fos);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                srcBitmap.recycle();
                outBitmap.recycle();
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return compressedImgFile;
    }
}
