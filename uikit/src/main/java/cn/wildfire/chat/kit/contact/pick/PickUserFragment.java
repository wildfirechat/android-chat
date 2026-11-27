/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.contact.pick;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.page.WfcPageCompat;
import cn.wildfire.chat.kit.contact.BaseUserListFragment;
import cn.wildfire.chat.kit.contact.UserListAdapter;
import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfire.chat.kit.organization.OrganizationRobotManager;
import cn.wildfire.chat.kit.organization.RobotListActivity;
import cn.wildfire.chat.kit.organization.model.Employee;
import cn.wildfire.chat.kit.organization.model.Organization;
import cn.wildfire.chat.kit.organization.model.Robot;
import cn.wildfire.chat.kit.third.utils.UIUtils;
import cn.wildfire.chat.kit.widget.RecyclerViewScrollToHideKeyboardListener;
import cn.wildfire.chat.kit.widget.QuickIndexBar;
import cn.wildfire.chat.kit.widget.SimpleTextWatcher;

public abstract class PickUserFragment extends BaseUserListFragment implements QuickIndexBar.OnLetterUpdateListener {
    /** 机器人列表的 requestCode，各子类共用，子类在 onActivityResult 里转给 {@link #applyRobotSelection} */
    protected static final int REQUEST_CODE_PICK_ROBOT = 300;

    private SearchAndPickUserFragment searchAndPickUserFragment;
    protected PickUserViewModel pickUserViewModel;

    protected RecyclerView pickedUserRecyclerView;
    EditText searchEditText;
    FrameLayout searchUserFrameLayout;
    protected View hintView;

    private boolean isSearchFragmentShowing = false;
    protected PickedUserAdapter pickedUserAdapter;

    private final Observer<Object> contactCheckStatusUpdateLiveDataObserver = obj -> {
        ((CheckableUserListAdapter) userListAdapter).updateUserStatus(pickUserViewModel.getCheckedUsers());
        hideSearchContactFragment();
        updatePickedUserView();
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pickUserViewModel = new ViewModelProvider(WfcPageCompat.pageScope(this)).get(PickUserViewModel.class);
        pickUserViewModel.userCheckStatusUpdateLiveData().observeForever(contactCheckStatusUpdateLiveDataObserver);
    }

