/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import me.aurelion.x.ui.view.watermark.WaterMarkManager;
import me.aurelion.x.ui.view.watermark.WaterMarkView;

public abstract class WfcBaseNoToolbarActivity extends AppCompatActivity {

    protected WaterMarkView mWmv;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        beforeViews();
        setContentView(contentLayout());
        afterViews();

        if (Config.ENABLE_WATER_MARK) {
            mWmv = WaterMarkManager.getView(this);
            ((ViewGroup) findViewById(android.R.id.content)).addView(mWmv);
        }
    }

    @Override
    protected void onDestroy() {
        if (mWmv != null) {
            mWmv.onDestroy();
        }
        super.onDestroy();
    }

    protected void hideInputMethod() {
        InputMethodManager imm = (InputMethodManager) this.getSystemService(Activity.INPUT_METHOD_SERVICE);
        View view = getCurrentFocus();
        if (view != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    /**
     * @return 布局文件
     */
    protected abstract @LayoutRes
    int contentLayout();

    /**
     * {@link AppCompatActivity#setContentView(int)}之前调用
     */
    protected void beforeViews() {

    }

    /**
     * {@link AppCompatActivity#setContentView(int)}之后调用
     * <p>
     */
    protected void afterViews() {

    }

    @Override
    protected void onPause() {
        super.onPause();
        hideInputMethod();
    }

    public boolean checkPermission(String permission) {
        return checkPermission(new String[]{permission});
    }

    public boolean checkPermission(String[] permissions) {
        boolean granted = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (String permission : permissions) {
                granted = checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
                if (!granted) {
                    break;
                }
            }
        }
        return granted;
    }

    /**
     * Changes the System Bar Theme.
     */
    public static void setStatusBarTheme(final Activity pActivity, final boolean pIsDark) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Fetch the current flags.
            final int lFlags = pActivity.getWindow().getDecorView().getSystemUiVisibility();
            // Update the SystemUiVisibility dependening on whether we want a Light or Dark theme.
            pActivity.getWindow().getDecorView().setSystemUiVisibility(pIsDark ? (lFlags & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR) : (lFlags | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR));
        }
    }

    /**
     * 设置状态栏的颜色
     *
     * @param resId 颜色资源id
     */
    protected void setStatusBarColor(int resId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, resId));
        }
    }
}
