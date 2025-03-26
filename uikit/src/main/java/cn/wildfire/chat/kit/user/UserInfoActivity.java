/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.user;

import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProviders;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.contact.ContactViewModel;
import cn.wildfire.chat.kit.contact.newfriend.InviteFriendActivity;
import cn.wildfirechat.client.FriendSource;
import cn.wildfirechat.client.GroupMemberSource;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

public class UserInfoActivity extends WfcBaseActivity {
    private UserInfo userInfo;
    // TODO
    private GroupMemberSource groupMemberSource;
    private FriendSource friendSource;

    @Override
    protected int contentLayout() {
        return R.layout.fragment_container_activity;
    }

    @Override
    protected void afterViews() {
        if (!isDarkTheme()) {
            setTitleBackgroundResource(R.color.white, false);
        }
        Intent intent = getIntent();
        userInfo = intent.getParcelableExtra("userInfo");
        String groupId = intent.getStringExtra("groupId");
        groupMemberSource = intent.getParcelableExtra("groupMemberSource");
        if (userInfo == null) {
            finish();
        } else {
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.containerFrameLayout, UserInfoFragment.newInstance(userInfo, groupId, groupMemberSource, friendSource))
                .commit();
        }
    }

    @Override
    protected int menu() {
        return R.menu.user_info;
    }

    @Override
    protected void afterMenus(Menu menu) {
        super.afterMenus(menu);
        ContactViewModel contactViewModel = WfcUIKit.getAppScopeViewModel(ContactViewModel.class);

        MenuItem itemDelete = menu.findItem(R.id.delete);
        MenuItem itemAddFriend = menu.findItem(R.id.addFriend);
        MenuItem itemAddBlacklist = menu.findItem(R.id.addBlacklist);
        MenuItem itemRemoveBlacklist = menu.findItem(R.id.removeBlacklist);
        MenuItem itemSetAlias = menu.findItem(R.id.setAlias);
        MenuItem itemSetFav = menu.findItem(R.id.setFav);
        MenuItem itemRemoveFav = menu.findItem(R.id.removeFav);
        MenuItem itemSetName = menu.findItem(R.id.setName);

        if (userInfo.type == 0) {

            if (ChatManager.Instance().getUserId().equals(userInfo.uid)) {
                itemAddBlacklist.setEnabled(false);
                itemAddBlacklist.setVisible(false);
                itemAddFriend.setEnabled(false);
                itemAddFriend.setVisible(false);
                itemDelete.setEnabled(false);
                itemDelete.setVisible(false);
                itemRemoveBlacklist.setEnabled(false);
                itemRemoveBlacklist.setVisible(false);
                itemSetAlias.setEnabled(false);
                itemSetAlias.setVisible(false);
                itemSetName.setEnabled(true);
                itemSetName.setVisible(true);
                itemSetFav.setVisible(false);
                itemSetFav.setEnabled(false);
                itemRemoveFav.setVisible(false);
                itemRemoveFav.setEnabled(false);
            } else {
                if (contactViewModel.isFriend(userInfo.uid)) {
                    itemAddFriend.setEnabled(false);
                    itemAddFriend.setVisible(false);
                    itemDelete.setEnabled(true);
                    itemDelete.setVisible(true);
                    itemSetAlias.setEnabled(true);
                    itemSetAlias.setVisible(true);
                } else {
                    itemAddFriend.setEnabled(true);
                    itemAddFriend.setVisible(true);
                    itemDelete.setEnabled(false);
                    itemDelete.setVisible(false);
                }

                if (contactViewModel.isBlacklisted(userInfo.uid)) {
                    itemAddBlacklist.setEnabled(false);
                    itemAddBlacklist.setVisible(false);
                    itemRemoveBlacklist.setEnabled(true);
                    itemRemoveBlacklist.setVisible(true);
                } else {
                    itemAddBlacklist.setEnabled(true);
                    itemAddBlacklist.setVisible(true);
                    itemRemoveBlacklist.setEnabled(false);
                    itemRemoveBlacklist.setVisible(false);
                }

                if (contactViewModel.isFav(userInfo.uid)) {
                    itemSetFav.setEnabled(false);
                    itemSetFav.setVisible(false);
                    itemRemoveFav.setEnabled(true);
                    itemRemoveFav.setVisible(true);
                } else {
                    itemSetFav.setEnabled(true);
                    itemSetFav.setVisible(true);
                    itemRemoveFav.setEnabled(false);
                    itemRemoveFav.setVisible(false);
                }
            }
        } else {
            itemRemoveBlacklist.setVisible(false);
            itemAddBlacklist.setVisible(false);
        }
    }

