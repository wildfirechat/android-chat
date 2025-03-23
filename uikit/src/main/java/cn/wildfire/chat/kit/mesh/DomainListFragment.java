/*
 * Copyright (c) 2023 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.mesh;

import android.content.Intent;
import android.view.View;

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.contact.ContactViewModel;
import cn.wildfire.chat.kit.widget.ProgressFragment;
import cn.wildfirechat.model.DomainInfo;

public class DomainListFragment extends ProgressFragment implements DomainListAdapter.OnExternalOrganizationClickListener {
    private RecyclerView recyclerView;
    private DomainListAdapter adapter;

    private ContactViewModel contactViewModel;

    @Override
    protected int contentLayout() {
        return R.layout.domain_list_fragment;
    }

    @Override
    protected void afterViews(View view) {
        super.afterViews(view);
        recyclerView = view.findViewById(R.id.recyclerView);
        adapter = new DomainListAdapter(this);
        adapter.setOnExternalOrganizationClickListener(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        contactViewModel = WfcUIKit.getAppScopeViewModel(ContactViewModel.class);
        loadRemoteDomains();
    }


    @Override
    public void onExternalOrganizationClick(DomainInfo domainInfo) {
        Intent intent = new Intent(getContext(), DomainInfoActivity.class);
        intent.putExtra("domainInfo", domainInfo);
        startActivity(intent);
    }

    private void loadRemoteDomains() {

        this.contactViewModel.loadRemoteDomains()
            .observe(this, new Observer<List<DomainInfo>>() {
                @Override
                public void onChanged(List<DomainInfo> domainInfos) {
                    if (domainInfos != null && !domainInfos.isEmpty()) {

                        adapter.setDomainInfos(domainInfos);
                        showContent();
                    } else {
                        showEmpty(getString(R.string.no_external_organization));
                    }
                }
            });
    }
}
