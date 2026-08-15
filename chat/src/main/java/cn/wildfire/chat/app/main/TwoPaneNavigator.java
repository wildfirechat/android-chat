/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.main;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Lifecycle;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.conversation.ConversationActivity;
import cn.wildfire.chat.kit.conversationlist.ConversationListFragment;
import cn.wildfire.chat.kit.pane.PaneRegistry;
import cn.wildfire.chat.kit.pane.PaneStackFragment;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.ConversationInfo;

/**
 * 平板双栏主界面「右栏」的导航器：<strong>每个 tab 一条独立的导航栈</strong>。
 * <p>
 * 只有 {@code WfcDeviceUtils.isTwoPaneLayout()} 为 true 时 {@link MainActivity} 才创建它，
 * 手机上这个类不会被实例化，因此其中的任何逻辑都影响不到手机端。
 * <p>
 * 三条规则（与 flutter 端 {@code PadHome} 一致）：
 * <ol>
 *   <li>左栏点开的页面进<strong>当前 tab</strong> 的右栏栈，右栏内再点开的页面压在同一条栈上；</li>
 *   <li>切到别的 tab，右栏显示那条栈自己的内容，没进过的 tab 是欢迎页；</li>
 *   <li>媒体预览、音视频通话等未在 {@link PaneRegistry} 登记的页面仍然全屏打开。</li>
 * </ol>
 */
public class TwoPaneNavigator {

    private static final String STATE_CONVERSATION = "twoPaneConversation";

    private final MainActivity activity;
    private final View paneContainer;

    /**
     * 每个 tab 一条栈，下标与左栏 ViewPager 的页下标一一对应。没进过的 tab 为 null——
     * 工作台那条栈一建出来就是个 WebView，会去拉远端页面，不该因为「开了双栏」就白拉一次。
     */
    private final List<PaneStackFragment> stacks = new ArrayList<>();
    /**
     * 每条栈的构造方式。栈是懒建的，但「这个 tab 的栈底该是什么」在 tab 装配时就定了。
     */
    private final List<StackSpec> stackSpecs = new ArrayList<>();

    private int currentTab = 0;

    /**
     * 最近一次按下发生在右栏里。<strong>仅作兜底</strong>：调用点用
     * {@link cn.wildfire.chat.kit.page.WfcPageCompat#startPage} 时发起者是已知的，压栈还是换内容
     * 由它确定；但仓库里还有上百处裸 {@code startActivity}，那条路上发起者拿不到 ——
     * androidx fragment 1.5 起 {@code Fragment.startActivity} 直接调
     * {@code ContextCompat.startActivity(Activity, ...)}，到达主界面时已经不知道是谁发起的。
     * <p>
     * 对这些调用点，按下点落在哪一栏是唯一可用的信号，且导航几乎都由点击触发，
     * 判错的后果也只是多压一层还是少压一层。页面逐个改用 {@code startPage} 之后，
     * 走到这里的情况会越来越少。
     */
    private boolean lastTouchInPane;

    /**
     * IM 服务是否已就绪。冷启动（通知点击拉起 App）时路由 intent 会早于 IM 连接到达，先暂存。
     */
    private boolean imServiceReady;
    private Intent pendingIntent;
    private int pendingIntentTab = -1;

    private ConversationListFragment conversationListFragment;
    private Conversation selectedConversation;
    /**
     * 选中的会话压在哪条栈上。五条栈各自独立，会话可能压在一条已经切走的栈上，
     * 只记一个「有没有」是不够的。
     */
    private int selectedConversationTab = -1;
    /**
     * 选中的会话在左栏列表里出现过。新建的会话在发出首条消息前本来就不在列表里，
     * 不先确认「来过」就清栏，会把刚点开的新会话立刻关掉。
     */
    private boolean selectedSeenInList;

    private static final class StackSpec {
        final CharSequence welcomeHint;
        final Class<? extends Fragment> rootFragmentClass;
        final Bundle rootFragmentArgs;
        final CharSequence rootTitle;

        StackSpec(CharSequence welcomeHint) {
            this(welcomeHint, null, null, null);
        }

        StackSpec(CharSequence welcomeHint, Class<? extends Fragment> rootFragmentClass,
                  Bundle rootFragmentArgs, CharSequence rootTitle) {
            this.welcomeHint = welcomeHint;
            this.rootFragmentClass = rootFragmentClass;
            this.rootFragmentArgs = rootFragmentArgs;
            this.rootTitle = rootTitle;
        }
    }