    private void bindViews(View view) {
        pickedUserRecyclerView = view.findViewById(R.id.pickedUserRecyclerView);
        searchEditText = view.findViewById(R.id.searchEditText);
        searchUserFrameLayout = view.findViewById(R.id.searchFrameLayout);
        hintView = view.findViewById(R.id.hint_view);

        searchEditText.setOnFocusChangeListener(this::onSearchEditTextFocusChange);
        searchEditText.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                search(s);
            }
        });
    }

    @Override
    protected void afterViews(View view) {
        super.afterViews(view);
        bindViews(view);
        initView();
        setupPickFromUsers();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        pickUserViewModel.userCheckStatusUpdateLiveData().removeObserver(contactCheckStatusUpdateLiveDataObserver);
    }

    private void initView() {
        configPickedUserRecyclerView();
        pickedUserAdapter = getPickedUserAdapter();
        pickedUserRecyclerView.setAdapter(pickedUserAdapter);
        usersRecyclerView.addOnScrollListener(new RecyclerViewScrollToHideKeyboardListener());
    }

    protected void configPickedUserRecyclerView() {
        RecyclerView.LayoutManager pickedContactRecyclerViewLayoutManager = new LinearLayoutManager(getActivity(), GridLayoutManager.HORIZONTAL, false);
        pickedUserRecyclerView.setLayoutManager(pickedContactRecyclerViewLayoutManager);
        pickedUserRecyclerView.addItemDecoration(new Decoration());
    }

    protected PickedUserAdapter getPickedUserAdapter() {
        return new PickedUserAdapter(pickUserViewModel);
    }

    void onSearchEditTextFocusChange(View view, boolean focus) {
        if (getActivity() == null || getActivity().isFinishing()) {
            return;
        }
        if (focus) {
            showSearchContactFragment();
        } else {
            hideSearchContactFragment();
        }
        handleHintView(focus);
    }

    protected void handleHintView(boolean focus) {
        if (pickedUserAdapter.getItemCount() == 0 && !focus) {
            hintView.setVisibility(View.VISIBLE);
        } else {
            hintView.setVisibility(View.GONE);
        }
    }

    void search(Editable editable) {
        // restore view state
        if (searchAndPickUserFragment == null) {
            return;
        }
        String key = editable.toString();
        if (!TextUtils.isEmpty(key)) {
            searchAndPickUserFragment.search(key);
        } else {
            searchAndPickUserFragment.rest();
        }
    }

    @Override
    public int getContentLayoutResId() {
        return R.layout.contact_pick_fragment;
    }

    @Override
    public UserListAdapter onCreateUserListAdapter() {
        return new CheckableUserListAdapter(this);
    }

    abstract protected void setupPickFromUsers();

    private void showSearchContactFragment() {
        if (searchAndPickUserFragment == null) {
            searchAndPickUserFragment = new SearchAndPickUserFragment();
            searchAndPickUserFragment.setPickUserFragment(this);
        }
        searchUserFrameLayout.setVisibility(View.VISIBLE);
        getChildFragmentManager().beginTransaction()
                .replace(R.id.searchFrameLayout, searchAndPickUserFragment)
                .commit();
        isSearchFragmentShowing = true;
    }

    public void hideSearchContactFragment() {
        if (!isSearchFragmentShowing) {
            return;
        }

        searchEditText.setText("");
        searchEditText.clearFocus();
        searchUserFrameLayout.setVisibility(View.GONE);
        getChildFragmentManager().beginTransaction().remove(searchAndPickUserFragment).commit();
        isSearchFragmentShowing = false;
    }

    @Override
    public void onUserClick(UIUserInfo userInfo) {
        if (userInfo.isCheckable()) {
            if (!pickUserViewModel.checkUser(userInfo, !userInfo.isChecked())) {
                Toast.makeText(getActivity(), R.string.pick_user_limit_exceeded, Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ==================== 组织架构机器人 ====================

    /**
     * 打开机器人列表做多选。只在选人页最外层调用（机器人入口与组织入口并列，
     * 进入组织内部后没有机器人）。初始勾选来自 {@link PickUserViewModel} 当前勾选里的机器人。
     *
     * @param uncheckableIds 不可勾选的机器人（例如已经是群成员）
     */
    protected void openRobotListForPick(@Nullable List<String> uncheckableIds) {
        if (getActivity() == null) {
            return;
        }
        Set<String> robotIdSet = OrganizationRobotManager.getInstance().getRobotIdSet();

        ArrayList<String> initialCheckedIds = new ArrayList<>();
        for (UIUserInfo userInfo : pickUserViewModel.getCheckedUsers()) {
            String userId = userInfo.getUserInfo().uid;
            if (robotIdSet.contains(userId) && !initialCheckedIds.contains(userId)) {
                initialCheckedIds.add(userId);
            }
        }

        Intent intent = new Intent(getActivity(), RobotListActivity.class);
        intent.putExtra(RobotListActivity.PARAM_PICK, true);
        intent.putExtra(RobotListActivity.PARAM_MULTI_SELECT, true);
        intent.putExtra(RobotListActivity.PARAM_MAX_COUNT, remainingRobotPickCount(robotIdSet));
        intent.putStringArrayListExtra(RobotListActivity.PARAM_INITIAL_CHECKED_IDS, initialCheckedIds);
        if (uncheckableIds != null && !uncheckableIds.isEmpty()) {
            intent.putStringArrayListExtra(RobotListActivity.PARAM_UNCHECKABLE_IDS,
                new ArrayList<>(uncheckableIds));
        }
        WfcPageCompat.startPageForResult(this, intent, REQUEST_CODE_PICK_ROBOT);
    }

    /**
     * 机器人列表只管理机器人，最大可选数要把已选的普通联系人、员工、组织占用的名额扣掉。
     */
    private int remainingRobotPickCount(Set<String> robotIdSet) {
        int max = pickUserViewModel.getMaxPickCount();
        if (max == Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        int used = 0;
        for (UIUserInfo userInfo : pickUserViewModel.getCheckedUsers()) {
            if (!robotIdSet.contains(userInfo.getUserInfo().uid)) {
                used++;
            }
        }
        used += pickUserViewModel.getCheckedEmployees().size();
        for (Organization organization : pickUserViewModel.getCheckedOrganizations()) {
            used += Math.max(organization.memberCount, 0);
        }
        return Math.max(0, max - used);
    }

    /**
     * 机器人列表返回后，用返回的机器人集合替换当前勾选中的机器人部分：
     * 取消的移除，新选的加入，普通联系人/员工/组织不受影响。
     */
    protected void applyRobotSelection(@Nullable Intent data) {
        if (data == null) {
            return;
        }
        ArrayList<String> checkedIds = data.getStringArrayListExtra(RobotListActivity.RESULT_ROBOT_IDS);
        Set<String> newCheckedIds = checkedIds == null ? new HashSet<>() : new HashSet<>(checkedIds);
        Set<String> robotIdSet = OrganizationRobotManager.getInstance().getRobotIdSet();

        List<UIUserInfo> currentChecked = new ArrayList<>(pickUserViewModel.getCheckedUsers());
        for (UIUserInfo userInfo : currentChecked) {
            String userId = userInfo.getUserInfo().uid;
            if (robotIdSet.contains(userId) && !newCheckedIds.contains(userId)) {
                pickUserViewModel.checkUser(userInfo, false);
            }
        }

        ArrayList<Robot> robots = data.getParcelableArrayListExtra(RobotListActivity.RESULT_ROBOTS);
        if (robots != null) {
            for (Robot robot : robots) {
                if (robot == null || robot.robotId == null || !newCheckedIds.contains(robot.robotId)) {
                    continue;
                }
                UIUserInfo userInfo = new UIUserInfo(robot.toUserInfo());
                if (!pickUserViewModel.checkUser(userInfo, true)) {
                    Toast.makeText(getActivity(), R.string.pick_user_limit_exceeded, Toast.LENGTH_SHORT).show();
                    break;
                }
            }
        }
    }

    protected void handleEditText() {
        if (pickedUserAdapter.getItemCount() == 0) {
            searchEditText.setHint(R.string.pick_user_empty_hint);
        } else {
            searchEditText.setHint(R.string.pick_user_search_hint);
        }
    }

    private void updatePickedUserView() {
        pickedUserAdapter.setUsers(pickUserViewModel.getCheckedUsers());
        pickedUserAdapter.setEmployees(pickUserViewModel.getCheckedEmployees());
        pickedUserAdapter.setOrganizations(pickUserViewModel.getCheckedOrganizations());
        handleHintView(false);
        handleEditText();
    }

    private static class Decoration extends RecyclerView.ItemDecoration {
        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);
            if (position < parent.getAdapter().getItemCount() - 1) {
                outRect.right = UIUtils.dip2Px(4);
            } else {
                outRect.right = 0;
            }
        }
    }
}
