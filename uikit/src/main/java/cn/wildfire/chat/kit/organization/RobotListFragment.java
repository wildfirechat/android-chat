/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.organization;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.contact.UserListAdapter;
import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfire.chat.kit.contact.pick.CheckableUserListAdapter;
import cn.wildfire.chat.kit.organization.model.Robot;
import cn.wildfire.chat.kit.page.WfcPage;
import cn.wildfire.chat.kit.page.WfcPageCompat;
import cn.wildfire.chat.kit.user.UserInfoActivity;
import cn.wildfire.chat.kit.widget.ProgressFragment;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

/**
 * 组织架构机器人列表页：名称/头像，顺序完全按服务端下发的顺序。
 * <p>
 * 三种形态：
 * <ul>
 *   <li>从通讯录进入（非选人）：点击进入用户资料；</li>
 *   <li>单选（发名片等）：点击即回传机器人 userId；</li>
 *   <li>多选（发起聊天、转发、拉群、加群成员）：带初始勾选，取消后点「完成」同步回调用方。</li>
 * </ul>
 * 机器人数据只来自 {@link OrganizationRobotManager}，与组织树无关。
 */
public class RobotListFragment extends ProgressFragment implements WfcPage, UserListAdapter.OnUserClickListener {

    private RecyclerView recyclerView;
    private UserListAdapter adapter;

    private boolean pick;
    private boolean multiSelect;
    private int maxCount = Integer.MAX_VALUE;

    private final Set<String> selectedIds = new LinkedHashSet<>();
    private final Set<String> uncheckableIds = new HashSet<>();
    private boolean hasInitialSelection;

    private TextView confirmTextView;

