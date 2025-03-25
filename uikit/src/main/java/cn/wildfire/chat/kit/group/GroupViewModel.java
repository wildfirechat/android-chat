/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.group;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.bumptech.glide.Glide;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.wildfire.chat.kit.ChatManagerHolder;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.common.OperateResult;
import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfire.chat.kit.third.utils.FileUtils;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfire.chat.kit.utils.PinyinUtils;
import cn.wildfire.chat.kit.utils.portrait.CombineBitmapTools;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.MessageContent;
import cn.wildfirechat.message.MessageContentMediaType;
import cn.wildfirechat.message.notification.ModifyGroupSettingsNotificationContent;
import cn.wildfirechat.message.notification.NotificationMessageContent;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.GroupMember;
import cn.wildfirechat.model.ModifyGroupInfoType;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GeneralCallback;
import cn.wildfirechat.remote.GeneralCallback2;
import cn.wildfirechat.remote.GetGroupMembersCallback;
import cn.wildfirechat.remote.GetGroupsCallback;
import cn.wildfirechat.remote.GetUserInfoListCallback;
import cn.wildfirechat.remote.OnGroupInfoUpdateListener;
import cn.wildfirechat.remote.OnGroupMembersUpdateListener;
import cn.wildfirechat.remote.OnReceiveMessageListener;
import cn.wildfirechat.remote.UploadMediaCallback;
import cn.wildfirechat.utils.WfcUtils;

public class GroupViewModel extends ViewModel implements OnGroupInfoUpdateListener, OnGroupMembersUpdateListener, OnReceiveMessageListener {
    private MutableLiveData<List<GroupInfo>> groupInfoUpdateLiveData;
    private MutableLiveData<List<GroupMember>> groupMembersUpdateLiveData;

    public GroupViewModel() {
        super();
        ChatManager.Instance().addGroupInfoUpdateListener(this);
        ChatManager.Instance().addGroupMembersUpdateListener(this);
        ChatManager.Instance().addOnReceiveMessageListener(this);
    }

    @Override
    protected void onCleared() {
        ChatManager.Instance().removeGroupInfoUpdateListener(this);
        ChatManager.Instance().removeGroupMembersUpdateListener(this);
        ChatManager.Instance().removeOnReceiveMessageListener(this);
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
        ChatManager.Instance().getGroupMemberUserInfosAsync(groupId, refresh, new GetUserInfoListCallback() {
            @Override
            public void onSuccess(List<UserInfo> userInfos) {
                List<UIUserInfo> users = UIUserInfo.fromUserInfos(userInfos);
                groupMemberLiveData.postValue(users);
            }

            @Override
            public void onFail(int errorCode) {

            }
        });
        return groupMemberLiveData;
    }

    public MutableLiveData<List<UserInfo>> getGroupMemberUserInfosLiveData(String groupId, boolean refresh) {

        return getGroupMemberUserInfosLiveData(groupId, refresh, Long.MAX_VALUE);
    }

    public MutableLiveData<List<UserInfo>> getGroupMemberUserInfosLiveData(String groupId, boolean refresh, long joinBeforeDt) {
        MutableLiveData<List<UserInfo>> groupMemberLiveData = new MutableLiveData<>();
        ChatManager.Instance().getGroupMembers(groupId, refresh, new GetGroupMembersCallback() {
            @Override
            public void onSuccess(List<GroupMember> groupMembers) {
                List<String> memberIds = new ArrayList<>(groupMembers.size());
                for (GroupMember member : groupMembers) {
                    if (member.createDt <= joinBeforeDt) {
                        memberIds.add(member.memberId);
                    }
                }
                ChatManager.Instance().getWorkHandler().post(() -> {
                    List<UserInfo> userInfos = ChatManager.Instance().getUserInfos(memberIds, groupId);
                    groupMemberLiveData.postValue(userInfos);
                });
            }

            @Override
            public void onFail(int errorCode) {
                groupMemberLiveData.postValue(new ArrayList<>());
            }
        });
        return groupMemberLiveData;
    }

    @Override
    public void onGroupInfoUpdate(List<GroupInfo> groupInfos) {
        if (groupInfoUpdateLiveData != null) {
            groupInfoUpdateLiveData.setValue(groupInfos);
        }
    }

