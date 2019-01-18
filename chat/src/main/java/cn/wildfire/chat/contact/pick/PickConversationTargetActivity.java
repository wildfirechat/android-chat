package cn.wildfire.chat.contact.pick;

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import android.content.Intent;
import androidx.annotation.Nullable;
import android.view.Menu;
import android.view.MenuItem;

import cn.wildfirechat.chat.R;

import java.util.List;

import cn.wildfire.chat.WfcBaseActivity;
import cn.wildfire.chat.contact.model.UIUserInfo;

public abstract class PickConversationTargetActivity extends WfcBaseActivity implements PickConversationTargetFragment.OnGroupPickListener {
    public static final String CURRENT_PARTICIPANTS = "currentParticipants";

    private boolean pickGroupForResult = true;
    private boolean multiGroupMode = false;
    private MenuItem menuItem;

    private PickContactViewModel pickContactViewModel;
    private Observer<UIUserInfo> contactCheckStatusUpdateLiveDataObserver = new Observer<UIUserInfo>() {
        @Override
        public void onChanged(@Nullable UIUserInfo userInfo) {
            List<UIUserInfo> list = pickContactViewModel.getCheckedContacts();
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
        Intent intent = getIntent();
        List<String> initialParticipantsIds = intent.getStringArrayListExtra(CURRENT_PARTICIPANTS);

        pickContactViewModel = ViewModelProviders.of(this).get(PickContactViewModel.class);
        pickContactViewModel.contactCheckStatusUpdateLiveData().observeForever(contactCheckStatusUpdateLiveDataObserver);
        pickContactViewModel.setInitialCheckedIds(initialParticipantsIds);
        pickContactViewModel.setUncheckableIds(initialParticipantsIds);

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
        PickConversationTargetFragment fragment = PickConversationTargetFragment.newInstance(pickGroupForResult, multiGroupMode);
        fragment.setOnGroupPickListener(this);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.containerFrameLayout, fragment)
                .commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        pickContactViewModel.contactCheckStatusUpdateLiveData().removeObserver(contactCheckStatusUpdateLiveDataObserver);
    }

    /**
     * @param newlyCheckedUserInfos   新选中的
     * @param initialCheckedUserInfos 初始选中的，可能为空
     */
    protected abstract void onContactPicked(List<UIUserInfo> newlyCheckedUserInfos, @Nullable List<UIUserInfo> initialCheckedUserInfos);

    protected void onConfirmClick() {
        List<UIUserInfo> uiUserInfos = pickContactViewModel.getCheckedContacts();
        onContactPicked(uiUserInfos, pickContactViewModel.getInitialCheckedContacts());
    }
}
