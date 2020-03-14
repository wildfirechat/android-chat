package cn.wildfire.chat.kit.contact.pick;

import android.app.Activity;
import android.content.Context;
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
    public static final String PARAM_MAX_COUNT = "maxCount";
    public static final String PARAM_INITIAL_CHECKED_IDS = "initialCheckedIds";
    public static final String PARA_UNCHECKABLE_IDS = "uncheckableIds";
    public static final String RESULT_PICKED_USERS = "pickedUsers";

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
        Intent intent = getIntent();
        int maxCount = intent.getIntExtra(PARAM_MAX_COUNT, 0);
        if (maxCount > 0) {
            pickUserViewModel.setMaxPickCount(maxCount);
        }

        pickUserViewModel.setInitialCheckedIds(intent.getStringArrayListExtra(PARAM_INITIAL_CHECKED_IDS));
        pickUserViewModel.setUncheckableIds(intent.getStringArrayListExtra(PARA_UNCHECKABLE_IDS));

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

    protected void onContactPicked(List<UIUserInfo> newlyCheckedUserInfos) {
        Intent intent = new Intent();
        ArrayList<UserInfo> newlyPickedInfos = new ArrayList<>();
        for (UIUserInfo info : newlyCheckedUserInfos) {
            newlyPickedInfos.add(info.getUserInfo());
        }
        intent.putExtra(RESULT_PICKED_USERS, newlyPickedInfos);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    protected void onConfirmClick() {
        List<UIUserInfo> newlyCheckedUserInfos = pickUserViewModel.getCheckedUsers();
        onContactPicked(newlyCheckedUserInfos);
    }

    public static Intent buildPickIntent(Context context, int maxCount, ArrayList<String> initialChecedIds, ArrayList<String> uncheckableIds) {
        Intent intent = new Intent(context, PickContactActivity.class);
        intent.putExtra(PARAM_MAX_COUNT, maxCount);
        intent.putExtra(PARAM_INITIAL_CHECKED_IDS, initialChecedIds);
        intent.putExtra(PARA_UNCHECKABLE_IDS, uncheckableIds);
        return intent;
    }
}
