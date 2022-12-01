/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.main;

import android.Manifest;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.viewpager.widget.ViewPager;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.material.bottomnavigation.BottomNavigationMenuView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.king.zxing.Intents;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import butterknife.BindView;
import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.IMConnectionStatusViewModel;
import cn.wildfire.chat.kit.IMServiceStatusViewModel;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.WfcScheme;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.channel.ChannelInfoActivity;
import cn.wildfire.chat.kit.contact.ContactListActivity;
import cn.wildfire.chat.kit.contact.ContactListFragment;
import cn.wildfire.chat.kit.contact.ContactViewModel;
import cn.wildfire.chat.kit.contact.newfriend.SearchUserActivity;
import cn.wildfire.chat.kit.conversation.ConversationActivity;
import cn.wildfire.chat.kit.conversation.ConversationViewModel;
import cn.wildfire.chat.kit.conversation.CreateConversationActivity;
import cn.wildfire.chat.kit.conversation.forward.ForwardActivity;
import cn.wildfire.chat.kit.conversationlist.ConversationListFragment;
import cn.wildfire.chat.kit.conversationlist.ConversationListViewModel;
import cn.wildfire.chat.kit.conversationlist.ConversationListViewModelFactory;
import cn.wildfire.chat.kit.group.GroupInfoActivity;
import cn.wildfire.chat.kit.net.OKHttpHelper;
import cn.wildfire.chat.kit.qrcode.ScanQRCodeActivity;
import cn.wildfire.chat.kit.search.SearchPortalActivity;
import cn.wildfire.chat.kit.user.ChangeMyNameActivity;
import cn.wildfire.chat.kit.user.UserInfoActivity;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfire.chat.kit.viewmodel.MessageViewModel;
import cn.wildfire.chat.kit.voip.conference.ConferenceInfoActivity;
import cn.wildfire.chat.kit.widget.ViewPagerFixed;
import cn.wildfire.chat.kit.workspace.WebViewFragment;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.client.ConnectionStatus;
import cn.wildfirechat.message.LinkMessageContent;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.MessageContent;
import cn.wildfirechat.message.TextMessageContent;
import cn.wildfirechat.message.core.MessageContentType;
import cn.wildfirechat.message.core.MessageStatus;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;
import q.rorbin.badgeview.QBadgeView;

public class MainActivity extends WfcBaseActivity implements ViewPager.OnPageChangeListener {

    private List<Fragment> mFragmentList = new ArrayList<>(4);

    @BindView(R.id.bottomNavigationView)
    BottomNavigationView bottomNavigationView;
    @BindView(R.id.contentViewPager)
    ViewPagerFixed contentViewPager;
    @BindView(R.id.startingTextView)
    TextView startingTextView;
    @BindView(R.id.contentLinearLayout)
    LinearLayout contentLinearLayout;

    private QBadgeView unreadMessageUnreadBadgeView;
    private QBadgeView unreadFriendRequestBadgeView;
    private QBadgeView discoveryBadgeView;

    private static final int REQUEST_CODE_SCAN_QR_CODE = 100;
    private static final int REQUEST_CODE_PICK_CONTACT = 101;

    private boolean isInitialized = false;

    private ContactListFragment contactListFragment;

    private ContactViewModel contactViewModel;
    private ConversationListViewModel conversationListViewModel;

    private Observer<Boolean> imStatusLiveDataObserver = status -> {
        if (status && !isInitialized) {
            init();
            isInitialized = true;
        }
    };

