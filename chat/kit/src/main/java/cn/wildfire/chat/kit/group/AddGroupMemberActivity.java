package cn.wildfire.chat.kit.group;

import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfire.chat.kit.contact.pick.PickUserViewModel;
import cn.wildfire.chat.kit.third.utils.UIUtils;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.model.GroupInfo;

public class AddGroupMemberActivity extends WfcBaseActivity {
    private MenuItem menuItem;

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
                menuItem.setTitle("确定");
                menuItem.setEnabled(false);
            } else {
                menuItem.setTitle("确定(" + list.size() + ")");
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
        groupViewModel = ViewModelProviders.of(this).get(GroupViewModel.class);
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
    protected void onDestroy() {
        super.onDestroy();
        pickUserViewModel.userCheckStatusUpdateLiveData().removeObserver(contactCheckStatusUpdateLiveDataObserver);
    }


    void addMember() {
        MaterialDialog dialog = new MaterialDialog.Builder(this)
                .content("添加中...")
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
            groupViewModel.addGroupMember(groupInfo, checkedIds, null, Collections.singletonList(0)).observe(this, result -> {
                dialog.dismiss();
                Intent intent = new Intent();
                if (result) {
                    intent.putStringArrayListExtra("memberIds", checkedIds);
                    setResult(RESULT_ADD_SUCCESS, intent);
                    UIUtils.showToast(UIUtils.getString(R.string.add_member_success));
                } else {
                    UIUtils.showToast(UIUtils.getString(R.string.add_member_fail));
                    setResult(RESULT_ADD_FAIL);
                }
                finish();
            });
        }
    }

}
