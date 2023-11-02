/*
 * Copyright (c) 2023 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.contact.viewholder.header;

import android.view.View;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.contact.UserListAdapter;
import cn.wildfire.chat.kit.contact.model.OrganizationValue;
import cn.wildfire.chat.kit.organization.model.Organization;

public class OrganizationViewHolder extends HeaderViewHolder<OrganizationValue> {
    private TextView nameTextView;
    private Organization organization;

    public OrganizationViewHolder(Fragment fragment, UserListAdapter adapter, View itemView) {
        super(fragment, adapter, itemView);
        this.nameTextView = itemView.findViewById(R.id.nameTextView);
    }

    @Override
    public void onBind(OrganizationValue organizationValue) {
        Organization organization = (Organization) organizationValue.getValue();
        this.nameTextView.setText(organization.name);
        this.organization= organization;
    }

    public Organization getOrganization() {
        return organization;
    }
}
