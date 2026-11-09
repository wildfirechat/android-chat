package cn.wildfire.chat.moment;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.page.WfcPage;
import cn.wildfire.chat.kit.page.WfcPageCompat;
import cn.wildfire.chat.moment.third.utils.Utils;
import cn.wildfire.chat.moment.third.widgets.TitleBar;
import cn.wildfire.chat.moment.thirdbar.MultiClickListener;
import cn.wildfire.chat.moment.thirdbar.StatusBarHelper;
import cn.wildfire.chat.moment.thirdbar.StatusBarViewPlaceHolder;

/**
 * 朋友圈每一页的基类：一条自带的 {@link TitleBar} + 它上方的状态栏占位。
 * <p>
 * 逐行搬自朋友圈原来的两个 Activity 基类 {@code thirdbar/BaseTitleBarActivity} 和
 * {@code thirdbar/BaseStatusControlActivity}，那两个类随本次改造删掉 —— 它们的方法签名
 * （{@code setContentView} 的三个重载、{@code finish()}、{@code getWindow()}）与 Activity
 * 绑死，页面变成 Fragment 之后无法成立。搬过来的是同一段逻辑，只是宿主换了。
 * <p>
 * <strong>朋友圈自带标题栏，所以 {@link #providesOwnToolbar()} 恒为 true</strong>：
 * 手机端的壳（{@link MomentShellActivity}）没有 toolbar，平板右栏的 {@code PanePageFragment}
 * 也会把自己那条收起来。整个朋友圈的视觉与改造前一致 —— 它本来就长得跟 App 其余部分不一样。
 * <p>
 * 两处按宿主分岔的地方，都是因为「右栏只是主界面窗口里的一块，顶上并没有状态栏」：
 * 状态栏占位在右栏里收起（{@link #applyStatusBarPlaceHolder()}），改状态栏明暗的调用在右栏里
 * 直接跳过（{@link #setStatusBarDark(boolean)}）—— 那是窗口级设置，从右栏改会波及左栏。
 */
public abstract class MomentPageFragment extends Fragment implements WfcPage {

    protected TitleBar titleBar;

    @Nullable
    private StatusBarViewPlaceHolder statusBarPlaceHolder;

    /**
     * 本页的布局，等价于改造前各 Activity 传给 {@code setContentView} 的那个。
     */
    @LayoutRes
    protected abstract int contentLayoutResId();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(contentLayoutResId(), container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        titleBar = view.findViewById(R.id.title_bar_view);
        statusBarPlaceHolder = view.findViewById(R.id.statusbar_placeholder);
        bindTitleBarEvents();
        applyStatusBarPlaceHolder();
        initView(view);
    }

    @Override
    public void onDestroyView() {
        titleBar = null;
        statusBarPlaceHolder = null;
        super.onDestroyView();
    }

    /**
     * 视图建好、标题栏装配完之后的初始化。等价于改造前各 Activity 在 {@code onCreate} 里
     * {@code setContentView} 之后那一段。
     */
    protected void initView(View view) {
    }

    // ==================== WfcPage ====================

    /**
     * 朋友圈整页自带标题栏，宿主不要再给一条。
     */
    @Override
    public boolean providesOwnToolbar() {
        return true;
    }

    // ==================== 标题栏 ====================
    // 以下逐行搬自 BaseTitleBarActivity，除了 onTitleLeftClick 的默认实现：
    // 改造前是 finish()，右栏里绝不能 finish 宿主（那是双栏主界面），改走 WfcPageCompat。

    private void bindTitleBarEvents() {
        if (titleBar == null) {
            return;
        }
        titleBar.getTitleText().setOnClickListener(new MultiClickListener() {
            @Override
            public void onSingleClick() {
                onTitleSingleClick();
            }

            @Override
            public void onDoubleClick() {
                onTitleDoubleClick();
            }
        });
        titleBar.setOnTitleBarClickListener(new TitleBar.OnTitleBarClickListener() {
            @Override
            public boolean onLeftClick(View v, boolean isLongClick) {
                if (!isLongClick) {
                    onTitleLeftClick();
                    return false;
                }
                return onTitleLeftLongClick();
            }

            @Override
            public boolean onTitleLongClick(View v) {
                return MomentPageFragment.this.onTitleLongClick(v);
            }

            @Override
            public boolean onRightClick(View v, boolean isLongClick) {
                if (!isLongClick) {
                    onTitleRightClick();
                    return false;
                }
                return onTitleRightLongClick();
            }
        });
    }

