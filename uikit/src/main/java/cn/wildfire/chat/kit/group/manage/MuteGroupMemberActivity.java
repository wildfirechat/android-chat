package cn.wildfire.chat.kit.group.manage;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.common.OperateResult;
import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfire.chat.kit.group.BasePickGroupMemberActivity;
import cn.wildfire.chat.kit.group.GroupViewModel;

public class MuteGroupMemberActivity extends BasePickGroupMemberActivity {
    private MenuItem menuItem;
    private List<UIUserInfo> checkedGroupMembers;
    private boolean groupMuted = false;

    @Override
    protected void onGroupMemberChecked(List<UIUserInfo> checkedUserInfos) {
        this.checkedGroupMembers = checkedUserInfos;
        if (checkedUserInfos == null || checkedUserInfos.isEmpty()) {
            menuItem.setTitle("确定");
            menuItem.setEnabled(false);
        } else {
            menuItem.setTitle("确定(" + checkedUserInfos.size() + ")");
            menuItem.setEnabled(true);
        }
    }

    @Override
    protected void afterViews() {
        super.afterViews();
        groupMuted = getIntent().getBooleanExtra("groupMuted", false);
    }

    @Override
    protected int menu() {
        return R.menu.group_manage_add_manager;
    }

    @Override
    protected void afterMenus(Menu menu) {
        menuItem = menu.findItem(R.id.confirm);
        menuItem.setEnabled(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.confirm) {
            muteOrAllowGroupMembers();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void muteOrAllowGroupMembers() {
        GroupViewModel groupViewModel = ViewModelProviders.of(this).get(GroupViewModel.class);
        List<String> memberIds = new ArrayList<>(checkedGroupMembers.size());
        for (UIUserInfo info : checkedGroupMembers) {
            memberIds.add(info.getUserInfo().uid);
        }
        MaterialDialog dialog = new MaterialDialog.Builder(this)
            .content(groupMuted ? "加入白名单中..." : "禁言中...")
            .progress(true, 100)
            .cancelable(false)
            .build();
        dialog.show();
        Observer<OperateResult<Boolean>> observer =
            booleanOperateResult -> {
                dialog.dismiss();
                if (booleanOperateResult.isSuccess()) {
                    finish();
                } else {
                    Toast.makeText(this, groupMuted ? "添加白名单错误" : "设置禁言错误 " + booleanOperateResult.getErrorCode(), Toast.LENGTH_SHORT).show();
                }
            };
        if (groupMuted) {
            groupViewModel.allowGroupMember(groupInfo.target, true, memberIds, null, Collections.singletonList(0))
                .observe(this, observer);
        } else {
            groupViewModel.muteGroupMember(groupInfo.target, true, memberIds, null, Collections.singletonList(0))
                .observe(this, observer);
        }
    }
}
