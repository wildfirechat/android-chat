package cn.wildfire.chat.kit.group;

import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfirechat.chat.R;

public class PickGroupMemberActivity extends BasePickGroupMemberActivity {
    private MenuItem menuItem;
    private List<UIUserInfo> checkedGroupMembers;
    public static final String EXTRA_RESULT = "pickedMemberIds";

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
    protected int menu() {
        return R.menu.group_member_pick;
    }

    @Override
    protected void afterMenus(Menu menu) {
        menuItem = menu.findItem(R.id.confirm);
        menuItem.setEnabled(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.confirm) {
            Intent intent = new Intent();
            ArrayList<String> memberIds = new ArrayList<>();
            for (UIUserInfo userInfo : checkedGroupMembers) {
                memberIds.add(userInfo.getUserInfo().uid);
            }
            intent.putStringArrayListExtra(EXTRA_RESULT, memberIds);
            setResult(Activity.RESULT_OK, intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
