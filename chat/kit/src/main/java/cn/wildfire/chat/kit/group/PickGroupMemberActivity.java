package cn.wildfire.chat.kit.group;

import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfirechat.chat.R;

public class PickGroupMemberActivity extends BasePickGroupMemberActivity {
    private MenuItem menuItem;
    private TextView confirmTv;
    private List<UIUserInfo> checkedGroupMembers;
    public static final String EXTRA_RESULT = "pickedMemberIds";

    // disable the dark ui for voip
//    @Override
//    protected void afterViews() {
//        super.afterViews();
//        setTitleBackgroundResource(R.color.black5);
//        setTitleTextColor(Color.WHITE);
//    }
//
//    @Override
//    protected Fragment getFragment() {
//        return PickGroupMemberBlackFragment.newInstance(groupInfo);
//    }

    @Override
    protected void onGroupMemberChecked(List<UIUserInfo> checkedUserInfos) {
        this.checkedGroupMembers = checkedUserInfos;
        if (checkedUserInfos == null || checkedUserInfos.isEmpty()) {
            confirmTv.setText("完成");
            menuItem.setEnabled(false);
        } else {
            confirmTv.setText("完成(" + checkedUserInfos.size() + ")");
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
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.confirm);
        confirmTv = item.getActionView().findViewById(R.id.confirm_tv);
        confirmTv.setOnClickListener(v -> onOptionsItemSelected(menuItem));
        return super.onPrepareOptionsMenu(menu);
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