    public TwoPaneNavigator(MainActivity activity) {
        this.activity = activity;
        this.paneContainer = activity.findViewById(R.id.paneContainerFrameLayout);
    }

    // ==================== 装配 ====================

    /**
     * 登记一个「左栏是列表、右栏欢迎页打底」的 tab。
     */
    public void addTab(CharSequence welcomeHint) {
        stackSpecs.add(new StackSpec(welcomeHint));
        stacks.add(null);
    }

    /**
     * 登记一个「右栏栈底就是一个真实页面」的 tab（工作台：左栏欢迎，右栏始终是工作台网页）。
     */
    public void addTabWithRootPage(Class<? extends Fragment> rootFragmentClass, Bundle rootFragmentArgs,
                                   CharSequence rootTitle) {
        stackSpecs.add(new StackSpec(null, rootFragmentClass, rootFragmentArgs, rootTitle));
        stacks.add(null);
    }

    /**
     * tab 全部登记完成后调用：建出当前 tab 的栈并显示。
     */
    public void start(int initialTab) {
        adoptRestoredStacks();
        currentTab = clampTab(initialTab);
        showTab(currentTab);
        if (pendingIntent != null && imServiceReady) {
            flushPendingIntent();
        }
    }

    public void setConversationListFragment(ConversationListFragment fragment) {
        this.conversationListFragment = fragment;
        syncListSelection();
    }

    // ==================== tab 切换 ====================

    public void setCurrentTab(int tab) {
        tab = clampTab(tab);
        if (tab == currentTab && stacks.get(tab) != null) {
            return;
        }
        currentTab = tab;
        showTab(tab);
        syncListSelection();
    }

    private int clampTab(int tab) {
        if (stackSpecs.isEmpty()) {
            return 0;
        }
        return Math.max(0, Math.min(tab, stackSpecs.size() - 1));
    }

    private void showTab(int tab) {
        if (paneContainer == null || tab >= stackSpecs.size()) {
            return;
        }
        FragmentManager fm = activity.getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        for (int i = 0; i < stacks.size(); i++) {
            PaneStackFragment stack = stacks.get(i);
            if (stack != null && i != tab) {
                // 压到 STARTED，切走的那条栈上的会话页才会真的 onPause——
                // 只 hide 的话它仍是 RESUMED，会在后台继续把新消息标记成已读。
                transaction.hide(stack).setMaxLifecycle(stack, Lifecycle.State.STARTED);
            }
        }
        PaneStackFragment stack = stacks.get(tab);
        if (stack == null) {
            stack = createStack(tab);
            stacks.set(tab, stack);
            transaction.add(R.id.paneContainerFrameLayout, stack, stackTag(tab));
        } else {
            transaction.show(stack).setMaxLifecycle(stack, Lifecycle.State.RESUMED);
        }
        transaction.commitAllowingStateLoss();
    }

    /**
     * 认领配置变化（旋转、分屏）后 FragmentManager 自动恢复出来的那几条栈。
     * <p>
     * 不认领的话，{@link #showTab} 会用同一个 tag 再 add 一条新的空栈，
     * 旋转一下右栏里正在看的页面就没了，而旧栈还挂在容器里。
     */
    private void adoptRestoredStacks() {
        FragmentManager fm = activity.getSupportFragmentManager();
        for (int i = 0; i < stacks.size(); i++) {
            if (stacks.get(i) != null) {
                continue;
            }
            Fragment restored = fm.findFragmentByTag(stackTag(i));
            if (restored instanceof PaneStackFragment) {
                PaneStackFragment stack = (PaneStackFragment) restored;
                stack.setOnStackChangedListener(changed -> syncListSelection());
                stacks.set(i, stack);
            }
        }
    }

    private PaneStackFragment createStack(int tab) {
        StackSpec spec = stackSpecs.get(tab);
        PaneStackFragment stack;
        if (spec.rootFragmentClass != null) {
            stack = PaneStackFragment.newInstanceWithRootPage(
                cn.wildfire.chat.kit.pane.PanePageFragment.forFragment(
                    spec.rootFragmentClass, spec.rootFragmentArgs, spec.rootTitle, false));
        } else {
            stack = PaneStackFragment.newInstance(spec.welcomeHint);
        }
        stack.setOnStackChangedListener(changed -> syncListSelection());
        return stack;
    }

