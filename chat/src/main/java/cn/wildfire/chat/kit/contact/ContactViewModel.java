package cn.wildfire.chat.kit.contact;

import java.util.List;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import cn.wildfirechat.model.FriendRequest;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GeneralCallback;
import cn.wildfirechat.remote.OnFriendUpdateListener;
import cn.wildfirechat.remote.SearchUserCallback;

public class ContactViewModel extends ViewModel implements OnFriendUpdateListener {
    private MutableLiveData<Object> contactListUpdatedLiveData;
    private MutableLiveData<Integer> friendRequestUpdatedLiveData;

    public ContactViewModel() {
        super();
        ChatManager.Instance().addFriendUpdateListener(this);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        ChatManager.Instance().removeFriendUpdateListener(this);
    }

    public MutableLiveData<Object> contactListUpdatedLiveData() {
        if (contactListUpdatedLiveData == null) {
            contactListUpdatedLiveData = new MutableLiveData<>();
        }
        return contactListUpdatedLiveData;
    }

    public MutableLiveData<Integer> friendRequestUpdatedLiveData() {
        if (friendRequestUpdatedLiveData == null) {
            friendRequestUpdatedLiveData = new MutableLiveData<>();
        }
        return friendRequestUpdatedLiveData;
    }

    public List<String> getFriends(boolean refresh) {
        return ChatManager.Instance().getMyFriendList(refresh);
    }

    public List<UserInfo> getContacts(boolean refresh) {
        return ChatManager.Instance().getMyFriendListInfo(refresh);
    }

    public List<UserInfo> getContacts(List<String> ids) {
        return ChatManager.Instance().getUserInfos(ids);
    }

    public int getUnreadFriendRequestCount() {
        return ChatManager.Instance().getUnreadFriendRequestStatus();
    }

    public void clearUnreadFriendRequestStatus() {
        ChatManager.Instance().clearUnreadFriendRequestStatus();
    }

    @Override
    public void onFriendListUpdated() {
        if (contactListUpdatedLiveData != null) {
            contactListUpdatedLiveData.setValue(new Object());
        }
    }

    @Override
    public void onFriendRequestUpdated() {
        if (friendRequestUpdatedLiveData != null) {
            friendRequestUpdatedLiveData.setValue(getUnreadFriendRequestCount());
        }
    }

    public List<FriendRequest> getFriendRequest() {
        return ChatManager.Instance().getFriendRequest(true);
    }

    public MutableLiveData<Boolean> acceptFriendRequest(String friendId) {

        MutableLiveData<Boolean> result = new MutableLiveData<>();
        ChatManager.Instance().handleFriendRequest(friendId, true, new GeneralCallback() {
            @Override
            public void onSuccess() {
                ChatManager.Instance().loadFriendRequestFromRemote();
                List<FriendRequest> inComingFriendRequests = ChatManager.Instance().getFriendRequest(true);
                for (FriendRequest request : inComingFriendRequests) {
                    if (request.target.equals(friendId)) {
                        result.setValue(true);
                        return;
                    }
                }
                result.setValue(false);
            }

            @Override
            public void onFailure(int errorCode) {
                result.setValue(false);
            }
        });
        return result;
    }

    public MutableLiveData<List<UserInfo>> searchUser(String keyword) {
        MutableLiveData<List<UserInfo>> result = new MutableLiveData<>();
        ChatManager.Instance().searchUser(keyword, new SearchUserCallback() {
            @Override
            public void onSuccess(List<UserInfo> userInfos) {
                result.setValue(userInfos);
            }

            @Override
            public void onFailure(int errorCode) {
                result.setValue(null);
            }
        });

        return result;
    }

    public boolean isFriend(String targetUid) {
        return ChatManager.Instance().isMyFriend(targetUid);
    }

    public MutableLiveData<Boolean> invite(String targetUid, String message) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();
        ChatManager.Instance().sendFriendRequest(targetUid, message, new GeneralCallback() {
            @Override
            public void onSuccess() {
                result.setValue(true);
            }

            @Override
            public void onFailure(int errorCode) {
                result.setValue(false);
            }
        });
        return result;
    }

}
