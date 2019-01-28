package cn.wildfire.chat.main;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.material.bottomnavigation.BottomNavigationMenuView;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.viewpager.widget.ViewPager;
import butterknife.Bind;
import cn.wildfire.chat.WfcBaseActivity;
import cn.wildfire.chat.contact.ContactViewModel;
import cn.wildfire.chat.contact.newfriend.SearchUserActivity;
import cn.wildfire.chat.conversation.CreateConversationActivity;
import cn.wildfire.chat.conversationlist.ConversationListViewModel;
import cn.wildfire.chat.conversationlist.ConversationListViewModelFactory;
import cn.wildfire.chat.search.SearchPortalActivity;
import cn.wildfire.chat.third.location.ui.adapter.CommonFragmentPagerAdapter;
import cn.wildfire.chat.third.utils.UIUtils;
import cn.wildfire.chat.user.ChangeMyNameActivity;
import cn.wildfire.chat.user.UserViewModel;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.UserInfo;
import q.rorbin.badgeview.QBadgeView;

public class MainActivity extends WfcBaseActivity implements ViewPager.OnPageChangeListener {

    private List<Fragment> mFragmentList = new ArrayList<>(4);

    @Bind(R.id.bottomNavigationView)
    BottomNavigationView bottomNavigationView;
    @Bind(R.id.vpContent)
    ViewPager mVpContent;

    private QBadgeView unreadMessageUnreadBadgeView;
    private QBadgeView unreadFriendRequestBadgeView;

    @Override
    protected int contentLayout() {
        return R.layout.main_activity;
    }

    @Override
    protected void afterViews() {
        initView();

        ConversationListViewModel conversationListViewModel = ViewModelProviders
                .of(this, new ConversationListViewModelFactory(Arrays.asList(Conversation.ConversationType.Single, Conversation.ConversationType.Group, Conversation.ConversationType.Channel), Arrays.asList(0)))
                .get(ConversationListViewModel.class);
        conversationListViewModel.unreadCountLiveData().observe(this, unreadCount -> {

            if (unreadCount != null && unreadCount.unread > 0) {
                if (unreadMessageUnreadBadgeView == null) {
                    BottomNavigationMenuView bottomNavigationMenuView = ((BottomNavigationMenuView) bottomNavigationView.getChildAt(0));
                    View view = bottomNavigationMenuView.getChildAt(0);
                    unreadMessageUnreadBadgeView = new QBadgeView(MainActivity.this);
                    unreadMessageUnreadBadgeView.bindTarget(view);
                }
                unreadMessageUnreadBadgeView.setBadgeNumber(unreadCount.unread);
            } else if (unreadMessageUnreadBadgeView != null) {
                unreadMessageUnreadBadgeView.hide(true);
            }
        });

        ContactViewModel contactViewModel = ViewModelProviders.of(this).get(ContactViewModel.class);
        contactViewModel.friendRequestUpdatedLiveData().observe(this, count -> {
            if (count == null || count == 0) {
                if (unreadFriendRequestBadgeView != null) {
                    unreadFriendRequestBadgeView.hide(true);
                }
            } else {
                if (unreadFriendRequestBadgeView == null) {
                    BottomNavigationMenuView bottomNavigationMenuView = ((BottomNavigationMenuView) bottomNavigationView.getChildAt(0));
                    View view = bottomNavigationMenuView.getChildAt(1);
                    unreadFriendRequestBadgeView = new QBadgeView(MainActivity.this);
                    unreadFriendRequestBadgeView.bindTarget(view);
                }
                unreadFriendRequestBadgeView.setBadgeNumber(count);
            }
        });
        checkDisplayName();
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

    private void initView() {
        setTitle(UIUtils.getString(R.string.app_name));

        //设置ViewPager的最大缓存页面
        mVpContent.setOffscreenPageLimit(3);

        mFragmentList.add(FragmentFactory.getInstance().getRecentMessageFragment());
        mFragmentList.add(FragmentFactory.getInstance().getContactsFragment());
        mFragmentList.add(FragmentFactory.getInstance().getDiscoveryFragment());
        mFragmentList.add(FragmentFactory.getInstance().getMeFragment());
        mVpContent.setAdapter(new CommonFragmentPagerAdapter(getSupportFragmentManager(), mFragmentList));
        mVpContent.setOnPageChangeListener(this);

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.conversation_list:
                    mVpContent.setCurrentItem(0);
                    break;
                case R.id.contact:
                    mVpContent.setCurrentItem(1);
                    break;
                case R.id.discovery:
                    mVpContent.setCurrentItem(2);
                    break;
                case R.id.me:
                    mVpContent.setCurrentItem(3);
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
            case R.id.add_contact:
                searchUser();
                break;
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

    private void searchUser() {
        Intent intent = new Intent(this, SearchUserActivity.class);
        startActivity(intent);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    @Override
    public void onPageSelected(int position) {
        switch (position) {
            case 0:
                bottomNavigationView.setSelectedItemId(R.id.conversation_list);
                break;
            case 1:
                bottomNavigationView.setSelectedItemId(R.id.contact);
                break;
            case 2:
                bottomNavigationView.setSelectedItemId(R.id.discovery);
                break;
            case 3:
                bottomNavigationView.setSelectedItemId(R.id.me);
                break;
            default:
                break;
        }
        if (position == 1) {
            //如果是“通讯录”页被选中，则显示快速导航条
            FragmentFactory.getInstance().getContactsFragment().showQuickIndexBar(true);
        } else {
            FragmentFactory.getInstance().getContactsFragment().showQuickIndexBar(false);
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        if (state != ViewPager.SCROLL_STATE_IDLE) {
            //滚动过程中隐藏快速导航条
            FragmentFactory.getInstance().getContactsFragment().showQuickIndexBar(false);
        } else {
            FragmentFactory.getInstance().getContactsFragment().showQuickIndexBar(true);
        }
    }

    private void checkDisplayName() {
        UserViewModel userViewModel = ViewModelProviders.of(this).get(UserViewModel.class);
        SharedPreferences sp = getSharedPreferences("config", Context.MODE_PRIVATE);
        UserInfo userInfo = userViewModel.getUserInfo(userViewModel.getUserId(), false);
        if (TextUtils.equals(userInfo.displayName, userInfo.mobile)) {
            if (!sp.getBoolean("updatedDisplayName", false)) {
                sp.edit().putBoolean("updatedDisplayName", true).apply();
                updateDisplayName();
            }
        }
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
}
