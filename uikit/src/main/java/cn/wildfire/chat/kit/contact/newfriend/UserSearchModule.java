/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.contact.newfriend;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfire.chat.kit.contact.viewholder.UserViewHolder;
import cn.wildfire.chat.kit.search.SearchableModule;
import cn.wildfire.chat.kit.user.UserInfoActivity;
import cn.wildfirechat.model.DomainInfo;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.SearchUserCallback;

public class UserSearchModule extends SearchableModule<UserInfo, UserViewHolder> {
    private DomainInfo domainInfo;

    public UserSearchModule(DomainInfo domainInfo) {
        this.domainInfo = domainInfo;
    }

    public UserSearchModule() {

    }

    @Override
    public UserViewHolder onCreateViewHolder(Fragment fragment, @NonNull ViewGroup parent, int viewType) {
        View itemView;
        itemView = LayoutInflater.from(fragment.getActivity()).inflate(R.layout.search_item_contact, parent, false);
        return new UserViewHolder(fragment, null, itemView);
    }

    @Override
    public void onBind(Fragment fragment, UserViewHolder holder, UserInfo userInfo) {
        holder.onBind(new UIUserInfo(userInfo));
    }

    @Override
    public void onClick(Fragment fragment, UserViewHolder holder, View view, UserInfo userInfo) {
        Intent intent = new Intent(fragment.getActivity(), UserInfoActivity.class);
        intent.putExtra("userInfo", userInfo);
        fragment.startActivity(intent);
    }

    @Override
    public int getViewType(UserInfo userInfo) {
        return R.layout.contact_item_contact;
    }

    @Override
    public int priority() {
        return 100;
    }

    @Override
    public boolean expandable() {
        return false;
    }

    @Override
    public String category() {
        return this.domainInfo == null ?
            WfcUIKit.getWfcUIKit().getApplication().getString(R.string.search_user_category_local) :
            WfcUIKit.getWfcUIKit().getApplication().getString(R.string.search_user_category_domain, domainInfo.name);
    }

    @Override
    public List<UserInfo> search(String keyword) {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        List<UserInfo> userInfos = new ArrayList<>();
        if (this.domainInfo == null) {
            ChatManager.Instance().searchUser(keyword, ChatManager.SearchUserType.General, 0, new SearchUserCallback() {
                @Override
                public void onSuccess(List<UserInfo> infos) {
                    userInfos.addAll(infos);
                    countDownLatch.countDown();
                }

                @Override
                public void onFail(int errorCode) {
                    countDownLatch.countDown();
                }
            });
        } else {
            ChatManager.Instance().searchUserEx(this.domainInfo.domainId, keyword, ChatManager.SearchUserType.General, 0, new SearchUserCallback() {
                @Override
                public void onSuccess(List<UserInfo> infos) {
                    userInfos.addAll(infos);
                    countDownLatch.countDown();
                }

                @Override
                public void onFail(int errorCode) {
                    countDownLatch.countDown();
                }
            });
        }
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return userInfos;
    }
}