    private static String stackTag(int tab) {
        return "twoPaneStack_" + tab;
    }

    // ==================== 打开页面 ====================

    public boolean openInPane(Intent intent) {
        return openInPane(null, intent, -1);
    }

    /**
     * 尝试在右栏打开这个 intent。
     * <p>
     * <strong>压栈还是换内容，由发起者的位置决定</strong>：发起者本身就在某条右栏栈里
     * （在群信息页点「群成员」），说明是在往下钻，压到<strong>它所在的那条栈</strong>上；
     * 发起者在左栏或直接来自 Activity（点会话列表、点右上角搜索），说明是换内容，
     * 把当前 tab 的栈退回栈底再压入。
     * <p>
     * 改造前这里靠「上一次按下点落在哪一栏」来猜，异步回调、对话框、通知等非触摸触发的
     * 导航都会猜错；现在是确定的。
     *
     * @param caller      发起跳转的 Fragment，来自 {@code startActivityFromFragment}；
     *                    Activity 直接发起时为 null
     * @param requestCode 发起方的请求码，&lt; 0 表示不需要回传结果
     * @return true 表示已在右栏打开，调用方不要再 {@code startActivity}；
     * false 表示这个页面不在右栏承载（未登记、参数不全、或是必须全屏的选择器），按原路径全屏打开。
     */
    public boolean openInPane(@Nullable Fragment caller, Intent intent, int requestCode) {
        // 隐式 intent（仓库里确实有 new Intent(WfcIntent.ACTION_MOMENT) 这种写法）先补上 component，
        // 否则永远匹配不到注册项，只能全屏打开。补出来的只在右栏内部用；未登记时启动的仍是原 intent。
        intent = PaneRegistry.resolveComponent(activity, intent);
        if (intent == null || !PaneRegistry.isRegistered(intent)) {
            return false;
        }
        if (!imServiceReady) {
            // 冷启动阶段右栏还没装好，先记下来，等 onImServiceReady 再打开。
            // 此时还没有栈可压，需要结果的跳转只能走全屏。
            if (requestCode >= 0) {
                return false;
            }
            pendingIntent = intent;
            pendingIntentTab = currentTab;
            return true;
        }
        int callerTab = tabOf(callerStack(caller));
        if (callerTab >= 0) {
            // 发起者就在某条右栏栈里：往下钻，压到它那条栈上
            return openInTab(callerTab, intent, false, caller, requestCode);
        }
        if (caller != null) {
            // 发起者已知且不在右栏（左栏列表、ViewPager 里的页）：换当前 tab 的内容
            return openInTab(currentTab, intent, true, caller, requestCode);
        }
        // 发起者未知，只能按落点兜底，见 lastTouchInPane
        return openInTab(currentTab, intent, !lastTouchInPane, null, requestCode);
    }

    /**
     * 用新页面顶替发起者所在的那一页，见 {@link PaneStackFragment#replacePage}。
     * 发起者不在右栏（手机端、左栏、独立全屏页）时返回 false，调用方按原路径处理。
     */
    public boolean replaceInPane(@Nullable Fragment caller, Intent intent) {
        PaneStackFragment stack = callerStack(caller);
        int tab = tabOf(stack);
        if (tab < 0) {
            return false;
        }
        intent = PaneRegistry.resolveComponent(activity, intent);
        // 与 openInTab 同一道关：造一个丢弃的实例问「这个 intent 能不能进右栏」。
        // 进不了的话不能先把发起页弹掉——那会只剩一片空白。
        if (intent == null || PaneRegistry.createPage(activity, intent) == null) {
            return false;
        }
        if (!stack.replacePage(caller, intent)) {
            return false;
        }
        rememberSelectedConversation(intent, tab);
        return true;
    }

    /**
     * 发起者所在的右栏栈；发起者在左栏、或根本不是 Fragment 时返回 null。
     */
    @Nullable
    private PaneStackFragment callerStack(@Nullable Fragment caller) {
        for (Fragment f = caller; f != null; f = f.getParentFragment()) {
            if (f instanceof PaneStackFragment) {
                return (PaneStackFragment) f;
            }
        }
        return null;
    }

    private int tabOf(@Nullable PaneStackFragment stack) {
        return stack == null ? -1 : stacks.indexOf(stack);
    }