    public MutableLiveData<OperateResult<String>> createGroup(Context context, List<UserInfo> checkedUsers, MessageContent notifyMsg, List<Integer> lines) {
        return createGroup(context, checkedUsers, notifyMsg, lines, null, null);
    }

    public MutableLiveData<OperateResult<String>> createGroup(Context context, List<UserInfo> checkedUsers, MessageContent notifyMsg, List<Integer> lines, String groupExra, String memberExtra) {
        List<String> selectedIds = new ArrayList<>(checkedUsers.size());
        List<UserInfo> selectedUsers = new ArrayList<>();
        for (UserInfo userInfo : checkedUsers) {
            selectedIds.add(userInfo.uid);
            selectedUsers.add(userInfo);
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
        ChatManager.Instance().createGroup(null, finalGroupName, null, GroupInfo.GroupType.Restricted, groupExra, selectedIds, memberExtra, lines, notifyMsg, new GeneralCallback2() {
            @Override
            public void onSuccess(String groupId) {
                groupLiveData.setValue(new OperateResult<>(groupId, 0));
            }

            @Override
            public void onFail(int errorCode) {
                groupLiveData.setValue(new OperateResult<>(errorCode));
            }
        });
        return groupLiveData;
    }

    public MutableLiveData<Boolean> addGroupMember(GroupInfo groupInfo, List<String> memberIds, MessageContent notifyMsg, List<Integer> notifyLines) {
        return addGroupMember(groupInfo, memberIds, notifyMsg, notifyLines, null);
    }

    public MutableLiveData<Boolean> addGroupMember(GroupInfo groupInfo, List<String> memberIds, MessageContent notifyMsg, List<Integer> notifyLines, String memberExtra) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();
        // TODO need update group portrait or not?
        ChatManager.Instance().addGroupMembers(groupInfo.target, memberIds, memberExtra, notifyLines, notifyMsg, new GeneralCallback() {
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

    public MutableLiveData<OperateResult<Boolean>> muteGroupMember(String groupId, boolean mute, List<String> memberIds, NotificationMessageContent notifyMsg, List<Integer> lines) {
        MutableLiveData<OperateResult<Boolean>> result = new MutableLiveData<>();
        ChatManager.Instance().muteGroupMember(groupId, mute, memberIds, lines, notifyMsg, new GeneralCallback() {
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

    public MutableLiveData<OperateResult<Boolean>> allowGroupMember(String groupId, boolean allow, List<String> memberIds, NotificationMessageContent notifyMsg, List<Integer> lines) {
        MutableLiveData<OperateResult<Boolean>> result = new MutableLiveData<>();
        ChatManager.Instance().allowGroupMember(groupId, allow, memberIds, lines, notifyMsg, new GeneralCallback() {
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

    public MutableLiveData<OperateResult<Boolean>> enablePrivateChat(String groupId, boolean enablePrivateChat, MessageContent notifyMsg, List<Integer> notifyLines) {
        MutableLiveData<OperateResult<Boolean>> result = new MutableLiveData<>();
        ChatManager.Instance().modifyGroupInfo(groupId, ModifyGroupInfoType.Modify_Group_PrivateChat, enablePrivateChat ? "0" : "1", notifyLines, notifyMsg, new GeneralCallback() {
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

    public MutableLiveData<OperateResult<Boolean>> setGroupHistoryMessage(String groupId, int searchType, MessageContent notifyMsg, List<Integer> notifyLines) {
        MutableLiveData<OperateResult<Boolean>> result = new MutableLiveData<>();
        ChatManager.Instance().modifyGroupInfo(groupId, ModifyGroupInfoType.Modify_Group_History_Message, searchType + "", notifyLines, notifyMsg, new GeneralCallback() {
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

    public @Nullable GroupInfo getGroupInfo(String groupId, boolean refresh) {
        return ChatManager.Instance().getGroupInfo(groupId, refresh);
    }

    public LiveData<GroupInfo> getGroupInfoAsync(String groupId, boolean refresh) {
        MutableLiveData<GroupInfo> data = new MutableLiveData<>();
        ChatManager.Instance().getWorkHandler().post(() -> {
            GroupInfo groupInfo = ChatManager.Instance().getGroupInfo(groupId, refresh);
            data.postValue(groupInfo);
        });
        return data;
    }

    public List<GroupMember> getGroupMembers(String groupId, boolean forceRefresh) {
        return ChatManager.Instance().getGroupMembers(groupId, forceRefresh);
    }

    public MutableLiveData<List<GroupMember>> getGroupMembersLiveData(String groupId, int maxCount) {
        MutableLiveData<List<GroupMember>> data = new MutableLiveData<>();
        ChatManager.Instance().getWorkHandler().post(() -> {
            List<GroupMember> groupMembers = ChatManager.Instance().getGroupMembersByCount(groupId, maxCount);
            data.postValue(groupMembers);
        });
        return data;
    }

    public MutableLiveData<List<UIUserInfo>> getGroupManagerUIUserInfosLiveData(String groupId, boolean refresh) {
        MutableLiveData<List<UIUserInfo>> data = new MutableLiveData<>();
        ChatManager.Instance().getWorkHandler().post(() -> {
            List<GroupMember> managers = getGroupManagers(groupId);
            List<UIUserInfo> userInfos = managerMemberToUIUserInfo(groupId, managers);
            data.postValue(userInfos);
        });

        return data;
    }

    public MutableLiveData<List<UIUserInfo>> getMutedOrAllowedMemberUIUserInfosLiveData(String groupId, boolean muted, boolean refresh) {
        MutableLiveData<List<UIUserInfo>> data = new MutableLiveData<>();
        ChatManager.Instance().getWorkHandler().post(() -> {
            List<GroupMember> mutedMembers = getMutedOrAllowedMembers(groupId, muted);
            List<UIUserInfo> userInfos = mutedOrAllowedMemberToUIUserInfo(groupId, muted, mutedMembers);
            data.postValue(userInfos);
        });

        return data;
    }

    public List<GroupMember> getGroupManagers(String groupId) {
        List<GroupMember> members = ChatManager.Instance().getGroupMembers(groupId, false);
        List<GroupMember> managers = new ArrayList<>();
        if (members != null) {
            for (GroupMember member : members) {
                if (member.type == GroupMember.GroupMemberType.Manager || member.type == GroupMember.GroupMemberType.Owner) {
                    managers.add(member);
                }
            }
        }
        return managers;
    }

    public List<String> getGroupManagerIds(String groupId) {
        List<GroupMember> managers = getGroupManagers(groupId);
        List<String> mangerIds = new ArrayList<>();
        if (managers != null) {
            for (GroupMember manager : managers) {
                mangerIds.add(manager.memberId);
            }
        }
        return mangerIds;
    }


    public List<GroupMember> getMutedOrAllowedMembers(String groupId, boolean muted) {
        List<GroupMember> members = ChatManager.Instance().getGroupMembers(groupId, false);
        List<GroupMember> managers = new ArrayList<>();
        if (members != null) {
            for (GroupMember member : members) {
                if ((muted && member.type == GroupMember.GroupMemberType.Allowed)
                    || !muted && member.type == GroupMember.GroupMemberType.Muted) {
                    managers.add(member);
                }
            }
        }
        return managers;
    }


    public List<String> getMutedOrAllowedMemberIds(String groupId, boolean muted) {
        List<GroupMember> mutedMembers = getMutedOrAllowedMembers(groupId, muted);
        List<String> mutedIds = new ArrayList<>();
        if (mutedMembers != null) {
            for (GroupMember manager : mutedMembers) {
                mutedIds.add(manager.memberId);
            }
        }
        return mutedIds;

    }

    private List<UIUserInfo> managerMemberToUIUserInfo(String groupId, List<GroupMember> members) {
        if (members == null || members.isEmpty()) {
            return null;
        }

        List<String> memberIds = new ArrayList<>(members.size());
        for (GroupMember member : members) {
            memberIds.add(member.memberId);
        }

        List<UIUserInfo> uiUserInfos = new ArrayList<>();
        List<UserInfo> userInfos = UserViewModel.getUsers(memberIds, groupId);
        boolean showManagerCategory = false;
        for (UserInfo userInfo : userInfos) {
            UIUserInfo info = new UIUserInfo(userInfo);
            String name = ChatManager.Instance().getGroupMemberDisplayName(userInfo);
            if (!TextUtils.isEmpty(name)) {
                String pinyin = PinyinUtils.getPinyin(name);
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

    private List<UIUserInfo> mutedOrAllowedMemberToUIUserInfo(String groupId, boolean muted, List<GroupMember> members) {
        if (members == null || members.isEmpty()) {
            return null;
        }

        List<String> memberIds = new ArrayList<>(members.size());
        for (GroupMember member : members) {
            memberIds.add(member.memberId);
        }

        List<UIUserInfo> uiUserInfos = new ArrayList<>();
        List<UserInfo> userInfos = UserViewModel.getUsers(memberIds, groupId);
        boolean showManagerCategory = false;
        for (UserInfo userInfo : userInfos) {
            UIUserInfo info = new UIUserInfo(userInfo);
            String name = ChatManager.Instance().getGroupMemberDisplayName(userInfo);
            if (!TextUtils.isEmpty(name)) {
                String pinyin = PinyinUtils.getPinyin(name);
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
            info.setCategory(muted ? "白名单列表" : "禁言列表");
            if (!showManagerCategory) {
                showManagerCategory = true;
                info.setShowCategory(true);
            }
            uiUserInfos.add(info);
        }
        return uiUserInfos;
    }

    public GroupMember getGroupMember(String groupId, String memberId) {
        return ChatManager.Instance().getGroupMember(groupId, memberId);
    }

    public String getGroupMemberDisplayName(String groupId, String memberId) {
        return ChatManager.Instance().getGroupMemberDisplayName(groupId, memberId);
    }

    public CharSequence getGroupMemberDisplayNameEx(String groupId, String memberId, int spanFontSize) {
        String displayName = ChatManager.Instance().getGroupMemberDisplayName(groupId, memberId);
        if (WfcUtils.isExternalTarget(memberId)) {
            return WfcUtils.buildExternalDisplayNameSpannableString(displayName, spanFontSize);
        } else {
            return displayName;
        }
    }

    public MutableLiveData<OperateResult<List<GroupInfo>>> getFavGroups() {
        MutableLiveData<OperateResult<List<GroupInfo>>> result = new MutableLiveData<>();
        ChatManager.Instance().getFavGroups(new GetGroupsCallback() {
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

    public MutableLiveData<OperateResult<Boolean>> updateGroupPortrait(String groupId, String localImagePath) {
        MutableLiveData<OperateResult<Boolean>> resultLiveData = new MutableLiveData<>();
        byte[] content = FileUtils.readFile(localImagePath);
        if (content != null) {
            ChatManager.Instance().uploadMediaFile(localImagePath, MessageContentMediaType.PORTRAIT.getValue(), new UploadMediaCallback() {
                @Override
                public void onSuccess(String result) {
                    ChatManager.Instance().modifyGroupInfo(groupId, ModifyGroupInfoType.Modify_Group_Portrait, result, Collections.singletonList(0), null, new GeneralCallback() {
                        @Override
                        public void onSuccess() {
                            resultLiveData.setValue(new OperateResult<>(true, 0));
                        }

                        @Override
                        public void onFail(int errorCode) {
                            resultLiveData.setValue(new OperateResult<>(false, errorCode));
                        }
                    });
                }

                @Override
                public void onProgress(long uploaded, long total) {

                }


                @Override
                public void onFail(int errorCode) {
                    resultLiveData.setValue(new OperateResult<>(errorCode));
                }
            });
        }
        return resultLiveData;
    }

    public MutableLiveData<OperateResult<Boolean>> setGroupRemark(String groupId, String remark) {
        MutableLiveData<OperateResult<Boolean>> result = new MutableLiveData<>();
        ChatManager.Instance().setGroupRemark(groupId, remark, new GeneralCallback() {
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
        ChatManager.Instance().setFavGroup(groupId, fav, new GeneralCallback() {
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
                drawable = Glide.with(context).load(userInfo.portrait).placeholder(R.mipmap.avatar_def).submit(60, 60).get();
            } catch (Exception e) {
                e.printStackTrace();
                drawable = Glide.with(context).load(R.mipmap.avatar_def).submit(60, 60).get();
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

    @Override
    public void onReceiveMessage(List<Message> messages, boolean hasMore) {
        Set<String> groupIds = new HashSet<>();
        for (Message msg : messages) {
            if (msg.content instanceof ModifyGroupSettingsNotificationContent) {
                groupIds.add(((ModifyGroupSettingsNotificationContent) msg.content).groupId);
            }
        }
        if (groupIds.size() > 0) {
            ChatManager.Instance().getGroupInfos(new ArrayList<>(groupIds), true);
        }
    }
}
