/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.contact.pick;


import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.contact.ContactViewModel;

public class PickContactFragment extends PickUserFragment {
    @Override
    protected void setupPickFromUsers() {
        ContactViewModel contactViewModel = WfcUIKit.getAppScopeViewModel(ContactViewModel.class);
        contactViewModel.contactListLiveData().observe(this, userInfos -> {
            showContent();
            pickUserViewModel.setUsers(userInfos);
            userListAdapter.setUsers(userInfos);
        });
    }
}
