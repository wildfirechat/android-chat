package cn.wildfire.chat.kit.group;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.bumptech.glide.Glide;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import cn.wildfire.chat.kit.ChatManagerHolder;
import cn.wildfire.chat.kit.common.AppScopeViewModel;
import cn.wildfire.chat.kit.common.OperateResult;
import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfire.chat.kit.third.utils.FileUtils;
import cn.wildfire.chat.kit.utils.portrait.CombineBitmapTools;
import cn.wildfirechat.message.MessageContentMediaType;
import cn.wildfirechat.message.notification.GroupNotificationMessageContent;
import cn.wildfirechat.message.notification.NotificationMessageContent;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.GroupMember;
import cn.wildfirechat.model.ModifyGroupInfoType;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GeneralCallback;
import cn.wildfirechat.remote.GeneralCallback2;
import cn.wildfirechat.remote.GetGroupsCallback;
import cn.wildfirechat.remote.OnGroupInfoUpdateListener;
import cn.wildfirechat.remote.OnGroupMembersUpdateListener;
import cn.wildfirechat.remote.UserSettingScope;

public class GroupViewModel extends ViewModel implements AppScopeViewModel, OnGroupInfoUpdateListener, OnGroupMembersUpdateListener {
    private MutableLiveData<List<GroupInfo>> groupInfoUpdateLiveData;
    private MutableLiveData<List<GroupMember>> groupMembersUpdateLiveData;

    public GroupViewModel() {
        super();
        ChatManager.Instance().addGroupInfoUpdateListener(this);
        ChatManager.Instance().addGroupMembersUpdateListener(this);
    }

    @Override
    protected void onCleared() {
        ChatManager.Instance().removeGroupInfoUpdateListener(this);
        ChatManager.Instance().removeGroupMembersUpdateListener(this);
    }

    public MutableLiveData<List<GroupInfo>> groupInfoUpdateLiveData() {
        if (groupInfoUpdateLiveData == null) {
            groupInfoUpdateLiveData = new MutableLiveData<>();
        }
        return groupInfoUpdateLiveData;
    }

    public MutableLiveData<List<GroupMember>> groupMembersUpdateLiveData() {
        if (groupMembersUpdateLiveData == null) {
            groupMembersUpdateLiveData = new MutableLiveData<>();
        }
        return groupMembersUpdateLiveData;
    }

    @Override
    public void onGroupInfoUpdate(List<GroupInfo> groupInfos) {
        if (groupInfoUpdateLiveData != null) {
            groupInfoUpdateLiveData.setValue(groupInfos);
        }
    }

    public MutableLiveData<OperateResult<String>> createGroup(Context context, List<UIUserInfo> checkedUsers) {
        List<String> selectedIds = new ArrayList<>(checkedUsers.size());
        for (UIUserInfo userInfo : checkedUsers) {
            selectedIds.add(userInfo.getUserInfo().uid);
        }
        selectedIds.add(ChatManager.Instance().getUserId());
        String groupName = "";
        if (checkedUsers.size() > 3) {
            for (int i = 0; i < 3; i++) {
                UserInfo friend = checkedUsers.get(i).getUserInfo();
                groupName += friend.displayName + "、";
            }
        } else {
            for (UIUserInfo friend : checkedUsers) {
                groupName += friend.getUserInfo().displayName + "、";
            }
        }
        groupName = groupName.substring(0, groupName.length() - 1);

        MutableLiveData<OperateResult<String>> groupLiveData = new MutableLiveData<>();
        String finalGroupName = groupName;
        ChatManager.Instance().getWorkHandler().post(() -> {
            String groupPortrait = null;
            try {
                groupPortrait = generateGroupPortrait(context, checkedUsers);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (groupPortrait != null) {
                byte[] content = FileUtils.readFile(groupPortrait);
                ChatManager.Instance().uploadMedia(content, MessageContentMediaType.PORTRAIT.getValue(), new GeneralCallback2() {
                    @Override
                    public void onSuccess(String result) {
                        ChatManager.Instance().createGroup(null, finalGroupName, result, GroupInfo.GroupType.Normal, selectedIds, Arrays.asList(0), null, new GeneralCallback2() {
                            @Override
                            public void onSuccess(String groupId) {
                                groupLiveData.setValue(new OperateResult<>(groupId, 0));
                            }

                            @Override
                            public void onFail(int errorCode) {
                                groupLiveData.setValue(new OperateResult<>(errorCode));
                            }
                        });
                    }

                    @Override
                    public void onFail(int errorCode) {
                        groupLiveData.setValue(new OperateResult<>("上传群头像失败", errorCode));
                    }
                });
            } else {
                ChatManager.Instance().createGroup(null, finalGroupName, null, GroupInfo.GroupType.Normal, selectedIds, Arrays.asList(0), null, new GeneralCallback2() {
                    @Override
                    public void onSuccess(String groupId) {
                        groupLiveData.setValue(new OperateResult<>(groupId, 0));
                    }

                    @Override
                    public void onFail(int errorCode) {
                        groupLiveData.setValue(new OperateResult<>(errorCode));
                    }
                });
            }
        });
        return groupLiveData;
    }

    public MutableLiveData<Boolean> addGroupMember(GroupInfo groupInfo, List<String> memberIds) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();
        // TODO need update group portrait or not?
        ChatManager.Instance().addGroupMembers(groupInfo.target, memberIds, Arrays.asList(0), null, new GeneralCallback() {
            @Override
            public void onSuccess() {
                if (groupInfoUpdateLiveData != null) {
                    groupInfo.memberCount -= groupInfo.memberCount + memberIds.size();
                    groupInfoUpdateLiveData.setValue(Collections.singletonList(groupInfo));
                }
                result.setValue(true);
            }

            @Override
            public void onFail(int errorCode) {
                result.setValue(false);
            }
        });

        return result;
    }

