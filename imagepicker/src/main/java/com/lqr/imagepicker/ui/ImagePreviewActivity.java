package com.lqr.imagepicker.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import androidx.viewpager.widget.ViewPager;
import android.text.format.Formatter;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.lqr.imagepicker.ImagePicker;
import com.lqr.imagepicker.R;
import com.lqr.imagepicker.bean.ImageItem;
import com.lqr.imagepicker.view.SuperCheckBox;

public class ImagePreviewActivity extends ImagePreviewBaseActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    private SuperCheckBox mCbCheck;                //是否选中当前图片的CheckBox
    private SuperCheckBox mCbOrigin;               //原图
    private Button mBtnOk;                         //确认图片的选择
    private View bottomBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBtnOk = (Button) topBar.findViewById(R.id.btn_ok);
        mBtnOk.setVisibility(View.VISIBLE);
        mBtnOk.setOnClickListener(this);

        bottomBar = findViewById(R.id.bottom_bar);
        bottomBar.setVisibility(View.VISIBLE);

        mCbCheck = (SuperCheckBox) findViewById(R.id.cb_check);
        mCbOrigin = (SuperCheckBox) findViewById(R.id.cb_origin);
        mCbOrigin.setText(getString(R.string.origin));
        mCbOrigin.setOnCheckedChangeListener(this);
        mCbOrigin.setChecked(!store.isCompress());

        //初始化当前页面的状态
        updatePickStatus();
        ImageItem item = mImageItems.get(mCurrentPosition);
        boolean isSelected = store.isSelect(item);
        mTitleCount.setText(getString(R.string.preview_image_count, mCurrentPosition + 1, mImageItems.size()));
        mCbCheck.setChecked(isSelected);
        updateOriginImageSize();
        //滑动ViewPager的时候，根据外界的数据改变当前的选中状态和当前的图片的位置描述文本
        mViewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                mCurrentPosition = position;
                ImageItem item = mImageItems.get(mCurrentPosition);
                boolean isSelected = store.isSelect(item);
                mCbCheck.setChecked(isSelected);
                mTitleCount.setText(getString(R.string.preview_image_count, mCurrentPosition + 1, mImageItems.size()));
            }
        });
        //当点击当前选中按钮的时候，需要根据当前的选中状态添加和移除图片
        mCbCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ImageItem imageItem = mImageItems.get(mCurrentPosition);
                if (mCbCheck.isChecked() && selectedImages.size() >= pickLimit) {
                    Toast.makeText(ImagePreviewActivity.this, ImagePreviewActivity.this.getString(R.string.select_limit, pickLimit), Toast.LENGTH_SHORT).show();
                    mCbCheck.setChecked(false);
                } else {
                    store.addSelectedImageItem(mCurrentPosition, imageItem, mCbCheck.isChecked());

                    //每次选择一张图片就计算一次图片总大小
                    if (selectedImages != null && selectedImages.size() > 0) {
                        updateOriginImageSize();
                    } else {
                        mCbOrigin.setText(getString(R.string.origin));
                    }
                    updatePickStatus();
                }
            }
        });
    }

    private void updateOriginImageSize() {
        long size = 0;
        for (ImageItem ii : selectedImages)
            size += ii.size;
        if (size > 0) {
            String fileSize = Formatter.formatFileSize(ImagePreviewActivity.this, size);
            mCbOrigin.setText(getString(R.string.origin_size, fileSize));
        } else {
            mCbOrigin.setText(getString(R.string.origin));
        }
    }

    public void updatePickStatus() {
        if (store.getSelectImageCount() > 0) {
            mBtnOk.setText(getString(R.string.select_complete, store.getSelectImageCount(), pickLimit));
            mBtnOk.setEnabled(true);
        } else {
            mBtnOk.setText(getString(R.string.complete));
            mBtnOk.setEnabled(false);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_ok) {
            Intent intent = new Intent();
            intent.putExtra(ImagePicker.EXTRA_RESULT_ITEMS, store.getSelectedImages());
            setResult(Activity.RESULT_OK, intent);
            finish();
        } else if (id == R.id.btn_back) {
            onBackPressed();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        int id = buttonView.getId();
        if (id == R.id.cb_origin) {
            store.setCompress(!isChecked);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * 单击时，隐藏头和尾
     */
    @Override
    public void onImageSingleTap() {
        if (topBar.getVisibility() == View.VISIBLE) {
            topBar.setAnimation(AnimationUtils.loadAnimation(this, R.anim.top_out));
            bottomBar.setAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_out));
            topBar.setVisibility(View.GONE);
            bottomBar.setVisibility(View.GONE);
            tintManager.setStatusBarTintResource(R.color.transparent);//通知栏所需颜色
            //给最外层布局加上这个属性表示，Activity全屏显示，且状态栏被隐藏覆盖掉。
            if (Build.VERSION.SDK_INT >= 16)
                content.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
        } else {
            topBar.setAnimation(AnimationUtils.loadAnimation(this, R.anim.top_in));
            bottomBar.setAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
            topBar.setVisibility(View.VISIBLE);
            bottomBar.setVisibility(View.VISIBLE);
            tintManager.setStatusBarTintResource(R.color.status_bar);//通知栏所需颜色
            //Activity全屏显示，但状态栏不会被隐藏覆盖，状态栏依然可见，Activity顶端布局部分会被状态遮住
            if (Build.VERSION.SDK_INT >= 16)
                content.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
    }
}
