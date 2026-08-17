/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.mention;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.search.SearchPageFragment;
import cn.wildfire.chat.kit.search.SearchableModule;
import cn.wildfirechat.model.GroupInfo;

// ATTENTION
// 现在单聊也支持@机器人，本页在单聊里 groupInfo 为 null，只列 AI 机器人
/**
 * 输入框里打 {@code @} 之后弹出的选人页：上半部分是一条搜索框，下面直接铺
 * {@link MentionGroupMemberFragment}（群成员 + AI 机器人），一旦开始搜索就由搜索结果盖住。
 * <p>
 * 本页要回传选中的人，调用方（{@code ConversationInputPanel}）必须用
 * {@code WfcPageCompat.startPageForResult} 打开，否则在右栏里拿不到结果。
 */
public class MentionGroupMemberPageFragment extends SearchPageFragment {

    private static final String ARG_GROUP_INFO = "groupInfo";

    private GroupInfo groupInfo;

    public static MentionGroupMemberPageFragment fromIntent(@Nullable Intent intent) {
        Bundle args = argsFromIntent(intent);
        if (intent != null) {
            args.putParcelable(ARG_GROUP_INFO, intent.getParcelableExtra(ARG_GROUP_INFO));
        }
        MentionGroupMemberPageFragment fragment = new MentionGroupMemberPageFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        groupInfo = getArguments() == null ? null : getArguments().getParcelable(ARG_GROUP_INFO);
    }

    @Override
    protected int contentLayout() {
        return R.layout.group_mention_activity;
    }

    @Override
    protected boolean hideSearchDescView() {
        return true;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getChildFragmentManager().findFragmentById(R.id.mentionGroupMemberContainer) == null) {
            getChildFragmentManager().beginTransaction()
                .replace(R.id.mentionGroupMemberContainer, MentionGroupMemberFragment.newInstance(groupInfo))
                .commit();
        }
    }

    @Override
    protected void initSearchModule(List<SearchableModule> modules) {
        if (groupInfo != null) {
            modules.add(new GroupMemberSearchModule(groupInfo.target));
        }
    }
}
