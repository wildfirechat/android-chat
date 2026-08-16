/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.qrcode;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomViewTarget;
import com.bumptech.glide.request.transition.Transition;
import com.king.zxing.util.CodeUtils;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.page.WfcPage;

/**
 * 二维码展示页：我的二维码、群二维码、频道二维码、会议二维码共用一份。
 * <p>
 * 逐行搬自 {@link QRCodeActivity}，那个类现在只是手机端的壳。前三个入口
 * （用户资料、群设置、频道设置）都已经在右栏里，不迁的话点一下二维码就跳全屏。
 */
public class QRCodeFragment extends Fragment implements WfcPage {
    private String title;
    private String logoUrl;
    private String qrCodeValue;

    private ImageView qrCodeImageView;

    /**
     * 没有二维码内容就没有这一页，返回 null 让壳 Activity 直接 finish（右栏同理不会压栈）。
     */
    @Nullable
    public static QRCodeFragment fromIntent(Intent intent) {
        String qrCodeValue = intent.getStringExtra("qrCodeValue");
        if (TextUtils.isEmpty(qrCodeValue)) {
            return null;
        }
        QRCodeFragment fragment = new QRCodeFragment();
        Bundle args = new Bundle();
        args.putString("title", intent.getStringExtra("title"));
        args.putString("logoUrl", intent.getStringExtra("logoUrl"));
        args.putString("qrCodeValue", qrCodeValue);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        title = args == null ? null : args.getString("title");
        logoUrl = args == null ? null : args.getString("logoUrl");
        qrCodeValue = args == null ? null : args.getString("qrCodeValue");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.qrcode_activity, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        qrCodeImageView = view.findViewById(R.id.qrCodeImageView);
        genQRCode();
    }

    /**
     * 标题是调用方传进来的（「群二维码」「频道二维码」），不是 manifest 里的固定 label。
     */
    @Nullable
    @Override
    public CharSequence pageTitle() {
        return TextUtils.isEmpty(title) ? null : title;
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
