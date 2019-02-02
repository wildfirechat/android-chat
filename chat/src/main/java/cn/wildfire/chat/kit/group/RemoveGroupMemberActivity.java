package cn.wildfire.chat.kit.group;

import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfire.chat.kit.contact.pick.PickContactViewModel;
import cn.wildfire.chat.kit.third.utils.UIUtils;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.model.GroupInfo;

public class RemoveGroupMemberActivity extends WfcBaseActivity {
    private MenuItem menuItem;
    private GroupInfo groupInfo;
    public static final int RESULT_REMOVE_SUCCESS = 2;
    public static final int RESULT_REMOVE_FAIL = 3;

    private PickContactViewModel pickContactViewModel;
    private GroupViewModel groupViewModel;
    private Observer<UIUserInfo> contactCheckStatusUpdateLiveDataObserver = new Observer<UIUserInfo>() {
        @Override
        public void onChanged(@Nullable UIUserInfo userInfo) {
            List<UIUserInfo> list = pickContactViewModel.getCheckedContacts();
            if (list == null || list.isEmpty()) {
                menuItem.setTitle("删除");
                menuItem.setEnabled(false);
            } else {
                menuItem.setTitle("删除(" + list.size() + ")");
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

        pickContactViewModel = ViewModelProviders.of(this).get(PickContactViewModel.class);
        pickContactViewModel.contactCheckStatusUpdateLiveData().observeForever(contactCheckStatusUpdateLiveDataObserver);
        UserViewModel userViewModel = ViewModelProviders.of(this).get(UserViewModel.class);
        pickContactViewModel.setUncheckableIds(Collections.singletonList(userViewModel.getUserId()));
        groupViewModel = ViewModelProviders.of(this).get(GroupViewModel.class);

        initView();
    }

    private void initView() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.containerFrameLayout, RemoveGroupMemberFragment.newInstance(groupInfo))
                .commit();
    }

    @Override
    protected int menu() {
        return R.menu.group_remove_member;
    }

    @Override
    protected void afterMenus(Menu menu) {
        menuItem = menu.findItem(R.id.remove);
        menuItem.setEnabled(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.remove) {
            removeMember();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        pickContactViewModel.contactCheckStatusUpdateLiveData().removeObserver(contactCheckStatusUpdateLiveDataObserver);
    }


    void removeMember() {
        MaterialDialog dialog = new MaterialDialog.Builder(this)
                .content("删除中...")
                .progress(true, 100)
                .cancelable(false)
                .build();
        dialog.show();
        List<UIUserInfo> checkedUsers = pickContactViewModel.getCheckedContacts();
        if (checkedUsers != null && !checkedUsers.isEmpty()) {
            ArrayList<String> checkedIds = new ArrayList<>(checkedUsers.size());
            for (UIUserInfo user : checkedUsers) {
                checkedIds.add(user.getUserInfo().uid);
            }
            groupViewModel.removeGroupMember(groupInfo, checkedIds).observe(this, result -> {
                dialog.dismiss();
                if (result) {
                    Intent intent = new Intent();
                    intent.putStringArrayListExtra("memberIds", checkedIds);
                    setResult(RESULT_REMOVE_SUCCESS, intent);
                    UIUtils.showToast(UIUtils.getString(R.string.del_member_success));
                } else {
                    setResult(RESULT_REMOVE_FAIL);
                    UIUtils.showToast(UIUtils.getString(R.string.del_member_fail));
                }
                finish();
            });

        }
    }

}
