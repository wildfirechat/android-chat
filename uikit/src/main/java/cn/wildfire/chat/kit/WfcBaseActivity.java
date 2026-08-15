/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.LayoutRes;
import androidx.annotation.MenuRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.appbar.AppBarLayout;

import cn.wildfire.chat.kit.page.WfcPage;
import cn.wildfire.chat.kit.page.WfcPageHost;
import cn.wildfire.chat.kit.utils.FontScaleUtils;
import cn.wildfire.chat.kit.utils.LayoutScale;
import cn.wildfire.chat.kit.utils.LocaleUtils;
import cn.wildfire.chat.kit.utils.WfcDeviceUtils;
import me.aurelion.x.ui.view.watermark.WaterMarkManager;
import me.aurelion.x.ui.view.watermark.WaterMarkView;

public abstract class WfcBaseActivity extends AppCompatActivity implements WfcPageHost {
    Toolbar toolbar;
    private AppBarLayout appBarLayout;

    protected WaterMarkView mWmv;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 手机维持原有的强制竖屏；平板不做限制，交给系统默认（也便于个别页面在 manifest 中单独锁定）。
        if (!WfcDeviceUtils.isLandscapeAllowed(this)) {
            this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        registerPageCallbacks();
        beforeViews();
        setContentView(contentLayout());
        bindViews();
        bindEvents();
        setSupportActionBar(toolbar);

        // 确保使用正确语言的标题
        updateActivityTitle();

        SharedPreferences sp = getSharedPreferences("wfc_kit_config", Context.MODE_PRIVATE);
        if (sp.getBoolean("darkTheme", false)) {
            // dark
            toolbar.getContext().setTheme(R.style.AppTheme_DarkAppbar);
            customToolbarAndStatusBarBackgroundColor(true);
        } else {
            // light
            toolbar.getContext().setTheme(R.style.AppTheme_LightAppbar);
            customToolbarAndStatusBarBackgroundColor(false);
        }
        afterViews();

        if (Config.ENABLE_WATER_MARK) {
            mWmv = WaterMarkManager.getView(this);
            ((ViewGroup) findViewById(android.R.id.content)).addView(mWmv);
        }

        // 字体放大时，放大设置类页面中开关项的固定行高（含异步添加的 Fragment，故 post 到布局之后）
        View content = findViewById(android.R.id.content);
        content.post(() -> LayoutScale.scaleSwitchRows(content));
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(FontScaleUtils.wrap(newBase));
    }

    @Override
    protected void onDestroy() {
        if (mWmv != null) {
            mWmv.onDestroy();
        }
        super.onDestroy();
    }

    protected void bindViews() {
        appBarLayout = findViewById(R.id.appbar);
        toolbar = findViewById(R.id.toolbar);
    }

    protected void bindEvents() {

    }

