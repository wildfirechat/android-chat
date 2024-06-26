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
import cn.wildfire.chat.kit.mesh.viewholder.DomainViewHolder;
import cn.wildfirechat.model.DomainInfo;

public class DomainListAdapter extends RecyclerView.Adapter<DomainViewHolder> {
    private Fragment fragment;
    private List<DomainInfo> domainInfos = new ArrayList<>();
    private OnExternalOrganizationClickListener onExternalOrganizationClickListener;

    public DomainListAdapter(Fragment fragment) {
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
    public DomainViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        DomainViewHolder holder;
        View view;
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        view = inflater.inflate(R.layout.organization_item_organization, parent, false);
        holder = new DomainViewHolder(view);
        view.setOnClickListener(v -> {
            if (onExternalOrganizationClickListener != null) {
                int position = holder.getAdapterPosition();
                onExternalOrganizationClickListener.onExternalOrganizationClick(this.domainInfos.get(position));
            }
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull DomainViewHolder holder, int position) {
        holder.onBind(this.domainInfos.get(position));
    }

    @Override
    public int getItemViewType(int position) {
        return R.layout.domain_item;
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
