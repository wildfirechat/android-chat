/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.contact.pick;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.contact.UserListAdapter;
import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfire.chat.kit.organization.model.Employee;

public class SearchAndPickUserFragment extends Fragment implements UserListAdapter.OnUserClickListener {
    private CheckableUserListAdapter contactAdapter;
    private PickUserViewModel pickUserViewModel;
    private PickUserFragment pickUserFragment;

    RecyclerView contactRecyclerView;
    TextView tipTextView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.contact_search_fragment, container, false);
        bindViews(view);
        bindEvents(view);
        init();
        return view;
    }

    private void bindEvents(View view) {
        view.findViewById(R.id.tipTextView).setOnClickListener(_v -> onTipTextViewClick());
    }

    private void bindViews(View view) {
        contactRecyclerView = view.findViewById(R.id.usersRecyclerView);
        tipTextView = view.findViewById(R.id.tipTextView);
    }

    public void setPickUserFragment(PickUserFragment pickUserFragment) {
        this.pickUserFragment = pickUserFragment;
    }


    void onTipTextViewClick() {
        pickUserFragment.hideSearchContactFragment();
    }

    private void init() {
        pickUserViewModel = new ViewModelProvider(getActivity()).get(PickUserViewModel.class);
        contactAdapter = new CheckableUserListAdapter(this);
        contactAdapter.setOnUserClickListener(this);
        contactRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        contactRecyclerView.setAdapter(contactAdapter);
    }

    public void search(String keyword) {
        if (TextUtils.isEmpty(keyword)) {
            return;
        }
        Toast.makeText(getContext(), getString(R.string.search_user), Toast.LENGTH_SHORT).show();
        pickUserViewModel.searchUser(keyword).observe(this, resulPair -> {
            List<UIUserInfo> result = new ArrayList<>();
            List<UIUserInfo> users = resulPair.first;
            if (users != null) {
                for (int i = 0; i < users.size(); i++) {
                    UIUserInfo ui = new UIUserInfo(users.get(i).getUserInfo());
                    if (i == 0) {
                        ui.setCategory(getString(R.string.contact_category));
                        ui.setShowCategory(true);
                    } else {
                        ui.setShowCategory(false);
                    }
                    result.add(ui);
                }
            }
            if (resulPair.second != null) {
                List<Employee> employees = resulPair.second;
                for (int i = 0; i < employees.size(); i++) {
                    UIUserInfo ui = new UIUserInfo(employees.get(i).toUserInfo());
                    if (i == 0) {
                        ui.setShowCategory(true);
                        ui.setCategory(getString(R.string.organization_directory));

                    } else {
                        ui.setShowCategory(false);
                    }
                    result.add(ui);
                }
            }
            if (result.isEmpty()) {
                contactRecyclerView.setVisibility(View.GONE);
                tipTextView.setVisibility(View.VISIBLE);
            } else {
                contactRecyclerView.setVisibility(View.VISIBLE);
                tipTextView.setVisibility(View.GONE);
            }
            contactAdapter.setUsers(result);
            contactAdapter.notifyDataSetChanged();
        });
    }

    public void rest() {
        tipTextView.setVisibility(View.VISIBLE);
        contactRecyclerView.setVisibility(View.GONE);
        contactAdapter.setUsers(null);
    }

    @Override
    public void onUserClick(UIUserInfo userInfo) {
        if (userInfo.isCheckable()) {
            pickUserViewModel.checkUser(userInfo, !userInfo.isChecked());
        }
    }
}
