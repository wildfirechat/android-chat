/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.main;

import android.Manifest;
import android.animation.ArgbEvaluator;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.webkit.CookieManager;
import android.webkit.WebStorage;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager2.widget.ViewPager2;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.material.bottomnavigation.BottomNavigationMenuView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.king.zxing.Intents;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import cn.wildfire.chat.app.AppService;
import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.IMConnectionStatusViewModel;
import cn.wildfire.chat.kit.IMServiceStatusViewModel;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.WfcScheme;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.contact.ContactListActivity;
import cn.wildfire.chat.kit.contact.ContactListFragment;
import cn.wildfire.chat.kit.contact.ContactViewModel;
import cn.wildfire.chat.kit.contact.newfriend.SearchUserActivity;
import cn.wildfire.chat.kit.conversation.ConversationActivity;
import cn.wildfire.chat.kit.conversation.ConversationRouter;
import cn.wildfire.chat.kit.conversation.ConversationViewModel;
import cn.wildfire.chat.kit.conversation.CreateConversationActivity;
import cn.wildfire.chat.kit.conversation.forward.ForwardActivity;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfire.chat.kit.conversationlist.ConversationListFragment;
import cn.wildfire.chat.kit.conversationlist.ConversationListViewModel;
import cn.wildfire.chat.kit.conversationlist.ConversationListViewModelFactory;
import cn.wildfire.chat.kit.net.OKHttpHelper;
import cn.wildfire.chat.kit.page.WfcPageNavigator;
import cn.wildfire.chat.kit.pane.PaneWelcomeFragment;
import cn.wildfire.chat.kit.qrcode.ScanQRCodeActivity;
import cn.wildfire.chat.kit.search.SearchPortalActivity;
import cn.wildfire.chat.kit.user.ChangeMyNameActivity;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfire.chat.kit.utils.FileUtils;
import cn.wildfire.chat.kit.utils.WfcDeviceUtils;
import cn.wildfire.chat.kit.viewmodel.MessageViewModel;
import cn.wildfire.chat.kit.workspace.WebViewFragment;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.client.ConnectionStatus;
import cn.wildfirechat.message.FileMessageContent;
import cn.wildfirechat.message.ImageMessageContent;
import cn.wildfirechat.message.LinkMessageContent;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.MessageContent;
import cn.wildfirechat.message.TextMessageContent;
import cn.wildfirechat.message.VideoMessageContent;
import cn.wildfirechat.message.core.MessageContentType;
import cn.wildfirechat.message.core.MessageStatus;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.uikit.menu.PopupMenu;
import cn.wildfirechat.uikit.permission.PermissionKit;
import q.rorbin.badgeview.QBadgeView;

public class MainActivity extends WfcBaseActivity implements WfcPageNavigator {

    private List<Fragment> mFragmentList = new ArrayList<>(4);

    /**
     * 平板双栏的右栏导航器（每个 tab 一条栈）。**手机端恒为 null**，本文件中所有
     * {@code twoPaneNavigator != null} 分支在手机上都走不到，手机路径与改造前逐行一致。
     */
    private TwoPaneNavigator twoPaneNavigator;

    BottomNavigationView bottomNavigationView;
    ViewPager2 contentViewPager;
    TextView startingTextView;
    LinearLayout contentLinearLayout;

    private QBadgeView unreadMessageUnreadBadgeView;
    private QBadgeView unreadFriendRequestBadgeView;
    private QBadgeView discoveryBadgeView;

    private static final int REQUEST_CODE_SCAN_QR_CODE = 100;
    private static final int REQUEST_CODE_PICK_CONTACT = 101;

    private boolean isInitialized = false;

    private ContactListFragment contactListFragment;
    private ConversationListFragment conversationListFragment;

    private ContactViewModel contactViewModel;
    private ConversationListViewModel conversationListViewModel;
    private long lastSelectConversatonListItemTimestamp = 0;
    private MenuItem secretChatMenuItem;
    private int appBarHeight = 0;
    private ArgbEvaluator argbEvaluator = new ArgbEvaluator();

    protected void bindViews() {
        super.bindViews();
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        contentViewPager = findViewById(R.id.contentViewPager);
        startingTextView = findViewById(R.id.startingTextView);
        contentLinearLayout = findViewById(R.id.contentLinearLayout);
    }

    @Override
    protected int contentLayout() {
        // 双栏布局只是在同一个 Activity 里多挂一个右栏，从而完整复用 manifest 里的
        // ${applicationId}.main、分享等 intent-filter 与 singleTask 语义——
        // 若另起一个 MainPadActivity，这些入口都要复制一份并处理两个主界面并存的问题。
        return WfcDeviceUtils.isTwoPaneLayout(this) ? R.layout.main_pad_activity : R.layout.main_activity;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (WfcDeviceUtils.isTwoPaneLayout(this)) {
            twoPaneNavigator = new TwoPaneNavigator(this);
            twoPaneNavigator.restoreState(savedInstanceState);
            setupTwoPaneImeAdjustment();
        } else {
            // 平板分屏把窗口拖窄到 600dp 以下时，Activity 会以单栏布局重建。
            // 单栏布局里没有右栏容器，先把 FragmentManager 自动恢复出来的那几条栈清掉，
            // 再把原来在右栏里的会话交还给独立会话页，避免用户正在聊的会话凭空消失。
            TwoPaneNavigator.removeRestoredStacks(this);
            Conversation conversation = TwoPaneNavigator.getSavedConversation(savedInstanceState);
            if (conversation != null) {
                startActivity(ConversationActivity.buildConversationIntent(this, conversation, null, -1));
            }
        }
        // 只在全新启动时处理启动 intent；配置变化重建时 savedInstanceState 非空，
        // 会话由上面的状态恢复负责，再处理一次会导致每次旋转都重新打开／重新高亮。
        if (savedInstanceState == null) {
            handleConversationLaunchIntent(getIntent());
        }
    }

