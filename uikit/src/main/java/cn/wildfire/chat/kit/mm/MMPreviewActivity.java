/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.mm;

import static cn.wildfire.chat.kit.mm.MediaEntry.TYPE_VIDEO;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.zxing.Result;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcScheme;
import cn.wildfire.chat.kit.conversation.forward.ForwardActivity;
import cn.wildfire.chat.kit.qrcode.QRCodeHelper;
import cn.wildfire.chat.kit.third.utils.ImageUtils;
import cn.wildfire.chat.kit.third.utils.UIUtils;
import cn.wildfire.chat.kit.utils.DownloadManager;
import cn.wildfire.chat.kit.voip.ZoomableFrameLayout;
import cn.wildfire.chat.kit.widget.PhotoView;
import cn.wildfirechat.message.ImageMessageContent;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.VideoMessageContent;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.uikit.permission.PermissionKit;
import me.aurelion.x.ui.view.watermark.WaterMarkManager;
import me.aurelion.x.ui.view.watermark.WaterMarkView;

/**
 * @author imndx
 */
public class MMPreviewActivity extends AppCompatActivity implements PhotoView.OnDragListener, ZoomableFrameLayout.OnDragListener {
    private SparseArray<View> views;
    private View currentVideoView;
    private ProgressBar videoLoadProgressBar;
    private ImageView videoPlayButton;

    private PhotoView currentPhotoView;
    private ViewPager viewPager;
    private MMPagerAdapter adapter;
    private boolean secret;
    private DiskCacheStrategy diskCacheStrategy = DiskCacheStrategy.AUTOMATIC;

    private static int currentPosition = -1;
    private static List<MediaEntry> entries;
    private boolean pendingPreviewInitialMedia;

    protected WaterMarkView mWmv;

    public static final String TAG = "MMPreviewActivity";

    // 长按菜单和二维码识别
    private volatile int recognitionPosition = -1; // 标记当前识别的图片位置


    @Override
    public void onDragToFinish() {
        if (currentPhotoView == null) {
            finish();
            overridePendingTransition(0, 0);
            return;
        }
        finish();
        overridePendingTransition(0, R.anim.fade_out);
    }

    @Override
    public void onDragOffset(float offset, float maxOffset) {
        View view = findViewById(R.id.bgMaskView);
        float alpha = 1 - offset / maxOffset;
        view.setAlpha(Math.max(alpha, 0.2f));

        if (videoPlayButton != null) {
            videoPlayButton.setVisibility(offset != 0.0 ? View.GONE : View.VISIBLE);
        }
        if (videoLoadProgressBar != null && offset != 0.0) {
            videoLoadProgressBar.setVisibility(View.GONE);
        }
    }

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
            // 取消之前的二维码识别任务
            recognitionPosition = -1;

