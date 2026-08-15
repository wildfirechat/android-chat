/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.pane;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.appbar.AppBarLayout;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.conversation.ConversationHost;
import cn.wildfire.chat.kit.conversation.ConversationTitleHelper;
import cn.wildfire.chat.kit.page.WfcPage;
import cn.wildfire.chat.kit.page.WfcPageHost;

/**
 * 平板右栏里<strong>一个页面</strong>的外壳：一条属于本页的 toolbar + 页面 Fragment 本体。
 * <p>
 * 手机端每个页面是一个 Activity，标题栏、菜单、返回箭头由 {@code WfcBaseActivity} 统一提供；
 * 右栏里没有 Activity 可用，本类就是它在右栏中的等价物 —— 两者实现同一个 {@link WfcPageHost}，
 * 页面 Fragment 只面向那个接口编程，因此菜单、标题、finish、setResult 全仓只写一份。
 * 这样右栏的每一层都自带标题栏，不需要「栈顶页面把标题交接给一条公用 toolbar」那种极易出错的编排。
 * <p>
 * 页面本体有两种来源：
 * <ul>
 *   <li><b>启动 intent</b>：交给 {@link PaneRegistry} 造 Fragment，标题取该 Activity 在 manifest
 *       里的 {@code android:label}（与 {@code WfcBaseActivity.updateActivityTitle()} 同一口径）；</li>
 *   <li><b>Fragment 类名 + 参数</b>：给没有对应 Activity 的页面用（如工作台那条栈的栈底）。</li>
 * </ul>
 * 用 arguments 而不是构造参数持有这些信息，是为了让配置变化（旋转、分屏）后 FragmentManager
 * 能自行重建整条栈，右栏不需要额外的状态保存代码。
 */
public class PanePageFragment extends Fragment implements ConversationHost, WfcPageHost {

    private static final String ARG_INTENT = "panePageIntent";
    private static final String ARG_SHOW_BACK = "panePageShowBack";
    private static final String ARG_FRAGMENT_CLASS = "panePageFragmentClass";
    private static final String ARG_FRAGMENT_ARGS = "panePageFragmentArgs";
    private static final String ARG_TITLE = "panePageTitle";
    private static final String ARG_PAGE_KEY = "panePageKey";
    private static final String ARG_ENTRY_NAME = "panePageEntryName";

    private static final String TAG_CONTENT = "panePageContent";

    private Toolbar toolbar;
    private AppBarLayout appBarLayout;
    private ConversationTitleHelper titleHelper;

    /**
     * 本页要回传给「打开本页的那一方」的结果，语义与 {@code Activity.setResult} 一致：
     * 只是记下来，真正的投递发生在本页出栈时（由 {@link PaneStackFragment} 完成）。
     * 没调用过 setPageResult 时 resultCode 保持 {@code RESULT_CANCELED}，与 Activity 一致。
     */
    private int pageResultCode = android.app.Activity.RESULT_CANCELED;
    @Nullable
    private Intent pageResultData;

