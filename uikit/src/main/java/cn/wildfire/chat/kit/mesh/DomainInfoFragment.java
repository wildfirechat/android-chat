/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.mesh;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.contact.newfriend.SearchUserActivity;
import cn.wildfire.chat.kit.page.WfcPageCompat;
import cn.wildfire.chat.kit.widget.OptionItemView;
import cn.wildfirechat.model.DomainInfo;

/**
 * 互联域详情页：显示对端域的联系方式，并提供「在该域里搜人」的入口。
 * <p>
 * 手机端装在 {@link DomainInfoActivity} 这个空壳里，平板上同一份实现进右栏。
 */
public class DomainInfoFragment extends Fragment {

    private DomainInfo domainInfo;

    /**
     * 没有 domainInfo 就没有可显示的域，返回 null 让调用方放弃。
     */
    @Nullable
    public static DomainInfoFragment fromIntent(@Nullable Intent intent) {
        DomainInfo domainInfo = intent == null ? null : intent.getParcelableExtra("domainInfo");
        if (domainInfo == null) {
            return null;
        }
        DomainInfoFragment fragment = new DomainInfoFragment();
        Bundle args = new Bundle();
        args.putParcelable("domainInfo", domainInfo);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.domain_info_activity, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.findViewById(R.id.searchTextView).setOnClickListener(v -> searchUser());

        domainInfo = getArguments() == null ? null : getArguments().getParcelable("domainInfo");
        if (domainInfo == null) {
            return;
        }
        ((OptionItemView) view.findViewById(R.id.nameOptionItemView)).setDesc(domainInfo.name);
        ((OptionItemView) view.findViewById(R.id.emailOptionItemView)).setDesc(domainInfo.email);
        ((OptionItemView) view.findViewById(R.id.telOptionItemView)).setDesc(domainInfo.tel);
        ((OptionItemView) view.findViewById(R.id.addrOptionItemView)).setDesc(domainInfo.address);
        ((OptionItemView) view.findViewById(R.id.descOptionItemView)).setDesc(domainInfo.desc);
    }

    private void searchUser() {
        Intent intent = new Intent(getActivity(), SearchUserActivity.class);
        intent.putExtra("domainInfo", domainInfo);
        WfcPageCompat.startPage(this, intent);
    }
}
