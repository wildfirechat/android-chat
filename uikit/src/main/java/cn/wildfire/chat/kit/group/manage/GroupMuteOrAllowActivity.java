package cn.wildfire.chat.kit.group.manage;

import android.widget.Toast;

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.kyleduo.switchbutton.SwitchButton;

import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.group.GroupViewModel;
import cn.wildfirechat.model.GroupInfo;

public class GroupMuteOrAllowActivity extends WfcBaseActivity {
    private GroupInfo groupInfo;
    private GroupViewModel groupViewModel;

    @BindView(R2.id.muteSwitchButton)
    SwitchButton switchButton;

    @Override
    protected int contentLayout() {
        return R.layout.group_manage_mute_activity;
    }

    @Override
    protected void afterViews() {
        super.afterViews();
        groupInfo = getIntent().getParcelableExtra("groupInfo");
        init();
    }

    private void init() {
        groupViewModel = ViewModelProviders.of(this).get(GroupViewModel.class);
        groupViewModel.groupInfoUpdateLiveData().observe(this, new Observer<List<GroupInfo>>() {
            @Override
            public void onChanged(List<GroupInfo> groupInfos) {
                if (groupInfos != null) {
                    for (GroupInfo info : groupInfos) {
                        if (info.target.equals(groupInfo.target)) {
                            boolean oMuted = groupInfo.mute == 1;
                            boolean nMuted = info.mute == 1;
                            groupInfo = info;

                            if (oMuted != nMuted) {
                                initGroupMemberMuteListFragment(true);
                            }
                            break;
                        }
                    }
                }

            }
        });
        switchButton.setCheckedNoEvent(groupInfo.mute == 1);
        switchButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            groupViewModel.muteAll(groupInfo.target, isChecked, null, Collections.singletonList(0)).observe(this, booleanOperateResult -> {
                if (!booleanOperateResult.isSuccess()) {
                    switchButton.setCheckedNoEvent(!isChecked);
                    Toast.makeText(this, "禁言失败 " + booleanOperateResult.getErrorCode(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        initGroupMemberMuteListFragment(false);
    }

    private GroupMemberMuteOrAllowListFragment fragment;

    private void initGroupMemberMuteListFragment(boolean forceUpdate) {
        if (fragment == null || forceUpdate) {
            fragment = GroupMemberMuteOrAllowListFragment.newInstance(groupInfo);
        }
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.containerFrameLayout, fragment)
            .commit();
    }
}
