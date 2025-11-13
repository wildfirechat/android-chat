package com.lqr.imagepicker;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;

import androidx.fragment.app.FragmentActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import com.lqr.imagepicker.bean.ImageFolder;
import com.lqr.imagepicker.bean.ImageItem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ImageDataSource implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final int LOADER_ALL = 0;         //加载所有图片
    public static final int LOADER_CATEGORY = 1;    //分类加载图片
    private final String[] IMAGE_PROJECTION = {     //查询图片需要的数据列
        MediaStore.Files.FileColumns.DISPLAY_NAME,   //图片的显示名称  aaa.jpg
        MediaStore.Files.FileColumns.DATA,           //图片的真实路径  /storage/emulated/0/pp/downloader/wallpaper/aaa.jpg
        MediaStore.Files.FileColumns.SIZE,           //图片的大小，long型  132492
        MediaStore.Files.FileColumns.WIDTH,          //图片的宽度，int型  1920
        MediaStore.Files.FileColumns.HEIGHT,         //图片的高度，int型  1080
        MediaStore.Files.FileColumns.MIME_TYPE,      //图片的类型     image/jpeg
        MediaStore.Files.FileColumns.DATE_ADDED,
        MediaStore.Files.FileColumns.DURATION       //视频持续时间
    };    //图片被添加的时间，long型  1450518608

    private final FragmentActivity activity;
    private final OnImageLoadListener loadedListener;                     //图片加载完成的回调接口
    private final ArrayList<ImageFolder> imageFolders = new ArrayList<>();   //所有的图片文件夹

    private boolean enableVideo;
    private final int loaderId;
    private final Bundle loaderArgs;

    /**
     * @param activity       用于初始化LoaderManager，需要兼容到2.3
     * @param path           指定扫描的文件夹目录，可以为 null，表示扫描所有图片
     * @param loadedListener 图片加载完成的监听
     */
    public ImageDataSource(FragmentActivity activity, String path, boolean enableVideo, OnImageLoadListener loadedListener) {
        this.activity = activity;
        this.enableVideo = enableVideo;
        this.loadedListener = loadedListener;

        LoaderManager loaderManager = LoaderManager.getInstance(activity);
        if (path == null) {
            this.loaderId = LOADER_ALL;
            loaderManager.initLoader(LOADER_ALL, null, this);//加载所有的图片
            this.loaderArgs = null;
        } else {
            //加载指定目录的图片
            Bundle bundle = new Bundle();
            bundle.putString("path", path);
            this.loaderArgs = bundle;
            this.loaderId = LOADER_CATEGORY;
            loaderManager.initLoader(LOADER_CATEGORY, bundle, this);
        }
    }

    public void refresh() {
        LoaderManager loaderManager = LoaderManager.getInstance(activity);
        loaderManager.restartLoader(this.loaderId, this.loaderArgs, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CursorLoader cursorLoader;
        //扫描所有图片
        String selection;
        String[] selectionArgs;
        Uri uri = MediaStore.Files.getContentUri("external");
        String sortOrder = MediaStore.Files.FileColumns.DATE_ADDED + " DESC";
        if (id == LOADER_ALL) {
            selection = !enableVideo ? MediaStore.Files.FileColumns.MEDIA_TYPE + " = ?" :
                MediaStore.Files.FileColumns.MEDIA_TYPE + " = ?" + " OR " + MediaStore.Files.FileColumns.MEDIA_TYPE + " = ?";
        } else {
            //扫描某个图片文件夹
            selection = !enableVideo ? MediaStore.Files.FileColumns.MEDIA_TYPE + " = ?" :
                "( " + MediaStore.Files.FileColumns.MEDIA_TYPE + " = ?" + " OR " + MediaStore.Files.FileColumns.MEDIA_TYPE + " = ?" + " )";
            selection += " AND " + MediaStore.Files.FileColumns.DATA + " like %?%";
        }
        if (enableVideo) {
            selectionArgs = new String[]{
                String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE),
                String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)
            };
        } else {
            selectionArgs = new String[]{
                String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE),
            };
        }
        cursorLoader = new CursorLoader(activity, uri, IMAGE_PROJECTION, selection, selectionArgs, sortOrder);

        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        imageFolders.clear();
        if (data != null && !data.isAfterLast()) {
            ArrayList<ImageItem> allImages = new ArrayList<>();   //所有图片的集合,不分文件夹
            while (data.moveToNext()) {
                //查询数据
                String imageName = data.getString(data.getColumnIndexOrThrow(IMAGE_PROJECTION[0]));
                String imagePath = data.getString(data.getColumnIndexOrThrow(IMAGE_PROJECTION[1]));
                long imageSize = data.getLong(data.getColumnIndexOrThrow(IMAGE_PROJECTION[2]));
                int imageWidth = data.getInt(data.getColumnIndexOrThrow(IMAGE_PROJECTION[3]));
                int imageHeight = data.getInt(data.getColumnIndexOrThrow(IMAGE_PROJECTION[4]));
                String imageMimeType = data.getString(data.getColumnIndexOrThrow(IMAGE_PROJECTION[5]));
                long imageAddTime = data.getLong(data.getColumnIndexOrThrow(IMAGE_PROJECTION[6]));
                long duration = data.getLong(data.getColumnIndexOrThrow(IMAGE_PROJECTION[7]));
                //封装实体
                ImageItem imageItem = new ImageItem();
                imageItem.name = imageName;
                imageItem.path = imagePath;
                imageItem.size = imageSize;
                imageItem.width = imageWidth;
                imageItem.height = imageHeight;
                imageItem.mimeType = imageMimeType;
                imageItem.createTime = imageAddTime;
                imageItem.duration = duration;
                allImages.add(imageItem);
                //根据父路径分类存放图片
                File imageFile = new File(imagePath);
                File imageParentFile = imageFile.getParentFile();
                ImageFolder imageFolder = new ImageFolder();
                imageFolder.name = imageParentFile.getName();
                imageFolder.path = imageParentFile.getAbsolutePath();

                if (!imageFolders.contains(imageFolder)) {
                    ArrayList<ImageItem> images = new ArrayList<>();
                    images.add(imageItem);
                    imageFolder.cover = imageItem;
                    imageFolder.images = images;
                    imageFolders.add(imageFolder);
                } else {
                    imageFolders.get(imageFolders.indexOf(imageFolder)).images.add(imageItem);
                }
            }
            //防止没有图片报异常
            if (data.getCount() > 0) {
                //构造所有图片的集合
                ImageFolder allImagesFolder = new ImageFolder();
                allImagesFolder.name = activity.getResources().getString(R.string.all_images);
                allImagesFolder.path = "/";
                allImagesFolder.cover = allImages.get(0);
                allImagesFolder.images = allImages;
                imageFolders.add(0, allImagesFolder);  //确保第一条是所有图片
            }
        }

        //回调接口，通知图片数据准备完成
        loadedListener.onImageLoad(imageFolders);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        System.out.println("--------");
    }

    /**
     * 所有图片加载完成的回调接口
     */
    public interface OnImageLoadListener {
        void onImageLoad(List<ImageFolder> imageFolders);
    }
}
