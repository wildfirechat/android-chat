/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.mm;

import static cn.wildfire.chat.kit.mm.MediaEntry.TYPE_VIDEO;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
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
import cn.wildfire.chat.kit.widget.OnDragToFinishListener;
import cn.wildfire.chat.kit.widget.ZoomableFrameLayout;
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
public class MMPreviewActivity extends AppCompatActivity implements OnDragToFinishListener {
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

    /**
     * Callback that looks up the on-screen rect of a media entry's thumbnail
     * in the conversation list at dismiss time. Returns null if the view is
     * not currently visible.
     */
    public interface SourceRectProvider {
        Rect getSourceRect(MediaEntry entry);
    }

    // Enter/exit animation state (set by callers before starting the activity)
    static Rect enterSourceRect;          // snapshot rect for the enter animation only
    static Bitmap enterThumbnail;
    static SourceRectProvider enterSourceRectProvider; // dynamic lookup for exit animation

    // Instance copies of animation state (consumed from static on create)
    private Rect instanceEnterSourceRect;  // used only for enter animation
    private Bitmap instanceEnterThumbnail;
    private SourceRectProvider instanceSourceRectProvider; // used for exit animation
    private int initialPosition;
    private ImageView animOverlayView;
    private View bgMaskView;

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
    public void onDragToFinishWithCurrentViewRect(RectF currentRect) {
        if (currentRect == null) {
            onDragToFinish();
            return;
        }
        Rect targetRect = findCurrentEntrySourceRect();
        if (targetRect != null) {
            startExitAnimation(currentRect, targetRect);
        } else {
            onDragToFinish();
        }
    }

    /**
     * Looks up the on-screen rect for the entry currently shown in the ViewPager.
     * Queries the RecyclerView dynamically so it works regardless of which page
     * the user has scrolled to.
     */
    private Rect findCurrentEntrySourceRect() {
        if (entries == null) return null;
        int page = viewPager.getCurrentItem();
        if (page < 0 || page >= entries.size()) return null;
        MediaEntry entry = entries.get(page);
        if (instanceSourceRectProvider != null) {
            return instanceSourceRectProvider.getSourceRect(entry);
        }
        // Fallback: the enter-animation snapshot for the originally clicked item
        if (page == initialPosition && instanceEnterSourceRect != null) {
            return instanceEnterSourceRect;
        }
        return null;
    }

    @Override
    public void onDragOffset(float offset, float maxOffset) {
        View view = findViewById(R.id.bgMaskView);
        float alpha = 1 - Math.abs(offset) / maxOffset;
        view.setAlpha(Math.max(alpha, 0.2f));

        if (videoPlayButton != null) {
            // 拖拽时隐藏播放按钮，回弹完成时显示
            if (Math.abs(offset) == 0.0f) {
                // 已经回弹到位，恢复播放按钮（如果视频未播放）
                if (currentVideoView == null) {
                    videoPlayButton.setVisibility(View.VISIBLE);
                }
            } else {
                videoPlayButton.setVisibility(View.GONE);
            }
        }
        if (videoLoadProgressBar != null && Math.abs(offset) == 0.0f) {
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
            if (pendingPreviewInitialMedia && position == initialPosition) {
                pendingPreviewInitialMedia = false;
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
        currentVideoView = null;
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

        // Consume static enter-animation fields immediately so they are not
        // accidentally re-used if the activity is recreated.
        instanceEnterSourceRect = enterSourceRect;
        instanceEnterThumbnail = enterThumbnail;
        instanceSourceRectProvider = enterSourceRectProvider;
        enterSourceRect = null;
        enterThumbnail = null;
        enterSourceRectProvider = null;
        initialPosition = currentPosition;

        bgMaskView = findViewById(R.id.bgMaskView);

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

        setupAnimOverlay();
        if (instanceEnterSourceRect != null || instanceSourceRectProvider != null) {
            bgMaskView.setAlpha(0f);
            viewPager.setVisibility(View.INVISIBLE);
            // Defer until the decor view has been measured so we know screen dimensions.
            viewPager.post(this::startEnterAnimation);
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
        if (animOverlayView != null) {
            animOverlayView.animate().cancel();
            ViewGroup parent = (ViewGroup) animOverlayView.getParent();
            if (parent != null) {
                parent.removeView(animOverlayView);
            }
            animOverlayView = null;
        }
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

    // -------------------------------------------------------------------------
    // Enter / exit shared-element-style animations
    // -------------------------------------------------------------------------

    private void setupAnimOverlay() {
        // Attach a full-screen ImageView to the decor view so its coordinate
        // space matches getLocationOnScreen() values directly.
        ViewGroup decorView = (ViewGroup) getWindow().getDecorView();
        animOverlayView = new ImageView(this);
        animOverlayView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        animOverlayView.setVisibility(View.GONE);
        decorView.addView(animOverlayView, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ));
    }

    /**
     * Animate the thumbnail from the source thumbnail bounds to fill the screen,
     * then reveal the ViewPager content.
     */
    private void startEnterAnimation() {
        if (instanceEnterSourceRect == null || animOverlayView == null) {
            viewPager.setVisibility(View.VISIBLE);
            bgMaskView.setAlpha(1f);
            return;
        }

        ViewGroup decorView = (ViewGroup) getWindow().getDecorView();
        float screenW = decorView.getWidth();
        float screenH = decorView.getHeight();
        if (screenW == 0 || screenH == 0) {
            viewPager.setVisibility(View.VISIBLE);
            bgMaskView.setAlpha(1f);
            return;
        }

        Rect src = instanceEnterSourceRect;
        float srcCenterX = src.left + src.width() / 2f;
        float srcCenterY = src.top + src.height() / 2f;
        float screenCenterX = screenW / 2f;
        float screenCenterY = screenH / 2f;

        // Position the overlay at the source thumbnail location using scale + translation.
        // The overlay is MATCH_PARENT; pivot defaults to its center.
        animOverlayView.setImageBitmap(instanceEnterThumbnail);
        animOverlayView.setPivotX(screenCenterX);
        animOverlayView.setPivotY(screenCenterY);
        animOverlayView.setScaleX(src.width() / screenW);
        animOverlayView.setScaleY(src.height() / screenH);
        animOverlayView.setTranslationX(srcCenterX - screenCenterX);
        animOverlayView.setTranslationY(srcCenterY - screenCenterY);
        animOverlayView.setVisibility(View.VISIBLE);

        animOverlayView.animate()
            .scaleX(1f).scaleY(1f)
            .translationX(0f).translationY(0f)
            .setDuration(280)
            .setInterpolator(new DecelerateInterpolator(1.5f))
            .setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    animOverlayView.setVisibility(View.GONE);
                    viewPager.setVisibility(View.VISIBLE);
                    // Keep instanceEnterThumbnail alive for use in exit animation
                }
            })
            .start();

        ValueAnimator bgAnim = ValueAnimator.ofFloat(0f, 1f);
        bgAnim.setDuration(280);
        bgAnim.addUpdateListener(a -> bgMaskView.setAlpha((float) a.getAnimatedValue()));
        bgAnim.start();
    }