    /**
     * @param darkTheme 和toolbar.xml里面的 app:theme="@style/AppTheme.DarkAppbar" 相关
     */
    private void customToolbarAndStatusBarBackgroundColor(boolean darkTheme) {
        int toolbarBackgroundColorResId = darkTheme ? R.color.colorPrimary : R.color.gray5;
        Drawable drawable = getResources().getDrawable(R.mipmap.ic_back);
        if (darkTheme) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                drawable.setTint(Color.WHITE);
            }
            toolbar.setTitleTextColor(Color.WHITE);
            toolbar.setSubtitleTextColor(Color.parseColor("#F5F5F5"));
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                drawable.setTintList(null);
            }
        }
        getSupportActionBar().setHomeAsUpIndicator(drawable);
        if (showHomeMenuItem()) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        setTitleBackgroundResource(toolbarBackgroundColorResId, darkTheme);
    }

    /**
     * 设置状态栏和标题栏的颜色
     *
     * @param resId 颜色资源id
     */
    protected void setTitleBackgroundResource(int resId, boolean dark) {
        toolbar.setBackgroundResource(resId);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, resId));
        }
        setStatusBarTheme(this, dark);
    }

    protected boolean isDarkTheme() {
        SharedPreferences sp = getSharedPreferences("wfc_kit_config", MODE_PRIVATE);
        return sp.getBoolean("darkTheme", false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        WfcPage page = contentPage();
        // 菜单优先由 Activity 自己声明（老页面），其次问内容 Fragment（已下沉到 WfcPage 的页面）。
        // 两者都是 0 时不 inflate，与改造前一致。
        int menuRes = menu();
        if (menuRes == 0 && page != null) {
            menuRes = page.pageMenu();
        }
        if (menuRes != 0) {
            getMenuInflater().inflate(menuRes, menu);
        }
        if (page != null) {
            page.onPreparePageMenu(menu);
        }
        afterMenus(menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            hideInputMethod();
            onBackPressed();
            return true;
        }
        // 子类的 onOptionsItemSelected 先跑（它们处理完自己的项才调到 super），
        // 这里兜底交给内容 Fragment，与右栏 PanePageFragment 的顺序一致。
        WfcPage page = contentPage();
        if (page != null && page.onPageMenuItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        WfcPage page = contentPage();
        if (page != null && page.onPageBackPressed()) {
            return;
        }
        super.onBackPressed();
    }

    // ==================== WfcPage / WfcPageHost ====================

    /**
     * 本 Activity 承载的内容页面。
     * <p>
     * 仓库里 35 个页面是「{@code fragment_container_activity} + 一个 Fragment」的壳，
     * 所以先按容器 id 找；个别自定义布局的页面找不到时退化为在 FragmentManager 里找第一个
     * {@link WfcPage} —— {@code WfcPage} 是页面主动实现的，不会误命中普通子 Fragment。
     * 内容 Fragment 没实现 {@code WfcPage} 时返回 null，本类所有委托分支都跳过，行为与改造前一致。
     */
    @Nullable
    protected WfcPage contentPage() {
        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.containerFrameLayout);
        if (fragment instanceof WfcPage) {
            return (WfcPage) fragment;
        }
        for (Fragment f : fm.getFragments()) {
            if (f instanceof WfcPage) {
                return (WfcPage) f;
            }
        }
        return null;
    }

    /**
     * 内容 Fragment 是在 {@link #afterViews()} 里异步 commit 的，等它视图就绪时
     * {@code onCreateOptionsMenu} 往往已经跑过了（拿不到页面的菜单）。这里在它视图创建完成后
     * 补一次菜单重建与标题应用。
     */
    private void registerPageCallbacks() {
        getSupportFragmentManager().registerFragmentLifecycleCallbacks(
            new FragmentManager.FragmentLifecycleCallbacks() {
                @Override
                public void onFragmentViewCreated(@NonNull FragmentManager fm, @NonNull Fragment f,
                                                  @NonNull View v, @Nullable Bundle savedInstanceState) {
                    if (!(f instanceof WfcPage)) {
                        return;
                    }
                    invalidateOptionsMenu();
                    CharSequence title = ((WfcPage) f).pageTitle();
                    if (title != null) {
                        setTitle(title);
                    }
                }
            }, false);
    }

    @Override
    public void setPageTitle(CharSequence title) {
        setTitle(title);
    }

    @Override
    public void setPageSubtitle(@Nullable CharSequence subtitle) {
        if (toolbar != null) {
            toolbar.setSubtitle(subtitle);
        }
    }

    @Nullable
    @Override
    public CharSequence getPageTitle() {
        return toolbar == null ? getTitle() : toolbar.getTitle();
    }

    @Override
    public void invalidatePageMenu() {
        invalidateOptionsMenu();
    }

    @Override
    public void finishPage() {
        finish();
    }

    @Override
    public void setPageResult(int resultCode, @Nullable Intent data) {
        if (data == null) {
            setResult(resultCode);
        } else {
            setResult(resultCode, data);
        }
    }

    @Override
    public boolean isPaneHost() {
        return false;
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
     * @return menu
     */
    protected @MenuRes
    int menu() {
        return 0;
    }

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

    /**
     * {@code getMenuInflater().inflate(menu(), menu);}之后调用
     *
     * @param menu
     */
    protected void afterMenus(Menu menu) {

    }

    @Override
    protected void onPause() {
        super.onPause();
        hideInputMethod();
    }

    protected boolean showHomeMenuItem() {
        return true;
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
     * 更新 Activity 的标题，确保使用正确语言的资源
     */
    private void updateActivityTitle() {
        try {
            // 获取 Activity 的 label 资源 ID
            PackageManager pm = getPackageManager();
            android.content.pm.ActivityInfo info = pm.getActivityInfo(getComponentName(), 0);

            if (info.labelRes != 0) {
                // 使用当前语言的资源设置标题
                setTitle(info.labelRes);
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
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

    public Toolbar getToolbar() {
        return toolbar;
    }

    public AppBarLayout getAppBarLayout() {
        return appBarLayout;
    }

    protected void setAppBarLayoutElevation(float elevation) {
        appBarLayout.setElevation(elevation);
    }
}