    /**
     * 在指定 tab 的栈里打开页面，并把左栏切到该 tab。用于外部入口（通知点击、深链）。
     */
    public boolean openInTab(int tab, Intent intent, boolean resetFirst) {
        return openInTab(tab, intent, resetFirst, null, -1);
    }

    public boolean openInTab(int tab, Intent intent, boolean resetFirst,
                             @Nullable Fragment caller, int requestCode) {
        if (stackSpecs.isEmpty()) {
            return false;
        }
        tab = clampTab(tab);
        intent = PaneRegistry.resolveComponent(activity, intent);
        // 造一个丢弃的实例来问「这个 intent 到底能不能进右栏」：工厂对选择器形态（带 forResult /
        // pick）会返回 null。此刻只是建对象、没有视图，代价可以忽略。
        if (intent == null || PaneRegistry.createPage(activity, intent) == null) {
            return false;
        }
        if (tab != currentTab) {
            // 先自己切，再通知左栏：ViewPager2 的 onPageSelected 是异步回调的，
            // 等它回来再建栈就晚了（本方法紧接着就要用这条栈）。
            currentTab = tab;
            showTab(tab);
            activity.selectTab(tab);
        } else if (stacks.get(tab) == null) {
            showTab(tab);
        }
        PaneStackFragment stack = stacks.get(tab);
        if (stack == null) {
            return false;
        }
        if (resetFirst && requestCode < 0 && isSameConversationAsTop(stack, intent)) {
            // 重复点开同一个会话：直接返回，避免重建 Fragment 丢掉草稿与滚动位置
            return true;
        }
        stack.openPage(intent, resetFirst, caller, requestCode);
        rememberSelectedConversation(intent, tab);
        return true;
    }

    /**
     * 栈顶已经是同一个会话，且本次不需要定位到某条消息。
     * <p>
     * 需要定位时即使是同一个会话也要重建，否则从搜索结果点进「当前已打开的会话」不会跳转。
     */
    private boolean isSameConversationAsTop(PaneStackFragment stack, Intent intent) {
        Conversation conversation = intent.getParcelableExtra("conversation");
        if (conversation == null || !stack.canPop()) {
            return false;
        }
        if (intent.getLongExtra("toFocusMessageId", -1) > 0
            || intent.getLongExtra("highlightMessageId", 0) != 0) {
            return false;
        }
        Intent top = stack.getTopPageIntent();
        if (top == null || top.getComponent() == null
            || !ConversationActivity.class.getName().equals(top.getComponent().getClassName())) {
            return false;
        }
        return Conversation.equals(top.getParcelableExtra("conversation"), conversation);
    }

    /**
     * @param tab 这个会话被打开在哪条栈上。不能一律用 {@code currentTab}：顶替
     *            （{@link #replaceInPane}）发生在发起者自己那条栈上，可能并非当前 tab。
     */
    private void rememberSelectedConversation(Intent intent, int tab) {
        Conversation conversation = intent.getParcelableExtra("conversation");
        if (conversation != null && intent.getComponent() != null
            && ConversationActivity.class.getName().equals(intent.getComponent().getClassName())) {
            selectedConversation = conversation;
            selectedConversationTab = tab;
            selectedSeenInList = false;
        }
    }

    // ==================== 外部入口（通知、深链） ====================

    /**
     * IM 服务就绪（{@code MainActivity.init()} 之后）。冲掉暂存的 intent。
     */
    public void onImServiceReady() {
        imServiceReady = true;
        flushPendingIntent();
    }

    private void flushPendingIntent() {
        if (pendingIntent == null || stackSpecs.isEmpty()) {
            return;
        }
        Intent intent = pendingIntent;
        int tab = pendingIntentTab < 0 ? currentTab : pendingIntentTab;
        pendingIntent = null;
        pendingIntentTab = -1;
        openInTab(tab, intent, true);
    }

    /**
     * 从其他页面或通知路由过来的「打开会话」启动 intent：进消息 tab 的栈。
     */
    public void handleLaunchIntent(Intent conversationIntent) {
        if (conversationIntent == null) {
            return;
        }
        if (!imServiceReady || stackSpecs.isEmpty()) {
            pendingIntent = conversationIntent;
            pendingIntentTab = 0;
            return;
        }
        openInTab(0, conversationIntent, true);
    }

    // ==================== 返回键与触点 ====================

    public boolean onBackPressed() {
        PaneStackFragment stack = currentStack();
        return stack != null && stack.onBackPressed();
    }

