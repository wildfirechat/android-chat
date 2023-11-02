/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.qrcode;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomViewTarget;
import com.bumptech.glide.request.transition.Transition;
import com.king.zxing.util.CodeUtils;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;

public class QRCodeActivity extends WfcBaseActivity {
    private String title;
    private String logoUrl;
    private String qrCodeValue;

    ImageView qrCodeImageView;

    protected void bindViews() {
        super.bindViews();
        qrCodeImageView = findViewById(R.id.qrCodeImageView);
    }

    public static Intent buildQRCodeIntent(Context context, String title, String logoUrl, String qrCodeValue) {
        Intent intent = new Intent(context, QRCodeActivity.class);
        intent.putExtra("title", title);
        intent.putExtra("logoUrl", logoUrl);
        intent.putExtra("qrCodeValue", qrCodeValue);
        return intent;
    }

    @Override
    protected void beforeViews() {
        Intent intent = getIntent();
        title = intent.getStringExtra("title");
        qrCodeValue = intent.getStringExtra("qrCodeValue");
        logoUrl = intent.getStringExtra("logoUrl");
    }

    @Override
    protected int contentLayout() {
        return R.layout.qrcode_activity;
    }

    @Override
    protected void afterViews() {
        bindViews();
        setTitle(title);
        if (TextUtils.isEmpty(qrCodeValue)) {
            finish();
            return;
        }
        genQRCode();
    }

    private void genQRCode() {
        Glide.with(this)
            .asBitmap()
            .load(logoUrl)
            .placeholder(R.mipmap.ic_launcher)
            .into(new CustomViewTarget<ImageView, Bitmap>(qrCodeImageView) {
                @Override
                public void onLoadFailed(@Nullable Drawable errorDrawable) {
                    // the errorDrawable will always be bitmapDrawable here
                    if (errorDrawable instanceof BitmapDrawable) {
                        Bitmap bitmap = ((BitmapDrawable) errorDrawable).getBitmap();
                        Bitmap qrBitmap = CodeUtils.createQRCode(qrCodeValue, 400, bitmap);
                        qrCodeImageView.setImageBitmap(qrBitmap);
                    }
                }

                @Override
                public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition transition) {
                    Bitmap bitmap = CodeUtils.createQRCode(qrCodeValue, 400, resource);
                    qrCodeImageView.setImageBitmap(bitmap);
                }

                @Override
                protected void onResourceCleared(@Nullable Drawable placeholder) {

                }
            });
    }
}
