package cn.wildfire.chat.channel;

import android.content.Intent;
import android.view.MenuItem;

import cn.wildfirechat.chat.R;

import cn.wildfire.chat.WfcBaseActivity;

public class ChannelListActivity extends WfcBaseActivity {

    @Override
    protected int menu() {
        return R.menu.channel_list;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.create) {
            createChannel();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected int contentLayout() {
        return R.layout.fragment_container_activity;
    }

    @Override
    protected void afterViews() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.containerFrameLayout, new ChannelListFragment())
                .commit();
    }

    void createChannel() {
        Intent intent = new Intent(this, CreateChannelActivity.class);
        startActivity(intent);
        finish();
    }
}
