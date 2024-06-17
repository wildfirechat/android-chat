/*
 * Copyright (c) 2023 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.mesh;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.mesh.viewholder.ExternalOrganizationViewHolder;
import cn.wildfirechat.model.DomainInfo;

public class ExternalOrganizationListAdapter extends RecyclerView.Adapter<ExternalOrganizationViewHolder> {
    private Fragment fragment;
    private List<DomainInfo> domainInfos = new ArrayList<>();
    private OnExternalOrganizationClickListener onExternalOrganizationClickListener;

    public ExternalOrganizationListAdapter(Fragment fragment) {
        this.fragment = fragment;
    }

    public void setDomainInfos(List<DomainInfo> domainInfos) {
        this.domainInfos = domainInfos;
    }

    public void setOnExternalOrganizationClickListener(OnExternalOrganizationClickListener onExternalOrganizationClickListener) {
        this.onExternalOrganizationClickListener = onExternalOrganizationClickListener;
    }

    @NonNull
    @Override
    public ExternalOrganizationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ExternalOrganizationViewHolder holder;
        View view;
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        view = inflater.inflate(R.layout.organization_item_organization, parent, false);
        holder = new ExternalOrganizationViewHolder(view);
        view.setOnClickListener(v -> {
            if (onExternalOrganizationClickListener != null) {
                int position = holder.getAdapterPosition();
                onExternalOrganizationClickListener.onExternalOrganizationClick(this.domainInfos.get(position));
            }
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ExternalOrganizationViewHolder holder, int position) {
        holder.onBind(this.domainInfos.get(position));
    }

    @Override
    public int getItemViewType(int position) {
        return R.layout.organization_item_organization;
    }

    @Override
    public int getItemCount() {
        if (this.domainInfos == null) {
            return 0;
        }
        return this.domainInfos.size();
    }

    interface OnExternalOrganizationClickListener {
        void onExternalOrganizationClick(DomainInfo domainInfo);
    }
}
