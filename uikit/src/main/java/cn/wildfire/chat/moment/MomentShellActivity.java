package cn.wildfire.chat.moment;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseNoToolbarActivity;
import cn.wildfire.chat.moment.thirdbar.StatusBarHelper;

/**
 * 朋友圈各页在<strong>手机端</strong>的宿主壳。整页逻辑住在对应的 {@link MomentPageFragment} 里，
 * 手机端与平板右栏共用同一份。
 * <p>
 * 没有 toolbar：朋友圈自带标题栏（{@link MomentPageFragment#providesOwnToolbar()}）。
 * <p>
 * 沉浸式状态栏留在这一层，因为它是<strong>窗口级</strong>设置，只在「一个页面 = 一个窗口」的
 * 手机端成立。改造前这段代码在 {@code BaseStatusControlActivity.onAfterSetedContentView()}，
 * 逐行搬过来，参数取的是朋友圈五个页面一致的那组取值
 * （{@code isTranslucentStatus()==true}、{@code isFitsSystemWindows()==false}）。
 * <p>
 * 改造前朋友圈直接继承 {@code AppCompatActivity}，绕过了 App 统一的那套页面策略；
 * 现在换成 {@link WfcBaseNoToolbarActivity}，于是朋友圈也跟着有了全局字号设置与水印（若开启）
 * ——这两项本来就该是全 App 一致的。<strong>唯独屏幕方向刻意不跟</strong>，见 {@link #onCreate}。
 */
public abstract class MomentShellActivity extends WfcBaseNoToolbarActivity {

    /**
     * 由启动 intent 造出本页。返回 null 表示参数不全，直接关掉。
     */
    @Nullable
    protected abstract Fragment createPage(Intent intent);

    /**
     * 把屏幕方向恢复成「不限制」，抵消基类在手机上加的竖屏锁。
     * <p>
     * 朋友圈是全 App 唯一从来没锁过竖屏的一族（它原本不继承 {@code WfcBaseActivity}，
     * manifest 里也没写 {@code screenOrientation}），手机上一直可以横过来看。
     * 本次改造只换宿主形态，不改这条既有行为 —— 手机端零回归优先于「和其余页面对齐」。
     * <p>
     * 若产品确认希望统一（朋友圈也锁竖屏），删掉本方法即可，基类的策略会自动生效。
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    @Override
    protected int contentLayout() {
        return R.layout.fragment_container_no_toolbar_activity;
    }

    @Override
    protected void afterViews() {
        // 朋友圈整页画到状态栏底下（头图沉浸），所以根视图不吃系统窗口 + 状态栏透明。
        // 顺序与改造前一致：先 setRootViewFitsSystemWindows 再 setTranslucentStatus。
        StatusBarHelper.setRootViewFitsSystemWindows(this, false);
        StatusBarHelper.setTranslucentStatus(this);

        // 配置变化后 FragmentManager 已经把页面恢复出来了，无条件 replace 会丢掉页面状态
        if (getSupportFragmentManager().findFragmentById(R.id.containerFrameLayout) != null) {
            return;
        }
        Fragment page = createPage(getIntent());
        if (page == null) {
            finish();
            return;
        }
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.containerFrameLayout, page)
            .commit();
    }

    /**
     * 返回键先交给页面（朋友圈里是收起评论输入条），未消费才走系统默认。
     * 右栏里的等价编排由 {@code PanePageFragment.onBackPressed()} 完成。
     */
    @Override
    public void onBackPressed() {
        Fragment page = getSupportFragmentManager().findFragmentById(R.id.containerFrameLayout);
        if (page instanceof MomentPageFragment && page.getView() != null
            && ((MomentPageFragment) page).onPageBackPressed()) {
            return;
        }
        super.onBackPressed();
    }
}
