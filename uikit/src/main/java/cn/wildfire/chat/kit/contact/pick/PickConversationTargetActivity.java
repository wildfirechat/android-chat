package cn.wildfire.chat.kit.contact.pick;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import java.util.LinkedList;
import java.util.List;

import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;

public abstract class PickConversationTargetActivity extends WfcBaseActivity implements PickConversationTargetFragment.OnGroupPickListener {
    public static final String CURRENT_PARTICIPANTS = "currentParticipants";

    private boolean pickGroupForResult = true;
    private boolean multiGroupMode = false;
    private MenuItem menuItem;
    private TextView confirmTv;

    protected PickUserViewModel pickUserViewModel;
    private Observer<UIUserInfo> contactCheckStatusUpdateLiveDataObserver = new Observer<UIUserInfo>() {
        @Override
        public void onChanged(@Nullable UIUserInfo userInfo) {
            List<UIUserInfo> list = pickUserViewModel.getCheckedUsers();
            updatePickStatus(list);
        }
    };

    protected void updatePickStatus(List<UIUserInfo> userInfos) {
        if (userInfos == null || userInfos.isEmpty()) {
            confirmTv.setText("完成");
            menuItem.setEnabled(false);
        } else {
            confirmTv.setText("完成(" + userInfos.size() + ")");
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

        pickUserViewModel = ViewModelProviders.of(this).get(PickUserViewModel.class);
        pickUserViewModel.userCheckStatusUpdateLiveData().observeForever(contactCheckStatusUpdateLiveDataObserver);
        pickUserViewModel.setInitialCheckedIds(initialParticipantsIds);
        pickUserViewModel.setUncheckableIds(initialParticipantsIds);

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
    public boolean onPrepareOptionsMenu(Menu menu) {
        View view = menuItem.getActionView();
        confirmTv = view.findViewById(R.id.confirm_tv);
        confirmTv.setOnClickListener(v -> onOptionsItemSelected(menuItem));
        return super.onPrepareOptionsMenu(menu);
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
        pickUserViewModel.userCheckStatusUpdateLiveData().removeObserver(contactCheckStatusUpdateLiveDataObserver);
    }

    protected abstract void onContactPicked(List<UIUserInfo> newlyCheckedUserInfos);

    protected void onConfirmClick() {
        List<UIUserInfo> newlyCheckedUserInfos = pickUserViewModel.getCheckedUsers();
        onContactPicked(newlyCheckedUserInfos);
    }
}
