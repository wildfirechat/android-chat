package cn.wildfire.chat.kit.third.location.ui.base;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.AppBarLayout;
import com.jaeger.library.StatusBarUtil;

import cn.wildfire.chat.kit.R;

public abstract class BaseActivity<V, T extends BasePresenter<V>> extends AppCompatActivity {

    protected T mPresenter;

    //以下是所有Activity中可能会出现的控件
    protected AppBarLayout mAppBar;
    //    @BindView(R2.id.toolbar)
    //    protected Toolbar mToolbar;
    public FrameLayout mToolbar;
    public ImageView mToolbarNavigation;
    public View mToolbarDivision;
    public LinearLayout mLlToolbarTitle;
    public TextView mToolbarTitle;
    public TextView mToolbarSubTitle;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();

        //判断是否使用MVP模式
        mPresenter = createPresenter();
        if (mPresenter != null) {
            mPresenter.attachView((V) this);//因为之后所有的子类都要实现对应的View接口
        }

        setContentView(provideContentViewId());
        bindViews();

        setupAppBarAndToolbar();

        //沉浸式状态栏
        StatusBarUtil.setColor(this, ContextCompat.getColor(this, R.color.colorPrimaryDark), 10);

        initView();
        initData();
        initListener();
    }

    private void bindViews() {
        mAppBar = findViewById(R.id.appBarLayout);
        mToolbar = findViewById(R.id.toolbarContainerFrameLayout);
        mToolbarNavigation = findViewById(R.id.backImageView);
        mToolbarDivision = findViewById(R.id.backDividerView);
        mLlToolbarTitle = findViewById(R.id.titleLinearLayout);
        mToolbarTitle = findViewById(R.id.titleTextView);
        mToolbarSubTitle = findViewById(R.id.subTitleTextView);
    }

    /**
     * 设置AppBar和Toolbar
     */
    private void setupAppBarAndToolbar() {
        //如果该应用运行在android 5.0以上设备，设置标题栏的z轴高度
        if (mAppBar != null && Build.VERSION.SDK_INT > 21) {
            mAppBar.setElevation(10.6f);
        }

        //如果界面中有使用toolbar，则使用toolbar替代actionbar
        //默认不是使用NoActionBar主题，所以如果需要使用Toolbar，需要自定义NoActionBar主题后，在AndroidManifest.xml中对指定Activity设置theme
//        if (mToolbar != null) {
//            setSupportActionBar(mToolbar);
//            if (isToolbarCanBack()) {
//                ActionBar actionBar = getSupportActionBar();
//                if (actionBar != null) {
//                    actionBar.setDisplayHomeAsUpEnabled(true);
//                }
//            }
//        }

        mToolbarNavigation.setVisibility(isToolbarCanBack() ? View.VISIBLE : View.GONE);
        mToolbarDivision.setVisibility(isToolbarCanBack() ? View.VISIBLE : View.GONE);
        mToolbarNavigation.setOnClickListener(v -> onBackPressed());
        mLlToolbarTitle.setPadding(isToolbarCanBack() ? 0 : 40, 0, 0, 0);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPresenter != null) {
            mPresenter.detachView();
        }
    }

    //在setContentView()调用之前调用，可以设置WindowFeature(如：this.requestWindowFeature(Window.FEATURE_NO_TITLE);)
    public void init() {
    }

    public void initView() {
    }

    public void initData() {
    }

    public void initListener() {
    }

    //用于创建Presenter和判断是否使用MVP模式(由子类实现)
    protected abstract T createPresenter();

    //得到当前界面的布局文件id(由子类实现)
    protected abstract int provideContentViewId();

    /**
     * 是否让Toolbar有返回按钮(默认可以，一般一个应用中除了主界面，其他界面都是可以有返回按钮的)
     */
    protected boolean isToolbarCanBack() {
        return true;
    }


    public void jumpToActivity(Intent intent) {
        startActivity(intent);
    }

    public void jumpToActivity(Class activity) {
        Intent intent = new Intent(this, activity);
        startActivity(intent);
    }

    public void jumpToActivityAndClearTask(Class activity) {
        Intent intent = new Intent(this, activity);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    public void jumpToActivityAndClearTop(Class activity) {
        Intent intent = new Intent(this, activity);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    /*------------------ toolbar的一些视图操作 ------------------*/
    public void setToolbarTitle(String title) {
        mToolbarTitle.setText(title);
    }

    public void setToolbarSubTitle(String subTitle) {
        mToolbarSubTitle.setText(subTitle);
        mToolbarSubTitle.setVisibility(subTitle.length() > 0 ? View.VISIBLE : View.GONE);
    }

}
