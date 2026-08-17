/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.pick;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.conversationlist.ConversationListFragment;
import cn.wildfire.chat.kit.conversationlist.OnClickConversationItemListener;
import cn.wildfire.chat.kit.page.WfcPage;
import cn.wildfire.chat.kit.page.WfcPageCompat;
import cn.wildfirechat.model.ConversationInfo;

/**
 * 「挑一个已有会话」整页：一张会话列表，点中即回传。
 * <p>
 * 逐行搬自 {@link PickConversationActivity}。结果 extra 仍是 {@code conversationInfo}。
 */
public class PickConversationPageFragment extends Fragment
    implements WfcPage, OnClickConversationItemListener {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_container_no_toolbar_activity, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ConversationListFragment fragment =
            (ConversationListFragment) getChildFragmentManager().findFragmentById(R.id.containerFrameLayout);
        if (fragment == null) {
            fragment = new ConversationListFragment();
            getChildFragmentManager()
                .beginTransaction()
                .add(R.id.containerFrameLayout, fragment)
                .commit();
        }
        // 监听器不是可保存的状态，重建后要重新挂上
        fragment.setOnClickConversationItemListener(this);
    }

    @Override
    public void onClickConversationItem(ConversationInfo conversationInfo) {
        Intent data = new Intent();
        data.putExtra("conversationInfo", conversationInfo);
        WfcPageCompat.setPageResult(this, Activity.RESULT_OK, data);
        WfcPageCompat.finishPage(this);
    }
}
