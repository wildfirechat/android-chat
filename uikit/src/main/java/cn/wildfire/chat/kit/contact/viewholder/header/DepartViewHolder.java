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

public class DepartViewHolder extends HeaderViewHolder<OrganizationValue> {
    private Organization organization;
    private TextView nameTextView;

    public DepartViewHolder(Fragment fragment, UserListAdapter adapter, View itemView) {
        super(fragment, adapter, itemView);
        this.nameTextView = itemView.findViewById(R.id.nameTextView);
    }

    @Override
    public void onBind(OrganizationValue value) {
        Organization organization = (Organization) value.getValue();
        this.nameTextView.setText(organization.name);
        this.organization = organization;
    }

    public Organization getOrganization() {
        return organization;
    }
}