    /**
     * 记录按下点落在哪一栏，见 {@link #lastTouchInPane}。
     */
    public void recordTouchOrigin(float rawX, float rawY) {
        if (paneContainer == null) {
            return;
        }
        int[] location = new int[2];
        paneContainer.getLocationOnScreen(location);
        Rect rect = new Rect(location[0], location[1],
            location[0] + paneContainer.getWidth(), location[1] + paneContainer.getHeight());
        lastTouchInPane = rect.contains((int) rawX, (int) rawY);
    }

    @Nullable
    private PaneStackFragment currentStack() {
        return currentTab < stacks.size() ? stacks.get(currentTab) : null;
    }

    // ==================== 左栏选中态与会话删除 ====================

    private void syncListSelection() {
        if (conversationListFragment == null) {
            return;
        }
        conversationListFragment.setSelectedConversation(conversationOnTopOfMessageTab());
    }

    /**
     * 消息 tab 的栈顶是不是一个会话页；不是则左栏不该有高亮。
     * 高亮只跟消息 tab 走：在通讯录那一栏打开的会话，左栏此刻显示的是通讯录，没有可高亮的行。
     */
    @Nullable
    private Conversation conversationOnTopOfMessageTab() {
        if (currentTab != 0 || stacks.isEmpty()) {
            return null;
        }
        PaneStackFragment stack = stacks.get(0);
        if (stack == null) {
            return null;
        }
        Intent top = stack.getTopPageIntent();
        if (top == null || top.getComponent() == null
            || !ConversationActivity.class.getName().equals(top.getComponent().getClassName())) {
            return null;
        }
        return top.getParcelableExtra("conversation");
    }

    /**
     * 左栏会话列表更新：右栏打开的会话若已从列表中消失（删除会话、退群等），把那一栏退回栈底。
     */
    public void onConversationListChanged(List<ConversationInfo> conversationInfos) {
        if (selectedConversation == null || conversationInfos == null) {
            return;
        }
        for (ConversationInfo info : conversationInfos) {
            if (Conversation.equals(info.conversation, selectedConversation)) {
                selectedSeenInList = true;
                return;
            }
        }
        if (!selectedSeenInList) {
            return;
        }
        selectedSeenInList = false;
        selectedConversation = null;
        int tab = selectedConversationTab;
        selectedConversationTab = -1;
        // 只退它所在的那一栏。别的 tab 的栈上可能压着完全无关的页面，不该被连累。
        if (tab >= 0 && tab < stacks.size() && stacks.get(tab) != null) {
            stacks.get(tab).reset();
        }
        syncListSelection();
    }

    // ==================== 状态保存 ====================

    public void onSaveInstanceState(Bundle outState) {
        Conversation conversation = currentPaneConversation();
        if (conversation != null) {
            outState.putParcelable(STATE_CONVERSATION, conversation);
        }
    }

    /**
     * 当前 tab 右栏栈顶正在显示的会话。窗口从双栏变窄到单栏时，
     * {@link MainActivity} 用它把会话交还给独立会话页。
     */
    @Nullable
    private Conversation currentPaneConversation() {
        PaneStackFragment stack = currentStack();
        if (stack == null) {
            return null;
        }
        Intent top = stack.getTopPageIntent();
        if (top == null || top.getComponent() == null
            || !ConversationActivity.class.getName().equals(top.getComponent().getClassName())) {
            return null;
        }
        return top.getParcelableExtra("conversation");
    }

    public static Conversation getSavedConversation(Bundle savedInstanceState) {
        return savedInstanceState == null ? null : savedInstanceState.getParcelable(STATE_CONVERSATION);
    }

    /**
     * 单栏布局下重建时，把 FragmentManager 自动恢复出来的右栏栈清掉：
     * 单栏布局里没有 {@code paneContainerFrameLayout}，留着它们只会挂在一个不存在的容器上。
     */
    public static void removeRestoredStacks(FragmentActivity activity) {
        FragmentManager fm = activity.getSupportFragmentManager();
        FragmentTransaction transaction = null;
        for (Fragment fragment : fm.getFragments()) {
            if (fragment instanceof PaneStackFragment) {
                if (transaction == null) {
                    transaction = fm.beginTransaction();
                }
                transaction.remove(fragment);
            }
        }
        if (transaction != null) {
            transaction.commitNowAllowingStateLoss();
        }
    }
}
