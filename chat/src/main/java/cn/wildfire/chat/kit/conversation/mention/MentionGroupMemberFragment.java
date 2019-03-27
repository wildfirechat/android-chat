package cn.wildfire.chat.kit.conversation.mention;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import cn.wildfire.chat.kit.contact.model.HeaderValue;
import cn.wildfire.chat.kit.group.GroupMemberListFragment;
import cn.wildfirechat.model.GroupInfo;

public class MentionGroupMemberFragment extends GroupMemberListFragment {

    public static MentionGroupMemberFragment newInstance(GroupInfo groupInfo) {
        Bundle args = new Bundle();
        args.putParcelable("groupInfo", groupInfo);
        MentionGroupMemberFragment fragment = new MentionGroupMemberFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void initHeaderViewHolders() {
        addHeaderViewHolder(MentionAllHeaderViewHolder.class, new HeaderValue());
    }

    @Override
    public void onHeaderClick(int index) {
        Intent intent = new Intent();
        intent.putExtra("mentionAll", true);
        getActivity().setResult(Activity.RESULT_OK, intent);
        getActivity().finish();
    }
}