    /**
     * 由启动 intent 造一个右栏页面。
     *
     * @param showBack  是否显示返回箭头。栈底之下只有欢迎页时不显示——返回过去是一片空白。
     * @param pageKey   本页在所属栈内的身份，见 {@link PaneRegistry.PageKey}；null 表示不参与去重
     * @param entryName 本页在所属栈返回栈里的条目名，用于精确定位自己那一层
     */
    public static PanePageFragment forIntent(Intent intent, boolean showBack,
                                             @Nullable String pageKey, @Nullable String entryName) {
        PanePageFragment fragment = new PanePageFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_INTENT, intent);
        args.putBoolean(ARG_SHOW_BACK, showBack);
        args.putString(ARG_PAGE_KEY, pageKey);
        args.putString(ARG_ENTRY_NAME, entryName);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * 由 Fragment 类名造一个右栏页面，用于没有对应 Activity 的页面（工作台）。
     */
    public static PanePageFragment forFragment(Class<? extends Fragment> fragmentClass, Bundle fragmentArgs,
                                               CharSequence title, boolean showBack) {
        PanePageFragment fragment = new PanePageFragment();
        Bundle args = new Bundle();
        args.putString(ARG_FRAGMENT_CLASS, fragmentClass.getName());
        args.putBundle(ARG_FRAGMENT_ARGS, fragmentArgs);
        args.putCharSequence(ARG_TITLE, title);
        args.putBoolean(ARG_SHOW_BACK, showBack);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    public Intent getPageIntent() {
        return getArguments() == null ? null : getArguments().getParcelable(ARG_INTENT);
    }

    /**
     * 本页在所属栈内的身份，见 {@link PaneRegistry.PageKey}。
     */
    @Nullable
    String getPageKey() {
        return getArguments() == null ? null : getArguments().getString(ARG_PAGE_KEY);
    }

    /**
     * 本页在所属栈返回栈里的条目名。栈底页不在返回栈里，返回 null。
     */
    @Nullable
    String getStackEntryName() {
        return getArguments() == null ? null : getArguments().getString(ARG_ENTRY_NAME);
    }

    @Nullable
    public Fragment getContentFragment() {
        return getChildFragmentManager().findFragmentByTag(TAG_CONTENT);
    }

    @Nullable
    private WfcPage contentPage() {
        Fragment content = getContentFragment();
        return content instanceof WfcPage ? (WfcPage) content : null;
    }

    int getPageResultCode() {
        return pageResultCode;
    }

    @Nullable
    Intent getPageResultData() {
        return pageResultData;
    }

    /**
     * 所属栈。在 {@code onDestroy} 时父 Fragment 链可能已经断开，所以 attach 时先记下来。
     */
    @Nullable
    private PaneStackFragment attachedStack;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        attachedStack = paneStack();
        // 内容 Fragment 是异步 commit 的，它的视图就绪时本页 toolbar 往往已经装配过了
        // （那时 getContentFragment() 还是 null，页面声明的菜单、标题都拿不到）。
        // 在它视图创建完成后补一次，与 WfcBaseActivity 里那段回调是同一套机制。
        getChildFragmentManager().registerFragmentLifecycleCallbacks(
            new FragmentManager.FragmentLifecycleCallbacks() {
                @Override
                public void onFragmentViewCreated(@NonNull FragmentManager fm, @NonNull Fragment f,
                                                  @NonNull View v, @Nullable Bundle savedState) {
                    if (toolbar == null || f != getContentFragment()) {
                        return;
                    }
                    applyToolbarVisibility(f);
                    setupTitle(f);
                    setupMenu(f);
                }
            }, false);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.pane_page_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        toolbar = view.findViewById(R.id.panePageToolbar);
        appBarLayout = view.findViewById(R.id.panePageAppBarLayout);
        titleHelper = new ConversationTitleHelper(requireContext(), toolbar, toolbar::setTitle);
        applyToolbarTheme();
        setupNavigationIcon();

        Fragment content = getContentFragment();
        if (content == null) {
            content = createContentFragment();
            if (content == null) {
                // 走不到：能进右栏的 intent 一定先过了 PaneRegistry.createPage 非空判断。
                // 真的发生了就保持空白页，而不是崩掉整个主界面。
                return;
            }
            getChildFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .add(R.id.panePageContainerFrameLayout, content, TAG_CONTENT)
                .commitAllowingStateLoss();
            // 这一批事务还没执行，此刻内容 Fragment 的视图不存在；标题与菜单由 onCreate 里注册的
            // onFragmentViewCreated 回调补齐。这里只先按 intent 摆一个标题，避免空白闪一下。
            // toolbar 的显隐则必须现在就定：晚一帧收起会看到一条标题栏闪过去。
            applyToolbarVisibility(content);
            setupTitle(content);
            applyPageIntentWhenViewReady(content);
            return;
        }
        applyToolbarVisibility(content);
        setupTitle(content);
        setupMenu(content);
        applyPageIntentWhenViewReady(content);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        toolbar = null;
        appBarLayout = null;
        titleHelper = null;
    }

    /**
     * 本页真的被移除（出栈）时，把 {@link #setPageResult} 记下的结果投递给发起方 ——
     * 手机端这一步由系统在 Activity finish 时完成。
     * <p>
     * {@code isRemoving()} 把配置变化排除在外：旋转时本页也会走 onDestroy，
     * 但它随后会被重建，那时候投递结果就错了。
     */
    @Override
    public void onDestroy() {
        if (isRemoving() && attachedStack != null) {
            attachedStack.deliverPageResult(this);
        }
        super.onDestroy();
    }

    @Nullable
    private Fragment createContentFragment() {
        Bundle args = getArguments();
        if (args == null) {
            return null;
        }
        String className = args.getString(ARG_FRAGMENT_CLASS);
        if (className != null) {
            Fragment fragment = getChildFragmentManager().getFragmentFactory()
                .instantiate(requireContext().getClassLoader(), className);
            fragment.setArguments(args.getBundle(ARG_FRAGMENT_ARGS));
            return fragment;
        }
        Intent intent = args.getParcelable(ARG_INTENT);
        return intent == null ? null : PaneRegistry.createPage(requireContext(), intent);
    }

