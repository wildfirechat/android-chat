/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.contact.pick.PickConversationTargetActivity;

/**
 * 「发起群聊 / 新建会话」在手机端的宿主壳。
 * <p>
 * 整页逻辑（选人列表 + 确定菜单 + 建会话）住在 {@link CreateConversationPageFragment}，
 * 手机端与平板右栏共用同一份。改造前本类继承抽象的 {@link PickConversationTargetActivity}，
 * 确定菜单在基类、建会话在子类，都挂在 Activity 上，页面因此无法进入右栏。
 */
public class CreateConversationActivity extends WfcBaseActivity {

    /**
     * 进入时已经在会话里、不可取消勾选的成员 id 列表。键名与
     * {@link PickConversationTargetActivity#CURRENT_PARTICIPANTS} 保持一致，调用方无需改动。
     */
    public static final String CURRENT_PARTICIPANTS = PickConversationTargetActivity.CURRENT_PARTICIPANTS;

    @Override
    protected int contentLayout() {
        return R.layout.fragment_container_activity;
    }

    @Override
    protected void afterViews() {
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.containerFrameLayout, CreateConversationPageFragment.fromIntent(getIntent()))
            .commit();
    }
}
