package cn.wildfire.chat.kit.contact;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import cn.wildfire.chat.kit.common.AppScopeViewModel;
import cn.wildfire.chat.kit.common.OperateResult;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.FriendRequest;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GeneralCallback;
import cn.wildfirechat.remote.OnFriendUpdateListener;
import cn.wildfirechat.remote.SearchUserCallback;

public class ContactViewModel extends ViewModel implements OnFriendUpdateListener, AppScopeViewModel {
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
        int count = getUnreadFriendRequestCount();
        if (count > 0) {
            friendRequestUpdatedLiveData.setValue(count);
        }
        return friendRequestUpdatedLiveData;
    }

    public List<String> getFriends(boolean refresh) {
        return ChatManager.Instance().getMyFriendList(refresh);
    }

    public LiveData<List<UserInfo>> getContactsAsync(boolean refresh) {
        MutableLiveData<List<UserInfo>> data = new MutableLiveData<>();
        ChatManager.Instance().getWorkHandler().post(() -> {
            List<UserInfo> userInfos = ChatManager.Instance().getMyFriendListInfo(refresh);
            data.postValue(userInfos);
        });
        return data;
    }

    public List<UserInfo> getContacts(boolean refresh) {
        return ChatManager.Instance().getMyFriendListInfo(refresh);
    }

    public int getUnreadFriendRequestCount() {
        return ChatManager.Instance().getUnreadFriendRequestStatus();
    }

    public void clearUnreadFriendRequestStatus() {
        ChatManager.Instance().clearUnreadFriendRequestStatus();
    }

    @Override
    public void onFriendListUpdate(List<String> updateFriendList) {
        if (contactListUpdatedLiveData != null) {
            contactListUpdatedLiveData.setValue(new Object());
        }
    }

    @Override
    public void onFriendRequestUpdate() {
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
            public void onFail(int errorCode) {
                result.setValue(false);
            }
        });
        return result;
    }

    public MutableLiveData<List<UserInfo>> searchUser(String keyword, boolean fuzzy) {
        MutableLiveData<List<UserInfo>> result = new MutableLiveData<>();
        ChatManager.Instance().searchUser(keyword, fuzzy, new SearchUserCallback() {
            @Override
            public void onSuccess(List<UserInfo> userInfos) {
                result.setValue(userInfos);
            }

            @Override
            public void onFail(int errorCode) {
                result.setValue(null);
            }
        });

        return result;
    }

    public boolean isFriend(String targetUid) {
        return ChatManager.Instance().isMyFriend(targetUid);
    }

    public LiveData<OperateResult<Boolean>> deleteFriend(String userId) {
        MutableLiveData<OperateResult<Boolean>> result = new MutableLiveData<>();
        ChatManager.Instance().deleteFriend(userId, new GeneralCallback() {
            @Override
            public void onSuccess() {
                if (contactListUpdatedLiveData != null) {
                    contactListUpdatedLiveData.postValue(new Object());
                }
                ChatManager.Instance().removeConversation(new Conversation(Conversation.ConversationType.Single, userId, 0), true);
                result.postValue(new OperateResult<>(0));
            }

            @Override
            public void onFail(int errorCode) {
                result.postValue(new OperateResult<>(errorCode));
            }
        });

        return result;
    }

    public MutableLiveData<Boolean> invite(String targetUid, String message) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();
        ChatManager.Instance().sendFriendRequest(targetUid, message, new GeneralCallback() {
            @Override
            public void onSuccess() {
                result.setValue(true);
            }

            @Override
            public void onFail(int errorCode) {
                result.setValue(false);
            }
        });
        return result;
    }

}