            View view = views.get(position % 3);
            if (view == null) {
                // pending layout
                return;
            }
            if (currentVideoView != null) {
                resetVideoView(currentVideoView);
                currentVideoView = null;
                videoPlayButton = null;
                videoLoadProgressBar = null;
            }
            if (currentPhotoView != null) {
                currentPhotoView.resetScale();
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
        // 朋友圈
        if (message.getMessage() != null) {
            ChatManager.Instance().setMediaMessagePlayed(message.getMessage().messageId);
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
        photoView.setOnDragListener(this);
        currentPhotoView = photoView;

        ImageView saveImageView = view.findViewById(R.id.saveImageView);
        ZoomableFrameLayout zoomableFrameLayout = view.findViewById(R.id.zoomableFrameLayout);
        zoomableFrameLayout.setEnableZoom(true);
        zoomableFrameLayout.setEnableDrag(true);
        zoomableFrameLayout.setOnDragListener(this);
        saveImageView.setVisibility(View.GONE);

        if (entry.getThumbnail() != null) {
            Glide.with(photoView).load(entry.getThumbnail()).diskCacheStrategy(diskCacheStrategy).into(photoView);
        } else {
            Glide.with(photoView).load(entry.getThumbnailUrl()).diskCacheStrategy(diskCacheStrategy).into(photoView);
        }

        VideoView videoView = view.findViewById(R.id.videoView);
        videoView.setVisibility(View.INVISIBLE);

        videoLoadProgressBar = view.findViewById(R.id.loading);
        videoLoadProgressBar.setVisibility(View.GONE);

        videoPlayButton = view.findViewById(R.id.btnVideo);
        videoPlayButton.setVisibility(View.VISIBLE);


        videoPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                videoPlayButton.setVisibility(View.GONE);
                if (TextUtils.isEmpty(entry.getMediaLocalPath())) {
                    File videoFile;
                    if (entry.getMessage() != null) {
                        videoFile = DownloadManager.mediaMessageContentFile(entry.getMessage());
                    } else {
                        String name = DownloadManager.getNameFromUrl(entry.getMediaUrl());
                        name = TextUtils.isEmpty(name) ? System.currentTimeMillis() + "" : name;
                        videoFile = new File(Config.VIDEO_SAVE_DIR, name);
                    }
                    if (videoFile == null) {
                        return;
                    }
                    if (!videoFile.exists() || secret) {
                        String tag = System.currentTimeMillis() + "";
                        view.setTag(tag);
                        ProgressBar loadingProgressBar = view.findViewById(R.id.loading);
                        loadingProgressBar.setVisibility(View.VISIBLE);
                        final WeakReference<View> viewWeakReference = new WeakReference<>(view);
                        DownloadManager.download(entry.getMediaUrl(), videoFile.getParent(), videoFile.getName(), new DownloadManager.OnDownloadListener() {
                            @Override
                            public void onSuccess(File file) {
                                UIUtils.postTaskSafely(() -> {
                                    View targetView = viewWeakReference.get();
                                    if (targetView != null && tag.equals(targetView.getTag())) {
                                        targetView.findViewById(R.id.loading).setVisibility(View.GONE);
                                        playVideo(targetView, file.getAbsolutePath());
                                    }
                                    saveMedia2Album(file, false);
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
                                    if (targetView != null && tag.equals(targetView.getTag())) {
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
        videoView.setOnCompletionListener(mp -> {
            resetVideoView(view);
        });
        videoView.start();

    }

    private void previewImage(View view, MediaEntry entry) {
        PhotoView photoView = view.findViewById(R.id.photoView);
        photoView.setOnDragListener(this);
        currentPhotoView = photoView;
        ImageView saveImageView = view.findViewById(R.id.saveImageView);

        String mediaUrl = entry.getMediaUrl();
        if (TextUtils.isEmpty(entry.getMediaLocalPath()) && !TextUtils.isEmpty(mediaUrl)) {
            if (secret) {
                saveImageView.setVisibility(View.GONE);
            } else {
                saveImageView.setVisibility(View.VISIBLE);
                saveImageView.setOnClickListener(v -> {
                    Toast.makeText(this, getString(R.string.saving_image), Toast.LENGTH_SHORT).show();
                    downloadMediaFile(entry, file -> {
                        if (file != null) {
                            saveMedia2Album(file, true);
                        } else {
                            Toast.makeText(MMPreviewActivity.this, getString(R.string.image_save_failed_null_file), Toast.LENGTH_LONG).show();
                        }
                        return null;
                    });
                });
            }
        } else {
            saveImageView.setVisibility(View.GONE);
        }

        if (entry.getThumbnail() != null) {
            Glide.with(MMPreviewActivity.this).load(entry.getMediaUrl()).diskCacheStrategy(diskCacheStrategy)
                .placeholder(new BitmapDrawable(getResources(), entry.getThumbnail()))
                .into(photoView);
        } else {
            Glide.with(MMPreviewActivity.this).load(entry.getMediaUrl()).diskCacheStrategy(diskCacheStrategy)
                .placeholder(new BitmapDrawable(getResources(), entry.getThumbnailUrl()))
                .into(photoView);
        }

        // 添加长按监听器
        photoView.setOnLongClickListener(v -> {
            showPopupMenu();
            return true;
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mm_preview);
        supportPostponeEnterTransition();

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
        secret = getIntent().getBooleanExtra("secret", false);
        diskCacheStrategy = secret ? DiskCacheStrategy.NONE : DiskCacheStrategy.AUTOMATIC;

        if (Config.ENABLE_WATER_MARK) {
            mWmv = WaterMarkManager.getView(this);
            ((ViewGroup) findViewById(android.R.id.content)).addView(mWmv);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (currentVideoView != null) {
            resetVideoView(currentVideoView);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mWmv != null) {
            mWmv.onDestroy();
        }
        if (secret) {
            for (MediaEntry entry : entries) {
                if (entry.getType() == TYPE_VIDEO) {
                    File secretVideoFile = DownloadManager.mediaMessageContentFile(entry.getMessage());
                    if (secretVideoFile.exists()) {
                        secretVideoFile.delete();
                    }
                }
            }
        }
        entries = null;
    }

    private void saveMedia2Album(File file, boolean isImage) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT <= Build.VERSION_CODES.TIRAMISU) {
            String[] permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
            PermissionKit.PermissionReqTuple[] tuples = PermissionKit.buildRequestPermissionTuples(this, permissions);
            PermissionKit.checkThenRequestPermission(this, getSupportFragmentManager(), tuples, o -> {
                ImageUtils.saveMedia2Album(this, file, isImage);
                Toast.makeText(MMPreviewActivity.this, getString(R.string.image_save_success), Toast.LENGTH_LONG).show();
            });

        } else {
            ImageUtils.saveMedia2Album(this, file, isImage);
            Toast.makeText(MMPreviewActivity.this, getString(R.string.image_save_success), Toast.LENGTH_LONG).show();
        }
    }

    private void showPopupMenu() {
        List<String> menuItems = new ArrayList<>();
        menuItems.add(getString(R.string.message_forward));

        MediaEntry currentEntry = adapter.getEntry(viewPager.getCurrentItem());
        if (currentEntry.getType() == MediaEntry.TYPE_IMAGE) {
            menuItems.add(getString(R.string.recognize_qrcode));
        }

        if (menuItems.isEmpty()) {
            return;
        }

        new MaterialDialog.Builder(this)
            .items(menuItems)
            .itemsCallback((dialog, view, which, text) -> {
                if (which == 0) {
                    handleForward();
                } else if (which == 1) {
                    handleQRCodeRecognition();
                }
            })
            .show();
    }

    /**
     * 处理转发功能
     */
    private void handleForward() {
        MediaEntry entry = adapter.getEntry(viewPager.getCurrentItem());
        Toast.makeText(this, "下载中", Toast.LENGTH_SHORT).show();
        downloadMediaFile(entry, file -> {
            if (file == null) {
                return null;
            }
            // 启动转发界面
            Intent intent = new Intent(MMPreviewActivity.this, ForwardActivity.class);
            Message message = new Message();
            message.content = new ImageMessageContent(file.getAbsolutePath());
            ((ImageMessageContent) message.content).remoteUrl = entry.getMediaUrl();
            intent.putExtra("message", message);
            startActivity(intent);
            return null;
        });

    }

    private void downloadMediaFile(MediaEntry entry, Function<File, Void> function) {
        File file;
        if (entry.getMessage() != null) {
            file = DownloadManager.mediaMessageContentFile(entry.getMessage());
        } else {
            String name = DownloadManager.getNameFromUrl(entry.getMediaUrl());
            name = TextUtils.isEmpty(name) ? System.currentTimeMillis() + "" : name;
            file = new File(Config.FILE_SAVE_DIR, name);
        }
        if (file == null) {
            Toast.makeText(MMPreviewActivity.this, getString(R.string.image_save_failed_null_file), Toast.LENGTH_LONG).show();
            function.apply(null);
            return;
        }

        if (file.exists()) {
            function.apply(file);
        } else {
            DownloadManager.download(entry.getMediaUrl(), file.getParent(), file.getName(), new DownloadManager.SimpleOnDownloadListener() {
                @Override
                public void onUiSuccess(File file1) {
                    if (isFinishing()) {
                        function.apply(file1);
                    }
                }
            });
        }
    }

    /**
     * 处理二维码识别
     */
    private void handleQRCodeRecognition() {
        MediaEntry entry = adapter.getEntry(viewPager.getCurrentItem());

        if (entry.getType() != MediaEntry.TYPE_IMAGE) {
            return;
        }

        downloadMediaFile(entry, file -> {
            if (file == null) {
                Toast.makeText(MMPreviewActivity.this, R.string.image_file_not_exist, Toast.LENGTH_SHORT).show();
            } else {
                recognitionPosition = viewPager.getCurrentItem();

                // 显示加载对话框
                MaterialDialog loadingDialog = new MaterialDialog.Builder(MMPreviewActivity.this)
                    .content(R.string.recognizing_qrcode)
                    .progress(true, 0)
                    .cancelable(true)
                    .build();
                loadingDialog.show();

                // 后台线程识别二维码
                new Thread(() -> {
                    Result result = QRCodeHelper.decodeQR(file.getAbsolutePath());

                    runOnUiThread(() -> {
                        loadingDialog.dismiss();

                        // 检查用户是否已滑动到其他图片
                        if (recognitionPosition != viewPager.getCurrentItem()) {
                            return; // 用户已切换，忽略结果
                        }

                        if (result != null && !TextUtils.isEmpty(result.getText())) {
                            WfcScheme.handleQRCodeResult(MMPreviewActivity.this,result.getText());
                        } else {
                            Toast.makeText(MMPreviewActivity.this, R.string.qr_code_recognition_failed, Toast.LENGTH_SHORT).show();
                        }
                    });
                }).start();
            }
            return null;
        });
    }

    public static void previewMedia(Context context, List<MediaEntry> entries, int current) {
        previewMedia(context, entries, current, false);
    }

    public static void previewMedia(Context context, List<MediaEntry> entries, int current, boolean secret) {
        if (entries == null || entries.isEmpty()) {
            Log.w(MMPreviewActivity.class.getSimpleName(), "message is null or empty");
            return;
        }
        MMPreviewActivity.entries = entries;
        MMPreviewActivity.currentPosition = current;
        Intent intent = new Intent(context, MMPreviewActivity.class);
        intent.putExtra("secret", secret);
        context.startActivity(intent);
    }

    public static void previewImage(Context context, Message message) {
        if (!(message.content instanceof ImageMessageContent)) {
            Log.e(TAG, "previewImage without imageMessageContent");
            return;
        }

        List<MediaEntry> entries = new ArrayList<>();
        MediaEntry entry = new MediaEntry(message);
        entries.add(entry);
        previewMedia(context, entries, 0, false);
    }

    public static void previewImage(Context context, String imageUrl) {
        List<MediaEntry> entries = new ArrayList<>();

        MediaEntry entry = new MediaEntry();
        entry.setType(MediaEntry.TYPE_IMAGE);
        entry.setMediaUrl(imageUrl);
        entries.add(entry);
        previewMedia(context, entries, 0, false);
    }

    public static void previewVideo(Context context, Message message) {
        if (!(message.content instanceof VideoMessageContent)) {
            Log.e(TAG, "previewVideo without videoMessageContent");
            return;
        }
        List<MediaEntry> entries = new ArrayList<>();

        MediaEntry entry = new MediaEntry(message);
        entries.add(entry);
        previewMedia(context, entries, 0, false);
    }
}