    /**
     * 平板双栏下，软键盘弹出时把左栏底部导航藏起来，避免它被顶到键盘上方。
     * <p>
     * 手机端不会走到这里；双栏的输入框在右栏，但窗口级 adjustResize/adjustPan
     * 会让整个左栏一起变矮/上移，左栏底部的 tab 会被顶到键盘上方。这里监听窗口
     * 可见区域变化，识别到键盘高度后直接隐藏左栏底部导航，让左栏内容继续使用
     * 键盘上方的完整高度。
     */
    private void setupTwoPaneImeAdjustment() {
        final View content = findViewById(android.R.id.content);
        content.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (bottomNavigationView == null) {
                    return;
                }
                Rect rect = new Rect();
                content.getWindowVisibleDisplayFrame(rect);
                View root = content.getRootView();
                int rootHeight = root.getHeight();
                if (rootHeight <= 0 || rect.bottom <= 0) {
                    return;
                }

                int bottomInset = 0;
                WindowInsets windowInsets = root.getRootWindowInsets();
                if (windowInsets != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        bottomInset = windowInsets.getInsets(WindowInsets.Type.navigationBars()).bottom;
                    } else {
                        bottomInset = windowInsets.getStableInsetBottom();
                    }
                } else {
                    // 极少数情况下拿不到 Insets，退回资源里的导航栏高度，避免把导航栏误判成键盘。
                    int navBarResId = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
                    if (navBarResId > 0) {
                        bottomInset = getResources().getDimensionPixelSize(navBarResId);
                    }
                }

