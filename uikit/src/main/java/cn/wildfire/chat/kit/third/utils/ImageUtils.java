package cn.wildfire.chat.kit.third.utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.utils.portrait.CombineBitmapTools;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.GroupMember;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GetGroupInfoCallback;
import cn.wildfirechat.remote.GetGroupMembersCallback;

/**
 * @创建者 CSDN_LQR
 * @描述 图像处理工具类
 */
public class ImageUtils {
    private static final String THUMB_IMG_DIR_PATH = UIUtils.getContext().getCacheDir().getAbsolutePath();
    private static final int IMG_WIDTH = 480; //超過此寬、高則會 resize圖片
    private static final int IMG_HEIGHT = 800;
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

    public static @Nullable
    File compressImage(String srcImgPath) {
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

        if (height > IMG_HEIGHT || width > IMG_WIDTH) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= IMG_HEIGHT
                && (halfWidth / inSampleSize) >= IMG_WIDTH) {
                inSampleSize *= 2;
            }
        }

        options.inJustDecodeBounds = false;
        options.inSampleSize = inSampleSize;

        //取出原檔的 Bitmap(若寬高超過會 resize)並設定原始的旋轉角度
        Bitmap srcBitmap = BitmapFactory.decodeFile(srcImgPath, options);
        if (srcBitmap == null) {
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
        String hash = str.hashCode() + "";
        return hash.replaceAll("-", "");
    }

    private static void generateNewGroupPortrait(Context context, String groupId, int width) {
        ChatManager.Instance().getWorkHandler().post(() -> {
            ChatManager.Instance().getGroupMembers(groupId, false, new GetGroupMembersCallback() {
                @Override
                public void onSuccess(List<GroupMember> groupMembers) {
                    if (groupMembers == null || groupMembers.size() == 0) {
                        return;
                    }

                    ChatManager.Instance().getWorkHandler().post(() -> {
                        List<Bitmap> bitmaps = new ArrayList<>();
                        StringBuilder fullPathBuilder = new StringBuilder();
                        List<String> memberIds = new ArrayList<>();
                        for (int i = 0; i < Math.min(groupMembers.size(), 9); i++) {
                            memberIds.add(groupMembers.get(i).memberId);
                        }

                        List<UserInfo> userInfos = ChatManager.Instance().getUserInfos(memberIds, groupId);
                        if (userInfos == null) {
                            return;
                        }

                        for (UserInfo info : userInfos) {
                            fullPathBuilder.append(info.portrait);

                            Drawable drawable = null;
                            try {
                                drawable = Glide.with(context).load(info.portrait).placeholder(R.mipmap.avatar_def).submit(60, 60).get();
                            } catch (Exception e) {
                                e.printStackTrace();
                                try {
                                    drawable = Glide.with(context).load(R.mipmap.avatar_def).submit(60, 60).get();
                                } catch (ExecutionException ex) {
                                    ex.printStackTrace();
                                } catch (InterruptedException ex) {
                                    ex.printStackTrace();
                                }
                            }
                            if (drawable instanceof BitmapDrawable) {
                                bitmaps.add(((BitmapDrawable) drawable).getBitmap());
                            }
                        }

                        Bitmap bitmap = CombineBitmapTools.combimeBitmap(context, 60, 60, bitmaps);
                        if (bitmap == null) {
                            return;
                        }

                        String hash = getDigest(fullPathBuilder.toString());
                        //Path 格式为 groupId-updatetime-width-hash
                        String fileName = groupId + "-" + System.currentTimeMillis() + "-" + width + "-" + hash;
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
                            String key = "wfc_group_generated_portrait_" + groupId + "_" + width;
                            sp.edit().putString(key, f.getAbsolutePath()).apply();

                            //Todo notify
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                }

                @Override
                public void onFail(int errorCode) {

                }
            });
        });
    }

    public static String getGroupGridPortrait(Context context, String groupId, int width) {
        SharedPreferences sp = context.getSharedPreferences("wfc", Context.MODE_PRIVATE);
        String path = sp.getString("wfc_group_generated_portrait_" + groupId + "_" + width, null);
        if (TextUtils.isEmpty(path)) {
            generateNewGroupPortrait(context, groupId, width);
        } else {
            File file = new File(path);
            if (file.exists()) {
                ChatManager.Instance().getGroupInfo(groupId, false, new GetGroupInfoCallback() {
                    @Override
                    public void onSuccess(GroupInfo groupInfo) {
                        //分析文件名，获取更新时间，hash值
                        //Path 格式为 groupId-updatetime-width-hash
                        String name = file.getName();
                        if (!name.startsWith(groupId) || name.length() <= groupId.length()) {
                            return;
                        }
                        name = name.substring(groupId.length() + 1);
                        String[] arr = name.split("-");
                        if (arr.length != 3) {
                            return;
                        }
                        long timestamp;
                        try {
                            timestamp = Long.parseLong(arr[0]);
                        } catch (Exception e) {
                            e.printStackTrace();
                            return;
                        }

                        long now = System.currentTimeMillis();
                        if (now - timestamp > 7 * 24 * 3600 * 1000 || timestamp < groupInfo.updateDt) {
                            ChatManager.Instance().getGroupMembers(groupId, false, new GetGroupMembersCallback() {
                                @Override
                                public void onSuccess(List<GroupMember> groupMembers) {
                                    if (groupMembers == null || groupMembers.size() == 0) {
                                        return;
                                    }

                                    StringBuilder fullPathBuilder = new StringBuilder();
                                    List<String> memberIds = new ArrayList<>();
                                    for (int i = 0; i < Math.min(groupMembers.size(), 9); i++) {
                                        memberIds.add(groupMembers.get(i).memberId);
                                    }
                                    List<UserInfo> userInfos = ChatManager.Instance().getUserInfos(memberIds, groupId);
                                    for (UserInfo info : userInfos) {
                                        fullPathBuilder.append(info.portrait);
                                    }

                                    String fullPath = fullPathBuilder.toString();

                                    if (!arr[2].equals(getDigest(fullPath))) {
                                        generateNewGroupPortrait(context, groupId, width);
                                    }
                                }

                                @Override
                                public void onFail(int errorCode) {

                                }
                            });
                        }
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

    /**
     * 图片入系统相册
     */
    public static void saveMedia2Album(Context context, File mediaFile, boolean isImage) {
        ContentResolver contentResolver = context.getContentResolver();
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, System.currentTimeMillis() + "-" + mediaFile.getName());
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, isImage ? "image/jpg" : "video/mp4");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 1);
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
            Uri uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
            try (FileOutputStream fos = (FileOutputStream) contentResolver.openOutputStream(uri);
                 FileInputStream fis = new FileInputStream(mediaFile)
            ) {
                fos.getChannel().transferFrom(fis.getChannel(), 0, mediaFile.length());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            contentValues.clear();
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0);
            contentResolver.update(uri, contentValues, null, null);
        } else {
            Uri uri = contentResolver.insert(isImage ? MediaStore.Images.Media.EXTERNAL_CONTENT_URI : MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues);
            try (OutputStream os = contentResolver.openOutputStream(uri);
                 FileInputStream fis = new FileInputStream(mediaFile)
            ) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = fis.read(buffer)) > 0) {
                    os.write(buffer, 0, length);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
        }
    }

    public static int[] scaleDown(int width, int height, int maxWidth, int maxHeight) {
        int[] size = new int[2];
        if (width < maxWidth && height < maxHeight) {
            size[0] = width;
            size[1] = height;
            return size;
        }

        float widthRatio = (float) maxWidth / width;
        float heightRatio = (float) maxHeight / height;

        float scale = Math.min(widthRatio, heightRatio);

        float scaledWidth = width * scale;
        float scaledHeight = height * scale;
        size[0] = (int) Math.ceil(scaledWidth);
        size[1] = (int) Math.ceil(scaledHeight);

        return size;
    }

}
