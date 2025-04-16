/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.contact;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

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
import cn.wildfire.chat.kit.widget.ProgressFragment;
import cn.wildfire.chat.kit.widget.QuickIndexBar;
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper;

/**
 * @author dhl
 * 应该使用懒加载
 */
public abstract class BaseUserListFragment extends ProgressFragment implements QuickIndexBar.OnLetterUpdateListener, UserListAdapter.OnUserClickListener, UserListAdapter.OnHeaderClickListener, UserListAdapter.OnFooterClickListener {

    RecyclerView usersRecyclerView;
    QuickIndexBar quickIndexBar;
    TextView indexLetterTextView;

    protected UserListAdapter userListAdapter;

    private LinearLayoutManager linearLayoutManager;
    private boolean showQuickIndexBar;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
                    linearLayoutManager.scrollToPositionWithOffset(i + userListAdapter.headerCount(), 0);
                    break;
                }
            }
        }
    }

    @Override
    public void onLetterCancel() {
        indexLetterTextView.setVisibility(View.GONE);
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
}
