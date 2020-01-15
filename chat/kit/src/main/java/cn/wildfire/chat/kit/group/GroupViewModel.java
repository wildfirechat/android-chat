package cn.wildfire.chat.kit.group;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.ChatManagerHolder;
import cn.wildfire.chat.kit.GlideApp;
import cn.wildfire.chat.kit.common.OperateResult;
import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfire.chat.kit.utils.PinyinUtils;
import cn.wildfire.chat.kit.utils.portrait.CombineBitmapTools;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.message.MessageContent;
import cn.wildfirechat.message.MessageContentMediaType;
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
import cn.wildfirechat.remote.UploadMediaCallback;
import cn.wildfirechat.remote.UserSettingScope;

public class GroupViewModel extends ViewModel implements OnGroupInfoUpdateListener, OnGroupMembersUpdateListener {
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

    public MutableLiveData<List<UIUserInfo>> getGroupMemberUIUserInfosLiveData(String groupId, boolean refresh) {
        MutableLiveData<List<UIUserInfo>> groupMemberLiveData = new MutableLiveData<>();
        ChatManager.Instance().getWorkHandler().post(() -> {
            List<GroupMember> members = ChatManager.Instance().getGroupMembers(groupId, refresh);
            List<String> memberIds = new ArrayList<>(members.size());
            for (GroupMember member : members) {
                memberIds.add(member.memberId);
            }
            List<UserInfo> userInfos = ChatManager.Instance().getUserInfos(memberIds, groupId);
            List<UIUserInfo> users = UIUserInfo.fromUserInfos(userInfos);
            groupMemberLiveData.postValue(users);
        });
        return groupMemberLiveData;
    }

    public MutableLiveData<List<UserInfo>> getGroupMemberUserInfosLiveData(String groupId, boolean refresh) {
        MutableLiveData<List<UserInfo>> groupMemberLiveData = new MutableLiveData<>();
        ChatManager.Instance().getWorkHandler().post(() -> {
            List<GroupMember> members = ChatManager.Instance().getGroupMembers(groupId, refresh);
            List<String> memberIds = new ArrayList<>(members.size());
            for (GroupMember member : members) {
                memberIds.add(member.memberId);
            }
            List<UserInfo> userInfos = ChatManager.Instance().getUserInfos(memberIds, groupId);
            groupMemberLiveData.postValue(userInfos);
        });
        return groupMemberLiveData;
    }

    @Override
    public void onGroupInfoUpdate(List<GroupInfo> groupInfos) {
        if (groupInfoUpdateLiveData != null) {
            groupInfoUpdateLiveData.setValue(groupInfos);
        }
    }