    public MutableLiveData<Boolean> removeGroupMember(GroupInfo groupInfo, List<String> memberIds) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();
        ChatManagerHolder.gChatManager.removeGroupMembers(groupInfo.target, memberIds, Arrays.asList(0), null, new GeneralCallback() {
            @Override
            public void onSuccess() {
                if (groupInfoUpdateLiveData != null) {
                    groupInfo.memberCount -= groupInfo.memberCount + memberIds.size();
                    groupInfoUpdateLiveData.setValue(Collections.singletonList(groupInfo));
                }
                result.setValue(true);
            }

            @Override
            public void onFail(int errorCode) {
                result.setValue(false);
            }
        });

        return result;
    }

    public MutableLiveData<OperateResult<Boolean>> setGroupManager(String groupId, boolean isSet, List<String> memberIds, List<Integer> lines, NotificationMessageContent notifyMsg) {
        MutableLiveData<OperateResult<Boolean>> result = new MutableLiveData<>();
        ChatManager.Instance().setGroupManager(groupId, isSet, memberIds, lines, notifyMsg, new GeneralCallback() {
            @Override
            public void onSuccess() {
                result.setValue(new OperateResult<>(0));
            }

            @Override
            public void onFail(int errorCode) {
                result.setValue(new OperateResult<>(errorCode));
            }
        });
        return result;
    }

    public MutableLiveData<OperateResult<Boolean>> muteAll(String groupId, boolean mute) {
        MutableLiveData<OperateResult<Boolean>> result = new MutableLiveData<>();
        ChatManager.Instance().modifyGroupInfo(groupId, ModifyGroupInfoType.Modify_Group_Mute, mute ? "1" : "0", Collections.singletonList(0), null, new GeneralCallback() {
            @Override
            public void onSuccess() {
                result.setValue(new OperateResult<>(0));
            }

            @Override
            public void onFail(int errorCode) {
                result.setValue(new OperateResult<>(errorCode));
            }
        });
        return result;
    }

    public MutableLiveData<OperateResult<Boolean>> preventPrivateChat(String groupId, boolean preventPrivateChat) {
        MutableLiveData<OperateResult<Boolean>> result = new MutableLiveData<>();
        ChatManager.Instance().modifyGroupInfo(groupId, ModifyGroupInfoType.Modify_Group_PrivateChat, preventPrivateChat ? "1" : "0", Collections.singletonList(0), null, new GeneralCallback() {
            @Override
            public void onSuccess() {
                result.setValue(new OperateResult<>(0));
            }

            @Override
            public void onFail(int errorCode) {
                result.setValue(new OperateResult<>(errorCode));
            }
        });
        return result;
    }

    public MutableLiveData<OperateResult<Boolean>> setGroupJoinType(String groupId, int joinType) {
        MutableLiveData<OperateResult<Boolean>> result = new MutableLiveData<>();
        ChatManager.Instance().modifyGroupInfo(groupId, ModifyGroupInfoType.Modify_Group_JoinType, joinType + "", Collections.singletonList(0), null, new GeneralCallback() {
            @Override
            public void onSuccess() {
                result.setValue(new OperateResult<>(0));
            }

            @Override
            public void onFail(int errorCode) {
                result.setValue(new OperateResult<>(errorCode));
            }
        });
        return result;
    }

    public MutableLiveData<OperateResult<Boolean>> setGroupSearchType(String groupId, int searchType) {
        MutableLiveData<OperateResult<Boolean>> result = new MutableLiveData<>();
        ChatManager.Instance().modifyGroupInfo(groupId, ModifyGroupInfoType.Modify_Group_Searchable, searchType + "", Collections.singletonList(0), null, new GeneralCallback() {
            @Override
            public void onSuccess() {
                result.setValue(new OperateResult<>(0));
            }

            @Override
            public void onFail(int errorCode) {
                result.setValue(new OperateResult<>(errorCode));
            }
        });
        return result;
    }

    public @Nullable
    GroupInfo getGroupInfo(String groupId, boolean refresh) {
        return ChatManager.Instance().getGroupInfo(groupId, refresh);
    }

    public List<GroupMember> getGroupMembers(String groupId, boolean forceRefresh) {
        return ChatManager.Instance().getGroupMembers(groupId, forceRefresh);
    }

    public GroupMember getGroupMember(String groupId, String memberId) {
        return ChatManager.Instance().getGroupMember(groupId, memberId);
    }

    // 优先级如下：
    // 1. 群备注 2. 好友备注 3. 用户displayName 4. <uid>
    public String getGroupMemberDisplayName(String groupId, String memberId) {
        GroupMember groupMember = ChatManager.Instance().getGroupMember(groupId, memberId);
        if (groupMember != null && !TextUtils.isEmpty(groupMember.alias)) {
            return groupMember.alias;
        }

        String alias = ChatManager.Instance().getFriendAlias(memberId);
        if (!TextUtils.isEmpty(alias)) {
            return alias;
        }
        UserInfo userInfo = ChatManager.Instance().getUserInfo(memberId, false);
        if (userInfo != null && !TextUtils.isEmpty(userInfo.displayName)) {
            return userInfo.displayName;
        }
        return "<" + memberId + ">";
    }

    public MutableLiveData<OperateResult<List<GroupInfo>>> getMyGroups() {
        MutableLiveData<OperateResult<List<GroupInfo>>> result = new MutableLiveData<>();
        ChatManager.Instance().getMyGroups(new GetGroupsCallback() {
            @Override
            public void onSuccess(List<GroupInfo> groupInfos) {
                result.setValue(new OperateResult<>(groupInfos, 0));
            }

            @Override
            public void onFail(int errorCode) {
                result.setValue(new OperateResult<>(null, 0));
            }
        });
        return result;
    }

    public MutableLiveData<OperateResult<Boolean>> modifyGroupInfo(String groupId, ModifyGroupInfoType modifyType, String newValue, GroupNotificationMessageContent notifyMsg) {
        MutableLiveData<OperateResult<Boolean>> result = new MutableLiveData<>();
        ChatManager.Instance().modifyGroupInfo(groupId, modifyType, newValue, Collections.singletonList(0), null, new GeneralCallback() {
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

    public MutableLiveData<OperateResult> modifyMyGroupAlias(String groupId, String alias) {
        MutableLiveData<OperateResult> result = new MutableLiveData<>();
        ChatManager.Instance().modifyGroupAlias(groupId, alias, Collections.singletonList(0), null, new GeneralCallback() {
            @Override
            public void onSuccess() {
                result.setValue(new OperateResult<>(0));
            }

            @Override
            public void onFail(int errorCode) {
                result.setValue(new OperateResult<>(errorCode));
            }
        });
        return result;
    }

    public MutableLiveData<OperateResult<Boolean>> setFavGroup(String groupId, boolean fav) {
        MutableLiveData<OperateResult<Boolean>> result = new MutableLiveData<>();
        ChatManager.Instance().setUserSetting(UserSettingScope.FavoriteGroup, groupId, fav ? "1" : "0", new GeneralCallback() {
            @Override
            public void onSuccess() {
                result.setValue(new OperateResult<>(0));
            }

            @Override
            public void onFail(int errorCode) {
                result.setValue(new OperateResult<>(errorCode));
            }
        });
        return result;
    }

    public MutableLiveData<Boolean> quitGroup(String groupId, List<Integer> lines) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();
        ChatManager.Instance().quitGroup(groupId, lines, null, new GeneralCallback() {
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

    public MutableLiveData<Boolean> dismissGroup(String groupId, List<Integer> lines) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();
        ChatManager.Instance().dismissGroup(groupId, lines, null, new GeneralCallback() {
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

    private @Nullable
    String generateGroupPortrait(Context context, List<UIUserInfo> userInfos) throws Exception {
        List<Bitmap> bitmaps = new ArrayList<>();
        for (UIUserInfo userInfo : userInfos) {
            try {
                Drawable drawable = Glide.with(context).load(userInfo.getUserInfo().portrait).submit(60, 60).get();
                if (drawable instanceof BitmapDrawable) {
                    bitmaps.add(((BitmapDrawable) drawable).getBitmap());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Bitmap bitmap = CombineBitmapTools.combimeBitmap(context, 60, 60, bitmaps);
        if (bitmap == null) {
            return null;
        }
        //create a file to write bitmap data
        File f = new File(context.getCacheDir(), System.currentTimeMillis() + ".png");
        f.createNewFile();

        //Convert bitmap to byte array
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 0 /*ignored for PNG*/, bos);
        byte[] bitmapData = bos.toByteArray();

        //write the bytes in file
        FileOutputStream fos = new FileOutputStream(f);
        fos.write(bitmapData);
        fos.flush();
        fos.close();

        return f.getAbsolutePath();
    }

    @Override
    public void onGroupMembersUpdate(String groupId, List<GroupMember> groupMembers) {
        if (groupMembersUpdateLiveData != null && groupMembers != null && !groupMembers.isEmpty()) {
            groupMembersUpdateLiveData.setValue(groupMembers);
        }
    }
}
