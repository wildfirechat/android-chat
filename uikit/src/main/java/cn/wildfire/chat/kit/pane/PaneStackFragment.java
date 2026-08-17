/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.pane;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Lifecycle;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import cn.wildfire.chat.kit.R;

/**
 * 一个 tab 专属的右栏导航栈。
 * <p>
 * <strong>为什么每个 tab 一条栈</strong>：在通讯录里点开的用户资料应该留在通讯录那一栏，
 * 切到消息 tab 看到的是消息自己的会话，切回通讯录资料还在。共用一条栈的话，切个 tab
 * 右栏就挂着上一个 tab 的东西。这与微信 Pad、以及 flutter 端 {@code PadHome} 的做法一致。
 * <p>
 * 实现上就是一个自带 {@code childFragmentManager} 的容器 Fragment：栈 = 该 FragmentManager
 * 的返回栈，五条栈天然互不干扰。主界面把它们全部 add 进右栏容器，用 show/hide 切换
 * （不是 replace）——切走的那条栈要<strong>活着</strong>，切回来才还在原处，
 * 而且工作台那条栈里装的是 WebView，重建一次就是重新拉一遍远端页面。
 * <p>
 * 入栈用 {@code hide(当前页) + add(新页)} 而不是 {@code replace}：replace 会销毁被盖住那页的视图，
 * 返回时列表滚动位置、WebView 内容全部重来。
 */
public class PaneStackFragment extends Fragment {

    private static final String ARG_ROOT_PAGE_ARGS = "paneStackRootPageArgs";
    private static final String ARG_WELCOME_HINT = "paneStackWelcomeHint";

    private static final String TAG_ROOT = "paneStackRoot";
    private static final String ENTRY_NAME_PREFIX = "panePage_";

    /**
     * 栈内容变化时回调。主界面据此同步左栏会话列表的选中高亮。
     */
    public interface OnStackChangedListener {
        void onPaneStackChanged(PaneStackFragment stack);
    }

    private OnStackChangedListener onStackChangedListener;

    /**
     * 等待结果的页面：栈条目名 → 发起方。
     * <p>
     * 手机端 {@code startActivityForResult} 的结果由系统投递回发起的 Activity/Fragment；
     * 右栏里没有 Activity 边界，由本栈在页面出栈时投递（见 {@link #deliverPageResult}）。
     * <p>
     * 用弱引用持有发起方，避免它已经被销毁时还被这张表拖着。<strong>配置变化会清空本表</strong>
     * ——旋转时整条栈会重建，此时正开着的选择器拿不到回调。这是有意的取舍：为了跨进程/跨重建
     * 也能找回发起方，需要给每个 Fragment 分配可持久化的身份，代价远大于收益。
     */
    private final Map<String, PendingResult> pendingResults = new HashMap<>();

    private static final class PendingResult {
        final WeakReference<Fragment> caller;
        final int requestCode;

        PendingResult(Fragment caller, int requestCode) {
            this.caller = new WeakReference<>(caller);
            this.requestCode = requestCode;
        }
    }

