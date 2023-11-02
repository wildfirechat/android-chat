/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.contact.pick;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfire.chat.kit.organization.model.Organization;

public class PickedUserAdapter extends RecyclerView.Adapter {

    private PickUserViewModel pickUserViewModel;

    List<UIUserInfo> users;
    List<Organization> organizations;
    RequestOptions mOptions;

    public PickedUserAdapter(PickUserViewModel pickUserViewModel) {
        this.pickUserViewModel = pickUserViewModel;
        this.users = new ArrayList<>();
        mOptions = new RequestOptions()
            .placeholder(R.mipmap.avatar_def)
            .transforms(new CenterCrop(), new RoundedCorners(4));
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == R.layout.picked_user) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.picked_user, parent, false);
            return new UserViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.picked_organization, parent, false);
            return new OrganizationViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (position < getOrganizationCount()) {
            ((OrganizationViewHolder) holder).bind(organizations.get(position));
        } else {
            ((UserViewHolder) holder).bind(users.get(position - getOrganizationCount()));
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position < getOrganizationCount()) {
            return R.layout.picked_organization;
        } else {
            return R.layout.picked_user;
        }
    }

    @Override
    public int getItemCount() {
        return getOrganizationCount() + users.size();
    }

    public void setOrganizations(List<Organization> organizations) {
        this.organizations = organizations;
        notifyDataSetChanged();
    }

    public void addUser(UIUserInfo uiUserInfo) {
        users.add(uiUserInfo);
        notifyDataSetChanged();
    }

    public void removeUser(UIUserInfo uiUserInfo) {
        users.remove(uiUserInfo);
        notifyDataSetChanged();
    }

    private int getOrganizationCount() {
        return organizations == null ? 0 : organizations.size();
    }

    private class UserViewHolder extends RecyclerView.ViewHolder {

        private ImageView imageView;
        private UIUserInfo uiUserInfo;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.avatar);
            itemView.setOnClickListener(v -> pickUserViewModel.checkUser(uiUserInfo, false));
        }

        void bind(UIUserInfo uiUserInfo) {
            this.uiUserInfo = uiUserInfo;
            Glide.with(imageView)
                .load(uiUserInfo.getUserInfo().portrait)
                .apply(mOptions)
                .into(imageView);
        }
    }

    private class OrganizationViewHolder extends RecyclerView.ViewHolder {

        private ImageView imageView;
        private Organization organization;

        OrganizationViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.avatar);
            itemView.setOnClickListener(v -> {
                organizations.remove(getAdapterPosition());
                notifyDataSetChanged();
            });
        }

        void bind(Organization organization) {
            this.organization = organization;
            Glide.with(imageView)
                .load(organization.portraitUrl)
                .error(R.mipmap.ic_deparment)
                .apply(mOptions)
                .into(imageView);
        }
    }
}
