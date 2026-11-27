/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.contact;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.contact.model.FooterValue;
import cn.wildfire.chat.kit.contact.model.HeaderValue;
import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfire.chat.kit.contact.viewholder.footer.FooterViewHolder;
import cn.wildfire.chat.kit.contact.viewholder.header.HeaderViewHolder;
import cn.wildfire.chat.kit.contact.viewholder.header.RobotViewHolder;
import cn.wildfire.chat.kit.organization.OrganizationRobotManager;
import cn.wildfire.chat.kit.widget.ProgressFragment;
import cn.wildfire.chat.kit.widget.QuickIndexBar;
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper;

/**
 * @author dhl
 * 应该使用懒加载
 */
public abstract class BaseUserListFragment extends ProgressFragment implements QuickIndexBar.OnLetterUpdateListener, UserListAdapter.OnUserClickListener, UserListAdapter.OnHeaderClickListener, UserListAdapter.OnFooterClickListener {

    protected RecyclerView usersRecyclerView;
    QuickIndexBar quickIndexBar;
    TextView indexLetterTextView;

    protected UserListAdapter userListAdapter;

    /** 「机器人」入口是联系人/选人页最外层的一行，与组织树节点无关，只加一次。 */
    private boolean organizationRobotHeaderAdded;

    private LinearLayoutManager linearLayoutManager;
    private boolean showQuickIndexBar;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            showQuickIndexBar = savedInstanceState.getBoolean("showQuickIndexBar", false);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("showQuickIndexBar", showQuickIndexBar);
    }

    @Override
    protected int contentLayout() {
        return getContentLayoutResId();
    }

    @Override
    protected void afterViews(View view) {
        bindViews(view);
        initView();
    }

    private void bindViews(View view) {
        usersRecyclerView = view.findViewById(R.id.usersRecyclerView);
        quickIndexBar = view.findViewById(R.id.quickIndexBar);
        indexLetterTextView = view.findViewById(R.id.indexLetterTextView);
    }

    private void initView() {

        userListAdapter = onCreateUserListAdapter();
        userListAdapter.setOnUserClickListener(this);
        userListAdapter.setOnHeaderClickListener(this);
        userListAdapter.setOnFooterClickListener(this);

        initHeaderViewHolders();
        initFooterViewHolders();

        usersRecyclerView.setAdapter(userListAdapter);
        linearLayoutManager = new LinearLayoutManager(getActivity());
        usersRecyclerView.setLayoutManager(linearLayoutManager);
        OverScrollDecoratorHelper.setUpOverScroll(usersRecyclerView, OverScrollDecoratorHelper.ORIENTATION_VERTICAL);

        if (showQuickIndexBar) {
            quickIndexBar.setVisibility(View.VISIBLE);
            quickIndexBar.setOnLetterUpdateListener(this);
        } else {
            quickIndexBar.setVisibility(View.GONE);
        }
    }

    public void initHeaderViewHolders() {
        // no header
    }

    public void initFooterViewHolders() {
        // no footer
    }

    /**
     * 一定要包含一个id为contactRecyclerView的RecyclerView
     *
     * @return
     */
    protected int getContentLayoutResId() {
        return R.layout.contact_contacts_fragment;
    }

    /**
     * the data for this userListAdapter should be set here
     *
     * @return
     */
    protected UserListAdapter onCreateUserListAdapter() {
        userListAdapter = new UserListAdapter(this);
        return userListAdapter;
    }

    protected void addHeaderViewHolder(Class<? extends HeaderViewHolder> clazz, int layoutResId, HeaderValue value) {
        userListAdapter.addHeaderViewHolder(clazz, layoutResId, value);
        // to do notify header changed
    }

    /**
     * 在组织架构入口之后维护「机器人」入口：有机器人数据才显示。
     * <p>
     * 机器人虽然来自组织通讯录服务，但只是与组织入口并列的一行，不放进组织树内部；
     * 因此调用方要等组织入口这一批 header 已经加完（{@code organizationEntriesLoaded}）
     * 再调用，保证它排在组织入口后面。数据被清空时同步移除这一行。
     *
     * @return 本次是否真的加上了
     */
    protected boolean addOrganizationRobotHeaderIfNeeded(boolean organizationEntriesLoaded) {
        if (!organizationEntriesLoaded) {
            return false;
        }
        if (OrganizationRobotManager.getInstance().isEmpty()) {
            if (organizationRobotHeaderAdded) {
                userListAdapter.removeHeaderViewHolder(RobotViewHolder.class);
                organizationRobotHeaderAdded = false;
            }
            return false;
        }
        if (organizationRobotHeaderAdded) {
            return false;
        }
        addHeaderViewHolder(RobotViewHolder.class, R.layout.contact_header_robot, new HeaderValue());
        organizationRobotHeaderAdded = true;
        return true;
    }

    protected void addFooterViewHolder(Class<? extends FooterViewHolder> clazz, int layoutResId, FooterValue value) {
        userListAdapter.addFooterViewHolder(clazz, layoutResId, value);
    }

    @Override
    public void onLetterUpdate(String letter) {
        indexLetterTextView.setVisibility(View.VISIBLE);
        indexLetterTextView.setText(letter);

        List<UIUserInfo> data = userListAdapter.getUsers();
        if(data == null || data.isEmpty()){
            return;
        }

        if ("↑".equalsIgnoreCase(letter)) {
            linearLayoutManager.scrollToPositionWithOffset(0, 0);
        } else if ("☆".equalsIgnoreCase(letter)) {
            linearLayoutManager.scrollToPositionWithOffset(userListAdapter.headerCount(), 0);
        } else if ("#".equalsIgnoreCase(letter)) {
            for (int i = 0; i < data.size(); i++) {
                UIUserInfo friend = data.get(i);
                if (friend.getCategory().equals("#")) {
                    linearLayoutManager.scrollToPositionWithOffset(userListAdapter.headerCount() + i, 0);
                    break;
                }
            }
        } else {
            for (int i = 0; i < data.size(); i++) {
                UIUserInfo friend = data.get(i);
                if (friend.getCategory().compareTo(letter) >= 0) {
                    linearLayoutManager.scrollToPositionWithOffset(userListAdapter.headerCount() + userListAdapter.favUserCount() + userListAdapter.aiRobotUserCount() + i, 0);
                    break;
                }
            }
        }
    }

    @Override
    public void onLetterCancel() {
        indexLetterTextView.setVisibility(View.GONE);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (indexLetterTextView != null) {
            indexLetterTextView.setVisibility(View.GONE);
        }
    }

    /**
     * 是否显示快速导航条
     *
     * @param show
     */
    public void showQuickIndexBar(boolean show) {
        this.showQuickIndexBar = show;
        if (quickIndexBar != null) {
            quickIndexBar.setVisibility(show ? View.VISIBLE : View.GONE);
            quickIndexBar.setOnLetterUpdateListener(this);
            quickIndexBar.invalidate();
        }
        if (!show && indexLetterTextView != null) {
            indexLetterTextView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onUserClick(UIUserInfo userInfo) {

    }

    @Override
    public void onHeaderClick(HeaderViewHolder holder) {

    }

    @Override
    public void onFooterClick(int index) {

    }

    public void scrollToPosition(int position){
        linearLayoutManager.scrollToPosition(position);
    }
}