    /**
     * @param welcomeHint 栈底欢迎页的提示文案，null 表示只显示图标
     */
    public static PaneStackFragment newInstance(CharSequence welcomeHint) {
        PaneStackFragment fragment = new PaneStackFragment();
        Bundle args = new Bundle();
        args.putCharSequence(ARG_WELCOME_HINT, welcomeHint);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * 栈底不是欢迎页而是一个真实页面（工作台 tab：左栏是欢迎，右栏始终是工作台网页）。
     *
     * @param rootPage 由 {@link PanePageFragment#forFragment} 造出来的栈底页
     */
    public static PaneStackFragment newInstanceWithRootPage(PanePageFragment rootPage) {
        PaneStackFragment fragment = new PaneStackFragment();
        Bundle args = new Bundle();
        args.putBundle(ARG_ROOT_PAGE_ARGS, rootPage.getArguments());
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnStackChangedListener(OnStackChangedListener listener) {
        this.onStackChangedListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.pane_stack_fragment, container, false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FragmentManager fm = getChildFragmentManager();
        if (savedInstanceState == null && fm.findFragmentByTag(TAG_ROOT) == null) {
            fm.beginTransaction()
                .setReorderingAllowed(true)
                .add(R.id.paneStackContainerFrameLayout, createRootFragment(), TAG_ROOT)
                .commitAllowingStateLoss();
        }
        // 装在 onCreate 而不是 onViewCreated：视图每次配置变化都会重建，
        // 装在那里会一次次叠加同一个监听器。
        fm.addOnBackStackChangedListener(() -> {
            if (onStackChangedListener != null) {
                onStackChangedListener.onPaneStackChanged(this);
            }
        });
    }

    private Fragment createRootFragment() {
        Bundle rootPageArgs = getArguments() == null ? null : getArguments().getBundle(ARG_ROOT_PAGE_ARGS);
        if (rootPageArgs != null) {
            PanePageFragment rootPage = new PanePageFragment();
            rootPage.setArguments(rootPageArgs);
            return rootPage;
        }
        CharSequence hint = getArguments() == null ? null : getArguments().getCharSequence(ARG_WELCOME_HINT);
        return PaneWelcomeFragment.newInstance(hint);
    }

    public void openPage(Intent intent, boolean resetFirst) {
        openPage(intent, resetFirst, null, -1);
    }

    /**
     * 在本栈打开一个页面。
     *
     * @param resetFirst  true 表示先把本栈退回栈底再压入（从<strong>左栏</strong>选了另一项：换内容）；
     *                    false 表示直接压栈（在<strong>右栏内部</strong>往下钻：开一层，返回要回得去）。
     * @param caller      发起跳转的 Fragment，需要回传结果时非 null
     * @param requestCode 发起方的请求码，&lt; 0 表示不需要结果
     */
    public void openPage(Intent intent, boolean resetFirst, @Nullable Fragment caller, int requestCode) {
        FragmentManager fm = getChildFragmentManager();
        if (resetFirst && fm.getBackStackEntryCount() > 0) {
            fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            pushAfterPop(intent, caller, requestCode);
            return;
        }
        pushPage(intent, caller, requestCode);
    }

    /**
     * 用一个新页面<strong>顶替</strong>发起者所在的那一页：先把那一页（连同压在它上面的）弹掉，
     * 再把新页压到它原来的位置。这是手机端 {@code startActivity(下一页)} 紧跟
     * {@code finish()} 在右栏里的等价物 —— 发起页从返回栈里消失，返回时直接回到它下面那一层。
     * <p>
     * 与「压栈」的区别只在产品语义：从用户资料点「发消息」，资料页该留着（返回回得去）；
     * 从「发起群聊」的选人页建完群，选人页就该消失（返回不该再回到一个已经用完的选择器）。
     *
     * @return false 表示顶替不了（发起者不在本栈里，或它就是栈底），调用方按原路径处理
     */
    public boolean replacePage(@Nullable Fragment caller, Intent intent) {
        PanePageFragment page = pageOf(caller);
        if (page == null || !popPage(page)) {
            return false;
        }
        pushAfterPop(intent, null, -1);
        return true;
    }

    /**
     * 刚刚请求过出栈，紧接着要压入新页。
     * <p>
     * 两者不能挤在同一批事务里：FragmentManager 在一批里会做操作合并/重排，入栈可能被排到
     * 出栈前面，届时新页刚 add 进去就被随后的出栈反向操作波及（那些出栈条目里记着 hide(下层页)，
     * 反向执行会把下层页又显示出来）。post 一下，等这一批执行完再压入，顺序就确定了。
     * <p>
     * post 到<strong>本栈</strong>的视图上而不是发起页的视图：顶替场景里发起页正要被移除。
     */
    private void pushAfterPop(Intent intent, @Nullable Fragment caller, int requestCode) {
        View view = getView();
        if (view == null) {
            pushPage(intent, caller, requestCode);
            return;
        }
        view.post(() -> {
            if (isAdded()) {
                pushPage(intent, caller, requestCode);
            }
        });
    }

    /**
     * 发起者所在的那一页。发起者是页面内容 Fragment（或它的子 Fragment），
     * 沿父链往上找到包着它的那层 {@link PanePageFragment}。
     */
    @Nullable
    private static PanePageFragment pageOf(@Nullable Fragment caller) {
        for (Fragment f = caller; f != null; f = f.getParentFragment()) {
            if (f instanceof PanePageFragment) {
                return (PanePageFragment) f;
            }
        }
        return null;
    }

    private void pushPage(Intent intent, @Nullable Fragment caller, int requestCode) {
        FragmentManager fm = getChildFragmentManager();
        // 栈内单例：这个页面已经在本条栈里了，就退回到它，而不是在上面再叠一个。
        // 「会话A → 用户资料 → 发消息」正是这条路径，不去重的话会得到两层会话A。
        String pageKey = PaneRegistry.pageKeyOf(intent);
        PanePageFragment existing = pageKey == null ? null : findPageByKey(pageKey);
        if (existing != null) {
            String entryName = existing.getStackEntryName();
            if (entryName == null) {
                // 栈底页（不在返回栈里）：把压在它上面的全部弹掉
                fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            } else {
                // flags = 0：只弹掉压在它上面的，它自己留在栈顶
                fm.popBackStack(entryName, 0);
            }
            // 相当于 singleTop 的 onNewIntent：这次打开若还带了新的意图（定位到某条消息），
            // 由页面自己决定要不要响应。
            existing.onNewPageIntent(intent);
            // 已在栈里的页面被再次打开时不接受新的结果订阅：它上一次的发起方可能还等着，
            // 覆盖掉会让那一方永远收不到回调。这与 singleTop 的 Activity 语义一致
            // （onNewIntent 不改变原来的 result receiver）。
            return;
        }

        // 当前显示的那一页：容器里最后一个被 add 且仍在的 Fragment。
        // 不按 tag 索引——出栈是异步的，按「栈深」拼出来的 tag 会和还没真正移除的旧页撞名。
        Fragment top = fm.findFragmentById(R.id.paneStackContainerFrameLayout);
        // 压进来的页面一律给返回箭头：它下面至少还有本栈的栈底（欢迎页或工作台网页），
        // 返回过去是有意义的。与微信 Pad 一致 —— 从左栏点开任何一项后，右栏左上角都能退回欢迎页。
        boolean showBack = true;
        // 每一层一个唯一的返回栈条目名。有了它，页面自己请求「关闭」时可以精确地
        // popBackStack(自己的名字, INCLUSIVE)：把自己连同压在自己上面的一起弹掉，
        // 且与「此刻是不是栈顶」「有没有别的出栈还排在队列里」都无关。
        // 用 UUID 而不是自增序号：条目名会随返回栈一起被保存/恢复，而进程被杀后重建时
        // 一个自增计数器是从 0 开始的，会和恢复出来的条目重名。
        String entryName = ENTRY_NAME_PREFIX + UUID.randomUUID();
        // setReorderingAllowed(true)：官方对返回栈事务的要求，setMaxLifecycle 与后续可能引入的
        // saveBackStack/restoreBackStack 都以它为前提。
        FragmentTransaction transaction = fm.beginTransaction().setReorderingAllowed(true);
        if (top != null) {
            // hide() 只是把视图设为不可见，Fragment 仍停在 RESUMED——被盖住的会话页会继续
            // 当自己在前台，把新到的消息标记为已读。压到 STARTED 才等价于手机端被覆盖的 Activity。
            // 出栈时事务反向执行，会自动还原成 RESUMED。
            transaction.hide(top).setMaxLifecycle(top, Lifecycle.State.STARTED);
        }
        // 必须是异步 commit：本方法可能在右栏页面自己的点击回调里被调用（会话页点头像进私聊），
        // commitNow 会在回调执行到一半时同步动这条栈；外层正在执行事务时还会抛
        // "FragmentManager is already executing transactions"。
        transaction
            .add(R.id.paneStackContainerFrameLayout, PanePageFragment.forIntent(intent, showBack, pageKey, entryName))
            .addToBackStack(entryName)
            .commitAllowingStateLoss();
        if (caller != null && requestCode >= 0) {
            pendingResults.put(entryName, new PendingResult(caller, requestCode));
        }
    }

    /**
     * 页面出栈时把它的结果投递给当初的发起方，等价于系统把 {@code onActivityResult} 送回
     * 发起 Activity/Fragment。由 {@link PanePageFragment#onDestroy()} 在确认自己是被移除
     * （而不是配置变化重建）时调用。
     * <p>
     * 没有登记过发起方（普通的 {@code startActivity}）时什么也不做。
     */
    void deliverPageResult(PanePageFragment page) {
        String entryName = page.getStackEntryName();
        if (entryName == null) {
            return;
        }
        PendingResult pending = pendingResults.remove(entryName);
        if (pending == null) {
            return;
        }
        Fragment caller = pending.caller.get();
        if (caller == null || !caller.isAdded()) {
            return;
        }
        caller.onActivityResult(pending.requestCode, page.getPageResultCode(), page.getPageResultData());
    }

    /**
     * 本条栈里 key 相同的那一页。只在<strong>本条栈</strong>里找：别的 tab 的栈里开着同一个会话
     * 是允许的，那是另一条导航路径上的另一个页面。
     */
    @Nullable
    private PanePageFragment findPageByKey(String pageKey) {
        for (Fragment fragment : getChildFragmentManager().getFragments()) {
            if (fragment instanceof PanePageFragment && !fragment.isRemoving()
                && pageKey.equals(((PanePageFragment) fragment).getPageKey())) {
                return (PanePageFragment) fragment;
            }
        }
        return null;
    }

    /**
     * 栈顶页面。栈底是欢迎页（不是 {@link PanePageFragment}）时返回 null。
     */
    @Nullable
    public PanePageFragment getTopPage() {
        Fragment fragment = getChildFragmentManager().findFragmentById(R.id.paneStackContainerFrameLayout);
        return fragment instanceof PanePageFragment ? (PanePageFragment) fragment : null;
    }

    /**
     * 栈顶页面的启动 intent，用于判重（同一个会话重复点击不该重建，否则草稿和滚动位置全丢）。
     */
    @Nullable
    public Intent getTopPageIntent() {
        PanePageFragment top = getTopPage();
        return top == null ? null : top.getPageIntent();
    }

    public boolean canPop() {
        return getChildFragmentManager().getBackStackEntryCount() > 0;
    }

    /**
     * 关闭指定的那一页：把它连同压在它上面的页面一起弹掉。这是手机端
     * {@code Activity.finish()} 在右栏里的等价物，点返回箭头、会话页自知失效走的也是它。
     * <p>
     * 用<strong>条目名</strong>而不是「此刻是不是栈顶」来定位，于是与队列里排在前面的其它
     * 出栈操作互不干扰：若那一页已经被别的操作弹掉了，{@code popBackStack} 找不到该条目，
     * 自然什么也不做。用户资料页「发消息后关掉自己」正是这种叠加场景
     * ——发消息那一步已经退回到栈里的会话页，把用户资料页一并带走了。
     *
     * @return false 表示这一页是栈底（不在返回栈里），无处可退
     */
    boolean popPage(PanePageFragment page) {
        String entryName = page.getStackEntryName();
        if (entryName == null || !canPop()) {
            return false;
        }
        getChildFragmentManager().popBackStack(entryName, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        return true;
    }

    /**
     * 返回键。先交给栈顶页面消费，未消费则出栈；栈已空返回 false，由主界面按原语义处理。
     */
    public boolean onBackPressed() {
        PanePageFragment top = getTopPage();
        if (top != null && top.getView() != null && top.onBackPressed()) {
            return true;
        }
        if (canPop()) {
            getChildFragmentManager().popBackStack();
            return true;
        }
        return false;
    }

    /**
     * 把本栈退回栈底（会话被删除、退群等，栈上那页已经没有意义）。
     */
    public void reset() {
        if (canPop()) {
            getChildFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
    }

    @Override
    public void onDestroy() {
        pendingResults.clear();
        super.onDestroy();
    }
}