    /**
     * 把启动 intent 交给页面。必须等到页面视图创建完成之后：会话页的 {@code setupConversation}
     * 依赖 {@code onCreateView} 里建出来的 adapter / 输入面板，早了会 NPE。
     * {@code getViewLifecycleOwnerLiveData()} 在视图就绪时才发出非空值，视图已就绪时会立即回调。
     */
    private void applyPageIntentWhenViewReady(Fragment content) {
        Intent intent = getPageIntent();
        if (intent == null || !(content instanceof WfcPage)) {
            return;
        }
        WfcPage page = (WfcPage) content;
        content.getViewLifecycleOwnerLiveData().observe(getViewLifecycleOwner(), viewLifecycleOwner -> {
            if (viewLifecycleOwner == null) {
                return;
            }
            page.onPageIntent(intent);
        });
    }

    /**
     * 自带标题栏的页面（搜索页顶部是「搜索框 + 取消」）把本页这条 toolbar 收起来，
     * 见 {@link WfcPage#providesOwnToolbar()}。
     */
    private void applyToolbarVisibility(@Nullable Fragment content) {
        if (appBarLayout == null) {
            return;
        }
        boolean ownToolbar = content instanceof WfcPage && ((WfcPage) content).providesOwnToolbar();
        // 收的是整条 AppBarLayout 而不只是里面的 Toolbar：只藏 Toolbar 的话，
        // AppBarLayout 自己的背景与阴影仍会在页面顶上留一道。
        appBarLayout.setVisibility(ownToolbar ? View.GONE : View.VISIBLE);
    }

    private void setupTitle(Fragment content) {
        CharSequence title = content instanceof WfcPage ? ((WfcPage) content).pageTitle() : null;
        if (title == null && getArguments() != null) {
            title = getArguments().getCharSequence(ARG_TITLE);
        }
        if (title == null) {
            title = resolveActivityLabel();
        }
        toolbar.setTitle(title == null ? "" : title);
    }

