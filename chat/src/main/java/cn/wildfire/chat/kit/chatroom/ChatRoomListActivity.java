package cn.wildfire.chat.kit.chatroom;

import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfirechat.chat.R;

public class ChatRoomListActivity extends WfcBaseActivity {

    @Override
    protected void afterViews() {
        getSupportFragmentManager().
                beginTransaction()
                .replace(R.id.containerFrameLayout, new ChatRoomListFragment())
                .commit();
    }

    @Override
    protected int contentLayout() {
        return R.layout.fragment_container_activity;
    }
}
