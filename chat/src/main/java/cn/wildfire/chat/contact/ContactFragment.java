package cn.wildfire.chat.contact;

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;

import java.util.List;

import cn.wildfire.chat.channel.ChannelListActivity;
import cn.wildfire.chat.contact.model.ContactCountFooterValue;
import cn.wildfire.chat.contact.model.FriendRequestValue;
import cn.wildfire.chat.contact.model.GroupValue;
import cn.wildfire.chat.contact.model.HeaderValue;
import cn.wildfire.chat.contact.model.UIUserInfo;
import cn.wildfire.chat.contact.newfriend.FriendRequestListActivity;
import cn.wildfire.chat.contact.viewholder.footer.ContactCountViewHolder;
import cn.wildfire.chat.contact.viewholder.header.ChannelViewHolder;
import cn.wildfire.chat.contact.viewholder.header.FriendRequestViewHolder;
import cn.wildfire.chat.contact.viewholder.header.GroupViewHolder;
import cn.wildfire.chat.group.GroupListActivity;
import cn.wildfire.chat.main.MainActivity;
import cn.wildfire.chat.user.UserInfoActivity;
import cn.wildfire.chat.user.UserViewModel;
import cn.wildfire.chat.widget.QuickIndexBar;
import cn.wildfirechat.model.UserInfo;

public class ContactFragment extends BaseContactFragment implements QuickIndexBar.OnLetterUpdateListener {
    private UserViewModel userViewModel;

    private Observer<Integer> friendRequestUpdateLiveDataObserver = count -> {
        FriendRequestValue requestValue = new FriendRequestValue(count == null ? 0 : count);
        contactAdapter.updateHeader(0, requestValue);
    };

    private Observer<Object> contactListUpdateLiveDataObserver = o -> {
        List<UserInfo> userInfos = contactViewModel.getContacts(false);
        contactAdapter.setContacts(userInfoToUIUserInfo(userInfos));
        contactAdapter.notifyDataSetChanged();

        for (UserInfo info : userInfos) {
            if (info.name == null || info.displayName == null) {
                userViewModel.getUserInfo(info.uid, true);
            }
        }
    };

    private Observer<List<UserInfo>> userInfoLiveDataObserver = userInfos -> {
        contactAdapter.updateContacts(userInfoToUIUserInfo(userInfos));
    };


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        contactViewModel.contactListUpdatedLiveData().observeForever(contactListUpdateLiveDataObserver);
        contactViewModel.friendRequestUpdatedLiveData().observeForever(friendRequestUpdateLiveDataObserver);

        userViewModel = ViewModelProviders.of(this).get(UserViewModel.class);
        userViewModel.userInfoLiveData().observeForever(userInfoLiveDataObserver);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        contactViewModel.contactListUpdatedLiveData().removeObserver(contactListUpdateLiveDataObserver);
        contactViewModel.friendRequestUpdatedLiveData().removeObserver(friendRequestUpdateLiveDataObserver);
        userViewModel.userInfoLiveData().removeObserver(userInfoLiveDataObserver);
    }

    @Override
    public void initHeaderViewHolders() {
        addHeaderViewHolder(FriendRequestViewHolder.class, new FriendRequestValue(contactViewModel.getUnreadFriendRequestCount()));
        addHeaderViewHolder(GroupViewHolder.class, new GroupValue());
        addHeaderViewHolder(ChannelViewHolder.class, new HeaderValue());
    }

    @Override
    public void initFooterViewHolders() {
        addFooterViewHolder(ContactCountViewHolder.class, new ContactCountFooterValue());
    }

    @Override
    public void onContactClick(UIUserInfo userInfo) {
        Intent intent = new Intent(getActivity(), UserInfoActivity.class);
        intent.putExtra("userInfo", userInfo.getUserInfo());
        startActivity(intent);
    }

    @Override
    public void onHeaderClick(int index) {
        switch (index) {
            case 0:
                ((MainActivity) getActivity()).hideUnreadFriendRequestBadgeView();
                showFriendRequest();
                break;
            case 1:
                showGroupList();
                break;
            case 2:
                showChannelList();
                break;
            default:
                break;
        }
    }

    private void showFriendRequest() {
        FriendRequestValue value = new FriendRequestValue(0);
        contactAdapter.updateHeader(0, value);

        contactViewModel.clearUnreadFriendRequestStatus();
        Intent intent = new Intent(getActivity(), FriendRequestListActivity.class);
        startActivity(intent);
    }

    private void showGroupList() {
        Intent intent = new Intent(getActivity(), GroupListActivity.class);
        startActivity(intent);
    }

    private void showChannelList() {
        Intent intent = new Intent(getActivity(), ChannelListActivity.class);
        startActivity(intent);
    }
}
