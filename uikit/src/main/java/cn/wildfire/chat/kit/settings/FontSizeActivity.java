/*
 * Copyright (c) 2025 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.settings;

import android.content.Intent;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.utils.FontScaleUtils;
import cn.wildfire.chat.kit.utils.LayoutScale;

/**
 * 字体大小设置页面。参考微信，通过 SeekBar 拖动选择字体缩放级别，并实时预览效果。
 * 选择的级别保存后，离开页面时若发生变化则重启 App 使其全局生效。
 */
public class FontSizeActivity extends WfcBaseActivity {

    // 预览文本的基准字号（sp），实际显示尺寸 = 基准字号 * 选中的缩放比例
    private static final float PREVIEW_BASE_TEXT_SP = 17f;
    // 预览头像的基准尺寸（dp）
    private static final float PREVIEW_BASE_PORTRAIT_DP = 40f;

    private SeekBar seekBar;
    private TextView currentLevelTextView;
    private TextView previewReceiveTextView;
    private TextView previewSendTextView;
    private ImageView previewReceivePortraitImageView;
    private ImageView previewSendPortraitImageView;
    private TextView standardMarkerTextView;

    private String[] levelNames;
    private int entryIndex;

    @Override
    protected int contentLayout() {
        return R.layout.activity_font_size;
    }

    @Override
    protected void afterViews() {
        super.afterViews();
        levelNames = getResources().getStringArray(R.array.font_sizes);

        seekBar = findViewById(R.id.fontSizeSeekBar);
        currentLevelTextView = findViewById(R.id.currentLevelTextView);
        previewReceiveTextView = findViewById(R.id.previewReceiveTextView);
        previewSendTextView = findViewById(R.id.previewSendTextView);
        previewReceivePortraitImageView = findViewById(R.id.previewReceivePortraitImageView);
        previewSendPortraitImageView = findViewById(R.id.previewSendPortraitImageView);
        standardMarkerTextView = findViewById(R.id.standardMarkerTextView);

        entryIndex = FontScaleUtils.getFontScaleIndex(this);
        seekBar.setMax(FontScaleUtils.FONT_SCALES.length - 1);
        seekBar.setProgress(entryIndex);
        updatePreview(entryIndex);

        // 将“标准”标记定位到滑块标准档位的正上方，使用 OnGlobalLayoutListener 确保布局尺寸测量完成
        seekBar.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                seekBar.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                positionStandardMarker();
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updatePreview(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                FontScaleUtils.setFontScaleIndex(FontSizeActivity.this, seekBar.getProgress());
            }
        });
    }

    private void updatePreview(int index) {
        if (index < 0 || index >= FontScaleUtils.FONT_SCALES.length) {
            index = FontScaleUtils.DEFAULT_INDEX;
        }
        float scale = FontScaleUtils.FONT_SCALES[index];

        // 预览不依赖资源的 fontScale（资源里用的是旧设置），直接按目标缩放计算像素值
        float density = getResources().getDisplayMetrics().density;
        float px = PREVIEW_BASE_TEXT_SP * density * scale;
        previewReceiveTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, px);
        previewSendTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, px);

        // 头像按封顶比例缩放，与真实聊天界面（LayoutScale.CAP）保持一致
        float portraitCap = Math.min(scale, LayoutScale.CAP);
        int portraitPx = Math.round(PREVIEW_BASE_PORTRAIT_DP * density * portraitCap);
        setViewSize(previewReceivePortraitImageView, portraitPx);
        setViewSize(previewSendPortraitImageView, portraitPx);

        currentLevelTextView.setText(levelNames[index]);
    }

    private void setViewSize(View view, int sizePx) {
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        lp.width = sizePx;
        lp.height = sizePx;
        view.setLayoutParams(lp);
    }

    /**
     * 将“标准”标记水平居中对齐到滑块标准档位（{@link FontScaleUtils#DEFAULT_INDEX}）的位置。
     */
    private void positionStandardMarker() {
        int max = FontScaleUtils.FONT_SCALES.length - 1;
        if (max <= 0) {
            return;
        }
        int trackWidth = seekBar.getWidth() - seekBar.getPaddingLeft() - seekBar.getPaddingRight();
        float thumbXInSeekBar = seekBar.getPaddingLeft()
            + trackWidth * ((float) FontScaleUtils.DEFAULT_INDEX / max);

        int[] sbLoc = new int[2];
        seekBar.getLocationInWindow(sbLoc);
        View markerParent = (View) standardMarkerTextView.getParent();
        int[] mpLoc = new int[2];
        markerParent.getLocationInWindow(mpLoc);

        float absThumbX = sbLoc[0] + thumbXInSeekBar;
        float xInParent = absThumbX - mpLoc[0] - standardMarkerTextView.getWidth() / 2f;
        standardMarkerTextView.setTranslationX(xInParent);
    }

    @Override
    public void onBackPressed() {
        // 确保保存最新选择（用户可能点击 SeekBar 而非拖动）
        FontScaleUtils.setFontScaleIndex(this, seekBar.getProgress());
        if (seekBar.getProgress() != entryIndex) {
            restart();
        } else {
            super.onBackPressed();
        }
    }

    /**
     * 重启应用以应用字体大小更改，与切换语言的处理方式一致。
     * 不依赖具体的 App 入口 Activity，使用包的启动 Intent。
     */
    private void restart() {
        Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
        finish();
    }
}
