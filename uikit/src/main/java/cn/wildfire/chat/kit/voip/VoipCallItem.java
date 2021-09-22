/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.voip;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import cn.wildfire.chat.kit.R;

public class VoipCallItem extends FrameLayout {
    public ImageView portraitImageView;
    public TextView statusTextView;
    public ZoomableFrameLayout videoContainer;
    private boolean enableVideoZoom = true;

    public VoipCallItem(@NonNull Context context) {
        super(context);
        init(context, null);
    }

    public VoipCallItem(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public VoipCallItem(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public VoipCallItem(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private void init(Context context, AttributeSet attrs) {
        View view = inflate(context, R.layout.av_multi_call_item, this);
        portraitImageView = view.findViewById(R.id.portraitImageView);
        statusTextView = view.findViewById(R.id.statusTextView);
        videoContainer = view.findViewById(R.id.videoContainer);
        videoContainer.setEnableZoom(enableVideoZoom);
    }

    public ImageView getPortraitImageView() {
        return portraitImageView;
    }

    public TextView getStatusTextView() {
        return statusTextView;
    }

    @Override
    public void setOnClickListener(@Nullable OnClickListener l) {
        super.setOnClickListener(l);
        if (l != null) {
            videoContainer.setOnClickListener(v -> l.onClick(VoipCallItem.this));
        } else {
            videoContainer.setOnClickListener(null);
        }
    }

    /**
     * 是否开启视频缩放
     */
    public void setEnableVideoZoom(boolean enable) {
        this.enableVideoZoom = enable;
        videoContainer.setEnableZoom(enable);
    }
}