//    android:id="@+id/delete"
//    android:id="@+id/addFriend"
//    android:id="@+id/addBlacklist"
//    android:id="@+id/removeBlacklist"
//    android:id="@+id/setAlias"

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        ContactViewModel contactViewModel =WfcUIKit.getAppScopeViewModel(ContactViewModel.class);

        if (item.getItemId() == R.id.delete) {
            contactViewModel.deleteFriend(userInfo.uid).observe(
                this, booleanOperateResult -> {
                    if (booleanOperateResult.isSuccess()) {
                        Intent intent = new Intent(getPackageName() + ".main");
                        startActivity(intent);
                    } else {
                        Toast.makeText(this, getString(R.string.delete_friend_error, booleanOperateResult.getErrorCode()), Toast.LENGTH_SHORT).show();
                    }
                }
            );
            return true;
        } else if (item.getItemId() == R.id.addFriend) {
            Intent intent = new Intent(this, InviteFriendActivity.class);
            intent.putExtra("userInfo", userInfo);
            startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.addBlacklist) {
            contactViewModel.setBlacklist(userInfo.uid, true).observe(
                this, booleanOperateResult -> {
                    if (booleanOperateResult.isSuccess()) {
                        Toast.makeText(this, getString(R.string.set_success), Toast.LENGTH_SHORT).show();
                        invalidateOptionsMenu();
                    } else {
                        Toast.makeText(this, getString(R.string.blacklist_add_error, booleanOperateResult.getErrorCode()), Toast.LENGTH_SHORT).show();
                    }
                }
            );
            return true;
        } else if (item.getItemId() == R.id.removeBlacklist) {
            contactViewModel.setBlacklist(userInfo.uid, false).observe(
                this, booleanOperateResult -> {
                    if (booleanOperateResult.isSuccess()) {
                        Toast.makeText(this, getString(R.string.set_success), Toast.LENGTH_SHORT).show();
                        invalidateOptionsMenu();
                    } else {
                        Toast.makeText(this, getString(R.string.blacklist_remove_error, booleanOperateResult.getErrorCode()), Toast.LENGTH_SHORT).show();
                    }
                }
            );
            return true;
        } else if (item.getItemId() == R.id.setAlias) {
            Intent intent = new Intent(this, SetAliasActivity.class);
            intent.putExtra("userId", userInfo.uid);
            startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.setFav) {
            contactViewModel.setFav(userInfo.uid, true).observe(
                this, booleanOperateResult -> {
                    if (booleanOperateResult.isSuccess()) {
                        Toast.makeText(this, getString(R.string.set_success), Toast.LENGTH_SHORT).show();
                        invalidateOptionsMenu();
                    } else {
                        Toast.makeText(this, getString(R.string.fav_set_error, booleanOperateResult.getErrorCode()), Toast.LENGTH_SHORT).show();
                    }
                }
            );
            return true;

        } else if (item.getItemId() == R.id.removeFav) {
            contactViewModel.setFav(userInfo.uid, false).observe(
                this, booleanOperateResult -> {
                    if (booleanOperateResult.isSuccess()) {
                        Toast.makeText(this, getString(R.string.set_success), Toast.LENGTH_SHORT).show();
                        invalidateOptionsMenu();
                    } else {
                        Toast.makeText(this, getString(R.string.fav_remove_error, booleanOperateResult.getErrorCode()), Toast.LENGTH_SHORT).show();
                    }
                }
            );
            return true;
        } else if (item.getItemId() == R.id.setName) {
            Intent intent = new Intent(this, SetNameActivity.class);
            intent.putExtra("userInfo", userInfo);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
