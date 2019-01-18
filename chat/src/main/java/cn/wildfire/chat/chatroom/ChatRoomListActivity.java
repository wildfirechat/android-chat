package cn.wildfire.chat.chatroom;

import cn.wildfirechat.chat.R;

import cn.wildfire.chat.WfcBaseActivity;

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
