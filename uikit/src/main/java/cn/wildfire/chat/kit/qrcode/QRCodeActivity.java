package cn.wildfire.chat.kit.qrcode;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import com.bumptech.glide.request.target.CustomViewTarget;
import com.bumptech.glide.request.transition.Transition;
import com.king.zxing.util.CodeUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import butterknife.BindView;
import cn.wildfire.chat.kit.GlideApp;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.chat.R2;

public class QRCodeActivity extends WfcBaseActivity {
    private String title;
    private String logoUrl;
    private String qrCodeValue;

    @BindView(R2.id.qrCodeImageView)
    ImageView qrCodeImageView;

    public static Intent buildQRCodeIntent(Context context, String title, String logoUrl, String qrCodeValue) {
        Intent intent = new Intent(context, QRCodeActivity.class);
        intent.putExtra("title", title);
        intent.putExtra("logoUrl", logoUrl);
        intent.putExtra("qrCodeValue", qrCodeValue);
        return intent;
    }

    @Override
    protected void beforeViews() {
        super.beforeViews();
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
        setTitle(title);

        genQRCode();
    }

    private void genQRCode() {
        GlideApp.with(this)
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
