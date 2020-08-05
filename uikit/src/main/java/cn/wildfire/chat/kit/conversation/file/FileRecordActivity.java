package cn.wildfire.chat.kit.conversation.file;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfirechat.model.Conversation;

public class FileRecordActivity extends WfcBaseActivity {

    @Override
    protected int contentLayout() {
        return R.layout.fragment_container_activity;
    }

    @Override
    protected void afterViews() {
        if (!isDarkTheme()) {
            setTitleBackgroundResource(R.color.white, false);
        }
        Conversation conversation = getIntent().getParcelableExtra("conversation");
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.containerFrameLayout, FileRecordFragment.newInstance(conversation))
            .commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
