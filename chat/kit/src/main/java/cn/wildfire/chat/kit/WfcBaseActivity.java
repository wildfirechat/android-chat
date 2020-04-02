package cn.wildfire.chat.kit;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.MenuRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.wildfirechat.chat.R;

public abstract class WfcBaseActivity extends AppCompatActivity {
    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        beforeViews();
        setContentView(contentLayout());
        ButterKnife.bind(this);
        if (toolbar.getOverflowIcon() != null) {
            DrawableCompat.setTint(toolbar.getOverflowIcon(), getResources().getColor(R.color.gray15));
        }
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeAsUpIndicator(R.mipmap.ic_back);
        if (showHomeMenuItem()) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        setTitleBackgroundResource(R.color.gray5);
        afterViews();
    }

    /**
     * 设置状态栏和标题栏的颜色
     *
     * @param resId 颜色资源id
     */
    protected void setTitleBackgroundResource(int resId) {
        toolbar.setBackgroundResource(resId);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, resId));
        }
    }

    protected void setTitleTextColor(int color) {
        toolbar.setTitleTextColor(color);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (menu() != 0) {
            getMenuInflater().inflate(menu(), menu);
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
        return super.onOptionsItemSelected(item);
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
     * 此时已经调用了{@link ButterKnife#bind(Activity)}, 子类里面不需要再次调用
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
}
