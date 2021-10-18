/*
 * Copyright (c) 2021 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.ptt;

import android.content.Intent;
import android.view.MenuItem;

import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfirechat.chat.R;

public class PttListActivity extends WfcBaseActivity {
    @Override
    protected int contentLayout() {
        return R.layout.ptt_list_activity;
    }

    @Override
    protected int menu() {
        return R.menu.ptt_list;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.create) {
            Intent intent = new Intent(this, CreatePttChannelActivity.class);
            startActivity(intent);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }
}
