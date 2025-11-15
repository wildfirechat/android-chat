package com.lqr.imagepicker.ui;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.lqr.imagepicker.ImagePickStore;
import com.lqr.imagepicker.ImagePicker;
import com.lqr.imagepicker.R;
import com.lqr.imagepicker.adapter.ImagePageAdapter;
import com.lqr.imagepicker.bean.ImageItem;
import com.lqr.imagepicker.view.ViewPagerFixed;

import java.util.ArrayList;

public abstract class ImagePreviewBaseActivity extends ImageBaseActivity {

    protected ImagePickStore store;
    protected ArrayList<ImageItem> mImageItems;      //跳转进ImagePreviewFragment的图片文件夹
    protected int mCurrentPosition = 0;              //跳转进ImagePreviewFragment时的序号，第几个图片
    protected TextView mTitleCount;                  //显示当前图片的位置  例如  5/31
    protected ArrayList<ImageItem> selectedImages;   //所有已经选中的图片
    protected View content;
    protected View topBar;
    protected ViewPagerFixed mViewPager;
    protected ImagePageAdapter mAdapter;
    protected int pickLimit;
    protected boolean isBarShowing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        tintManager.setStatusBarTintResource(R.color.transparent);  //设置上方状态栏的颜色

        setContentView(R.layout.activity_image_preview);
        pickLimit = getIntent().getIntExtra("pickLimit", 9);

        store = ImagePickStore.getInstance();
        mCurrentPosition = getIntent().getIntExtra(ImagePicker.EXTRA_SELECTED_IMAGE_POSITION, 0);
        mImageItems = (ArrayList<ImageItem>) getIntent().getSerializableExtra(ImagePicker.EXTRA_IMAGE_ITEMS);
        if (mImageItems == null) {
            mImageItems = store.getCurrentImageFolderItems();
        }

        selectedImages = store.getSelectedImages();

        //初始化控件
        content = findViewById(R.id.content);

        topBar = findViewById(R.id.top_bar);
        topBar.findViewById(R.id.btn_ok).setVisibility(View.GONE);
        topBar.findViewById(R.id.btn_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mTitleCount = (TextView) findViewById(R.id.tv_des);

        mViewPager = (ViewPagerFixed) findViewById(R.id.viewpager);
        mAdapter = new ImagePageAdapter(this, mImageItems);
        mAdapter.setPhotoViewClickListener(new ImagePageAdapter.PhotoViewClickListener() {
            @Override
            public void OnPhotoTapListener(View view, float v, float v1) {
                onImageSingleTap();
            }
        });
        mViewPager.setAdapter(mAdapter);
        mViewPager.setCurrentItem(mCurrentPosition, false);

        //初始化当前页面的状态
        mTitleCount.setText(getString(R.string.preview_image_count, mCurrentPosition + 1, mImageItems.size()));

        showBars(topBar, null);
    }

    /**
     * 单击时，隐藏头和尾
     */
    public abstract void onImageSingleTap();

    /**
     * 平滑显示标题栏和底部栏
     */
    protected void showBars(View topBar, View bottomBar) {
        if (isBarShowing) return;
        isBarShowing = true;

        // 标题栏从上方滑入（操作容器）
        tintManager.setStatusBarTintResource(R.color.status_bar);  //设置上方状态栏的颜色

        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) topBar.getLayoutParams();
        lp.topMargin = tintManager.getConfig().getStatusBarHeight();
        topBar.setLayoutParams(lp);
        topBar.setVisibility(View.VISIBLE);
        topBar.animate()
            .translationY(0)
            .setDuration(250)
            .start();

        // 底部栏淡入
        if (bottomBar != null) {
            bottomBar.setVisibility(View.VISIBLE);
            bottomBar.setAlpha(0f);
            bottomBar.animate()
                .alpha(1f)
                .setDuration(250)
                .start();
        }

    }

    /**
     * 平滑隐藏标题栏和底部栏
     */
    protected void hideBars(View topBar, View bottomBar) {
        if (!isBarShowing) return;
        isBarShowing = false;

        // 标题栏向上滑出（操作容器）
        topBar.animate()
            .translationY(-topBar.getHeight())
            .setDuration(250)
            .withEndAction(() -> {
                topBar.setVisibility(View.GONE);
                tintManager.setStatusBarTintResource(R.color.transparent);  //设置上方状态栏的颜色
            })
            .start();

        // 底部栏淡出
        if (bottomBar != null) {
            bottomBar.animate()
                .alpha(0f)
                .setDuration(250)
                .withEndAction(() -> bottomBar.setVisibility(View.GONE))
                .start();
        }
    }

    /**
     * 状态栏颜色渐变动画
     */
    protected void animateStatusBarColor(int toColor) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int fromColor = getWindow().getStatusBarColor();
            ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), fromColor, toColor);
            colorAnimation.setDuration(250);
            colorAnimation.addUpdateListener(animator -> {
                getWindow().setStatusBarColor((int) animator.getAnimatedValue());
            });
            colorAnimation.start();
        }
    }
}