                // 与 KeyboardAwareLinearLayout 相同的键盘高度估算：
                // root 高度 - 当前可见区域底部 - 导航栏高度。
                int keyboardHeight = Math.max(0, rootHeight - rect.bottom - bottomInset);
                boolean keyboardShowing = keyboardHeight > 100;
                int targetVisibility = keyboardShowing ? View.GONE : View.VISIBLE;
                if (bottomNavigationView.getVisibility() != targetVisibility) {
                    bottomNavigationView.setVisibility(targetVisibility);
                }
            }
        });
    }

    /**
     * 处理 {@link ConversationRouter} 从其他页面（或通知）路由过来的「打开会话」启动 intent。
     * 手机端不会收到这种 intent（路由器在手机端直接 startActivity(ConversationActivity)）。
     */
    private void handleConversationLaunchIntent(Intent intent) {
        if (intent == null || !intent.getBooleanExtra(ConversationRouter.EXTRA_OPEN_CONVERSATION_IN_PANE, false)) {
            return;
        }
        if (twoPaneNavigator != null) {
            // 会话统一进消息 tab 那条栈；左栏由导航器一并切回会话列表，
            // 否则列表里的选中态用户根本看不见。
            twoPaneNavigator.handleLaunchIntent(intent);
            return;
        }
        // 通知只有 Application Context 可用，那里判定双栏在多窗口窄窗口下可能不准；
        // 真到了主界面发现是单栏，就退回独立会话页，语义与手机端一致。
        if (intent.getParcelableExtra("conversation") == null) {
            return;
        }
        Intent conversationIntent = new Intent(this, ConversationActivity.class);
        conversationIntent.putExtras(intent);
        startActivity(conversationIntent);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (twoPaneNavigator != null) {
            twoPaneNavigator.onSaveInstanceState(outState);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (contactViewModel != null) {
            contactViewModel.reloadFriendRequestStatus();
            conversationListViewModel.reloadConversationUnreadStatus();
        }
        updateMomentBadgeView();
    }

    @Override
    protected void afterViews() {
        if (!showWorkSpace()) {
            bottomNavigationView.getMenu().removeItem(R.id.workspace);
        }

        IMServiceStatusViewModel imServiceStatusViewModel = new ViewModelProvider(this).get(IMServiceStatusViewModel.class);
        imServiceStatusViewModel.imServiceStatusLiveData().observe(this, (status) -> {
            if (status && !isInitialized) {
                handleShareIntent();
                init();
                isInitialized = true;
                if (twoPaneNavigator != null) {
                    // 通知点击冷启动时，路由 intent 早于 IM 服务就绪到达，这里把它冲掉
                    twoPaneNavigator.onImServiceReady();
                }
            }
        });

        IMConnectionStatusViewModel connectionStatusViewModel = new ViewModelProvider(this).get(IMConnectionStatusViewModel.class);
        connectionStatusViewModel.connectionStatusLiveData().observe(this, status -> {
            if (status == ConnectionStatus.ConnectionStatusTokenIncorrect
                || status == ConnectionStatus.ConnectionStatusSecretKeyMismatch
                || status == ConnectionStatus.ConnectionStatusRejected
                || status == ConnectionStatus.ConnectionStatusLogout
                || status == ConnectionStatus.ConnectionStatusKickedoff) {
                SharedPreferences sp = getSharedPreferences(Config.SP_CONFIG_FILE_NAME, Context.MODE_PRIVATE);
                sp.edit()
                    .clear()
                    .putBoolean("hasReadUserAgreement", true)
                    .apply();
                sp = getSharedPreferences("moment", Context.MODE_PRIVATE);
                sp.edit().clear().apply();
                OKHttpHelper.clearCookies();

                WebStorage.getInstance().deleteAllData();
                CookieManager.getInstance().removeAllCookies(null);
                CookieManager.getInstance().flush();

                if (status == ConnectionStatus.ConnectionStatusLogout) {
                    reLogin(false);
                } else {
                    ChatManager.Instance().disconnect(true, false);
                    if (status == ConnectionStatus.ConnectionStatusKickedoff) {
                        reLogin(true);
                    }
                }
            } else if (status == ConnectionStatus.ConnectionStatusNotLicensed) {
                Toast.makeText(MainActivity.this, "专业版IM服务没有授权或者授权过期！！！", Toast.LENGTH_LONG).show();
            } else if (status == ConnectionStatus.ConnectionStatusTimeInconsistent) {
                Toast.makeText(MainActivity.this, "服务器和客户端时间相差太大！！！", Toast.LENGTH_LONG).show();
            } else if (status == ConnectionStatus.ConnectionStatusConnected) {
                if (secretChatMenuItem != null) {
                    boolean isEnableSecretChat = ChatManager.Instance().isEnableSecretChat();
                    secretChatMenuItem.setEnabled(isEnableSecretChat);
                }
            }
        });
        MessageViewModel messageViewModel = new ViewModelProvider(this).get(MessageViewModel.class);
        messageViewModel.messageLiveData().observe(this, uiMessages -> {
            for (UiMessage uiMessage : uiMessages) {
                if (uiMessage.message.messageId > 0 && (uiMessage.message.content.getMessageContentType() == MessageContentType.MESSAGE_CONTENT_TYPE_FEED
                    || uiMessage.message.content.getMessageContentType() == MessageContentType.MESSAGE_CONTENT_TYPE_FEED_COMMENT)) {
                    updateMomentBadgeView();
                }
            }
        });

        requestMandatoryPermissions();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String action = intent.getAction();
        String type = intent.getType();
        if (Intent.ACTION_SEND.equals(action)) {
            if ("text/plain".equals(type)) {
                handleSendText(intent);
            } else {
                Uri fileUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                handleSendFile(fileUri);
            }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
            handleSendMultiple(intent); // Handle multiple items being sent
        }
        handleConversationLaunchIntent(intent);
    }

    @Override
    protected void afterMenus(Menu menu) {
        super.afterMenus(menu);
        boolean isEnableSecretChat = ChatManager.Instance().isEnableSecretChat();
        if (!isEnableSecretChat) {
//            secretChatMenuItem = menu.findItem(R.id.secretChat);
//            secretChatMenuItem.setEnabled(false);
        }
    }

    private void handleShareIntent() {
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        if (Intent.ACTION_SEND.equals(action)) {
            if ("text/plain".equals(type)) {
                handleSendText(intent);
            } else {
                Uri fileUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                handleSendFile(fileUri);
            }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
            handleSendMultiple(intent);
        }
    }


    private void reLogin(boolean isKickedOff) {
        if (isFinishing()) {
            return;
        }
        Intent intent = new Intent(this, SplashActivity.class);
        intent.putExtra("isKickedOff", isKickedOff);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void init() {
        checkVersion();
        initView();

        conversationListViewModel = new ViewModelProvider(this, new ConversationListViewModelFactory(Arrays.asList(Conversation.ConversationType.Single, Conversation.ConversationType.Group, Conversation.ConversationType.Channel, Conversation.ConversationType.SecretChat), Arrays.asList(0)))
            .get(ConversationListViewModel.class);
        conversationListViewModel.unreadCountLiveData().observe(this, unreadCount -> {

            if (unreadCount != null && unreadCount.unread > 0) {
                showUnreadMessageBadgeView(unreadCount.unread);
            } else {
                hideUnreadMessageBadgeView();
            }
        });
        if (twoPaneNavigator != null) {
            // 双栏：会话被删除/退群后，右栏回到欢迎页
            conversationListViewModel.conversationListLiveData()
                .observe(this, conversationInfos -> twoPaneNavigator.onConversationListChanged(conversationInfos));
        }

        contactViewModel = WfcUIKit.getAppScopeViewModel(ContactViewModel.class);
        contactViewModel.friendRequestUpdatedLiveData().observe(this, count -> {
            if (count == null || count == 0) {
                hideUnreadFriendRequestBadgeView();
            } else {
                showUnreadFriendRequestBadgeView(count);
            }
        });

        checkDisplayName();
    }

    private void showUnreadMessageBadgeView(int count) {
        if (unreadMessageUnreadBadgeView == null) {
            BottomNavigationMenuView bottomNavigationMenuView = ((BottomNavigationMenuView) bottomNavigationView.getChildAt(0));
            View view = bottomNavigationMenuView.getChildAt(0);
            unreadMessageUnreadBadgeView = new QBadgeView(MainActivity.this);
            unreadMessageUnreadBadgeView.bindTarget(view);
        }
        unreadMessageUnreadBadgeView.setBadgeNumber(count);
    }

    private void hideUnreadMessageBadgeView() {
        if (unreadMessageUnreadBadgeView != null) {
            unreadMessageUnreadBadgeView.hide(true);
            unreadMessageUnreadBadgeView = null;
        }
    }

    private void updateMomentBadgeView() {
        if (!WfcUIKit.getWfcUIKit().isSupportMoment()) {
            return;
        }
        List<Message> messages = ChatManager.Instance().getMessagesEx2(Collections.singletonList(Conversation.ConversationType.Single), Collections.singletonList(1), Arrays.asList(MessageStatus.Unread), 0, true, 100, null);
        int count = messages == null ? 0 : messages.size();
        if (count > 0) {
            if (discoveryBadgeView == null) {
                BottomNavigationMenuView bottomNavigationMenuView = ((BottomNavigationMenuView) bottomNavigationView.getChildAt(0));
                int index = TextUtils.isEmpty(Config.getWorkspaceUrl()) ? 2 : 3;
                View view = bottomNavigationMenuView.getChildAt(index);
                discoveryBadgeView = new QBadgeView(MainActivity.this);
                discoveryBadgeView.bindTarget(view);
            }
            discoveryBadgeView.setBadgeNumber(count);
        } else {
            if (discoveryBadgeView != null) {
                discoveryBadgeView.hide(true);
                discoveryBadgeView = null;
            }
        }
    }

    private void showUnreadFriendRequestBadgeView(int count) {
        if (unreadFriendRequestBadgeView == null) {
            BottomNavigationMenuView bottomNavigationMenuView = ((BottomNavigationMenuView) bottomNavigationView.getChildAt(0));
            View view = bottomNavigationMenuView.getChildAt(1);
            unreadFriendRequestBadgeView = new QBadgeView(MainActivity.this);
            unreadFriendRequestBadgeView.bindTarget(view);
        }
        unreadFriendRequestBadgeView.setBadgeNumber(count);
    }

    public void hideUnreadFriendRequestBadgeView() {
        if (unreadFriendRequestBadgeView != null) {
            unreadFriendRequestBadgeView.hide(true);
            unreadFriendRequestBadgeView = null;
        }
    }

    @Override
    protected int menu() {
        return R.menu.main;
    }

    @Override
    protected boolean showHomeMenuItem() {
        return false;
    }

    @Override
    public void onBackPressed() {
        // 双栏下先交给当前 tab 的右栏栈：栈顶页面自己消费（收起表情/扩展面板、退出多选），
        // 否则出栈一层；栈空了才按主界面语义退到后台。
        if (twoPaneNavigator != null && twoPaneNavigator.onBackPressed()) {
            return;
        }
        moveTaskToBack(true);
    }

    // ==================== 双栏右栏：页面拦截与 tab 联动（仅双栏下生效） ====================

    /**
     * 双栏下把「能在右栏承载的页面」拦下来，改为压进当前 tab 的右栏栈。
     * <p>
     * 之所以拦这里而不是逐个改调用点：{@code Activity.startActivity(intent)}、
     * {@code Fragment.startActivity(intent)}、{@code context.startActivity(intent)}
     * 最终都汇到本方法（requestCode 为 -1），这是全仓库唯一的公共出口，
     * 相当于 flutter 端那个唯一的 {@code openPage} 入口。
     * <p>
     * 三重保险确保手机端零影响：{@code twoPaneNavigator} 手机端恒为 null；
     * {@code requestCode == -1} 把所有等结果的跳转排除在外（选择器、拍照、扫码）；
     * 未在 {@code PaneRegistry} 登记的页面（媒体预览、音视频通话）一律放行走原路径。
     */
    @Override
    public void startActivityForResult(@NonNull Intent intent, int requestCode, @Nullable Bundle options) {
        if (requestCode == -1 && twoPaneNavigator != null && twoPaneNavigator.openInPane(intent)) {
            return;
        }
        super.startActivityForResult(intent, requestCode, options);
    }

    /**
     * 页面显式发起的跳转（{@code WfcPageCompat.startPage / startPageForResult}）。
     * <p>
     * <strong>这是唯一能同时拿到「谁发起的」和「要什么结果」的入口。</strong>
     * 覆写 {@code startActivityFromFragment} 是行不通的：androidx fragment 1.5 的
     * {@code FragmentActivity$HostCallbacks} 不再覆写 {@code onStartActivityFromFragment}，
     * {@code Fragment.startActivity} 直接落到 {@code ContextCompat.startActivity(Activity, ...)}；
     * 而 {@code Fragment.startActivityForResult} 走 {@code FragmentManager.launchStartActivityForResult}，
     * 到达 Activity 时 requestCode 已被换成内部生成的码。
     */
    @Override
    public boolean openPageInPane(@Nullable Fragment caller, Intent intent, int requestCode) {
        return twoPaneNavigator != null && twoPaneNavigator.openInPane(caller, intent, requestCode);
    }

    /**
     * 页面显式发起的「打开下一页并把自己从栈里去掉」（{@code WfcPageCompat.replaceSelfWithPage}）。
     */
    @Override
    public boolean replacePageInPane(@Nullable Fragment caller, Intent intent) {
        return twoPaneNavigator != null && twoPaneNavigator.replaceInPane(caller, intent);
    }

    /**
     * 记录按下点落在左栏还是右栏。这是<strong>兜底</strong>信号，只在发起者未知
     * （尚未改用 {@code WfcPageCompat.startPage} 的裸 {@code startActivity}）时才被用到，
     * 见 {@code TwoPaneNavigator.lastTouchInPane}。
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (twoPaneNavigator != null && ev.getActionMasked() == MotionEvent.ACTION_DOWN) {
            twoPaneNavigator.recordTouchOrigin(ev.getRawX(), ev.getRawY());
        }
        return super.dispatchTouchEvent(ev);
    }

    /**
     * 供右栏导航器把左栏切到某个 tab（通知点击落到消息 tab、从通讯录发消息等）。
     */
    void selectTab(int index) {
        if (contentViewPager != null) {
            setCurrentViewPagerItem(index, false);
        }
    }

    private void checkVersion() {
        try {
            String currentVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            int buildNumber = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
            AppService.Instance().checkVersion(currentVersion, buildNumber, new AppService.CheckVersionCallback() {
                @Override
                public void onUiSuccess(boolean needUpdate, boolean forceUpdate, String latestVersion, String title, String message, String url) {
                    SharedPreferences sp = getSharedPreferences("version_info", Context.MODE_PRIVATE);
                    sp.edit()
                        .putBoolean("needUpdate", needUpdate)
                        .putBoolean("forceUpdate", forceUpdate)
                        .putString("latestVersion", latestVersion != null ? latestVersion : "")
                        .putString("title", title != null ? title : "发现新版本")
                        .putString("message", message != null ? message : "")
                        .putString("url", url != null ? url : "")
                        .apply();

                    if (!needUpdate) {
                        return;
                    }
                    runOnUiThread(() -> {
                        String dialogTitle = title != null ? title : "发现新版本";
                        String dialogMessage = message != null ? message : "";
                        if (forceUpdate) {
                            new MaterialDialog.Builder(MainActivity.this)
                                .title(dialogTitle)
                                .content(dialogMessage)
                                .cancelable(false)
                                .positiveText("立即更新")
                                .onPositive((dialog, which) -> {
                                    if (!TextUtils.isEmpty(url)) {
                                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                                        startActivity(intent);
                                    }
                                    finish();
                                })
                                .show();
                        } else {
                            new MaterialDialog.Builder(MainActivity.this)
                                .title(dialogTitle)
                                .content(dialogMessage)
                                .positiveText("立即更新")
                                .negativeText("以后再说")
                                .onPositive((dialog, which) -> {
                                    if (!TextUtils.isEmpty(url)) {
                                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                                        startActivity(intent);
                                    }
                                })
                                .show();
                        }
                    });
                }

                @Override
                public void onUiFailure(int code, String msg) {
                    // 静默处理，版本检查失败不影响使用
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initView() {
        setTitle(getString(R.string.app_title_chat));

        startingTextView.setVisibility(View.GONE);
        contentLinearLayout.setVisibility(View.VISIBLE);

        //设置ViewPager的最大缓存页面
        contentViewPager.setOffscreenPageLimit(4);

        DiscoveryFragment discoveryFragment = null;
        MeFragment meFragment = null;
        WebViewFragment workspaceFragment = null;

        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        if (fragments != null) {
            for (Fragment fragment : fragments) {
                if (fragment instanceof ConversationListFragment) {
                    conversationListFragment = (ConversationListFragment) fragment;
                } else if (fragment instanceof ContactListFragment) {
                    contactListFragment = (ContactListFragment) fragment;
                } else if (fragment instanceof DiscoveryFragment) {
                    discoveryFragment = (DiscoveryFragment) fragment;
                } else if (fragment instanceof MeFragment) {
                    meFragment = (MeFragment) fragment;
                } else if (fragment instanceof WebViewFragment) {
                    workspaceFragment = (WebViewFragment) fragment;
                }
            }
        }

        if (conversationListFragment == null) {
            conversationListFragment = new ConversationListFragment();
        }
        conversationListFragment.setOnClickConversationItemListener(conversationInfo -> {
            Intent intent = new Intent(this, ConversationActivity.class);
            intent.putExtra("conversation", conversationInfo.conversation);
            // 手机端等价于 startActivity(intent)；双栏下由 ConversationRouter 找到本 Activity
            // 这个 WfcPageNavigator，直接换右栏
            ConversationRouter.open(this, intent);
        });
        if (twoPaneNavigator != null) {
            twoPaneNavigator.setConversationListFragment(conversationListFragment);
        }
        if (contactListFragment == null) {
            contactListFragment = new ContactListFragment();
        }
        if (discoveryFragment == null) {
            discoveryFragment = new DiscoveryFragment();
        }
        if (meFragment == null) {
            meFragment = new MeFragment();
        }

        mFragmentList.clear();
        mFragmentList.add(conversationListFragment);
        mFragmentList.add(contactListFragment);
        if (twoPaneNavigator != null) {
            // 四个列表 tab 共用同一个欢迎页（与微信 Pad 一致），不再按 tab 给不同文案：
            // 右栏是同一块区域，切 tab 时文案跟着变反而像是内容变了。
            twoPaneNavigator.addTab(getString(R.string.pad_select_an_entry));
            twoPaneNavigator.addTab(getString(R.string.pad_select_an_entry));
        }
        if (showWorkSpace()) {
            if (twoPaneNavigator != null) {
                // 工作台没有「列表 → 详情」的层次：左栏放欢迎占位，网页始终占着右栏栈底。
                mFragmentList.add(PaneWelcomeFragment.newInstance(getString(R.string.pad_workspace_in_right_pane)));
                Bundle workspaceArgs = new Bundle();
                workspaceArgs.putString("url", Config.getWorkspaceUrl());
                twoPaneNavigator.addTabWithRootPage(WebViewFragment.class, workspaceArgs,
                    getString(R.string.app_title_workspace));
            } else {
                if (workspaceFragment == null) {
                    workspaceFragment = WebViewFragment.loadUrl(Config.getWorkspaceUrl());
                }
                mFragmentList.add(workspaceFragment);
            }
        }
        mFragmentList.add(discoveryFragment);
        mFragmentList.add(meFragment);
        if (twoPaneNavigator != null) {
            twoPaneNavigator.addTab(getString(R.string.pad_select_an_entry));
            twoPaneNavigator.addTab(getString(R.string.pad_select_an_entry));
            twoPaneNavigator.start(contentViewPager.getCurrentItem());
        }

        contentViewPager.setAdapter(new HomeFragmentPagerAdapter(this, mFragmentList));
        contentViewPager.registerOnPageChangeCallback(this.onPageChangeCallback);
        this.onPageChangeCallback.onPageSelected(contentViewPager.getCurrentItem());


        bottomNavigationView.setOnItemReselectedListener(item -> {
            if (item.getItemId() == R.id.conversation_list) {
                long now = System.currentTimeMillis();
                if (now - lastSelectConversatonListItemTimestamp < 200) {
                    conversationListFragment.scrollToNextUnreadConversation();
                }
                lastSelectConversatonListItemTimestamp = now;
            }
        });
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.conversation_list) {
                setCurrentViewPagerItem(0, false);
                setTitle(R.string.app_title_chat);
                if (!isDarkTheme()) {
                    setTitleBackgroundResource(R.color.gray5, false);
                }
            } else if (itemId == R.id.contact) {
                setCurrentViewPagerItem(1, false);
                setTitle(R.string.app_title_contact);
                if (!isDarkTheme()) {
                    setTitleBackgroundResource(R.color.gray5, false);
                }
            } else if (itemId == R.id.workspace) {
                setCurrentViewPagerItem(2, false);
                setTitle(R.string.app_title_workspace);
                if (!isDarkTheme()) {
                    setTitleBackgroundResource(R.color.gray5, false);
                }
            } else if (itemId == R.id.discovery) {
                setCurrentViewPagerItem(showWorkSpace() ? 3 : 2, false);
                setTitle(R.string.app_title_discover);
                if (!isDarkTheme()) {
                    setTitleBackgroundResource(R.color.gray5, false);
                }
            } else if (itemId == R.id.me) {
                setCurrentViewPagerItem(showWorkSpace() ? 4 : 3, false);
                setTitle(R.string.app_title_me);
                if (!isDarkTheme()) {
                    setTitleBackgroundResource(R.color.white, false);
                }
            }
            return true;
        });
    }

    private void showMoreActionMenu() {
        List<Pair<Integer, String>> menuItems = new ArrayList<>();
        menuItems.add(new Pair<>(R.mipmap.ic_start_chat, getString(R.string.start_group_chat)));
        menuItems.add(new Pair<>(R.mipmap.ic_start_chat, getString(R.string.start_secret_chat)));
        menuItems.add(new Pair<>(R.mipmap.ic_add_friend, getString(R.string.add_friend)));
        if (!TextUtils.isEmpty(Config.PSTN_ASSISTANT_ID)) {
            menuItems.add(new Pair<>(R.mipmap.ic_start_chat, "落地电话"));
        }
        menuItems.add(new Pair<>(R.mipmap.ic_qr_code, getString(R.string.scan_qrcode)));
        PopupMenu moreActionsMenu = new PopupMenu(this, menuItems, position -> {
            if (position == 0) {
                createConversation();
            } else if (position == 1) {
                boolean isEnableSecretChat = ChatManager.Instance().isEnableSecretChat();
                if (isEnableSecretChat) {
                    pickContactToCreateSecretConversation();
                } else {
                    Toast.makeText(this, R.string.e2e_not_enable, Toast.LENGTH_SHORT).show();
                }
            } else if (position == 2) {
                searchUser();
            } else if (!TextUtils.isEmpty(Config.PSTN_ASSISTANT_ID) && position == 3) {
                Intent intent = new Intent(MainActivity.this, cn.wildfire.chat.app.voip.PstnDialActivity.class);
                startActivity(intent);
            } else if ((!TextUtils.isEmpty(Config.PSTN_ASSISTANT_ID) && position == 4) || (TextUtils.isEmpty(Config.PSTN_ASSISTANT_ID) && position == 3)) {
                String[] permissions = new String[]{Manifest.permission.CAMERA};
                PermissionKit.PermissionReqTuple[] tuples = PermissionKit.buildRequestPermissionTuples(this, permissions);
                PermissionKit.checkThenRequestPermission(this, getSupportFragmentManager(), tuples, o -> {
                    startActivityForResult(new Intent(MainActivity.this, ScanQRCodeActivity.class), REQUEST_CODE_SCAN_QR_CODE);
                });
            }
        });
        View view = findViewById(R.id.more);
        View toolbar = findViewById(R.id.toolbar);
        int[] location = new int[2];
        toolbar.getLocationOnScreen(location);
        int y = location[1] + toolbar.getHeight();
        view.getLocationOnScreen(location);
        int yOffset = y - (location[1] + view.getHeight());

        moreActionsMenu.showAsListMenu(view, 0, yOffset);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.more) {
            showMoreActionMenu();
            return true;
        } else if (itemId == R.id.search) {
            showSearchPortal();
        }
        return super.onOptionsItemSelected(item);
    }

    private void showSearchPortal() {
        Intent intent = new Intent(this, SearchPortalActivity.class);
        startActivity(intent);
    }

    private void createConversation() {
        Intent intent = new Intent(this, CreateConversationActivity.class);
        startActivity(intent);
    }

    private void createSecretChat(String userId) {
        ConversationViewModel conversationViewModel = new ViewModelProvider(this).get(ConversationViewModel.class);
        conversationViewModel.createSecretChat(userId).observeForever(stringOperateResult -> {
            if (stringOperateResult.isSuccess()) {
                Conversation conversation = new Conversation(Conversation.ConversationType.SecretChat, stringOperateResult.getResult().first, stringOperateResult.getResult().second);
                Intent intent = new Intent(this, ConversationActivity.class);
                intent.putExtra("conversation", conversation);
                ConversationRouter.open(this, intent);
            } else {
                if (stringOperateResult.getErrorCode() == 86) {
                    //自己关闭了密聊功能
                } else if (stringOperateResult.getErrorCode() == 87) {
                    //对方关闭了密聊功能
                } else {
                    //提示网络错误
                }
            }
        });
    }

    private void pickContactToCreateSecretConversation() {
        Intent intent = new Intent(this, ContactListActivity.class);
        intent.putExtra("showChannel", false);
        startActivityForResult(intent, REQUEST_CODE_PICK_CONTACT);
    }

    private void searchUser() {
        Intent intent = new Intent(this, SearchUserActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode != RESULT_OK) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }
        switch (requestCode) {
            case REQUEST_CODE_SCAN_QR_CODE:
                String result = data.getStringExtra(Intents.Scan.RESULT);
                onScanPcQrCode(result);
                break;
            case REQUEST_CODE_PICK_CONTACT:
                UserInfo userInfo = data.getParcelableExtra("userInfo");
                if (userInfo != null) {
                    createSecretChat(userInfo.uid);
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    private void onScanPcQrCode(String qrcode) {
        WfcScheme.handleQRCodeResult(this, qrcode);
    }

    private boolean checkDisplayName() {
        UserViewModel userViewModel = WfcUIKit.getAppScopeViewModel(UserViewModel.class);
        SharedPreferences sp = getSharedPreferences("wfc_config", Context.MODE_PRIVATE);
        UserInfo userInfo = userViewModel.getUserInfo(userViewModel.getUserId(), false);
        if (userInfo != null && TextUtils.equals(userInfo.displayName, userInfo.mobile)) {
            if (!sp.getBoolean("updatedDisplayName", false)) {
                sp.edit().putBoolean("updatedDisplayName", true).apply();
                updateDisplayName();
                return false;
            }
        }
        return true;
    }

    private void updateDisplayName() {
        MaterialDialog dialog = new MaterialDialog.Builder(this)
            .content("修改个人昵称？")
            .positiveText("修改")
            .negativeText("取消")
            .onPositive(new MaterialDialog.SingleButtonCallback() {
                @Override
                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                    Intent intent = new Intent(MainActivity.this, ChangeMyNameActivity.class);
                    startActivity(intent);
                }
            }).build();
        dialog.show();
    }

    // 分享
    private void handleSendText(Intent intent) {
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (!TextUtils.isEmpty(sharedText)) {
            MessageContent content = new TextMessageContent(sharedText);
            shareMessage(content);
        } else {
            ClipData clipData = intent.getClipData();
            if (clipData != null) {
                int count = clipData.getItemCount();
                if (count == 1) {
                    ClipData.Item item = clipData.getItemAt(0);
                    sharedText = (String) item.getText();

                    if (isMiShare(sharedText)) {
                        LinkMessageContent content = parseMiShare(sharedText);
                        shareMessage(content);
                    } else {
                        MessageContent content = new TextMessageContent(sharedText);
                        shareMessage(content);
                    }
                }
            }
        }
    }

    private void handleSendMultiple(Intent intent) {
        // TODO 暂不支持一次分享多个文件，分享页面不支持，没有相关 UI
//        ArrayList<Uri> imageUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
//        if (imageUris != null) {
//            for (Uri uri : imageUris) {
//                handleSendFile(uri);
//            }
//        }
    }

    private void handleSendFile(Uri fileUri) {
        if (fileUri == null) {
            return;
        }
        String filePath = FileUtils.getPath(this, fileUri);
        if (TextUtils.isEmpty(filePath)) {
            Toast.makeText(this, "Error selecting file", Toast.LENGTH_SHORT).show();
            return;
        }
        String suffix = filePath.substring(filePath.lastIndexOf("."));
        MessageContent content;
        switch (suffix) {
            case ".png":
            case ".jpg":
            case ".jpeg":
            case ".gif":
                content = new ImageMessageContent(filePath);
                break;
            case ".3gp":
            case ".mpg":
            case ".mpeg":
            case ".mpe":
            case ".mp4":
            case ".avi":
                try {
                    content = new VideoMessageContent(filePath);
                } catch (Exception e) {
                    content = new FileMessageContent(filePath);
                }
                break;
            default:
                content = new FileMessageContent(filePath);
                break;
        }
        shareMessage(content);
    }

    private void shareMessage(MessageContent content) {
        ArrayList<Message> msgs = new ArrayList<>();
        Message message = new Message();
        message.content = content;
        msgs.add(message);
        Intent intent = new Intent(this, ForwardActivity.class);
        intent.putExtra("messages", msgs);
        startActivity(intent);
    }

    // 小米浏览器 我分享了【xxxx】, 快来看吧！@小米浏览器 | https://xxx
    private boolean isMiShare(String text) {
        if (TextUtils.isEmpty(text)) {
            return false;
        }

        if (text.startsWith("我分享了【")
            && text.indexOf("】, 快来看吧！@小米浏览器 | http") > 1) {
            return true;
        }
        return false;
    }

    private LinkMessageContent parseMiShare(String text) {
        LinkMessageContent content = new LinkMessageContent();
        String title = text.substring(text.indexOf("【") + 1, text.indexOf("】"));
        content.setTitle(title);
        String desc = text.substring(0, text.indexOf("@小米浏览器"));
        content.setContentDigest(desc);
        String url = text.substring(text.indexOf("http"));
        content.setUrl(url);
        return content;
    }

    private void requestMandatoryPermissions() {
        boolean resumed = false;
//        if (Build.VERSION.SDK_INT >= 33) {
//            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
//            if (!alarmManager.canScheduleExactAlarms()) {
//                Toast.makeText(this, "需要精确闹钟权限，否则不能正常使用 IM 功能", Toast.LENGTH_LONG).show();
//                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + getPackageName()));
//                startActivity(intent);
//                resumed = true;
//            }
//        }


//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            if (!Settings.canDrawOverlays(this)) {
//                Toast.makeText(this, "需要后台弹出界面和显示悬浮窗权限，否则后台运行时，无法弹出音视频界面", Toast.LENGTH_LONG).show();
//                if (!resumed) {
//                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
//                    startActivity(intent);
//                }
//            }
//        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            String[] permissions = new String[]{Manifest.permission.POST_NOTIFICATIONS};
            PermissionKit.PermissionReqTuple[] tuples = PermissionKit.buildRequestPermissionTuples(this, permissions);
            PermissionKit.checkThenRequestPermission(this, getSupportFragmentManager(), tuples, o -> {
                // do nothing
            });
        }
    }

    private void setCurrentViewPagerItem(int item, boolean smoothScroll) {
        if (contentViewPager.getCurrentItem() != item) {
            contentViewPager.setCurrentItem(item, smoothScroll);
        }
    }

    private ViewPager2.OnPageChangeCallback onPageChangeCallback = new ViewPager2.OnPageChangeCallback() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            updateToolbar(position, positionOffset);
        }

        @Override
        public void onPageSelected(int position) {
            updateToolbar(position, 0);
            if (twoPaneNavigator != null) {
                // 右栏切到该 tab 自己的那条栈；没进过的 tab 显示欢迎页
                twoPaneNavigator.setCurrentTab(position);
            }
            if (!showWorkSpace()) {
                if (position > 1) {
                    position++;
                }
            }
            switch (position) {
                case 0:
                    bottomNavigationView.setSelectedItemId(R.id.conversation_list);
                    break;
                case 1:
                    bottomNavigationView.setSelectedItemId(R.id.contact);
                    break;
                case 2:
                    bottomNavigationView.setSelectedItemId(R.id.workspace);
                    break;
                case 3:
                    bottomNavigationView.setSelectedItemId(R.id.discovery);
                    break;
                case 4:
                    bottomNavigationView.setSelectedItemId(R.id.me);
                    break;
                default:
                    break;
            }
            contactListFragment.showQuickIndexBar(position == 1);
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            if (state != ViewPager.SCROLL_STATE_IDLE) {
                //滚动过程中隐藏快速导航条
                contactListFragment.showQuickIndexBar(false);
            } else {
                int contactIndex = 1;
                contactListFragment.showQuickIndexBar(contentViewPager.getCurrentItem() == contactIndex);
            }
        }
    };

    private void updateToolbar(int position, float positionOffset) {
        if (getAppBarLayout() == null) {
            return;
        }
        if (appBarHeight == 0) {
            appBarHeight = getAppBarLayout().getHeight();
        }
        if (appBarHeight == 0) {
            return;
        }

        int meIndex = showWorkSpace() ? 4 : 3;
        int discoveryIndex = showWorkSpace() ? 3 : 2;

        int toolbarColor = ContextCompat.getColor(this, isDarkTheme() ? R.color.colorPrimary : R.color.gray5);
        int mePageColor = Color.WHITE;

        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) getAppBarLayout().getLayoutParams();
        if (position == discoveryIndex) {
            params.topMargin = (int) (-appBarHeight * positionOffset);
            getAppBarLayout().setAlpha(1.0f - positionOffset);

            int statusBarColor = (int) argbEvaluator.evaluate(positionOffset, toolbarColor, mePageColor);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                getWindow().setStatusBarColor(statusBarColor);
            }
            // 切换状态栏图标颜色，当背景足够亮时切换为深色图标
            if (positionOffset > 0.45f) {
                setStatusBarTheme(this, false);
            } else {
                setStatusBarTheme(this, isDarkTheme());
            }
        } else if (position >= meIndex) {
            params.topMargin = -appBarHeight;
            getAppBarLayout().setAlpha(0.0f);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                getWindow().setStatusBarColor(mePageColor);
            }
            setStatusBarTheme(this, false);
        } else {
            params.topMargin = 0;
            getAppBarLayout().setAlpha(1.0f);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                getWindow().setStatusBarColor(toolbarColor);
            }
            setStatusBarTheme(this, isDarkTheme());
        }
        getAppBarLayout().setLayoutParams(params);
    }

    private boolean showWorkSpace() {
        return !TextUtils.isEmpty(Config.getWorkspaceUrl())
            && (Config.IM_SERVER_HOST.contains("wildfirechat.net") && Config.getWorkspaceUrl().contains("wildfirechat.cn")
            || (!Config.IM_SERVER_HOST.contains("wildfirechat.net") && !Config.getWorkspaceUrl().contains("wildfirechat.cn")));
    }
}