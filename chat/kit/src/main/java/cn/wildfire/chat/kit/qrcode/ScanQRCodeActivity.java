package cn.wildfire.chat.kit.qrcode;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceView;

import androidx.annotation.Nullable;

import com.google.zxing.Result;
import com.king.zxing.CaptureHelper;
import com.king.zxing.Intents;
import com.king.zxing.ViewfinderView;
import com.lqr.imagepicker.ImagePicker;
import com.lqr.imagepicker.bean.ImageItem;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;

import butterknife.Bind;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfirechat.chat.R;

public class ScanQRCodeActivity extends WfcBaseActivity {

    private CaptureHelper mCaptureHelper;
    @Bind(R.id.surfaceView)
    SurfaceView surfaceView;
    @Bind(R.id.viewfinderView)
    ViewfinderView viewfinderView;

    private static final int REQUEST_CODE_IMAGE = 100;

    @Override
    protected int contentLayout() {
        return R.layout.scan_qrcode_activity;
    }

    @Override
    protected void afterViews() {
        super.afterViews();
        mCaptureHelper = new CaptureHelper(this, surfaceView, viewfinderView);
        mCaptureHelper.onCreate();
        mCaptureHelper.vibrate(true)
                .fullScreenScan(true)//全屏扫码
                .supportVerticalCode(false)//支持扫垂直条码，建议有此需求时才使用。
                .continuousScan(false);
    }

    @Override
    protected int menu() {
        return R.menu.qrcode;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.gallery) {
            scanGalleryQRCode();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void scanGalleryQRCode() {
        Intent intent = ImagePicker.picker().showCamera(false).buildPickIntent(this);
        startActivityForResult(intent, REQUEST_CODE_IMAGE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCaptureHelper.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCaptureHelper.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCaptureHelper.onDestroy();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mCaptureHelper.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_CODE_IMAGE && resultCode == Activity.RESULT_OK) {

            ArrayList<ImageItem> images = (ArrayList<ImageItem>) data.getSerializableExtra(ImagePicker.EXTRA_RESULT_ITEMS);
            if (images != null && images.size() > 0) {
                String path = images.get(0).path;

                InputStream is = null;
                try {
                    is = new FileInputStream(path);
                    Bitmap bmpSource = BitmapFactory.decodeStream(is);
                    Result result = QRCodeHelper.decodeQR(bmpSource);
                    Intent intent = new Intent();
                    intent.putExtra(Intents.Scan.RESULT, result.getText());
                    setResult(Activity.RESULT_OK, intent);
                    finish();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
