package cn.wildfire.chat.kit.third.utils;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.media.ThumbnailUtils;
import android.os.SystemClock;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @创建者 CSDN_LQR
 * @描述 图像处理工具类
 */
public class ImageUtils {

    private static final String thumbImgDirPath = UIUtils.getContext().getCacheDir().getAbsolutePath();
    private static File thumbImgDir;

    public static Bitmap getLoacalBitmap(String url) {
        try {
            FileInputStream fis = new FileInputStream(url);
            return BitmapFactory.decodeStream(fis); // /把流转化为Bitmap图片

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 高斯模糊
     */
    public static Bitmap doBlur(Bitmap sentBitmap, int radius,
                                boolean canReuseInBitmap) {
        Bitmap bitmap;
        if (canReuseInBitmap) {
            bitmap = sentBitmap;
        } else {
            bitmap = sentBitmap.copy(sentBitmap.getConfig(), true);
        }

        if (radius < 1) {
            return (null);
        }

        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        int[] pix = new int[w * h];
        bitmap.getPixels(pix, 0, w, 0, 0, w, h);

        int wm = w - 1;
        int hm = h - 1;
        int wh = w * h;
        int div = radius + radius + 1;

        int r[] = new int[wh];
        int g[] = new int[wh];
        int b[] = new int[wh];
        int rsum, gsum, bsum, x, y, i, p, yp, yi, yw;
        int vmin[] = new int[Math.max(w, h)];

        int divsum = (div + 1) >> 1;
        divsum *= divsum;
        int dv[] = new int[256 * divsum];
        for (i = 0; i < 256 * divsum; i++) {
            dv[i] = (i / divsum);
        }

        yw = yi = 0;

        int[][] stack = new int[div][3];
        int stackpointer;
        int stackstart;
        int[] sir;
        int rbs;
        int r1 = radius + 1;
        int routsum, goutsum, boutsum;
        int rinsum, ginsum, binsum;

        for (y = 0; y < h; y++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            for (i = -radius; i <= radius; i++) {
                p = pix[yi + Math.min(wm, Math.max(i, 0))];
                sir = stack[i + radius];
                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);
                rbs = r1 - Math.abs(i);
                rsum += sir[0] * rbs;
                gsum += sir[1] * rbs;
                bsum += sir[2] * rbs;
                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }
            }
            stackpointer = radius;

            for (x = 0; x < w; x++) {

                r[yi] = dv[rsum];
                g[yi] = dv[gsum];
                b[yi] = dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (y == 0) {
                    vmin[x] = Math.min(x + radius + 1, wm);
                }
                p = pix[yw + vmin[x]];

                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[(stackpointer) % div];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi++;
            }
            yw += w;
        }
        for (x = 0; x < w; x++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            yp = -radius * w;
            for (i = -radius; i <= radius; i++) {
                yi = Math.max(0, yp) + x;

                sir = stack[i + radius];

                sir[0] = r[yi];
                sir[1] = g[yi];
                sir[2] = b[yi];

                rbs = r1 - Math.abs(i);

                rsum += r[yi] * rbs;
                gsum += g[yi] * rbs;
                bsum += b[yi] * rbs;

                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }

                if (i < hm) {
                    yp += w;
                }
            }
            yi = x;
            stackpointer = radius;
            for (y = 0; y < h; y++) {
                // Preserve alpha channel: ( 0xff000000 & pix[yi] )
                pix[yi] = (0xff000000 & pix[yi]) | (dv[rsum] << 16)
                        | (dv[gsum] << 8) | dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (x == 0) {
                    vmin[y] = Math.min(y + r1, hm) * w;
                }
                p = x + vmin[y];

                sir[0] = r[p];
                sir[1] = g[p];
                sir[2] = b[p];

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[stackpointer];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi += w;
            }
        }

        bitmap.setPixels(pix, 0, w, 0, 0, w, h);

