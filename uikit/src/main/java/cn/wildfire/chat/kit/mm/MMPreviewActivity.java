/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.mm;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.github.chrisbanes.photoview.PhotoView;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.GlideApp;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.third.utils.UIUtils;
import cn.wildfire.chat.kit.utils.DownloadManager;
import cn.wildfirechat.message.ImageMessageContent;
import cn.wildfirechat.message.VideoMessageContent;

/**
 * @author imndx
 */
public class MMPreviewActivity extends Activity {
    private SparseArray<View> views;
    private View currentVideoView;
    private ViewPager viewPager;
    private MMPagerAdapter adapter;

    private static int currentPosition = -1;
    private static List<MediaEntry> entries;
    private boolean pendingPreviewInitialMedia;

    private class MMPagerAdapter extends PagerAdapter {
        private List<MediaEntry> entries;

        public MMPagerAdapter(List<MediaEntry> entries) {
            this.entries = entries;
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            View view;
            MediaEntry entry = entries.get(position);
            if (entry.getType() == MediaEntry.TYPE_IMAGE) {
                view = LayoutInflater.from(MMPreviewActivity.this).inflate(R.layout.preview_photo, null);
            } else {
                view = LayoutInflater.from(MMPreviewActivity.this).inflate(R.layout.preview_video, null);
            }

            container.addView(view);
            views.put(position % 3, view);
            if (pendingPreviewInitialMedia) {
                preview(view, entry);
            }
            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, @NonNull Object object) {
            // do nothing ?
            container.removeView((View) object);
        }

        @Override
        public int getCount() {
            return entries == null ? 0 : entries.size();
        }

        public MediaEntry getEntry(int position) {
            return entries.get(position);
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }
    }

