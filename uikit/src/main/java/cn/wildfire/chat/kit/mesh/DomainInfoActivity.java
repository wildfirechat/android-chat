/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.mesh;

import android.content.Intent;

import androidx.lifecycle.ViewModelProvider;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.contact.ContactViewModel;
import cn.wildfire.chat.kit.contact.newfriend.SearchUserActivity;
import cn.wildfire.chat.kit.widget.OptionItemView;
import cn.wildfirechat.model.DomainInfo;

public class DomainInfoActivity extends WfcBaseActivity {
    private OptionItemView nameOptionItemView;
    private OptionItemView emailOptionItemView;
    private OptionItemView telOptionItemView;
    private OptionItemView addrOptionItemView;
    private OptionItemView descOptionItemView;

    private ContactViewModel contactViewModel;

    private DomainInfo domainInfo;

    protected void bindEvents() {
        super.bindEvents();
        findViewById(R.id.searchTextView).setOnClickListener(v -> searchUser());
    }

    protected void bindViews() {
        super.bindViews();
        nameOptionItemView = findViewById(R.id.nameOptionItemView);
        emailOptionItemView = findViewById(R.id.emailOptionItemView);
        telOptionItemView = findViewById(R.id.telOptionItemView);
        addrOptionItemView = findViewById(R.id.addrOptionItemView);
        descOptionItemView = findViewById(R.id.descOptionItemView);
    }

    @Override
    protected int contentLayout() {
        return R.layout.domain_info_activity;
    }

    @Override
    protected void afterViews() {
        super.afterViews();
        contactViewModel = WfcUIKit.getAppScopeViewModel(ContactViewModel.class);
        this.domainInfo = getIntent().getParcelableExtra("domainInfo");
        if (this.domainInfo != null) {


            nameOptionItemView.setDesc(domainInfo.name);
            emailOptionItemView.setDesc(domainInfo.email);
            telOptionItemView.setDesc(domainInfo.tel);
            addrOptionItemView.setDesc(domainInfo.address);
            descOptionItemView.setDesc(domainInfo.desc);
        }
    }

    void searchUser() {
        Intent intent = new Intent(this, SearchUserActivity.class);
        intent.putExtra("domainInfo", this.domainInfo);
        startActivity(intent);
    }
}
