/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.group;

import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfire.chat.kit.contact.pick.PickUserViewModel;
import cn.wildfirechat.client.GroupMemberSource;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.remote.ChatManager;

public class AddGroupMemberActivity extends WfcBaseActivity {
    private MenuItem menuItem;
    private TextView confirmTv;

    private GroupInfo groupInfo;
    public static final int RESULT_ADD_SUCCESS = 2;
    public static final int RESULT_ADD_FAIL = 3;

    private PickUserViewModel pickUserViewModel;
    private GroupViewModel groupViewModel;
    private Observer<UIUserInfo> contactCheckStatusUpdateLiveDataObserver = new Observer<UIUserInfo>() {
        @Override
        public void onChanged(@Nullable UIUserInfo userInfo) {
            List<UIUserInfo> list = pickUserViewModel.getCheckedUsers();
            if (list == null || list.isEmpty()) {
                confirmTv.setText(R.string.complete);
                menuItem.setEnabled(false);
            } else {
                confirmTv.setText(getString(R.string.complete_with_count, list.size()));
                menuItem.setEnabled(true);
            }
        }
    };

    @Override
    protected int contentLayout() {
        return R.layout.fragment_container_activity;
    }

    @Override
    protected void afterViews() {
        groupInfo = getIntent().getParcelableExtra("groupInfo");
        if (groupInfo == null) {
            finish();
            return;
        }

        pickUserViewModel = ViewModelProviders.of(this).get(PickUserViewModel.class);
        pickUserViewModel.userCheckStatusUpdateLiveData().observeForever(contactCheckStatusUpdateLiveDataObserver);
        groupViewModel = WfcUIKit.getAppScopeViewModel(GroupViewModel.class);
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.containerFrameLayout, AddGroupMemberFragment.newInstance(groupInfo))
            .commit();
    }

    @Override
    protected int menu() {
        return R.menu.group_add_member;
    }

    @Override
    protected void afterMenus(Menu menu) {
        menuItem = menu.findItem(R.id.add);
        super.afterMenus(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.add) {
            addMember();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.add);
        View actionView = item.getActionView();
        confirmTv = actionView.findViewById(R.id.confirm_tv);
        confirmTv.setOnClickListener(v -> onOptionsItemSelected(menuItem));
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        pickUserViewModel.userCheckStatusUpdateLiveData().removeObserver(contactCheckStatusUpdateLiveDataObserver);
    }


    void addMember() {
        MaterialDialog dialog = new MaterialDialog.Builder(this)
            .content(R.string.adding)
            .progress(true, 100)
            .cancelable(false)
            .build();
        dialog.show();
        List<UIUserInfo> checkedUsers = pickUserViewModel.getCheckedUsers();
        if (checkedUsers != null && !checkedUsers.isEmpty()) {
            ArrayList<String> checkedIds = new ArrayList<>(checkedUsers.size());
            for (UIUserInfo user : checkedUsers) {
                checkedIds.add(user.getUserInfo().uid);
            }
            String memberExtra = GroupMemberSource.buildGroupMemberSourceExtra(GroupMemberSource.Type_Invite, ChatManager.Instance().getUserId());
            groupViewModel.addGroupMember(groupInfo, checkedIds, null, Collections.singletonList(0), memberExtra).observe(this, result -> {
                dialog.dismiss();
                Intent intent = new Intent();
                if (result) {
                    intent.putStringArrayListExtra("memberIds", checkedIds);
                    setResult(RESULT_ADD_SUCCESS, intent);
                    Toast.makeText(this, getString(R.string.add_member_success), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, getString(R.string.add_member_fail), Toast.LENGTH_SHORT).show();
                    setResult(RESULT_ADD_FAIL);
                }
                finish();
            });
        }
    }

}