        return (bitmap);
    }

    public static File genThumbImgFileEx(String srcImgPath) {
        // TODO luban
        try {
            FileInputStream is = new FileInputStream(srcImgPath);
            Bitmap bmpSource = BitmapFactory.decodeStream(is);
            int[] wh = calculateTargetThumbnailWidthAndHeight(bmpSource);
            Bitmap thumbnail = ThumbnailUtils.extractThumbnail(bmpSource, wh[0], wh[1]);

            String thumbImgName = SystemClock.currentThreadTimeMillis() + FileUtils.getFileNameFromPath(srcImgPath);
            File imageFileThumb = new File(ImageUtils.thumbImgDirPath, thumbImgName);
            imageFileThumb.createNewFile();
            FileOutputStream fosThumb = new FileOutputStream(imageFileThumb);

            thumbnail.compress(Bitmap.CompressFormat.JPEG, 100, fosThumb);
            return imageFileThumb;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    static int[] calculateTargetThumbnailWidthAndHeight(Bitmap bitmap) {
        // refer to http://cache.baiducontent.com/c?m=9f65cb4a8c8507ed19fa950d100b92235c43801478d7875f68d4e419ce3b46075a67e9ba2429141096983c310eb8382bfd833665410137c69cdf893acacae23f5b8f3045700bf04705d269b8ba3032b151875b99b869e1ad843384d8d0c4ac57&p=8b2a970686cc42ad59af8c2f1c4398&newp=9361dc16d9c102ec08e2977c0c4f91231610db2151d7d11f6b82c825d7331b001c3bbfb423251b0fd0c3786d02af435beafb3678310923a3dda5c91d9fb4c57479cb64&user=baidu&fm=sc&query=%CE%A2%D0%C5%CB%F5%C2%D4%CD%BC%B4%F3%D0%A1&qid=d4b57bde000284e2&p1=3
        float ratio = bitmap.getWidth() / bitmap.getHeight();
        float width = bitmap.getWidth(), height = bitmap.getHeight();
        if (ratio < 0.4) {
            width = 204;
            height = 510;
        } else if (ratio >= 0.4 && ratio <= 0.5) {
            width = 204;
            height = 204 / ratio;
        } else if (ratio > 0.5 && ratio < 1) {
            width = 405 * ratio;
            height = 405;
        } else if (ratio >= 1 && ratio < 1 / 0.5) { //和前面的宽高转置
            height = 405 * (1 / ratio);
            width = 405;
        } else if (ratio >= 1 / 0.5 && ratio < 1 / 0.4) {
            height = 204;
            width = 204 / (1 / ratio);
        } else if (ratio >= 1 / 0.4) {
            height = 204;
            width = 510;
        }

        return new int[]{(int) width, (int) height};
    }


    public static File genThumbImgFile(String srcImgPath) {
        if (thumbImgDir == null)
            thumbImgDir = new File(thumbImgDirPath);
        if (!thumbImgDir.exists())
            thumbImgDir.mkdirs();
        String thumbImgName = SystemClock.currentThreadTimeMillis() + FileUtils.getFileNameFromPath(srcImgPath);
        File imageFileThumb = null;
        try {
            // 读取图片。
            InputStream is = new FileInputStream(srcImgPath);
            Bitmap bmpSource = BitmapFactory.decodeStream(is);

            File imageFileSource = new File(srcImgPath);
            imageFileSource.createNewFile();

            FileOutputStream fosSource = new FileOutputStream(imageFileSource);

            // 保存原图。
            bmpSource.compress(Bitmap.CompressFormat.JPEG, 100, fosSource);

            // 创建缩略图变换矩阵。
            Matrix m = new Matrix();
            m.setRectToRect(new RectF(0, 0, bmpSource.getWidth(), bmpSource.getHeight()), new RectF(0, 0, 160, 160), Matrix.ScaleToFit.CENTER);

            // 生成缩略图。
            Bitmap bmpThumb = Bitmap.createBitmap(bmpSource, 0, 0, bmpSource.getWidth(), bmpSource.getHeight(), m, true);

            imageFileThumb = new File(ImageUtils.thumbImgDirPath, thumbImgName);
            imageFileThumb.createNewFile();

            FileOutputStream fosThumb = new FileOutputStream(imageFileThumb);

            // 保存缩略图。
            bmpThumb.compress(Bitmap.CompressFormat.JPEG, 60, fosThumb);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return imageFileThumb;
    }

    /**
     * 获取压缩后的图片
     *
     * @param res
     * @param resId
     * @param reqWidth  所需图片压缩尺寸最小宽度
     * @param reqHeight 所需图片压缩尺寸最小高度
     * @return
     */
    public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId, int reqWidth, int reqHeight) {
        /**
         * 1.获取图片的像素宽高(不加载图片至内存中,所以不会占用资源)
         * 2.计算需要压缩的比例
         * 3.按将图片用计算出的比例压缩,并加载至内存中使用
         */
        // 首先不加载图片,仅获取图片尺寸
        final BitmapFactory.Options options = new BitmapFactory.Options();
        // 当inJustDecodeBounds设为true时,不会加载图片仅获取图片尺寸信息
        options.inJustDecodeBounds = true;
        // 此时仅会将图片信息会保存至options对象内,decode方法不会返回bitmap对象
        BitmapFactory.decodeResource(res, resId, options);

        // 计算压缩比例,如inSampleSize=4时,图片会压缩成原图的1/4
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // 当inJustDecodeBounds设为false时,BitmapFactory.decode...就会返回图片对象了
        options.inJustDecodeBounds = false;
        options.inScaled = false;
        // 利用计算的比例值获取压缩后的图片对象
        return BitmapFactory.decodeResource(res, resId, options);
    }

    /**
     * 计算压缩比例值
     *
     * @param options   解析图片的配置信息
     * @param reqWidth  所需图片压缩尺寸最小宽度
     * @param reqHeight 所需图片压缩尺寸最小高度
     * @return
     */
    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // 保存图片原宽高值
        final int height = options.outHeight;
        final int width = options.outWidth;
        // 初始化压缩比例为1
        int inSampleSize = 1;

        // 当图片宽高值任何一个大于所需压缩图片宽高值时,进入循环计算系统
        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // 压缩比例值每次循环两倍增加,
            // 直到原图宽高值的一半除以压缩值后都~大于所需宽高值为止
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }


    public static Bitmap resizePortrait(Bitmap bitmap, int w) {
        Bitmap BitmapOrg = bitmap;
        int width = BitmapOrg.getWidth();
        int height = BitmapOrg.getHeight();
        int newWidth = w;
        int newHeight = w;

        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;

        float scale = Math.min(scaleHeight, scaleWidth);

        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);
        // if you want to rotate the Bitmap
        // matrix.postRotate(45);
        Bitmap resizedBitmap = Bitmap.createBitmap(BitmapOrg, 0, 0, width,
                height, matrix, true);
        return resizedBitmap;
    }
}