    /**
     * Animate the currently-displayed image back to the source thumbnail position,
     * fading out the background, then finish the activity.
     *
     * @param fromScreenRect current image rect in screen coordinates (from PhotoView)
     * @param targetRect     destination rect in screen coordinates (the thumbnail in the conversation list)
     */
    private void startExitAnimation(RectF fromScreenRect, Rect targetRect) {
        if (animOverlayView == null) {
            onDragToFinish();
            return;
        }

        ViewGroup decorView = (ViewGroup) getWindow().getDecorView();
        float screenW = decorView.getWidth();
        float screenH = decorView.getHeight();
        if (screenW == 0 || screenH == 0) {
            onDragToFinish();
            return;
        }

        float tgtCenterX = targetRect.left + targetRect.width() / 2f;
        float tgtCenterY = targetRect.top + targetRect.height() / 2f;
        float screenCenterX = screenW / 2f;
        float screenCenterY = screenH / 2f;

        // Grab the best available image for the overlay.
        Bitmap overlayBitmap = null;
        if (currentPhotoView != null) {
            android.graphics.drawable.Drawable d = currentPhotoView.getDrawable();
            if (d instanceof BitmapDrawable) {
                overlayBitmap = ((BitmapDrawable) d).getBitmap();
            }
        }
        if (overlayBitmap == null) overlayBitmap = instanceEnterThumbnail;

        animOverlayView.setImageBitmap(overlayBitmap);
        animOverlayView.setPivotX(screenCenterX);
        animOverlayView.setPivotY(screenCenterY);
        // Start the overlay exactly where the dragged image currently is.
        animOverlayView.setScaleX(fromScreenRect.width() / screenW);
        animOverlayView.setScaleY(fromScreenRect.height() / screenH);
        animOverlayView.setTranslationX(fromScreenRect.centerX() - screenCenterX);
        animOverlayView.setTranslationY(fromScreenRect.centerY() - screenCenterY);
        animOverlayView.setVisibility(View.VISIBLE);

        // Hide the actual pager content (already mostly invisible from drag alpha).
        viewPager.setVisibility(View.INVISIBLE);

        float currentBgAlpha = bgMaskView.getAlpha();
        float targetScaleX = targetRect.width() / screenW;
        float targetScaleY = targetRect.height() / screenH;

        animOverlayView.animate()
            .scaleX(targetScaleX).scaleY(targetScaleY)
            .translationX(tgtCenterX - screenCenterX)
            .translationY(tgtCenterY - screenCenterY)
            .setDuration(250)
            .setInterpolator(new DecelerateInterpolator())
            .setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    finish();
                    overridePendingTransition(0, 0);
                }
            })
            .start();

        ValueAnimator bgAnim = ValueAnimator.ofFloat(currentBgAlpha, 0f);
        bgAnim.setDuration(250);
        bgAnim.addUpdateListener(a -> bgMaskView.setAlpha((float) a.getAnimatedValue()));
        bgAnim.start();
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
        previewMedia(context, entries, current, secret, null, null);
    }

    public static void previewMedia(Context context, List<MediaEntry> entries, int current, boolean secret,
                                    Rect sourceRect, Bitmap thumbnail) {
        previewMedia(context, entries, current, secret, sourceRect, thumbnail, null);
    }

    public static void previewMedia(Context context, List<MediaEntry> entries, int current, boolean secret,
                                    Rect sourceRect, Bitmap thumbnail, SourceRectProvider provider) {
        if (entries == null || entries.isEmpty()) {
            Log.w(MMPreviewActivity.class.getSimpleName(), "message is null or empty");
            return;
        }
        MMPreviewActivity.entries = entries;
        MMPreviewActivity.currentPosition = current;
        MMPreviewActivity.enterSourceRect = sourceRect;
        MMPreviewActivity.enterThumbnail = thumbnail;
        MMPreviewActivity.enterSourceRectProvider = provider;
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