    /**
     * 取该页面对应 Activity 在 manifest 里的 {@code android:label}，与手机端
     * {@code WfcBaseActivity.updateActivityTitle()} 是同一份数据，因此右栏和全屏页标题必然一致。
     */
    @Nullable
    private CharSequence resolveActivityLabel() {
        Intent intent = getPageIntent();
        if (intent == null || intent.getComponent() == null) {
            return null;
        }
        try {
            PackageManager pm = requireContext().getPackageManager();
            ActivityInfo info = pm.getActivityInfo(intent.getComponent(), 0);
            if (info.labelRes != 0) {
                return getString(info.labelRes);
            }
            // manifest 里直接写死字符串（而不是 @string/…）时 labelRes 为 0，标题在这里
            if (info.nonLocalizedLabel != null) {
                return info.nonLocalizedLabel;
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 菜单全部来自内容 Fragment 的 {@link WfcPage} 实现 —— 与手机端
     * {@code WfcBaseActivity.onCreateOptionsMenu} 走的是同一份代码。
     */
    private void setupMenu(Fragment content) {
        toolbar.getMenu().clear();
        if (!(content instanceof WfcPage)) {
            return;
        }
        WfcPage page = (WfcPage) content;
        int menuRes = page.pageMenu();
        if (menuRes == 0) {
            return;
        }
        toolbar.inflateMenu(menuRes);
        page.onPreparePageMenu(toolbar.getMenu());
        toolbar.setOnMenuItemClickListener(page::onPageMenuItemSelected);
    }

    private void setupNavigationIcon() {
        boolean showBack = getArguments() != null && getArguments().getBoolean(ARG_SHOW_BACK, false);
        if (!showBack) {
            toolbar.setNavigationIcon(null);
            return;
        }
        Drawable back = ContextCompat.getDrawable(requireContext(), R.mipmap.ic_back);
        if (back != null && isDarkTheme() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // mutate()：getDrawable 返回的是共享常量态，直接 setTint 会把整个 App 的返回箭头染白
            back = back.mutate();
            back.setTint(Color.WHITE);
        }
        toolbar.setNavigationIcon(back);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    /**
     * 与手机端 toolbar 的配色保持一致（{@code WfcBaseActivity.customToolbarAndStatusBarBackgroundColor}）。
     * 不动状态栏：右栏只是窗口的一部分，改状态栏会波及左栏。
     */
    private void applyToolbarTheme() {
        boolean dark = isDarkTheme();
        toolbar.setBackgroundResource(dark ? R.color.colorPrimary : R.color.gray5);
        if (dark) {
            toolbar.setTitleTextColor(Color.WHITE);
            toolbar.setSubtitleTextColor(Color.parseColor("#F5F5F5"));
        }
    }

    private boolean isDarkTheme() {
        SharedPreferences sp = requireContext().getSharedPreferences("wfc_kit_config", Context.MODE_PRIVATE);
        return sp.getBoolean("darkTheme", false);
    }

    /**
     * 返回键 / 返回箭头：先让页面自己消费（会话页收起表情面板、退出多选），未消费才出栈。
     * 与 {@code WfcBaseActivity.onBackPressed()} 的顺序一致。
     */
    public boolean onBackPressed() {
        Fragment content = getContentFragment();
        if (content instanceof WfcPage && content.getView() != null
            && ((WfcPage) content).onPageBackPressed()) {
            return true;
        }
        return pop();
    }

    /**
     * 本页已经在栈里、又被同一个 key 再次打开时由 {@link PaneStackFragment} 调用，
     * 相当于手机端 {@code singleTop} 的 {@code onNewIntent}。
     * <p>
     * 先把新 intent 记下来（{@code getPageIntent()} 之后要以它为准，标题、
     * {@code getHighlightMessageId()} 都读这里），再转交给页面并重刷标题与菜单。
     */
    void onNewPageIntent(Intent intent) {
        if (getArguments() != null) {
            getArguments().putParcelable(ARG_INTENT, intent);
        }
        Fragment content = getContentFragment();
        if (content == null) {
            return;
        }
        if (content instanceof WfcPage && content.getView() != null) {
            ((WfcPage) content).onNewPageIntent(intent);
        }
        if (toolbar != null) {
            setupTitle(content);
            setupMenu(content);
        }
    }

    /**
     * 关闭本页：把本页连同压在本页上面的页面一起弹掉，等价于手机端的 {@code Activity.finish()}。
     */
    boolean pop() {
        PaneStackFragment stack = paneStack();
        return stack != null && stack.popPage(this);
    }

    @Nullable
    private PaneStackFragment paneStack() {
        for (Fragment parent = getParentFragment(); parent != null; parent = parent.getParentFragment()) {
            if (parent instanceof PaneStackFragment) {
                return (PaneStackFragment) parent;
            }
        }
        return null;
    }

    // ==================== WfcPageHost ====================

    @Override
    public void setPageTitle(CharSequence title) {
        if (toolbar != null) {
            toolbar.setTitle(TextUtils.isEmpty(title) ? "" : title);
        }
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
        return toolbar == null ? null : toolbar.getTitle();
    }

    @Override
    public void invalidatePageMenu() {
        Fragment content = getContentFragment();
        if (toolbar != null && content != null) {
            setupMenu(content);
        }
    }

    @Override
    public void finishPage() {
        pop();
    }

    @Override
    public void setPageResult(int resultCode, @Nullable Intent data) {
        this.pageResultCode = resultCode;
        this.pageResultData = data;
    }

    @Override
    public boolean isPaneHost() {
        return true;
    }

    // ==================== ConversationHost ====================

    @Override
    public void setConversationTitle(CharSequence title, CharSequence subTitle, boolean silent, boolean earpiece) {
        if (titleHelper != null) {
            titleHelper.setTitle(title, subTitle, silent, earpiece);
        }
    }

    @Override
    public CharSequence getConversationTitle() {
        return toolbar == null ? null : toolbar.getTitle();
    }

    /**
     * 会话页自知失效（聊天室加入失败、密聊信息缺失）时调用。右栏里等价于把本页弹掉。
     */
    @Override
    public void closeConversation() {
        pop();
    }

    @Override
    public long getHighlightMessageId() {
        Intent intent = getPageIntent();
        return intent == null ? 0 : intent.getLongExtra("highlightMessageId", 0);
    }

    /**
     * @deprecated 用 {@link cn.wildfire.chat.kit.page.WfcPageCompat#setPageTitle} 或
     * {@link #setPageTitle(CharSequence)}。保留以免外部调用点编译不过。
     */
    @Deprecated
    public void setPaneTitle(CharSequence title) {
        setPageTitle(title);
    }
}
