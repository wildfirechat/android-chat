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

import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfire.chat.kit.contact.viewholder.UserViewHolder;
import cn.wildfire.chat.kit.search.SearchableModule;
import cn.wildfire.chat.kit.user.UserInfoActivity;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.SearchUserCallback;

public class UserSearchModule extends SearchableModule<UserInfo, UserViewHolder> {
    @Override
    public UserViewHolder onCreateViewHolder(Fragment fragment, @NonNull ViewGroup parent, int viewType) {
        View itemView;
        itemView = LayoutInflater.from(fragment.getActivity()).inflate(R.layout.contact_item_contact, parent, false);
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
        return null;
    }

    @Override
    public List<UserInfo> search(String keyword) {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        List<UserInfo> userInfos = new ArrayList<>();
        ChatManager.Instance().searchUser(keyword, true, new SearchUserCallback() {
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
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return userInfos;
    }
}
