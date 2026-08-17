/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.group;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.lifecycle.Observer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfire.chat.kit.page.WfcPage;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfirechat.model.GroupInfo;

/**
 * 「从群成员里选人，然后做一件事」这一类<strong>页面</strong>的公共部分：
 * 读参数、装配 {@code PickUserViewModel}、确认菜单及其「(n)」计数文案。
 * <p>
 * 它是 {@link BasePickGroupMemberActivity} 的 Fragment 版。老基类把这些逻辑放在 Activity 上，
 * 于是「页面 = Activity」，永远进不了平板右栏；收敛到 Fragment 之后，手机端（一个空壳 Activity）
 * 与右栏共用同一份实现。四个子类各自只剩「菜单是哪个」「按下确认干什么」：
 * <ul>
 *   <li>{@link PickGroupMemberPageFragment} —— 选人并回传结果（发起群语音/视频）；</li>
 *   <li>{@link RemoveGroupMemberPageFragment} —— 移出群成员；</li>
 *   <li>{@code MuteGroupMemberPageFragment} —— 禁言 / 加白名单；</li>
 *   <li>{@code AddGroupManagerPageFragment} —— 添加群管理员。</li>
 * </ul>
 */
public abstract class BasePickGroupMemberPageFragment extends PickGroupMemberFragment implements WfcPage {

    protected GroupInfo groupInfo;
    /**
     * 当前勾选的成员。未勾选任何人时为 null 或空表，子类的确认逻辑要自行判空。
     */
    protected List<UIUserInfo> checkedGroupMembers;

    private MenuItem confirmMenuItem;
    private TextView confirmTextView;

    private final Observer<Object> checkStatusObserver = obj -> {
        checkedGroupMembers = pickUserViewModel.getCheckedUsers();
        updateConfirmState();
    };

    /**
     * 造 arguments。键名与 {@link BasePickGroupMemberActivity} 的 intent extra 完全一致，
     * 于是 {@link #argsFromIntent} 可以原样搬过来，两端读的是同一套参数。
     *
     * @param unCheckableMemberIds 不可选中的成员（自己、群主、已在通话中的人）
     * @param checkedMemberIds     进入时就已勾选且不可取消的成员
     * @param maxCount             最多可选人数
     */
    protected static Bundle buildArgs(GroupInfo groupInfo,
                                      @Nullable ArrayList<String> unCheckableMemberIds,
                                      @Nullable ArrayList<String> checkedMemberIds,
                                      int maxCount) {
        Bundle args = new Bundle();
        args.putParcelable(BasePickGroupMemberActivity.GROUP_INFO, groupInfo);
        args.putStringArrayList(BasePickGroupMemberActivity.UNCHECKABLE_MEMBER_IDS, unCheckableMemberIds);
        args.putStringArrayList(BasePickGroupMemberActivity.CHECKED_MEMBER_IDS, checkedMemberIds);
        args.putInt(BasePickGroupMemberActivity.MAX_COUNT, maxCount);
        return args;
    }

    /**
     * 由启动 intent 造 arguments，供手机端的空壳 Activity 与 {@code PaneRegistry} 共用。
     *
     * @return null 表示 intent 里没有群资料，这个页面开不起来
     */
    @Nullable
    protected static Bundle argsFromIntent(Intent intent) {
        GroupInfo groupInfo = intent.getParcelableExtra(BasePickGroupMemberActivity.GROUP_INFO);
        if (groupInfo == null) {
            return null;
        }
        return buildArgs(groupInfo,
            intent.getStringArrayListExtra(BasePickGroupMemberActivity.UNCHECKABLE_MEMBER_IDS),
            intent.getStringArrayListExtra(BasePickGroupMemberActivity.CHECKED_MEMBER_IDS),
            intent.getIntExtra(BasePickGroupMemberActivity.MAX_COUNT, Integer.MAX_VALUE));
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        // super 里 PickUserFragment 会按「页面」作用域取到 PickUserViewModel，
        // 右栏下那是本页这一层，不会和别的 tab 串
        super.onCreate(savedInstanceState);
        groupInfo = getArguments() == null ? null
            : getArguments().getParcelable(BasePickGroupMemberActivity.GROUP_INFO);
        applyPickConstraints();
        pickUserViewModel.userCheckStatusUpdateLiveData().observeForever(checkStatusObserver);
    }

