package cn.wildfire.chat.kit.group;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

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
import cn.wildfire.chat.kit.common.OperateResult;
import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfire.chat.kit.third.utils.FileUtils;
import cn.wildfire.chat.kit.utils.portrait.CombineBitmapTools;
import cn.wildfirechat.message.MessageContentMediaType;
import cn.wildfirechat.message.notification.AddGroupMemberNotificationContent;
import cn.wildfirechat.message.notification.CreateGroupNotificationContent;
import cn.wildfirechat.message.notification.KickoffGroupMemberNotificationContent;
import cn.wildfirechat.message.notification.ModifyGroupAliasNotificationContent;
import cn.wildfirechat.message.notification.NotificationMessageContent;
import cn.wildfirechat.message.notification.QuitGroupNotificationContent;
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

public class GroupViewModel extends ViewModel implements OnGroupInfoUpdateListener, OnGroupMembersUpdateListener {
    private MutableLiveData<List<GroupInfo>> groupInfoUpdateLiveData;
    private MutableLiveData<List<GroupMember>> groupMembersUpdateLiveData;

    public GroupViewModel() {
        super();
        ChatManager.Instance().addGroupInfoUpdateListener(this);
        ChatManager.Instance().addGroupMembersUpdateListener(this);
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
    protected void onCleared() {
        super.onCleared();
        ChatManager.Instance().removeGroupInfoUpdateListener(this);
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
        String mGroupName = "";
        if (checkedUsers.size() > 3) {
            for (int i = 0; i < 3; i++) {
                UserInfo friend = checkedUsers.get(i).getUserInfo();
                mGroupName += friend.displayName + "、";
            }
        } else {
            for (UIUserInfo friend : checkedUsers) {
                mGroupName += friend.getUserInfo().displayName + "、";
            }
        }
        mGroupName = mGroupName.substring(0, mGroupName.length() - 1);


        CreateGroupNotificationContent notifyCnt = new CreateGroupNotificationContent();
        notifyCnt.creator = ChatManager.Instance().getUserId();
        notifyCnt.groupName = mGroupName;
        notifyCnt.fromSelf = true;

        MutableLiveData<OperateResult<String>> groupLiveData = new MutableLiveData<>();
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
                        ChatManager.Instance().createGroup(null, notifyCnt.groupName, result, selectedIds, Arrays.asList(0), notifyCnt, new GeneralCallback2() {
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
                ChatManager.Instance().createGroup(null, notifyCnt.groupName, null, selectedIds, Arrays.asList(0), notifyCnt, new GeneralCallback2() {
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
        AddGroupMemberNotificationContent notificationContent = new AddGroupMemberNotificationContent();
        notificationContent.fromSelf = true;
        notificationContent.invitor = ChatManager.Instance().getUserId();
        notificationContent.invitees = memberIds;
        MutableLiveData<Boolean> result = new MutableLiveData<>();
        // TODO need update group portrait or not?
        ChatManager.Instance().addGroupMembers(groupInfo.target, memberIds, Arrays.asList(0), notificationContent, new GeneralCallback() {
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
        KickoffGroupMemberNotificationContent notifyCnt = new KickoffGroupMemberNotificationContent();
        notifyCnt.operator = ChatManager.Instance().getUserId();
        notifyCnt.kickedMembers = memberIds;
        notifyCnt.fromSelf = true;

        MutableLiveData<Boolean> result = new MutableLiveData<>();
        ChatManagerHolder.gChatManager.removeGroupMembers(groupInfo.target, memberIds, Arrays.asList(0), notifyCnt, new GeneralCallback() {
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

    public MutableLiveData<OperateResult<Boolean>> modifyGroupInfo(String groupId, ModifyGroupInfoType modifyType, String newValue, NotificationMessageContent notifyMsg) {
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
        ModifyGroupAliasNotificationContent content = new ModifyGroupAliasNotificationContent();
        content.fromSelf = true;
        content.alias = alias;
        ChatManager.Instance().modifyGroupAlias(groupId, alias, Collections.singletonList(0), content, new GeneralCallback() {
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
        QuitGroupNotificationContent notifyCnt = new QuitGroupNotificationContent();
        notifyCnt.operator = ChatManagerHolder.gChatManager.getUserId();
        MutableLiveData<Boolean> result = new MutableLiveData<>();
        ChatManager.Instance().quitGroup(groupId, lines, notifyCnt, new GeneralCallback() {
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
        if (groupMembersUpdateLiveData != null) {
            groupMembersUpdateLiveData.setValue(groupMembers);
        }
    }
}