    public boolean onTitleLongClick(View v) {
        return false;
    }

    public boolean onTitleLeftLongClick() {
        return false;
    }

    public boolean onTitleRightLongClick() {
        return false;
    }

    /**
     * 标题栏左侧（返回箭头）。改造前是 {@code finish()}；右栏里等价的动作是把本页弹出栈。
     */
    public void onTitleLeftClick() {
        WfcPageCompat.finishPage(this);
    }

    public void onTitleRightClick() {
    }

    public void onTitleDoubleClick() {
    }

    public void onTitleSingleClick() {
    }

    public TitleBar getTitleBar() {
        return titleBar;
    }

    public void setTitle(int resId) {
        if (titleBar != null && resId != 0) {
            titleBar.setTitleText(resId);
        }
    }

    public void setTitle(String title) {
        if (titleBar != null && !TextUtils.isEmpty(title)) {
            titleBar.setTitleText(title);
        }
    }

    public void setTitleMode(@TitleBar.TitleBarMode int mode) {
        if (titleBar != null) {
            titleBar.setMode(mode);
        }
    }

    public void setTitleRightText(String text) {
        if (titleBar != null) {
            titleBar.setRightText(text);
        }
    }

    public void setTitleRightIcon(int resid) {
        if (titleBar != null) {
            titleBar.setRightIcon(resid);
        }
    }

    public void setTitleLeftText(String text) {
        if (titleBar != null) {
            titleBar.setLeftText(text);
        }
    }

    public void setTitleLeftIcon(int resid) {
        if (titleBar != null) {
            titleBar.setLeftIcon(resid);
        }
    }

    public void setLeftTextColor(int color) {
        if (titleBar != null) {
            titleBar.setLeftTextColor(color);
        }
    }

    public void setRightTextColor(int color) {
        if (titleBar != null) {
            titleBar.setRightTextColor(color);
        }
    }

    public void setTitleBarBackground(int color) {
        if (titleBar != null) {
            titleBar.setTitleBarBackgroundColor(color);
        }
    }

    // ==================== 状态栏 ====================

    /**
     * 标题栏上方那块状态栏占位。
     * <p>
     * 手机端朋友圈是沉浸式的：整页画到状态栏底下（窗口级设置由 {@link MomentShellActivity} 做），
     * 这块占位把标题栏顶下来，露出底下的头图。<strong>右栏里没有这回事</strong> ——
     * 右栏是主界面窗口内部的一块，顶上就是主界面自己，留着占位只会在标题栏上方多出一条空白。
     */
    private void applyStatusBarPlaceHolder() {
        if (statusBarPlaceHolder != null) {
            statusBarPlaceHolder.setVisibility(WfcPageCompat.isInPane(this) ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * 状态栏占位当前占掉的高度，收起时为 0。
     * 朋友圈主页用它算下拉刷新的触发距离（改造前直接写死取状态栏高度）。
     */
    protected int statusBarPlaceHolderHeight() {
        if (statusBarPlaceHolder == null || statusBarPlaceHolder.getVisibility() != View.VISIBLE) {
            return 0;
        }
        return Utils.calcStatusBarHeight(requireContext());
    }

    protected void setStatusBarHolderBackgroundColor(int color) {
        if (statusBarPlaceHolder != null) {
            statusBarPlaceHolder.setBackgroundColor(color);
        }
    }

    /**
     * 状态栏图标明暗。<strong>右栏里直接跳过</strong>：这是窗口级设置，右栏只占窗口的一部分，
     * 改了会连左栏的状态栏一起改掉。
     */
    protected void setStatusBarDark(boolean dark) {
        if (getActivity() == null || WfcPageCompat.isInPane(this)) {
            return;
        }
        StatusBarHelper.setStatusBarDarkTheme(getActivity(), dark);
    }
}
