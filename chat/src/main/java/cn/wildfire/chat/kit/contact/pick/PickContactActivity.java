package cn.wildfire.chat.kit.contact.pick;

import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.model.UserInfo;

public class PickContactActivity extends WfcBaseActivity {

    private MenuItem menuItem;

    private PickUserViewModel pickUserViewModel;
    private Observer<UIUserInfo> contactCheckStatusUpdateLiveDataObserver = new Observer<UIUserInfo>() {
        @Override
        public void onChanged(@Nullable UIUserInfo userInfo) {
            List<UIUserInfo> list = pickUserViewModel.getCheckedUsers();
            updatePickStatus(list);
        }
    };

    protected void updatePickStatus(List<UIUserInfo> userInfos) {
        if (userInfos == null || userInfos.isEmpty()) {
            menuItem.setTitle("确定");
            menuItem.setEnabled(false);
        } else {
            menuItem.setTitle("确定(" + userInfos.size() + ")");
            menuItem.setEnabled(true);
        }
    }

    @Override
    protected int contentLayout() {
        return R.layout.fragment_container_activity;
    }

    @Override
    protected void afterViews() {
        pickUserViewModel = ViewModelProviders.of(this).get(PickUserViewModel.class);
        pickUserViewModel.userCheckStatusUpdateLiveData().observeForever(contactCheckStatusUpdateLiveDataObserver);
        int maxCount = getIntent().getIntExtra("maxCount", 0);
        if (maxCount > 0) {
            pickUserViewModel.setMaxPickCount(maxCount);
        }
        pickUserViewModel.setInitialCheckedIds(null);
        pickUserViewModel.setUncheckableIds(null);

        initView();
    }

    @Override
    protected int menu() {
        return R.menu.contact_pick;
    }

    @Override
    protected void afterMenus(Menu menu) {
        menuItem = menu.findItem(R.id.confirm);
        menuItem.setEnabled(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.confirm) {
            onConfirmClick();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initView() {
        PickContactFragment fragment = new PickContactFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.containerFrameLayout, fragment)
                .commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        pickUserViewModel.userCheckStatusUpdateLiveData().removeObserver(contactCheckStatusUpdateLiveDataObserver);
    }

    protected void onContactPicked(List<UIUserInfo> initialCheckedUserInfos, List<UIUserInfo> newlyCheckedUserInfos) {
        Intent intent = new Intent();
        ArrayList<UserInfo> newlyPickedInfos = new ArrayList<>();
        if (newlyCheckedUserInfos != null) {
            for (UIUserInfo info : newlyCheckedUserInfos) {
                newlyPickedInfos.add(info.getUserInfo());
            }
        }
        intent.putExtra("pickedUsers", newlyPickedInfos);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    protected void onConfirmClick() {
        List<UIUserInfo> initialCheckedUserInfos = pickUserViewModel.getInitialCheckedUsers();
        List<UIUserInfo> newlyCheckedUserInfos = pickUserViewModel.getCheckedUsers();
        onContactPicked(initialCheckedUserInfos, newlyCheckedUserInfos);
    }
}
