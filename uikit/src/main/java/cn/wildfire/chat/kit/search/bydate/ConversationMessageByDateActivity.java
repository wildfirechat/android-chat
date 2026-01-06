package cn.wildfire.chat.kit.search.bydate;

import androidx.fragment.app.Fragment;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfirechat.model.Conversation;

public class ConversationMessageByDateActivity extends WfcBaseActivity {

    @Override
    protected int contentLayout() {
        return R.layout.fragment_container_activity;
    }

    @Override
    protected void afterViews() {
        if (!isDarkTheme()) {
            setTitleBackgroundResource(R.color.white, false);
        }
        setTitle("按日期查找");

        Conversation conversation = getIntent().getParcelableExtra("conversation");

        Fragment fragment = ConversationMessageByDateFragment.newInstance(conversation);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.containerFrameLayout, fragment)
                .commit();
    }
}
