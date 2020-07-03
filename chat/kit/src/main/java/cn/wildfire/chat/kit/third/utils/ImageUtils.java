package cn.wildfire.chat.kit.third.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.media.ThumbnailUtils;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

import cn.wildfire.chat.kit.GlideApp;
import cn.wildfire.chat.kit.utils.portrait.CombineBitmapTools;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.GroupMember;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GetGroupInfoCallback;
import cn.wildfirechat.remote.GetGroupMembersCallback;
import cn.wildfirechat.remote.GetUserInfoCallback;

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

    public static @Nullable File compressImage(String srcImgPath) {
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
        if(srcBitmap == null){
            Log.e("ImageUtils", "decode file error " + srcImgPath);
            return null;
        }
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

    private static String getDigest(String str) {
        if (TextUtils.isEmpty(str))
            return "";
        return str.hashCode()+"";
    }

    private static long lastGenerateTime = 0;

    private static void generateNewGroupPortrait(Context context, String groupId, int width) {
        if (System.currentTimeMillis() - lastGenerateTime < 15 * 1000) {
            return;
        }
        lastGenerateTime = System.currentTimeMillis();
        ChatManager.Instance().getGroupMembers(groupId, false, new GetGroupMembersCallback() {
            @Override
            public void onSuccess(List<GroupMember> groupMembers) {
                if (groupMembers == null || groupMembers.size() == 0) {
                    return;
                }

                new Thread(()->{
                    List<Bitmap> bitmaps = new ArrayList<>();
                    final String[] fullPath = {""};
                    for (int i = 0; i < Math.min(groupMembers.size(), 9); i++) {
                        String memberId = groupMembers.get(i).memberId;
                        final CountDownLatch latch = new CountDownLatch(1);

                        ChatManager.Instance().getUserInfo(memberId, false, new GetUserInfoCallback() {
                            @Override
                            public void onSuccess(UserInfo userInfo) {
                                new Thread(()->{
                                    fullPath[0] += userInfo.portrait;
                                    Drawable drawable = null;
                                    try {
                                        drawable = GlideApp.with(context).load(userInfo.portrait).placeholder(R.mipmap.avatar_def).submit(60, 60).get();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        try {
                                            drawable = GlideApp.with(context).load(R.mipmap.avatar_def).submit(60, 60).get();
                                        } catch (ExecutionException ex) {
                                            ex.printStackTrace();
                                        } catch (InterruptedException ex) {
                                            ex.printStackTrace();
                                        }
                                    }
                                    if (drawable instanceof BitmapDrawable) {
                                        bitmaps.add(((BitmapDrawable) drawable).getBitmap());
                                    }
                                    latch.countDown();
                                }).start();
                            }

                            @Override
                            public void onFail(int errorCode) {
                                new Thread(()->{
                                    Drawable drawable = null;
                                    try {
                                        drawable = GlideApp.with(context).load(R.mipmap.avatar_def).submit(60, 60).get();
                                        if (drawable instanceof BitmapDrawable) {
                                            bitmaps.add(((BitmapDrawable) drawable).getBitmap());
                                        }
                                    } catch (ExecutionException e) {
                                        e.printStackTrace();
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }

                                    latch.countDown();
                                });
                            }
                        });

                        try {
                            latch.await();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        Log.d("dd", "dd");
                    }

                    Bitmap bitmap = CombineBitmapTools.combimeBitmap(context, 60, 60, bitmaps);
                    if (bitmap == null) {
                        return;
                    }

                    String hash = getDigest(fullPath[0]);
                    //Path 格式为 groupId-updatetime-width-hash
                    String fileName = groupId+"-"+System.currentTimeMillis()+"-"+width+"-"+hash;
                    try {
                        //create a file to write bitmap data
                        File f = new File(context.getCacheDir(), fileName);
                        f.createNewFile();
                        //Convert bitmap to byte array
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.PNG, 0 /*ignored for PNG*/, bos);
                        byte[] bitmapData = bos.toByteArray();

                        //write the bytes in file
                        FileOutputStream fos = new FileOutputStream(f);
                        fos.write(bitmapData);
                        fos.flush();
                        fos.close();

                        SharedPreferences sp = context.getSharedPreferences("wfc", Context.MODE_PRIVATE);
                        String key = "wfc_group_generated_portrait_"+groupId+"_"+width;
                        sp.edit().putString(key, f.getAbsolutePath()).apply();

                        //Todo notify
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();


            }

            @Override
            public void onFail(int errorCode) {

            }
        });
    }
    public static String getGroupGridPortrait(Context context, String groupId, int width) {
        SharedPreferences sp = context.getSharedPreferences("wfc", Context.MODE_PRIVATE);
        String path = sp.getString("wfc_group_generated_portrait_"+groupId+"_"+width, null);
        if (TextUtils.isEmpty(path)) {
            generateNewGroupPortrait(context, groupId, width);
        } else {
            File file = new File(path);
            if (file.exists()) {
                ChatManager.Instance().getGroupInfo(groupId, false, new GetGroupInfoCallback() {
                    @Override
                    public void onSuccess(GroupInfo groupInfo) {
                        new Thread(()->{
                            //分析文件名，获取更新时间，hash值
                            //Path 格式为 groupId-updatetime-width-hash
                            String name = file.getName();
                            name = name.substring(groupId.length());
                            String[] arr = name.split("-");
                            long timestamp = Long.parseLong(arr[1]);

                            long now = System.currentTimeMillis();
                            if (now - timestamp > 60*1000 || timestamp < groupInfo.updateDt) {
                                ChatManager.Instance().getGroupMembers(groupId, false, new GetGroupMembersCallback() {
                                    @Override
                                    public void onSuccess(List<GroupMember> groupMembers) {
                                        if (groupMembers == null || groupMembers.size() == 0) {
                                            return;
                                        }

                                        new Thread(()->{
                                            final String[] fullPath = {""};
                                            for (int i = 0; i < Math.min(groupMembers.size(), 9); i++) {
                                                String memberId = groupMembers.get(i).memberId;
                                                CountDownLatch latch = new CountDownLatch(1);

                                                ChatManager.Instance().getUserInfo(memberId, false, new GetUserInfoCallback() {
                                                    @Override
                                                    public void onSuccess(UserInfo userInfo) {
                                                        new Thread(()->{
                                                            fullPath[0] += userInfo.portrait;
                                                            latch.countDown();
                                                        }).start();
                                                    }

                                                    @Override
                                                    public void onFail(int errorCode) {
                                                        new Thread(()->{
                                                            latch.countDown();
                                                        }).start();
                                                    }
                                                });
                                                try {
                                                    latch.await();
                                                } catch (InterruptedException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                            if (!arr[3].equals(getDigest(fullPath[0]))) {
                                                generateNewGroupPortrait(context, groupId, width);
                                            }
                                        }).start();
                                    }

                                    @Override
                                    public void onFail(int errorCode) {

                                    }
                                });
                            }
                        }).start();
                    }

                    @Override
                    public void onFail(int errorCode) {

                    }
                });

                return path;
            } else {
                generateNewGroupPortrait(context, groupId, width);
            }
        }

        return null;
    }
}