    final ViewPager.OnPageChangeListener pageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            // TODO 可以在此控制透明度
        }

        @Override
        public void onPageSelected(int position) {
            View view = views.get(position % 3);
            if (view == null) {
                // pending layout
                return;
            }
            if (currentVideoView != null) {
                resetVideoView(currentVideoView);
                currentVideoView = null;
            }
            MediaEntry entry = adapter.getEntry(position);
            preview(view, entry);
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    };

    private void preview(View view, MediaEntry message) {
        if (message.getType() == MediaEntry.TYPE_IMAGE) {
            previewImage(view, message);
        } else {
            previewVideo(view, message);
        }
    }

    private void resetVideoView(View view) {
        PhotoView photoView = view.findViewById(R.id.photoView);
        ProgressBar loadingProgressBar = view.findViewById(R.id.loading);
        ImageView playButton = view.findViewById(R.id.btnVideo);
        VideoView videoView = view.findViewById(R.id.videoView);

        photoView.setVisibility(View.VISIBLE);
        loadingProgressBar.setVisibility(View.GONE);
        playButton.setVisibility(View.VISIBLE);
        videoView.stopPlayback();
        videoView.setVisibility(View.INVISIBLE);
    }

    private void previewVideo(View view, MediaEntry entry) {

        PhotoView photoView = view.findViewById(R.id.photoView);
        ImageView saveImageView = view.findViewById(R.id.saveImageView);
        saveImageView.setVisibility(View.GONE);
        if (entry.getThumbnail() != null) {
            GlideApp.with(photoView).load(entry.getThumbnail()).into(photoView);
        } else {
            GlideApp.with(photoView).load(entry.getThumbnailUrl()).into(photoView);
        }

        VideoView videoView = view.findViewById(R.id.videoView);
        videoView.setVisibility(View.INVISIBLE);

        ProgressBar loadingProgressBar = view.findViewById(R.id.loading);
        loadingProgressBar.setVisibility(View.GONE);

        ImageView btn = view.findViewById(R.id.btnVideo);
        btn.setVisibility(View.VISIBLE);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(entry.getMediaUrl())) {
                    return;
                }
                btn.setVisibility(View.GONE);
                if (TextUtils.isEmpty(entry.getMediaLocalPath())) {
                    String name = DownloadManager.md5(entry.getMediaUrl());
                    File videoFile = new File(Config.VIDEO_SAVE_DIR, name);
                    if (!videoFile.exists()) {
                        view.setTag(name);
                        ProgressBar loadingProgressBar = view.findViewById(R.id.loading);
                        loadingProgressBar.setVisibility(View.VISIBLE);
                        final WeakReference<View> viewWeakReference = new WeakReference<>(view);
                        DownloadManager.download(entry.getMediaUrl(), Config.VIDEO_SAVE_DIR, name, new DownloadManager.OnDownloadListener() {
                            @Override
                            public void onSuccess(File file) {
                                UIUtils.postTaskSafely(() -> {
                                    View targetView = viewWeakReference.get();
                                    if (targetView != null && name.equals(targetView.getTag())) {
                                        targetView.findViewById(R.id.loading).setVisibility(View.GONE);
                                        playVideo(targetView, file.getAbsolutePath());
                                    }
                                });
                            }

                            @Override
                            public void onProgress(int progress) {
                                // TODO update progress
                                Log.e(MMPreviewActivity.class.getSimpleName(), "video downloading progress: " + progress);
                            }

                            @Override
                            public void onFail() {
                                View targetView = viewWeakReference.get();
                                UIUtils.postTaskSafely(() -> {
                                    if (targetView != null && name.equals(targetView.getTag())) {
                                        targetView.findViewById(R.id.loading).setVisibility(View.GONE);
                                        targetView.findViewById(R.id.btnVideo).setVisibility(View.VISIBLE);
                                    }
                                });
                            }
                        });
                    } else {
                        playVideo(view, videoFile.getAbsolutePath());
                    }
                } else {
                    playVideo(view, entry.getMediaLocalPath());
                }
            }
        });
    }

    private void playVideo(View view, String videoUrl) {
        VideoView videoView = view.findViewById(R.id.videoView);
        videoView.setVisibility(View.INVISIBLE);

        PhotoView photoView = view.findViewById(R.id.photoView);
        photoView.setVisibility(View.GONE);

        ImageView btn = view.findViewById(R.id.btnVideo);
        btn.setVisibility(View.GONE);

        ProgressBar loadingProgressBar = view.findViewById(R.id.loading);
        loadingProgressBar.setVisibility(View.GONE);
        view.findViewById(R.id.loading).setVisibility(View.GONE);
        currentVideoView = view;

        videoView.setVisibility(View.VISIBLE);
        videoView.setVideoPath(videoUrl);
        videoView.setOnErrorListener((mp, what, extra) -> {
            Toast.makeText(MMPreviewActivity.this, "play error", Toast.LENGTH_SHORT).show();
            resetVideoView(view);
            return true;
        });
        videoView.setOnCompletionListener(mp -> resetVideoView(view));
        videoView.start();

    }

    private void previewImage(View view, MediaEntry entry) {
        PhotoView photoView = view.findViewById(R.id.photoView);
        ImageView saveImageView = view.findViewById(R.id.saveImageView);

        String mediaUrl = entry.getMediaUrl();
        if (TextUtils.isEmpty(entry.getMediaLocalPath()) && !TextUtils.isEmpty(mediaUrl)) {
            String imageFileName = DownloadManager.md5(mediaUrl) + mediaUrl.substring(mediaUrl.lastIndexOf('.'));
            File file = new File(Config.PHOTO_SAVE_DIR, imageFileName);
            if (file.exists()) {
                saveImageView.setVisibility(View.GONE);
            } else {
                saveImageView.setVisibility(View.VISIBLE);
                saveImageView.setOnClickListener(v -> {
                    saveImageView.setVisibility(View.GONE);
                    DownloadManager.download(entry.getMediaUrl(), Config.PHOTO_SAVE_DIR, imageFileName, new DownloadManager.SimpleOnDownloadListener() {
                        @Override
                        public void onUiSuccess(File file1) {
                            if (isFinishing()) {
                                return;
                            }
                            Toast.makeText(MMPreviewActivity.this, "图片保存成功", Toast.LENGTH_LONG).show();
                        }
                    });
                });
            }
        } else {
            saveImageView.setVisibility(View.GONE);
        }

        if (entry.getThumbnail() != null) {
            GlideApp.with(MMPreviewActivity.this).load(entry.getMediaUrl())
                .placeholder(new BitmapDrawable(getResources(), entry.getThumbnail()))
                .into(photoView);
        } else {
            GlideApp.with(MMPreviewActivity.this).load(entry.getMediaUrl())
                .placeholder(new BitmapDrawable(getResources(), entry.getThumbnailUrl()))
                .into(photoView);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mm_preview);
        views = new SparseArray<>(3);
        viewPager = findViewById(R.id.viewPager);
        adapter = new MMPagerAdapter(entries);
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(1);
        viewPager.addOnPageChangeListener(pageChangeListener);
        if (currentPosition == 0) {
            viewPager.post(() -> {
                pageChangeListener.onPageSelected(0);
            });
        } else {
            viewPager.setCurrentItem(currentPosition);
            pendingPreviewInitialMedia = true;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        entries = null;
    }

    public static void previewMedia(Context context, List<MediaEntry> entries, int current) {
        if (entries == null || entries.isEmpty()) {
            Log.w(MMPreviewActivity.class.getSimpleName(), "message is null or empty");
            return;
        }
        MMPreviewActivity.entries = entries;
        MMPreviewActivity.currentPosition = current;
        Intent intent = new Intent(context, MMPreviewActivity.class);
        context.startActivity(intent);
    }

    public static void previewImage(Context context, ImageMessageContent imageMessageContent) {
        List<MediaEntry> entries = new ArrayList<>();

        MediaEntry entry = new MediaEntry();
        entry.setType(MediaEntry.TYPE_IMAGE);
        entry.setThumbnail(imageMessageContent.getThumbnail());
        entry.setMediaUrl(imageMessageContent.remoteUrl);
        entry.setMediaLocalPath(imageMessageContent.localPath);
        entries.add(entry);
        previewMedia(context, entries, 0);
    }

    public static void previewImage(Context context, String imageUrl) {
        List<MediaEntry> entries = new ArrayList<>();

        MediaEntry entry = new MediaEntry();
        entry.setType(MediaEntry.TYPE_IMAGE);
        entry.setMediaUrl(imageUrl);
        entries.add(entry);
        previewMedia(context, entries, 0);
    }

    public static void previewVideo(Context context, VideoMessageContent videoMessageContent) {
        List<MediaEntry> entries = new ArrayList<>();

        MediaEntry entry = new MediaEntry();
        entry.setType(MediaEntry.TYPE_VIDEO);
        entry.setThumbnail(videoMessageContent.getThumbnail());
        entry.setMediaUrl(videoMessageContent.remoteUrl);
        entry.setMediaLocalPath(videoMessageContent.localPath);
        entries.add(entry);
        previewMedia(context, entries, 0);
    }


    public static void previewVideo(Context context, String videoUrl) {
        List<MediaEntry> entries = new ArrayList<>();

        MediaEntry entry = new MediaEntry();
        entry.setType(MediaEntry.TYPE_VIDEO);
//        entry.setThumbnail(videoMessageContent.getThumbnail());
        entry.setMediaUrl(videoUrl);
//        entry.setMediaLocalPath(videoMessageContent.localPath);
        entries.add(entry);
        previewMedia(context, entries, 0);
    }
}
