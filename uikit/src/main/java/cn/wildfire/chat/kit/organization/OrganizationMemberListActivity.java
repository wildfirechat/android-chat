/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.organization;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.widget.SearchView;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;

public class OrganizationMemberListActivity extends WfcBaseActivity {
    private int organizationId;
    private boolean pick;
    private OrganizationMemberListFragment fragment;

    @Override
    protected void afterViews() {
        this.organizationId = getIntent().getIntExtra("organizationId", 0);
        this.pick = getIntent().getBooleanExtra("pick", false);
        fragment = new OrganizationMemberListFragment();
        Bundle args = new Bundle();
        args.putInt("organizationId", organizationId);
        args.putBoolean("pick", pick);
        fragment.setArguments(args);
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.containerFrameLayout, fragment)
            .commit();
    }

    @Override
    protected int contentLayout() {
        return R.layout.fragment_container_activity;
    }

    @Override
    protected int menu() {
        return R.menu.organization_member_list;
    }

    @Override
    protected void afterMenus(Menu menu) {
        MenuItem searchItem = menu.findItem(R.id.search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setQueryHint(getString(R.string.search_user_in_organization));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (fragment != null) {
                    if (newText.trim().isEmpty()) {
                        fragment.clearSearch();
                    } else {
                        fragment.searchEmployees(newText);
                    }
                }
                return true;
            }
        });
        searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                if (fragment != null) {
                    fragment.clearSearch();
                }
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                if (fragment != null) {
                    fragment.cancelSearch();
                }
                return true;
            }
        });
    }
}
