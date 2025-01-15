/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.main;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.List;

public class HomeFragmentPagerAdapter extends FragmentStateAdapter {

    private List<Fragment> mFragments;

    public HomeFragmentPagerAdapter(FragmentActivity fm, List<Fragment> fragments) {
        super(fm);
        mFragments = fragments;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return mFragments.get(position);
    }

    @Override
    public int getItemCount() {
        return mFragments != null ? mFragments.size() : 0;
    }
}
