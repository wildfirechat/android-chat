package cn.wildfire.chat.kit.group;

import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;

import androidx.lifecycle.ViewModelProviders;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfire.chat.kit.third.utils.UIUtils;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.model.GroupMember;
import cn.wildfirechat.remote.ChatManager;

public class RemoveGroupMemberActivity extends BasePickGroupMemberActivity {
    private MenuItem menuItem;
    public static final int RESULT_REMOVE_SUCCESS = 2;
    public static final int RESULT_REMOVE_FAIL = 3;
    private GroupViewModel groupViewModel;
    private List<UIUserInfo> checkedGroupMembers;

    @Override
    protected void onGroupMemberChecked(List<UIUserInfo> checkedUserInfos) {
        this.checkedGroupMembers = checkedUserInfos;
        if (checkedUserInfos == null || checkedUserInfos.isEmpty()) {
            menuItem.setTitle("删除");
            menuItem.setEnabled(false);
        } else {
            menuItem.setTitle("删除(" + checkedUserInfos.size() + ")");
            menuItem.setEnabled(true);
        }
    }

    @Override
    protected void afterViews() {
        super.afterViews();
        groupViewModel = ViewModelProviders.of(this).get(GroupViewModel.class);
        GroupMember groupMember = groupViewModel.getGroupMember(groupInfo.target, ChatManager.Instance().getUserId());
        if (groupMember.type == GroupMember.GroupMemberType.Manager) {
            pickUserViewModel.addUncheckableIds(Collections.singletonList(groupInfo.owner));
        }
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
            removeMember(checkedGroupMembers);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    void removeMember(List<UIUserInfo> checkedUsers) {
        MaterialDialog dialog = new MaterialDialog.Builder(this)
                .content("删除中...")
                .progress(true, 100)
                .cancelable(false)
                .build();
        dialog.show();
        if (checkedUsers != null && !checkedUsers.isEmpty()) {
            ArrayList<String> checkedIds = new ArrayList<>(checkedUsers.size());
            for (UIUserInfo user : checkedUsers) {
                checkedIds.add(user.getUserInfo().uid);
            }
            groupViewModel.removeGroupMember(groupInfo, checkedIds, null, Collections.singletonList(0)).observe(this, result -> {
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
