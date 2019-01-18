package com.lqr.imagepicker;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import androidx.fragment.app.Fragment;

import com.lqr.imagepicker.ui.ImageGridActivity;

import java.io.Serializable;

public class ImagePicker implements Serializable {

    public static final String TAG = ImagePicker.class.getSimpleName();
    public static final int REQUEST_CODE_TAKE = 1001;
    public static final int REQUEST_CODE_CROP = 1002;
    public static final int REQUEST_CODE_PREVIEW = 1003;

    public static final String EXTRA_RESULT_ITEMS = "extra_result_items";
    public static final String EXTRA_SELECTED_IMAGE_POSITION = "selected_image_position";
    public static final String EXTRA_IMAGE_ITEMS = "extra_image_items";
    public static final String EXTRA_COMPRESS = "extra_compress";

    private boolean multiMode = false;    //图片选择模式
    private int limit = 9;         //最大选择图片数量
    private boolean showCamera = false;   //显示相机

    private ImagePicker() {
    }

    public static ImagePicker picker() {
        return new ImagePicker();
    }

    public ImagePicker enableMultiMode(int limit) {
        this.multiMode = true;
        this.limit = limit;
        return this;
    }

    public ImagePicker showCamera(boolean show) {
        this.showCamera = show;
        return this;
    }

    public void pick(Activity activity, int requestCode) {
        activity.startActivityForResult(buildPickIntent(activity), requestCode);
    }

    public void pick(Fragment fragment, int requestCode) {
        fragment.startActivityForResult(buildPickIntent(fragment.getActivity()), requestCode);
    }

    public Intent buildPickIntent(Context context) {
        Intent intent = new Intent(context, ImageGridActivity.class);
        intent.putExtra("multiMode", multiMode);
        intent.putExtra("limit", limit);
        intent.putExtra("showCamera", showCamera);
        return intent;
    }
}