    public static RobotListFragment fromIntent(@Nullable Intent intent) {
        RobotListFragment fragment = new RobotListFragment();
        Bundle args = new Bundle();
        if (intent != null) {
            args.putBoolean(RobotListActivity.PARAM_PICK, intent.getBooleanExtra(RobotListActivity.PARAM_PICK, false));
            args.putBoolean(RobotListActivity.PARAM_MULTI_SELECT, intent.getBooleanExtra(RobotListActivity.PARAM_MULTI_SELECT, false));
            args.putInt(RobotListActivity.PARAM_MAX_COUNT, intent.getIntExtra(RobotListActivity.PARAM_MAX_COUNT, Integer.MAX_VALUE));
            args.putStringArrayList(RobotListActivity.PARAM_INITIAL_CHECKED_IDS,
                intent.getStringArrayListExtra(RobotListActivity.PARAM_INITIAL_CHECKED_IDS));
            args.putStringArrayList(RobotListActivity.PARAM_UNCHECKABLE_IDS,
                intent.getStringArrayListExtra(RobotListActivity.PARAM_UNCHECKABLE_IDS));
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected int contentLayout() {
        return R.layout.robot_list_fragment;
    }

    @Override
    protected void afterViews(View view) {
        super.afterViews(view);
        Bundle args = getArguments();
        if (args != null) {
            pick = args.getBoolean(RobotListActivity.PARAM_PICK, false);
            multiSelect = args.getBoolean(RobotListActivity.PARAM_MULTI_SELECT, false);
            maxCount = args.getInt(RobotListActivity.PARAM_MAX_COUNT, Integer.MAX_VALUE);
            if (maxCount < 0) {
                maxCount = Integer.MAX_VALUE;
            }
            ArrayList<String> uncheckable = args.getStringArrayList(RobotListActivity.PARAM_UNCHECKABLE_IDS);
            if (uncheckable != null) {
                uncheckableIds.addAll(uncheckable);
            }
            ArrayList<String> initialChecked = args.getStringArrayList(RobotListActivity.PARAM_INITIAL_CHECKED_IDS);
            if (initialChecked != null) {
                for (String robotId : initialChecked) {
                    if (!uncheckableIds.contains(robotId)) {
                        selectedIds.add(robotId);
                    }
                }
                hasInitialSelection = !initialChecked.isEmpty();
            }
        }

        recyclerView = view.findViewById(R.id.recyclerView);
        adapter = pick && multiSelect ? new CheckableUserListAdapter(this) : new UserListAdapter(this);
        adapter.setOnUserClickListener(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        LiveData<List<Robot>> liveData = OrganizationRobotManager.getInstance().robotsLiveData();
        liveData.observe(this, this::refresh);
    }

    private void refresh(List<Robot> robots) {
        List<UIUserInfo> users = new ArrayList<>();
        if (robots != null) {
            for (Robot robot : robots) {
                if (robot == null || robot.robotId == null) {
                    continue;
                }
                UIUserInfo userInfo = new UIUserInfo(robot.toUserInfo());
                userInfo.setExtra(robot);
                boolean checkable = !uncheckableIds.contains(robot.robotId);
                userInfo.setCheckable(checkable);
                userInfo.setChecked(checkable && selectedIds.contains(robot.robotId));
                users.add(userInfo);
            }
        }
        adapter.setUsers(users);
        if (users.isEmpty()) {
            showEmpty(getString(R.string.org_robot_empty));
        } else {
            showContent();
        }
        updateConfirmState();
    }

    @Override
    public void onUserClick(UIUserInfo userInfo) {
        Robot robot = userInfo.getExtra() instanceof Robot ? (Robot) userInfo.getExtra() : null;
        if (robot == null) {
            return;
        }
        if (!pick) {
            // 机器人可能还不是本地已知用户，直接开资料页会拿到 null；先确认 IM 侧能查到
            UserInfo imUserInfo = ChatManager.Instance().getUserInfo(robot.robotId, false);
            if (imUserInfo == null) {
                Toast.makeText(getActivity(), R.string.user_no_found, Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(getActivity(), UserInfoActivity.class);
            intent.putExtra("userInfo", imUserInfo);
            WfcPageCompat.startPage(this, intent);
            return;
        }
        if (!multiSelect) {
            if (!userInfo.isCheckable()) {
                return;
            }
            Intent data = new Intent();
            data.putExtra(RobotListActivity.RESULT_USER_INFO, robot.toUserInfo());
            finishWithResult(data);
            return;
        }

        // 多选：已选状态与调用方同步，全部取消后仍可点「完成」清空机器人选择
        if (!userInfo.isCheckable()) {
            return;
        }
        if (selectedIds.contains(robot.robotId)) {
            selectedIds.remove(robot.robotId);
            userInfo.setChecked(false);
        } else {
            if (selectedIds.size() >= maxCount) {
                Toast.makeText(getActivity(), R.string.pick_user_limit_exceeded, Toast.LENGTH_SHORT).show();
                return;
            }
            selectedIds.add(robot.robotId);
            userInfo.setChecked(true);
        }
        int index = adapter.getUsers() == null ? -1 : adapter.getUsers().indexOf(userInfo);
        if (index >= 0) {
            adapter.notifyItemChanged(index);
        }
        updateConfirmState();
    }

    // ==================== WfcPage ====================

    @Override
    public int pageMenu() {
        return pick && multiSelect ? R.menu.contact_pick : 0;
    }

    @Override
    public void onPreparePageMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.confirm);
        if (item == null) {
            return;
        }
        View actionView = item.getActionView();
        confirmTextView = actionView == null ? null : actionView.findViewById(R.id.confirm_tv);
        if (confirmTextView != null) {
            confirmTextView.setOnClickListener(v -> onConfirmClick());
        }
        updateConfirmState();
    }

    @Override
    public boolean onPageMenuItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.confirm) {
            onConfirmClick();
            return true;
        }
        return false;
    }

    private void updateConfirmState() {
        if (confirmTextView == null) {
            return;
        }
        int count = selectedIds.size();
        boolean enabled = count > 0 || hasInitialSelection;
        if (count > 0) {
            confirmTextView.setText(getString(R.string.complete_with_count, count));
        } else {
            confirmTextView.setText(R.string.complete);
        }
        confirmTextView.setEnabled(enabled);
    }

    private void onConfirmClick() {
        ArrayList<Robot> checkedRobots = new ArrayList<>();
        ArrayList<String> checkedIds = new ArrayList<>();
        for (Robot robot : OrganizationRobotManager.getInstance().getRobots()) {
            if (robot.robotId != null && selectedIds.contains(robot.robotId)
                && !uncheckableIds.contains(robot.robotId)) {
                checkedIds.add(robot.robotId);
                checkedRobots.add(robot);
            }
        }
        Intent data = new Intent();
        data.putStringArrayListExtra(RobotListActivity.RESULT_ROBOT_IDS, checkedIds);
        data.putParcelableArrayListExtra(RobotListActivity.RESULT_ROBOTS, checkedRobots);
        finishWithResult(data);
    }

    private void finishWithResult(Intent data) {
        WfcPageCompat.setPageResult(this, Activity.RESULT_OK, data);
        WfcPageCompat.finishPage(this);
    }
}
