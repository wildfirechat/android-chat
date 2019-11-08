package cn.wildfire.chat.kit.mm;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.cjt2325.cameralibrary.JCameraView;
import com.cjt2325.cameralibrary.listener.JCameraListener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import cn.wildfire.chat.app.Config;
import cn.wildfirechat.chat.R;

public class TakePhotoActivity extends AppCompatActivity {
    private JCameraView mJCameraView;
    public static final String MODE = "mode";
    public static final int MODE_RECORDER_ONLY = 0x102;
    public static final int MODE_CAPTURE_ONLY = 0x101;
    public static final int MODE_CAPTURE_AND_RECORDER = 0x103;
//    public static final int BUTTON_STATE_ONLY_CAPTURE = 0x101;      //只能拍照
//    public static final int BUTTON_STATE_ONLY_RECORDER = 0x102;     //只能录像
//    public static final int BUTTON_STATE_BOTH = 0x103;              //两者都可以

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_photo);
        initView();
    }

    private void initView() {
        mJCameraView = findViewById(R.id.cameraView);
        //(0.0.7+)设置视频保存路径（如果不设置默认为Environment.getExternalStorageDirectory().getPath()）
        mJCameraView.setSaveVideoPath(Environment.getExternalStorageDirectory().getPath());
        //(0.0.8+)设置手动/自动对焦，默认为自动对焦
        //设置小视频保存路径
        File file = new File(Config.VIDEO_SAVE_DIR);
        if (!file.exists()) {
            file.mkdirs();
        }

        int mode = getIntent().getIntExtra(MODE, MODE_CAPTURE_AND_RECORDER);
        mJCameraView.setFeatures(mode);
        mJCameraView.setSaveVideoPath(Config.VIDEO_SAVE_DIR);
        mJCameraView.setJCameraLisenter(new JCameraListener() {

            @Override
            public void captureSuccess(Bitmap bitmap) {
                //获取到拍照成功后返回的Bitmap
                String path = saveBitmap(bitmap, Config.PHOTO_SAVE_DIR);
                Intent data = new Intent();
                data.putExtra("take_photo", true);
                data.putExtra("path", path);
                setResult(RESULT_OK, data);
                finish();
            }

            @Override
            public void recordSuccess(String url, Bitmap firstFrame) {
                //获取成功录像后的视频路径
                Intent data = new Intent();
                data.putExtra("take_photo", false);
                data.putExtra("path", url);
                setResult(RESULT_OK, data);
                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mJCameraView != null) {
            mJCameraView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mJCameraView != null) {
            mJCameraView.onPause();
        }
    }

    public String saveBitmap(Bitmap bm, String dir) {
        String path = "";
        File f = new File(dir, "wfc_" + SystemClock.currentThreadTimeMillis() + ".png");
        if (f.exists()) {
            f.delete();
        }
        try {
            FileOutputStream out = new FileOutputStream(f);
            bm.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
            path = f.getAbsolutePath();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return path;
    }
}