    public MutableLiveData<OperateResult<String>> createGroup(Context context, List<UIUserInfo> checkedUsers, MessageContent notifyMsg, List<Integer> lines) {
        List<String> selectedIds = new ArrayList<>(checkedUsers.size());
        List<UserInfo> selectedUsers = new ArrayList<>();
        for (UIUserInfo userInfo : checkedUsers) {
            selectedIds.add(userInfo.getUserInfo().uid);
            selectedUsers.add(userInfo.getUserInfo());
        }
        String id = ChatManager.Instance().getUserId();
        if (!selectedIds.contains(id)) {
            selectedIds.add(id);
            selectedUsers.add(ChatManager.Instance().getUserInfo(id, false));
        }
        String groupName = "";
        for (int i = 0; i < 3 && i < selectedUsers.size(); i++) {
            groupName += selectedUsers.get(i).displayName + "、";
        }
        groupName = groupName.substring(0, groupName.length() - 1);
        if (selectedUsers.size() > 3) {
            groupName += " ...";
        }

        groupName = groupName.substring(0, groupName.length() - 1);

        MutableLiveData<OperateResult<String>> groupLiveData = new MutableLiveData<>();
        String finalGroupName = groupName;
        ChatManager.Instance().getWorkHandler().post(() -> {
            String groupPortrait = null;
            try {
                groupPortrait = generateGroupPortrait(context, selectedUsers);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (groupPortrait != null) {
                ChatManager.Instance().uploadMediaFile(groupPortrait, MessageContentMediaType.PORTRAIT.getValue(), new UploadMediaCallback() {
                    @Override
                    public void onSuccess(String result) {
                        ChatManager.Instance().createGroup(null, finalGroupName, result, GroupInfo.GroupType.Restricted, selectedIds, lines, notifyMsg, new GeneralCallback2() {
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
                    public void onProgress(long uploaded, long total) {

                    }

                    @Override
                    public void onFail(int errorCode) {
                        groupLiveData.setValue(new OperateResult<>("上传群头像失败", errorCode));
                    }
                });
            } else {
                ChatManager.Instance().createGroup(null, finalGroupName, null, GroupInfo.GroupType.Restricted, selectedIds, lines, notifyMsg, new GeneralCallback2() {
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

    public MutableLiveData<Boolean> addGroupMember(GroupInfo groupInfo, List<String> memberIds, MessageContent notifyMsg, List<Integer> notifyLines) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();
        // TODO need update group portrait or not?
        ChatManager.Instance().addGroupMembers(groupInfo.target, memberIds, notifyLines, notifyMsg, new GeneralCallback() {
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

    public MutableLiveData<Boolean> removeGroupMember(GroupInfo groupInfo, List<String> memberIds, MessageContent notifyMsg, List<Integer> notifyLines) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();
        ChatManagerHolder.gChatManager.removeGroupMembers(groupInfo.target, memberIds, notifyLines, notifyMsg, new GeneralCallback() {
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

    public MutableLiveData<OperateResult<Boolean>> setGroupManager(String groupId, boolean isSet, List<String> memberIds, NotificationMessageContent notifyMsg, List<Integer> lines) {
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

    public MutableLiveData<OperateResult<Boolean>> muteAll(String groupId, boolean mute, MessageContent notifyMsg, List<Integer> notifyLines) {
        MutableLiveData<OperateResult<Boolean>> result = new MutableLiveData<>();
        ChatManager.Instance().modifyGroupInfo(groupId, ModifyGroupInfoType.Modify_Group_Mute, mute ? "1" : "0", notifyLines, notifyMsg, new GeneralCallback() {
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

    public MutableLiveData<OperateResult<Boolean>> preventPrivateChat(String groupId, boolean preventPrivateChat, MessageContent notifyMsg, List<Integer> notifyLines) {
        MutableLiveData<OperateResult<Boolean>> result = new MutableLiveData<>();
        ChatManager.Instance().modifyGroupInfo(groupId, ModifyGroupInfoType.Modify_Group_PrivateChat, preventPrivateChat ? "1" : "0", notifyLines, notifyMsg, new GeneralCallback() {
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

    public MutableLiveData<OperateResult<Boolean>> setGroupJoinType(String groupId, int joinType, MessageContent notifyMsg, List<Integer> notifyLines) {
        MutableLiveData<OperateResult<Boolean>> result = new MutableLiveData<>();
        ChatManager.Instance().modifyGroupInfo(groupId, ModifyGroupInfoType.Modify_Group_JoinType, joinType + "", notifyLines, notifyMsg, new GeneralCallback() {
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

    public MutableLiveData<OperateResult<Boolean>> setGroupSearchType(String groupId, int searchType, MessageContent notifyMsg, List<Integer> notifyLines) {
        MutableLiveData<OperateResult<Boolean>> result = new MutableLiveData<>();
        ChatManager.Instance().modifyGroupInfo(groupId, ModifyGroupInfoType.Modify_Group_Searchable, searchType + "", notifyLines, notifyMsg, new GeneralCallback() {
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

    public MutableLiveData<List<GroupMember>> getGroupMembersLiveData(String groupId, boolean refresh) {
        MutableLiveData<List<GroupMember>> data = new MutableLiveData<>();
        ChatManager.Instance().getWorkHandler().post(() -> {
            List<GroupMember> members = ChatManager.Instance().getGroupMembers(groupId, refresh);
            data.postValue(members);
        });

        return data;
    }

    public MutableLiveData<List<UIUserInfo>> getGroupManagerUIUserInfosLiveData(String groupId, boolean refresh) {
        MutableLiveData<List<UIUserInfo>> data = new MutableLiveData<>();
        ChatManager.Instance().getWorkHandler().post(() -> {
            List<GroupMember> members = ChatManager.Instance().getGroupMembers(groupId, false);
            List<UIUserInfo> userInfos = memberToUIUserInfo(groupId, members);
            data.postValue(userInfos);
        });

        return data;
    }


    private List<UIUserInfo> memberToUIUserInfo(String groupId, List<GroupMember> members) {
        if (members == null || members.isEmpty()) {
            return null;
        }

        List<String> memberIds = new ArrayList<>(members.size());
        for (GroupMember member : members) {
            if (member.type == GroupMember.GroupMemberType.Owner || member.type == GroupMember.GroupMemberType.Manager) {
                memberIds.add(member.memberId);
            }
        }

        List<UIUserInfo> uiUserInfos = new ArrayList<>();
        List<UserInfo> userInfos = UserViewModel.getUsers(memberIds, groupId);
        boolean showManagerCategory = false;
        for (UserInfo userInfo : userInfos) {
            UIUserInfo info = new UIUserInfo(userInfo);
            if (!TextUtils.isEmpty(userInfo.displayName)) {
                String pinyin = PinyinUtils.getPinyin(userInfo.displayName);
                char c = pinyin.toUpperCase().charAt(0);
                if (c >= 'A' && c <= 'Z') {
                    info.setSortName(pinyin);
                } else {
                    // 为了让排序排到最后
                    info.setSortName("{" + pinyin);
                }
            } else {
                info.setSortName("");
            }

            for (GroupMember member : members) {
                if (userInfo.uid.equals(member.memberId)) {
                    if (member.type == GroupMember.GroupMemberType.Manager) {
                        info.setCategory("管理员");
                        if (!showManagerCategory) {
                            showManagerCategory = true;
                            info.setShowCategory(true);
                        }
                        uiUserInfos.add(info);
                    } else {
                        info.setCategory("群主");
                        info.setShowCategory(true);
                        uiUserInfos.add(0, info);
                    }
                    break;
                }
            }
        }
        return uiUserInfos;
    }

    public GroupMember getGroupMember(String groupId, String memberId) {
        return ChatManager.Instance().getGroupMember(groupId, memberId);
    }

    public String getGroupMemberDisplayName(String groupId, String memberId) {
        return ChatManager.Instance().getGroupMemberDisplayName(groupId, memberId);
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

    public MutableLiveData<OperateResult<Boolean>> modifyGroupInfo(String groupId, ModifyGroupInfoType modifyType, String newValue, MessageContent notifyMsg, List<Integer> notifyLines) {
        MutableLiveData<OperateResult<Boolean>> result = new MutableLiveData<>();
        ChatManager.Instance().modifyGroupInfo(groupId, modifyType, newValue, notifyLines, notifyMsg, new GeneralCallback() {
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

    public MutableLiveData<OperateResult> modifyMyGroupAlias(String groupId, String alias, MessageContent notifyMsg, List<Integer> notifyLines) {
        MutableLiveData<OperateResult> result = new MutableLiveData<>();
        ChatManager.Instance().modifyGroupAlias(groupId, alias, notifyLines, notifyMsg, new GeneralCallback() {
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

    public MutableLiveData<Boolean> quitGroup(String groupId, List<Integer> lines, MessageContent notifyMsg) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();
        ChatManager.Instance().quitGroup(groupId, lines, notifyMsg, new GeneralCallback() {
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

    public MutableLiveData<Boolean> dismissGroup(String groupId, List<Integer> lines, MessageContent notifyMsg) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();
        ChatManager.Instance().dismissGroup(groupId, lines, notifyMsg, new GeneralCallback() {
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
    String generateGroupPortrait(Context context, List<UserInfo> userInfos) throws Exception {
        List<Bitmap> bitmaps = new ArrayList<>();
        for (UserInfo userInfo : userInfos) {
            Drawable drawable;
            try {
                drawable = GlideApp.with(context).load(userInfo.portrait).error(R.mipmap.avatar_def).submit(60, 60).get();
            } catch (Exception e) {
                e.printStackTrace();
                drawable = GlideApp.with(context).load(R.mipmap.avatar_def).submit(60, 60).get();
            }
            if (drawable instanceof BitmapDrawable) {
                bitmaps.add(((BitmapDrawable) drawable).getBitmap());
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
