/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.group;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;

import java.util.ArrayList;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.page.WfcPageCompat;
import cn.wildfirechat.model.GroupInfo;

/**
 * 「从群成员里选人并把结果回传给调用方」（发起群语音/视频、多人通话加人）。
 * <p>
 * 列表、参数装配、确认菜单都在 {@link BasePickGroupMemberPageFragment} 里，本类只剩「确认时
 * 回传什么」。结果通过 {@link WfcPageCompat#setPageResult} 回传 —— 手机端就是
 * {@code Activity.setResult}，右栏则在本页出栈时投递回发起方的 {@code onActivityResult}，
 * 调用方两端写法完全一致。
 */
public class PickGroupMemberPageFragment extends BasePickGroupMemberPageFragment {

    /**
     * 结果里携带的成员 id 列表，键名与改造前 {@code PickGroupMemberActivity.EXTRA_RESULT} 相同，
     * 调用方不用改。
     */
    public static final String EXTRA_RESULT = PickGroupMemberActivity.EXTRA_RESULT;

    /**
     * @param unCheckableMemberIds 不可选中的成员（自己、已在通话中的人）
     * @param checkedMemberIds     进入时就已勾选且不可取消的成员
     * @param maxCount             最多可选人数
     */
    public static PickGroupMemberPageFragment newInstance(GroupInfo groupInfo,
                                                          @Nullable ArrayList<String> unCheckableMemberIds,
                                                          @Nullable ArrayList<String> checkedMemberIds,
                                                          int maxCount) {
        PickGroupMemberPageFragment fragment = new PickGroupMemberPageFragment();
        fragment.setArguments(buildArgs(groupInfo, unCheckableMemberIds, checkedMemberIds, maxCount));
        return fragment;
    }

    /**
     * 由启动 intent 造页面，供 {@link PickGroupMemberActivity} 与 {@code PaneRegistry} 共用，
     * 保证两端读的是同一套 extra。
     */
    @Nullable
    public static PickGroupMemberPageFragment fromIntent(Intent intent) {
        Bundle args = argsFromIntent(intent);
        if (args == null) {
            return null;
        }
        PickGroupMemberPageFragment fragment = new PickGroupMemberPageFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public int pageMenu() {
        return R.menu.group_member_pick;
    }

    @Override
    protected int confirmMenuItemId() {
        return R.id.confirm;
    }

    @Override
    protected int confirmLabelRes() {
        return R.string.complete;
    }

    @Override
    protected int confirmLabelWithCountRes() {
        return R.string.complete_with_count;
    }

    @Override
    protected void onConfirm() {
        Intent data = new Intent();
        data.putStringArrayListExtra(EXTRA_RESULT, checkedMemberIds());
        WfcPageCompat.setPageResult(this, Activity.RESULT_OK, data);
        WfcPageCompat.finishPage(this);
    }
}
