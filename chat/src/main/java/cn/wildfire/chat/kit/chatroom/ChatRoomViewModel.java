package cn.wildfire.chat.kit.chatroom;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import cn.wildfire.chat.kit.common.OperateResult;
import cn.wildfirechat.model.ChatRoomInfo;
import cn.wildfirechat.model.ChatRoomMembersInfo;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GeneralCallback;
import cn.wildfirechat.remote.GetChatRoomInfoCallback;
import cn.wildfirechat.remote.GetChatRoomMembersInfoCallback;

public class ChatRoomViewModel extends ViewModel {
    public MutableLiveData<OperateResult<Boolean>> joinChatRoom(String chatRoomId) {
        MutableLiveData<OperateResult<Boolean>> result = new MutableLiveData<>();
        ChatManager.Instance().joinChatRoom(chatRoomId, new GeneralCallback() {
            @Override
            public void onSuccess() {
                result.setValue(new OperateResult<>(true, 0));

            }

            @Override
            public void onFail(int errorCode) {
                result.setValue(new OperateResult<>(false, errorCode));
            }
        });
        return result;
    }

    public MutableLiveData<OperateResult<Boolean>> quitChatRoom(String chatRoomId) {
        MutableLiveData<OperateResult<Boolean>> result = new MutableLiveData<>();
        ChatManager.Instance().quitChatRoom(chatRoomId, new GeneralCallback() {
            @Override
            public void onSuccess() {
                result.setValue(new OperateResult<>(true, 0));
            }

            @Override
            public void onFail(int errorCode) {
                result.setValue(new OperateResult<>(false, 0));
            }
        });
        return result;
    }

    public MutableLiveData<OperateResult<ChatRoomInfo>> getChatRoomInfo(String chatRoomId, long updateDt) {
        MutableLiveData<OperateResult<ChatRoomInfo>> result = new MutableLiveData<>();
        ChatManager.Instance().getChatRoomInfo(chatRoomId, updateDt, new GetChatRoomInfoCallback() {
            @Override
            public void onSuccess(ChatRoomInfo chatRoomInfo) {
                result.setValue(new OperateResult<>(chatRoomInfo, 0));
            }

            @Override
            public void onFail(int errorCode) {
                result.setValue(new OperateResult<>(null, errorCode));

            }
        });
        return result;
    }

    public MutableLiveData<OperateResult<ChatRoomMembersInfo>> getChatRoomMembersInfo(String chatRoomId, int maxCount) {
        MutableLiveData<OperateResult<ChatRoomMembersInfo>> result = new MutableLiveData<>();
        ChatManager.Instance().getChatRoomMembersInfo(chatRoomId, maxCount, new GetChatRoomMembersInfoCallback() {
            @Override
            public void onSuccess(ChatRoomMembersInfo chatRoomMembersInfo) {
                result.setValue(new OperateResult<>(chatRoomMembersInfo, 0));
            }

            @Override
            public void onFail(int errorCode) {
                result.setValue(new OperateResult<>(null, errorCode));
            }
        });
        return result;
    }
}
