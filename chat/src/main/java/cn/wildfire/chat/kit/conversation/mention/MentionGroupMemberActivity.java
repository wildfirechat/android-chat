package cn.wildfire.chat.kit.conversation.mention;

import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.model.GroupInfo;

public class MentionGroupMemberActivity extends WfcBaseActivity {
    private GroupInfo groupInfo;

    @Override
    protected void afterViews() {
        groupInfo = getIntent().getParcelableExtra("groupInfo");
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.containerFrameLayout, MentionGroupMemberFragment.newInstance(groupInfo))
                .commit();
    }

    @Override
    protected int contentLayout() {
        return R.layout.fragment_container_activity;
    }
}