    @Override
    protected int contentLayout() {
        return R.layout.main_activity;
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
        bottomNavigationView.setItemIconTintList(null);
        if (TextUtils.isEmpty(Config.WORKSPACE_URL)) {
            bottomNavigationView.getMenu().removeItem(R.id.workspace);
        }
        IMServiceStatusViewModel imServiceStatusViewModel = ViewModelProviders.of(this).get(IMServiceStatusViewModel.class);
        imServiceStatusViewModel.imServiceStatusLiveData().observe(this, imStatusLiveDataObserver);
        IMConnectionStatusViewModel connectionStatusViewModel = ViewModelProviders.of(this).get(IMConnectionStatusViewModel.class);
        connectionStatusViewModel.connectionStatusLiveData().observe(this, status -> {
            if (status == ConnectionStatus.ConnectionStatusTokenIncorrect
                || status == ConnectionStatus.ConnectionStatusSecretKeyMismatch
                || status == ConnectionStatus.ConnectionStatusRejected
                || status == ConnectionStatus.ConnectionStatusLogout
                || status == ConnectionStatus.ConnectionStatusKickedoff) {
                SharedPreferences sp = getSharedPreferences(Config.SP_CONFIG_FILE_NAME, Context.MODE_PRIVATE);
                sp.edit().clear().apply();
                OKHttpHelper.clearCookies();
                if (status == ConnectionStatus.ConnectionStatusLogout) {
                    reLogin(false);
                } else {
                    ChatManager.Instance().disconnect(true, false);
                    if (status == ConnectionStatus.ConnectionStatusKickedoff) {
                        reLogin(true);
                    }
                }
            }
        });
        MessageViewModel messageViewModel = ViewModelProviders.of(this).get(MessageViewModel.class);
        messageViewModel.messageLiveData().observe(this, uiMessage -> {
            if (uiMessage.message.content.getMessageContentType() == MessageContentType.MESSAGE_CONTENT_TYPE_FEED
                || uiMessage.message.content.getMessageContentType() == MessageContentType.MESSAGE_CONTENT_TYPE_FEED_COMMENT) {
                updateMomentBadgeView();
            }
        });

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        if (Intent.ACTION_SEND.equals(action)) {
            if ("text/plain".equals(type)) {
                handleSend(intent);
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String action = intent.getAction();
        String type = intent.getType();
        if (Intent.ACTION_SEND.equals(action)) {
            if ("text/plain".equals(type)) {
                handleSend(intent);
            }
        }
    }

    @Override
    protected void afterMenus(Menu menu) {
        super.afterMenus(menu);
        boolean isEnableSecretChat = ChatManager.Instance().isEnableSecretChat();
        if (!isEnableSecretChat) {
            MenuItem menuItem = menu.findItem(R.id.secretChat);
            menuItem.setEnabled(false);
        }
    }

    private void reLogin(boolean isKickedOff) {
        Intent intent = new Intent(this, SplashActivity.class);
        intent.putExtra("isKickedOff", isKickedOff);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void init() {
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

        contactViewModel = ViewModelProviders.of(this).get(ContactViewModel.class);
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
                int index = TextUtils.isEmpty(Config.WORKSPACE_URL) ? 2 : 3;
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
        moveTaskToBack(true);
    }

    private void initView() {
        setTitle(getString(R.string.app_name));

        startingTextView.setVisibility(View.GONE);
        contentLinearLayout.setVisibility(View.VISIBLE);

        //设置ViewPager的最大缓存页面
        contentViewPager.setOffscreenPageLimit(4);

        ConversationListFragment conversationListFragment = new ConversationListFragment();
        contactListFragment = new ContactListFragment();
        DiscoveryFragment discoveryFragment = new DiscoveryFragment();
        MeFragment meFragment = new MeFragment();
        mFragmentList.add(conversationListFragment);
        mFragmentList.add(contactListFragment);
        boolean showWorkSpace = !TextUtils.isEmpty(Config.WORKSPACE_URL);
        if (showWorkSpace) {
            mFragmentList.add(WebViewFragment.loadUrl(Config.WORKSPACE_URL));
        }
        mFragmentList.add(discoveryFragment);
        mFragmentList.add(meFragment);
        contentViewPager.setAdapter(new HomeFragmentPagerAdapter(getSupportFragmentManager(), mFragmentList));
        contentViewPager.setOnPageChangeListener(this);

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.conversation_list:
                    contentViewPager.setCurrentItem(0, false);
                    setTitle("野火");
                    if (!isDarkTheme()) {
                        setTitleBackgroundResource(R.color.gray5, false);
                    }
                    break;
                case R.id.contact:
                    contentViewPager.setCurrentItem(1, false);
                    setTitle("通讯录");
                    if (!isDarkTheme()) {
                        setTitleBackgroundResource(R.color.gray5, false);
                    }
                    break;
                case R.id.workspace:
                    contentViewPager.setCurrentItem(2, false);
                    setTitle("工作台");
                    if (!isDarkTheme()) {
                        setTitleBackgroundResource(R.color.gray5, false);
                    }
                    break;
                case R.id.discovery:
                    contentViewPager.setCurrentItem(showWorkSpace ? 3 : 2, false);
                    setTitle("发现");
                    if (!isDarkTheme()) {
                        setTitleBackgroundResource(R.color.gray5, false);
                    }
                    break;
                case R.id.me:
                    contentViewPager.setCurrentItem(showWorkSpace ? 4 : 3, false);
                    setTitle("我的");
                    if (!isDarkTheme()) {
                        setTitleBackgroundResource(R.color.white, false);
                    }
                    break;
                default:
                    break;
            }
            return true;
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.search:
                showSearchPortal();
                break;
            case R.id.chat:
                createConversation();
                break;
            case R.id.secretChat:
                pickContactToCreateSecretConversation();
                break;
            case R.id.add_contact:
                searchUser();
                break;
            case R.id.scan_qrcode:
                String[] permissions = new String[]{Manifest.permission.CAMERA};
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!checkPermission(permissions)) {
                        requestPermissions(permissions, 100);
                        return true;
                    }
                }
                startActivityForResult(new Intent(this, ScanQRCodeActivity.class), REQUEST_CODE_SCAN_QR_CODE);
            default:
                break;
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
        ConversationViewModel conversationViewModel = ViewModelProviders.of(this).get(ConversationViewModel.class);
        conversationViewModel.createSecretChat(userId).observeForever(stringOperateResult -> {
            if (stringOperateResult.isSuccess()) {
                Conversation conversation = new Conversation(Conversation.ConversationType.SecretChat, stringOperateResult.getResult().first, stringOperateResult.getResult().second);
                Intent intent = new Intent(this, ConversationActivity.class);
                intent.putExtra("conversation", conversation);
                startActivity(intent);
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
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    @Override
    public void onPageSelected(int position) {
        if (TextUtils.isEmpty(Config.WORKSPACE_URL)) {
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
            contactListFragment.showQuickIndexBar(true);
        }
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startActivityForResult(new Intent(this, ScanQRCodeActivity.class), REQUEST_CODE_SCAN_QR_CODE);
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Intent i = getBaseContext().getPackageManager().getLaunchIntentForPackage(
            getBaseContext().getPackageName());
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
        finish();
    }

    private void onScanPcQrCode(String qrcode) {
        String prefix = qrcode.substring(0, qrcode.lastIndexOf('/') + 1);
        String value = qrcode.substring(qrcode.lastIndexOf("/") + 1);
        Uri uri = Uri.parse(qrcode);
        Set<String> queryNames = uri.getQueryParameterNames();
        Map<String, Object> params = new HashMap<>();
        for (String query : queryNames) {
            params.put(query, uri.getQueryParameter(query));
        }
        switch (prefix) {
            case WfcScheme.QR_CODE_PREFIX_PC_SESSION:
                pcLogin(value);
                break;
            case WfcScheme.QR_CODE_PREFIX_USER:
                showUser(value);
                break;
            case WfcScheme.QR_CODE_PREFIX_GROUP:
                joinGroup(value);
                break;
            case WfcScheme.QR_CODE_PREFIX_CHANNEL:
                subscribeChannel(value);
                break;
            case WfcScheme.QR_CODE_PREFIX_CONFERENCE:
                joinConference(value, params);
                break;
            default:
                Toast.makeText(this, "qrcode: " + qrcode, Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void pcLogin(String token) {
        Intent intent = new Intent(this, PCLoginActivity.class);
        intent.putExtra("token", token);
        startActivity(intent);
    }

    private void showUser(String uid) {

        UserViewModel userViewModel = ViewModelProviders.of(this).get(UserViewModel.class);
        UserInfo userInfo = userViewModel.getUserInfo(uid, true);
        if (userInfo == null) {
            return;
        }
        Intent intent = new Intent(this, UserInfoActivity.class);
        intent.putExtra("userInfo", userInfo);
        startActivity(intent);
    }

    private void joinGroup(String groupId) {
        Intent intent = new Intent(this, GroupInfoActivity.class);
        intent.putExtra("groupId", groupId);
        startActivity(intent);
    }

    private void subscribeChannel(String channelId) {
        Intent intent = new Intent(this, ChannelInfoActivity.class);
        intent.putExtra("channelId", channelId);
        startActivity(intent);
    }

    private void joinConference(String conferenceId, Map<String, Object> params) {
        Intent intent = new Intent(this, ConferenceInfoActivity.class);
        intent.putExtra("conferenceId", conferenceId);
        intent.putExtra("password", (String) params.get("pwd"));
        startActivity(intent);
    }

    private boolean checkDisplayName() {
        UserViewModel userViewModel = ViewModelProviders.of(this).get(UserViewModel.class);
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
    private void handleSend(Intent intent) {
        String sharedText = intent.getStringExtra(Intent.EXTRA_INTENT);
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
}
