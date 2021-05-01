/*
 * Copyright (c) 2021 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.voip.conference;

import android.content.Intent;
import android.view.MenuItem;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.message.ConferenceInviteMessageContent;

public class ConferenceParticipantListActivity extends WfcBaseActivity {
    @Override
    protected int contentLayout() {
        return R.layout.fragment_container_activity;
    }

    @Override
    protected void afterViews() {
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.containerFrameLayout, new ConferenceParticipantListFragment())
            .commit();
    }

    @Override
    protected int menu() {
        return R.menu.conference_participant_list;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_conference_participant_add) {
            addParticipant();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void addParticipant() {
        AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
        ConferenceInviteMessageContent invite = new ConferenceInviteMessageContent(session.getCallId(), session.getHost(), session.getTitle(), session.getDesc(), session.getStartTime(), session.isAudioOnly(), session.isDefaultAudience(), session.isAdvanced(), session.getPin());

        Intent intent = new Intent(this, ConferenceInviteActivity.class);
        intent.putExtra("inviteMessage", invite);
        startActivity(intent);
    }

}