    @Override
    public void onDestroy() {
        pickUserViewModel.userCheckStatusUpdateLiveData().removeObserver(checkStatusObserver);
        super.onDestroy();
    }

    /**
     * 与 {@code BasePickGroupMemberActivity.afterViews()} 中的那段逐行等价，只是参数来自
     * arguments 而不是 intent。
     */
    private void applyPickConstraints() {
        Bundle args = getArguments();
        if (args == null) {
            return;
        }
        List<String> checkedMemberIds = args.getStringArrayList(BasePickGroupMemberActivity.CHECKED_MEMBER_IDS);
        List<String> unCheckableMemberIds = args.getStringArrayList(BasePickGroupMemberActivity.UNCHECKABLE_MEMBER_IDS);

        if (checkedMemberIds != null && !checkedMemberIds.isEmpty()) {
            pickUserViewModel.setInitialCheckedIds(checkedMemberIds);
            pickUserViewModel.setUncheckableIds(checkedMemberIds);
        }
        if (unCheckableMemberIds != null && !unCheckableMemberIds.isEmpty()) {
            pickUserViewModel.setUncheckableIds(unCheckableMemberIds);
        } else {
            // 默认把自己排除掉
            UserViewModel userViewModel = WfcUIKit.getAppScopeViewModel(UserViewModel.class);
            pickUserViewModel.setUncheckableIds(
                new ArrayList<>(Collections.singletonList(userViewModel.getUserId())));
        }
        pickUserViewModel.setMaxPickCount(args.getInt(BasePickGroupMemberActivity.MAX_COUNT, Integer.MAX_VALUE));
    }

    // ==================== 子类要填的三件事 ====================

    /**
     * 确认菜单项的 id，须存在于 {@link #pageMenu()} 里。
     */
    protected abstract int confirmMenuItemId();

    /**
     * 按下确认。此时 {@link #checkedGroupMembers} 一定非空（没勾人时按钮是禁用的）。
     */
    protected abstract void onConfirm();

    /**
     * 未勾选任何人时的按钮文案（「完成」「删除」「确定」）。
     */
    @StringRes
    protected abstract int confirmLabelRes();

    /**
     * 勾选了 n 人时的按钮文案，须带一个 {@code %1$d} 占位（「完成(2)」）。
     */
    @StringRes
    protected abstract int confirmLabelWithCountRes();

    // ==================== WfcPage ====================

    @Override
    public void onPreparePageMenu(Menu menu) {
        confirmMenuItem = menu.findItem(confirmMenuItemId());
        if (confirmMenuItem == null) {
            return;
        }
        View actionView = confirmMenuItem.getActionView();
        confirmTextView = actionView == null ? null : actionView.findViewById(R.id.confirm_tv);
        if (confirmTextView != null) {
            confirmTextView.setOnClickListener(v -> confirmIfPicked());
        }
        updateConfirmState();
    }

    @Override
    public boolean onPageMenuItemSelected(MenuItem item) {
        if (item.getItemId() == confirmMenuItemId()) {
            confirmIfPicked();
            return true;
        }
        return false;
    }

    private void confirmIfPicked() {
        if (checkedGroupMembers == null || checkedGroupMembers.isEmpty()) {
            return;
        }
        onConfirm();
    }

    protected void updateConfirmState() {
        boolean hasChecked = checkedGroupMembers != null && !checkedGroupMembers.isEmpty();
        if (confirmMenuItem != null) {
            confirmMenuItem.setEnabled(hasChecked);
        }
        if (confirmTextView != null) {
            confirmTextView.setText(hasChecked
                ? getString(confirmLabelWithCountRes(), checkedGroupMembers.size())
                : getString(confirmLabelRes()));
            confirmTextView.setEnabled(hasChecked);
        }
    }

    /**
     * 勾选的成员 id 列表，几乎每个子类的确认逻辑第一步都要它。
     */
    protected ArrayList<String> checkedMemberIds() {
        ArrayList<String> memberIds = new ArrayList<>();
        if (checkedGroupMembers != null) {
            for (UIUserInfo userInfo : checkedGroupMembers) {
                memberIds.add(userInfo.getUserInfo().uid);
            }
        }
        return memberIds;
    }
}
