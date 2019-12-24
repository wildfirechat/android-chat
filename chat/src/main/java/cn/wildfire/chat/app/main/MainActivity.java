package cn.wildfire.chat.app.main;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.viewpager.widget.ViewPager;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.material.bottomnavigation.BottomNavigationMenuView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.Gson;
import com.google.zxing.common.StringUtils;
import com.king.zxing.Intents;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import cn.wildfire.chat.app.Config;
import cn.wildfire.chat.app.login.model.LoginResult;
import cn.wildfire.chat.app.login.model.RegResult;
import cn.wildfire.chat.app.main.model.ApiClientVO;
import cn.wildfire.chat.app.main.model.MainModel;
import cn.wildfire.chat.kit.ChatManagerHolder;
import cn.wildfire.chat.kit.IMConnectionStatusViewModel;
import cn.wildfire.chat.kit.IMServiceStatusViewModel;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.WfcScheme;
import cn.wildfire.chat.kit.channel.ChannelInfoActivity;
import cn.wildfire.chat.kit.contact.ContactListFragment;
import cn.wildfire.chat.kit.contact.ContactViewModel;
import cn.wildfire.chat.kit.contact.newfriend.SearchUserActivity;
import cn.wildfire.chat.kit.conversation.CreateConversationActivity;
import cn.wildfire.chat.kit.conversationlist.ConversationListFragment;
import cn.wildfire.chat.kit.conversationlist.ConversationListViewModel;
import cn.wildfire.chat.kit.conversationlist.ConversationListViewModelFactory;
import cn.wildfire.chat.kit.group.GroupInfoActivity;
import cn.wildfire.chat.kit.net.OKHttpHelper;
import cn.wildfire.chat.kit.net.SimpleCallback;
import cn.wildfire.chat.kit.qrcode.ScanQRCodeActivity;
import cn.wildfire.chat.kit.search.SearchPortalActivity;
import cn.wildfire.chat.kit.third.utils.UIUtils;
import cn.wildfire.chat.kit.user.ChangeMyNameActivity;
import cn.wildfire.chat.kit.user.UserInfoActivity;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfire.chat.kit.widget.ViewPagerFixed;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.client.ConnectionStatus;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
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

    MenuItem webback_btn;
    MenuItem flush_btn;

    private QBadgeView unreadMessageUnreadBadgeView;
    private QBadgeView unreadFriendRequestBadgeView;

    private static final int REQUEST_CODE_SCAN_QR_CODE = 100;
    private static final int REQUEST_IGNORE_BATTERY_CODE = 101;

    private boolean isInitialized = false;

    private ContactListFragment contactListFragment;

    private ContactViewModel contactViewModel;
    private ConversationListViewModel conversationListViewModel;

    private DiscoveryFragment discoveryFragment;

    private Observer<Boolean> imStatusLiveDataObserver = status -> {
        if (status && !isInitialized) {
            initGetConfig(new Runnable() {
                @Override
                public void run() {
                    init();
                }
            });
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
    }

    @Override
    protected void afterViews() {
        IMServiceStatusViewModel imServiceStatusViewModel = ViewModelProviders.of(this).get(IMServiceStatusViewModel.class);
        imServiceStatusViewModel.imServiceStatusLiveData().observe(this, imStatusLiveDataObserver);
        IMConnectionStatusViewModel connectionStatusViewModel = ViewModelProviders.of(this).get(IMConnectionStatusViewModel.class);
        connectionStatusViewModel.connectionStatusLiveData().observe(this, status -> {
            if (status == ConnectionStatus.ConnectionStatusTokenIncorrect || status == ConnectionStatus.ConnectionStatusSecretKeyMismatch || status == ConnectionStatus.ConnectionStatusRejected || status == ConnectionStatus.ConnectionStatusLogout) {
                ChatManager.Instance().disconnect(true);
                reLogin();
            }
        });
    }

    private void reLogin() {
        SharedPreferences sp = getSharedPreferences("config", Context.MODE_PRIVATE);
        sp.edit().clear().apply();

        Intent intent = new Intent(this, SplashActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void init() {
        initView();

        conversationListViewModel = ViewModelProviders
                .of(this, new ConversationListViewModelFactory(Arrays.asList(Conversation.ConversationType.Single, Conversation.ConversationType.Group, Conversation.ConversationType.Channel), Arrays.asList(0)))
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

        if (checkDisplayName()) {
            ignoreBatteryOption();
        }
    }

    private void initGetConfig(Runnable _init){
        String php_url = Config.APP_SERVER_PHP + "/yh/apiclient.php";

        MaterialDialog dialog = new MaterialDialog.Builder(this)
                .content("加载配置中...")
                .progress(true, 100)
                .cancelable(false)
                .build();
        dialog.show();

        OkHttpClient client = new OkHttpClient();
        RequestBody formBody = new FormBody.Builder().build();
        Request request = new Request.Builder().url(php_url)
                .post(formBody).build();

        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.dismiss();
                        Log.e("C:", "加载apiclient配置错误");
                        //Toast.makeText(getApplicationContext(), "加载apiclient配置错误", Toast.LENGTH_SHORT).show();
                        initGetConfig(_init);
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseStr = response.body().string();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.dismiss();
                        Log.e("D:", responseStr);
                        //Toast.makeText(activity, responseStr, Toast.LENGTH_SHORT).show();
                        Gson gson = new Gson();
                        MainModel.clientConfig = gson.fromJson(responseStr, ApiClientVO.class);
                        //discoveryFragment.webView.loadUrl(MainModel.clientConfig.getHomeUrl());
                        _init.run();

                        String _uid = ChatManagerHolder.gChatManager.getUserId();
                        if(_uid.isEmpty()) _uid = "";
                        getTimevalGetConfig(_uid);
                    }
                });

            }
        });
    }

    private void getTimevalGetConfig(String uid){
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                String php_url = Config.APP_SERVER_PHP + "/yh/apiclient.php?uid="+uid;
                Log.e("G:", php_url);

                OkHttpClient client = new OkHttpClient();
                RequestBody formBody = new FormBody.Builder().build();
                Request request = new Request.Builder().url(php_url)
                        .post(formBody).build();

                Call call = client.newCall(request);
                call.enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {

                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        final String responseStr = response.body().string();
                        Log.e("F:", responseStr);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Gson gson = new Gson();
                                MainModel.clientConfig = gson.fromJson(responseStr, ApiClientVO.class);

                                getTimevalGetConfig(uid);
                            }
                        });
                    }
                });
            }
        }, 1000 * 50);//延时1000=1s执行
    }

    private void showUnreadMessageBadgeView(int count) {
        if (unreadMessageUnreadBadgeView == null) {
            BottomNavigationMenuView bottomNavigationMenuView = ((BottomNavigationMenuView) bottomNavigationView.getChildAt(0));
            View view = bottomNavigationMenuView.getChildAt(1);
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


    private void showUnreadFriendRequestBadgeView(int count) {
        if (unreadFriendRequestBadgeView == null) {
            BottomNavigationMenuView bottomNavigationMenuView = ((BottomNavigationMenuView) bottomNavigationView.getChildAt(0));
            View view = bottomNavigationMenuView.getChildAt(2);
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

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        webback_btn = menu.findItem(R.id.webback_btn);
        flush_btn = menu.findItem(R.id.flush_btn);
        return true;
    }

    private void initView() {
        setTitle(UIUtils.getString(R.string.app_name));

        startingTextView.setVisibility(View.GONE);
        contentLinearLayout.setVisibility(View.VISIBLE);



        //webback_btn.setVisible(false);
        //flush_btn.setVisible(false);

        //设置ViewPager的最大缓存页面
        contentViewPager.setOffscreenPageLimit(3);
        contentViewPager.setNoScroll(true);
        contentViewPager.setScrollAnim(false);

        ConversationListFragment conversationListFragment = new ConversationListFragment();
        contactListFragment = new ContactListFragment();
        discoveryFragment = new DiscoveryFragment();
        MeFragment meFragment = new MeFragment();
        mFragmentList.add(discoveryFragment);
        mFragmentList.add(conversationListFragment);
        mFragmentList.add(contactListFragment);
        mFragmentList.add(meFragment);
        contentViewPager.setAdapter(new HomeFragmentPagerAdapter(getSupportFragmentManager(), mFragmentList));
        contentViewPager.setOnPageChangeListener(this);

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.discovery:
                    contentViewPager.setCurrentItem(0);
                    break;
                case R.id.conversation_list:
                    contentViewPager.setCurrentItem(1);
                    break;
                case R.id.contact:
                    contentViewPager.setCurrentItem(2);
                    break;
                case R.id.me:
                    contentViewPager.setCurrentItem(3);
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
            case R.id.webback_btn:
                backWebView();
                break;
            case R.id.flush_btn:
                flushWebView();
                break;
            case R.id.search:
                showSearchPortal();
                break;
            case R.id.chat:
                String _onfgc = MainModel.clientConfig.getOnfgroupchat();
                if(_onfgc.equals("1")) {
                    createConversation();
                }else{
                    Toast.makeText(this, "管理员禁止私建群聊!", Toast.LENGTH_SHORT).show();
                }
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

    private void backWebView(){
        discoveryFragment.webView.goBack();
        Log.e("B:","Back");
    }

    private void flushWebView(){
        if(old_page_index==0) {
            discoveryFragment.webView.reload();
            Log.e("B:","reload");
        }else if(old_page_index==2){
            Log.e("C:","reload friend");
            if (contactViewModel != null) {
                contactViewModel.reloadContact(true);
            }
        }
    }

    private void showSearchPortal() {
        Intent intent = new Intent(this, SearchPortalActivity.class);
        startActivity(intent);
    }

    private void createConversation() {
        Intent intent = new Intent(this, CreateConversationActivity.class);
        startActivity(intent);
    }

    private void searchUser() {
        Intent intent = new Intent(this, SearchUserActivity.class);
        startActivity(intent);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    private int old_page_index=0;

    @Override
    public void onPageSelected(int position) {
        Log.e("Anlei:", Integer.toString(position));
        if(webback_btn!=null) webback_btn.setVisible(false);
        if(flush_btn!=null) flush_btn.setVisible(false);
        switch (position) {
            case 0:
                bottomNavigationView.setSelectedItemId(R.id.discovery);
                if(webback_btn!=null) webback_btn.setVisible(true);
                if(flush_btn!=null) flush_btn.setVisible(true);
                break;
            case 1:
                bottomNavigationView.setSelectedItemId(R.id.conversation_list);
                break;
            case 2:
                bottomNavigationView.setSelectedItemId(R.id.contact);
                if(flush_btn!=null) flush_btn.setVisible(true);
                break;
            case 3:
                bottomNavigationView.setSelectedItemId(R.id.me);
                break;
            default:
                break;
        }
        old_page_index = position;
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
        switch (requestCode) {
            case REQUEST_CODE_SCAN_QR_CODE:
                if (resultCode == RESULT_OK) {
                    String result = data.getStringExtra(Intents.Scan.RESULT);
                    onScanPcQrCode(result);
                }
                break;
            case REQUEST_IGNORE_BATTERY_CODE:
                if (resultCode == RESULT_CANCELED) {
                    Toast.makeText(this, "允许此APP后台运行，更能保证消息的实时性", Toast.LENGTH_SHORT).show();
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

    private void onScanPcQrCode(String qrcode) {
        String prefix = qrcode.substring(0, qrcode.lastIndexOf('/') + 1);
        String value = qrcode.substring(qrcode.lastIndexOf("/") + 1);
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

    private boolean checkDisplayName() {
        UserViewModel userViewModel = ViewModelProviders.of(this).get(UserViewModel.class);
        SharedPreferences sp = getSharedPreferences("config", Context.MODE_PRIVATE);
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


    private void ignoreBatteryOption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                Intent intent = new Intent();
                String packageName = getPackageName();
                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                    intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + packageName));
                    startActivityForResult(intent, REQUEST_IGNORE_BATTERY_CODE);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
