/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.pane;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcWebViewActivity;
import cn.wildfire.chat.kit.WfcWebViewFragment;
import cn.wildfire.chat.kit.channel.ChannelInfoActivity;
import cn.wildfire.chat.kit.channel.ChannelInfoFragment;
import cn.wildfire.chat.kit.channel.ChannelListActivity;
import cn.wildfire.chat.kit.channel.ChannelListFragment;
import cn.wildfire.chat.kit.channel.CreateChannelActivity;
import cn.wildfire.chat.kit.channel.CreateChannelFragment;
import cn.wildfire.chat.kit.channel.SearchChannelActivity;
import cn.wildfire.chat.kit.channel.SearchChannelPageFragment;
import cn.wildfire.chat.kit.chatroom.ChatRoomListActivity;
import cn.wildfire.chat.kit.chatroom.ChatRoomListFragment;
import cn.wildfire.chat.kit.contact.ContactListActivity;
import cn.wildfire.chat.kit.contact.ContactListFragment;
import cn.wildfire.chat.kit.contact.newfriend.FriendRequestListActivity;
import cn.wildfire.chat.kit.contact.newfriend.FriendRequestListFragment;
import cn.wildfire.chat.kit.contact.newfriend.InviteFriendActivity;
import cn.wildfire.chat.kit.contact.newfriend.InviteFriendFragment;
import cn.wildfire.chat.kit.contact.newfriend.SearchUserActivity;
import cn.wildfire.chat.kit.contact.newfriend.SearchUserPageFragment;
import cn.wildfire.chat.kit.contact.pick.PickContactActivity;
import cn.wildfire.chat.kit.contact.pick.PickContactPageFragment;
import cn.wildfire.chat.kit.conversation.ChannelConversationInfoFragment;
import cn.wildfire.chat.kit.conversation.ChatRoomConversationInfoFragment;
import cn.wildfire.chat.kit.conversation.ConversationActivity;
import cn.wildfire.chat.kit.conversation.ConversationInfoActivity;
import cn.wildfire.chat.kit.conversation.ConversationPanePage;
import cn.wildfire.chat.kit.conversation.CreateConversationActivity;
import cn.wildfire.chat.kit.conversation.CreateConversationPageFragment;
import cn.wildfire.chat.kit.conversation.GroupConversationInfoFragment;
import cn.wildfire.chat.kit.conversation.SecretConversationInfoFragment;
import cn.wildfire.chat.kit.conversation.SingleConversationInfoFragment;
import cn.wildfire.chat.kit.conversation.ext.ImagePickerPanePageFragment;
import cn.wildfire.chat.kit.conversation.file.FileRecordActivity;
import cn.wildfire.chat.kit.conversation.file.FileRecordFragment;
import cn.wildfire.chat.kit.conversation.file.FileRecordListActivity;
import cn.wildfire.chat.kit.conversation.file.FileRecordListFragment;
import cn.wildfire.chat.kit.conversation.forward.ForwardActivity;
import cn.wildfire.chat.kit.conversation.forward.ForwardPageFragment;
import cn.wildfire.chat.kit.conversation.mention.MentionGroupMemberActivity;
import cn.wildfire.chat.kit.conversation.mention.MentionGroupMemberPageFragment;
import cn.wildfire.chat.kit.conversation.message.CompositeMessageContentActivity;
import cn.wildfire.chat.kit.conversation.message.CompositeMessageContentFragment;
import cn.wildfire.chat.kit.conversation.pick.PickConversationActivity;
import cn.wildfire.chat.kit.conversation.pick.PickConversationPageFragment;
import cn.wildfire.chat.kit.conversation.pick.PickOrCreateConversationTargetActivity;
import cn.wildfire.chat.kit.conversation.pick.PickOrCreateConversationTargetPageFragment;
import cn.wildfire.chat.kit.conversation.receipt.GroupMessageReceiptActivity;
import cn.wildfire.chat.kit.conversation.receipt.GroupMessageReceiptFragment;
import cn.wildfire.chat.kit.favorite.FavoriteListActivity;
import cn.wildfire.chat.kit.favorite.FavoriteListFragment;
import cn.wildfire.chat.kit.group.GroupInfoActivity;
import cn.wildfire.chat.kit.group.GroupInfoFragment;
import cn.wildfire.chat.kit.group.GroupListActivity;
import cn.wildfire.chat.kit.group.GroupListFragment;
import cn.wildfire.chat.kit.group.GroupMemberListActivity;
import cn.wildfire.chat.kit.group.GroupMemberListFragment;
import cn.wildfire.chat.kit.group.GroupMemberMessageHistoryActivity;
import cn.wildfire.chat.kit.group.GroupMemberMessageHistoryFragment;
import cn.wildfire.chat.kit.group.AddGroupMemberActivity;
import cn.wildfire.chat.kit.group.AddGroupMemberPageFragment;
import cn.wildfire.chat.kit.group.PickGroupMemberActivity;
import cn.wildfire.chat.kit.group.PickGroupMemberPageFragment;
import cn.wildfire.chat.kit.group.RemoveGroupMemberActivity;
import cn.wildfire.chat.kit.group.RemoveGroupMemberPageFragment;
import cn.wildfire.chat.kit.group.SetGroupAnnouncementActivity;
import cn.wildfire.chat.kit.group.SetGroupAnnouncementPageFragment;
import cn.wildfire.chat.kit.group.SetGroupNameActivity;
import cn.wildfire.chat.kit.group.SetGroupNamePageFragment;
import cn.wildfire.chat.kit.group.SetGroupRemarkActivity;
import cn.wildfire.chat.kit.group.SetGroupRemarkPageFragment;
import cn.wildfire.chat.kit.group.manage.AddGroupManagerActivity;
import cn.wildfire.chat.kit.group.manage.AddGroupManagerPageFragment;
import cn.wildfire.chat.kit.group.manage.MuteGroupMemberActivity;
import cn.wildfire.chat.kit.group.manage.MuteGroupMemberPageFragment;
import cn.wildfire.chat.kit.group.manage.GroupManageActivity;
import cn.wildfire.chat.kit.group.manage.GroupManageFragment;
import cn.wildfire.chat.kit.group.manage.GroupManagerListActivity;
import cn.wildfire.chat.kit.group.manage.GroupManagerListFragment;
import cn.wildfire.chat.kit.group.manage.GroupMemberPermissionActivity;
import cn.wildfire.chat.kit.group.manage.GroupMemberPermissionFragment;
import cn.wildfire.chat.kit.group.manage.GroupMuteOrAllowActivity;
import cn.wildfire.chat.kit.group.manage.GroupMuteOrAllowFragment;
import cn.wildfire.chat.kit.group.manage.JoinGroupRequestListActivity;
import cn.wildfire.chat.kit.group.manage.JoinGroupRequestListFragment;
import cn.wildfire.chat.kit.mesh.DomainInfoActivity;
import cn.wildfire.chat.kit.mesh.DomainInfoFragment;
import cn.wildfire.chat.kit.mesh.DomainListActivity;
import cn.wildfire.chat.kit.mesh.DomainListFragment;
import cn.wildfire.chat.kit.organization.EmployeeInfoActivity;
import cn.wildfire.chat.kit.organization.OrganizationMemberListActivity;
import cn.wildfire.chat.kit.organization.OrganizationMemberListFragment;
import cn.wildfire.chat.kit.organization.pick.PickOrganizationMemberActivity;
import cn.wildfire.chat.kit.organization.pick.PickOrganizationMemberPageFragment;
import cn.wildfire.chat.kit.pc.PCSessionActivity;
import cn.wildfire.chat.kit.pc.PCSessionFragment;
import cn.wildfire.chat.kit.poll.activity.CreatePollActivity;
import cn.wildfire.chat.kit.poll.activity.CreatePollPageFragment;
import cn.wildfire.chat.kit.poll.activity.PollDetailActivity;
import cn.wildfire.chat.kit.poll.activity.PollDetailPageFragment;
import cn.wildfire.chat.kit.poll.activity.PollHomeActivity;
import cn.wildfire.chat.kit.poll.activity.PollHomePageFragment;
import cn.wildfire.chat.kit.poll.activity.PollListActivity;
import cn.wildfire.chat.kit.poll.activity.PollListPageFragment;
import cn.wildfire.chat.kit.collection.CollectionDetailActivity;
import cn.wildfire.chat.kit.collection.CollectionDetailPageFragment;
import cn.wildfire.chat.kit.collection.CreateCollectionActivity;
import cn.wildfire.chat.kit.collection.CreateCollectionPageFragment;
import cn.wildfire.chat.kit.qrcode.QRCodeActivity;
import cn.wildfire.chat.kit.qrcode.QRCodeFragment;
import cn.wildfire.chat.kit.search.SearchMessageActivity;
import cn.wildfire.chat.kit.search.SearchMessagePageFragment;
import cn.wildfire.chat.kit.search.SearchPortalActivity;
import cn.wildfire.chat.kit.search.SearchPortalPageFragment;
import cn.wildfire.chat.kit.search.bydate.ConversationMessageByDateActivity;
import cn.wildfire.chat.kit.search.bydate.ConversationMessageByDateFragment;
import cn.wildfire.chat.kit.search.link.ConversationLinkRecordActivity;
import cn.wildfire.chat.kit.search.link.ConversationLinkRecordFragment;
import cn.wildfire.chat.kit.search.media.ConversationMediaActivity;
import cn.wildfire.chat.kit.search.media.ConversationMediaFragment;
import cn.wildfire.chat.kit.settings.ChatSettingActivity;
import cn.wildfire.chat.kit.settings.ChatSettingFragment;
import cn.wildfire.chat.kit.settings.FontSizeActivity;
import cn.wildfire.chat.kit.settings.FontSizeFragment;
import cn.wildfire.chat.kit.settings.MessageNotifySettingActivity;
import cn.wildfire.chat.kit.settings.MessageNotifySettingFragment;
import cn.wildfire.chat.kit.settings.PrivacyFindMeSettingActivity;
import cn.wildfire.chat.kit.settings.PrivacyFindMeSettingFragment;
import cn.wildfire.chat.kit.settings.PrivacySettingActivity;
import cn.wildfire.chat.kit.settings.PrivacySettingFragment;
import cn.wildfire.chat.kit.settings.blacklist.BlacklistListActivity;
import cn.wildfire.chat.kit.settings.blacklist.BlacklistListFragment;
import cn.wildfire.chat.kit.third.location.ui.activity.MyLocationActivity;
import cn.wildfire.chat.kit.third.location.ui.activity.ShowLocationActivity;
import cn.wildfire.chat.kit.third.location.ui.fragment.MyLocationPageFragment;
import cn.wildfire.chat.kit.third.location.ui.fragment.ShowLocationPageFragment;
import cn.wildfire.chat.kit.user.ChangeMyNameActivity;
import cn.wildfire.chat.kit.user.ChangeMyNameFragment;
import cn.wildfire.chat.kit.user.SetAliasActivity;
import cn.wildfire.chat.kit.user.SetAliasPageFragment;
import cn.wildfire.chat.kit.user.SetNameActivity;
import cn.wildfire.chat.kit.user.SetNamePageFragment;
import cn.wildfire.chat.kit.user.UserInfoActivity;
import cn.wildfire.chat.kit.user.UserInfoFragment;
import cn.wildfire.chat.kit.voip.conference.ConferenceHistoryListActivity;
import cn.wildfire.chat.kit.voip.conference.ConferenceHistoryListFragment;
import cn.wildfire.chat.kit.voip.conference.ConferenceInfoActivity;
import cn.wildfire.chat.kit.voip.conference.ConferenceInfoPageFragment;
import cn.wildfire.chat.kit.voip.conference.ConferencePortalActivity;
import cn.wildfire.chat.kit.voip.conference.ConferencePortalPageFragment;
import cn.wildfire.chat.kit.voip.conference.CreateConferenceActivity;
import cn.wildfire.chat.kit.voip.conference.CreateConferencePageFragment;
import cn.wildfire.chat.kit.voip.conference.OrderConferenceActivity;
import cn.wildfire.chat.kit.voip.conference.OrderConferencePageFragment;
import cn.wildfirechat.model.ChannelInfo;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.ConversationInfo;
import cn.wildfirechat.model.DomainInfo;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.PCOnlineInfo;
import cn.wildfirechat.model.UserInfo;
import com.lqr.imagepicker.ui.ImageGridActivity;

/**
 * 「哪些页面可以在平板右栏里打开」的注册表：{@code Activity 类} → 对应的 Fragment 工厂。
 * <p>
 * <strong>为什么需要注册而不是自动转换</strong>：Android 的「页面」是 Activity，右栏里只能装
 * Fragment，没有通用手段把任意 Activity 塞进一个 View 里。好在本仓库绝大多数页面本来就是
 * 「{@code fragment_container_activity} + 一个 Fragment」的壳，把壳去掉直接用里面那个 Fragment
 * 即可，注册项通常就是一行。
 * <p>
 * <strong>没注册的页面不是错误</strong>：{@link #createPage} 返回 null 时调用方原样
 * {@code startActivity}，仍然是全屏页面 —— 与改造前完全一致。因此可以按页面逐个接入，
 * 未接入的部分不会坏掉。媒体预览、音视频通话、扫码这类<strong>本来就该全屏</strong>的页面
 * 永远不要注册。
 * <p>
 * <strong>手机端不受影响</strong>：本类只被右栏的导航器调用，而右栏只在
 * {@code WfcDeviceUtils.isTwoPaneLayout()} 为 true 时存在，手机上这个类连初始化都不会发生。
 */
public final class PaneRegistry {

    private PaneRegistry() {
    }

    /**
     * 由启动 intent 造出右栏里的页面 Fragment。
     * <p>
     * 返回 null 表示「这次不要在右栏打开」——用于同一个 Activity 既是普通页面又是选择器的情况
     * （如 {@code GroupListActivity} 带 {@code forResult} 时要返回结果，只能全屏打开）。
     */
    public interface PageFactory {
        @Nullable
        Fragment create(Context context, Intent intent);
    }

    /**
     * 页面在<strong>同一条导航栈内</strong>的身份。两次打开算出同一个 key，就不再压一层新的，
     * 而是退回到已经在栈里的那一层（见 {@link PaneStackFragment#pushPage}）。
     * <p>
     * 典型场景：会话A → 点头像 → 用户资料 → 发消息，最后一步应该退回到栈里的会话A，
     * 而不是在它上面再叠一个会话A。返回 null 表示不去重，每次都压新的一层。
     * <p>
     * key 只在<strong>本条栈内</strong>比较。别的 tab 的栈里同时开着同一个会话是允许的
     * ——那是另一条导航路径上的另一个页面。
     */
    public interface PageKey {
        @Nullable
        String key(Intent intent);
    }

    static final class Entry {
        final PageFactory factory;
        final PageKey pageKey;

        Entry(PageFactory factory, PageKey pageKey) {
            this.factory = factory;
            this.pageKey = pageKey;
        }
    }

    private static final Map<String, Entry> ENTRIES = new HashMap<>();

    /**
     * 隐式 intent（只有 action）解析结果的缓存：action → Activity 类名，解析不到时缓存 null。
     * {@code PackageManager.resolveActivity} 要跨进程查 PMS，导航是高频路径，不该每次都查。
     */
    private static final Map<String, String> RESOLVED_ACTIONS = new HashMap<>();

    static {
        registerBuiltInPages();
        registerOptionalPages();
    }

    /**
     * 登记那些「源码不一定在」的页面。
     * <p>
     * 目前只有朋友圈：它是 {@code uikit/build.gradle} 里 {@code // moment start ... // moment end}
     * 之间那两行 srcDirs 指进来的一份可选源码（同级仓库 {@code ../android-momentkit}），
     * 不集成朋友圈的项目会把那两行删掉。本类一旦直接 import {@code cn.wildfire.chat.moment.*}，
     * uikit 在那些项目里就编译不过了，所以只能反射 —— 与 {@code WfcUIKit.initMomentClient()}
     * 反射 {@code MomentClient} 是同一个理由。
     * <p>
     * 找不到就什么也不做：朋友圈的页面照旧全屏打开，与改造前一致。
     */
    private static void registerOptionalPages() {
        try {
            Class.forName("cn.wildfire.chat.moment.MomentPaneRegistry")
                .getMethod("register")
                .invoke(null);
        } catch (ClassNotFoundException e) {
            // 没集成朋友圈
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void register(Class<? extends Activity> activityClass, PageFactory factory) {
        register(activityClass, factory, null);
    }

    public static void register(Class<? extends Activity> activityClass, PageFactory factory, PageKey pageKey) {
        ENTRIES.put(activityClass.getName(), new Entry(factory, pageKey));
    }

    /**
     * 把隐式 intent 补上 component，其余情况原样返回。
     * <p>
     * 右栏的一切（能不能进右栏、标题取哪个 {@code android:label}、栈内去重的 key）都以
     * component 为准，而仓库里确实存在 {@code new Intent(WfcIntent.ACTION_MOMENT)} 这种
     * 只带 action 的导航。不在这里补齐的话，这类页面永远匹配不到注册项，只能全屏打开。
     * <p>
     * 只认<strong>本应用内</strong>的解析结果：系统分享、外部浏览器这些本来就该交给系统全屏处理。
     * 补出来的 component 只用于右栏内部判断；未登记时调用方启动的仍是原始 intent。
     */
    public static Intent resolveComponent(Context context, Intent intent) {
        if (context == null || intent == null || intent.getComponent() != null || intent.getAction() == null) {
            return intent;
        }
        String className = resolveActivityClassName(context, intent);
        if (className == null) {
            return intent;
        }
        Intent resolved = new Intent(intent);
        resolved.setComponent(new ComponentName(context.getPackageName(), className));
        return resolved;
    }

    @Nullable
    private static String resolveActivityClassName(Context context, Intent intent) {
        String action = intent.getAction();
        // 只有「纯 action」的 intent 才能按 action 缓存：带 data/type 时解析结果依赖它们
        boolean cacheable = intent.getData() == null && intent.getType() == null;
        if (cacheable && RESOLVED_ACTIONS.containsKey(action)) {
            return RESOLVED_ACTIONS.get(action);
        }
        String className = null;
        ResolveInfo info = context.getPackageManager().resolveActivity(intent, 0);
        if (info != null && info.activityInfo != null
            && context.getPackageName().equals(info.activityInfo.packageName)) {
            className = info.activityInfo.name;
        }
        if (cacheable) {
            RESOLVED_ACTIONS.put(action, className);
        }
        return className;
    }

    /**
     * 这个 intent 指向的页面是否登记过。调用前请先用 {@link #resolveComponent} 补齐隐式 intent。
     */
    public static boolean isRegistered(Intent intent) {
        return entryOf(intent) != null;
    }

    @Nullable
    public static Fragment createPage(Context context, Intent intent) {
        Entry entry = entryOf(intent);
        return entry == null ? null : entry.factory.create(context, intent);
    }

    /**
     * 页面在本条栈内的身份，见 {@link PageKey}。
     */
    @Nullable
    static String pageKeyOf(Intent intent) {
        Entry entry = entryOf(intent);
        return entry == null || entry.pageKey == null ? null : entry.pageKey.key(intent);
    }

    @Nullable
    private static Entry entryOf(Intent intent) {
        if (intent == null) {
            return null;
        }
        ComponentName component = intent.getComponent();
        return component == null ? null : ENTRIES.get(component.getClassName());
    }

    // ==================== 栈内身份 ====================

    private static String conversationKey(String prefix, @Nullable Conversation conversation) {
        return conversation == null ? null
            : prefix + ':' + conversation.type.getValue() + ':' + conversation.target + ':' + conversation.line;
    }

    // ==================== uikit 内置页面 ====================

    private static void registerBuiltInPages() {
        // 会话页。右栏最主要的页面，参数多且需要在视图就绪后才能应用，见 ConversationPanePage。
        // 栈内单例：从「会话A → 用户资料 → 发消息」回到会话A时退回原来那一层，不再叠一层。
        register(ConversationActivity.class, (context, intent) -> new ConversationPanePage(),
            intent -> conversationKey("conversation", intent.getParcelableExtra("conversation")));

        // 用户资料。菜单随「是否好友/是否拉黑/是否星标」变化，这套逻辑现在住在
        // UserInfoFragment 里（实现 WfcPage），手机端与右栏共用同一份，这里无需再表达一次。
        register(UserInfoActivity.class, PaneRegistry::createUserInfoPage, PaneRegistry::userInfoKey);
        // 组织架构里的成员详情，页面与菜单和用户资料完全一致，只是入口不同
        register(EmployeeInfoActivity.class, PaneRegistry::createUserInfoPage, PaneRegistry::userInfoKey);

        // 会话信息（单聊/群聊/聊天室/频道/密聊）。右栏内从会话页往下钻最常用的一层。
        register(ConversationInfoActivity.class, (context, intent) -> {
            ConversationInfo conversationInfo = intent.getParcelableExtra("conversationInfo");
            if (conversationInfo == null) {
                return null;
            }
            switch (conversationInfo.conversation.type) {
                case Single:
                    return SingleConversationInfoFragment.newInstance(conversationInfo);
                case Group:
                    return GroupConversationInfoFragment.newInstance(conversationInfo);
                case ChatRoom:
                    return ChatRoomConversationInfoFragment.newInstance(conversationInfo);
                case Channel:
                    return ChannelConversationInfoFragment.newInstance(conversationInfo);
                case SecretChat:
                    return SecretConversationInfoFragment.newInstance(conversationInfo);
                default:
                    return null;
            }
        }, intent -> {
            ConversationInfo info = intent.getParcelableExtra("conversationInfo");
            return info == null ? null : conversationKey("conversationInfo", info.conversation);
        });

        // 群组：列表 / 成员 / 管理。带 forResult 的是选择器，必须全屏返回结果。
        register(GroupListActivity.class, (context, intent) ->
                intent.getBooleanExtra(GroupListActivity.INTENT_FOR_RESULT, false) ? null : new GroupListFragment(),
            intent -> GroupListActivity.class.getName());
        register(GroupMemberListActivity.class, (context, intent) -> {
            GroupInfo groupInfo = intent.getParcelableExtra("groupInfo");
            return groupInfo == null ? null : GroupMemberListFragment.newInstance(groupInfo);
        }, PaneRegistry::groupInfoKey);
        register(GroupManageActivity.class, (context, intent) -> {
            GroupInfo groupInfo = intent.getParcelableExtra("groupInfo");
            return groupInfo == null ? null : GroupManageFragment.newInstance(groupInfo);
        }, PaneRegistry::groupInfoKey);
        register(GroupManagerListActivity.class, (context, intent) -> {
            GroupInfo groupInfo = intent.getParcelableExtra("groupInfo");
            return groupInfo == null ? null : GroupManagerListFragment.newInstance(groupInfo);
        }, PaneRegistry::groupInfoKey);
        register(GroupMemberPermissionActivity.class, (context, intent) -> {
            GroupInfo groupInfo = intent.getParcelableExtra("groupInfo");
            return groupInfo == null ? null : GroupMemberPermissionFragment.newInstance(groupInfo);
        }, PaneRegistry::groupInfoKey);
        register(JoinGroupRequestListActivity.class, (context, intent) -> {
            String groupId = intent.getStringExtra("groupId");
            return groupId == null ? null : JoinGroupRequestListFragment.newInstance(groupId);
        }, intent -> {
            String groupId = intent.getStringExtra("groupId");
            return groupId == null ? null : JoinGroupRequestListActivity.class.getName() + ':' + groupId;
        });
        register(GroupMemberMessageHistoryActivity.class, (context, intent) -> {
            String groupId = intent.getStringExtra("groupId");
            String groupMemberId = intent.getStringExtra("groupMemberId");
            return groupId == null || groupMemberId == null ? null
                : GroupMemberMessageHistoryFragment.newInstance(groupId, groupMemberId);
        }, intent -> GroupMemberMessageHistoryActivity.class.getName()
            + ':' + intent.getStringExtra("groupId") + ':' + intent.getStringExtra("groupMemberId"));

        // 从群成员里选人（发起群语音/视频）。这是第一个进右栏的「选择器」：它需要回传结果，
        // 因此调用方必须用 WfcPageCompat.startPageForResult —— 裸 startActivityForResult
        // 的 requestCode 在到达主界面前就被 FragmentManager 换掉了，结果送不回去。
        register(PickGroupMemberActivity.class,
            (context, intent) -> PickGroupMemberPageFragment.fromIntent(intent));

        // 另外三个同族的选人页（移出成员、禁言/加白名单、加管理员），共用
        // BasePickGroupMemberPageFragment。都不去重：同一个群反复进出选人页时，
        // 每次都该是一张干净的空白勾选表。
        register(RemoveGroupMemberActivity.class,
            (context, intent) -> RemoveGroupMemberPageFragment.fromIntent(intent));
        register(MuteGroupMemberActivity.class,
            (context, intent) -> MuteGroupMemberPageFragment.fromIntent(intent));
        register(AddGroupManagerActivity.class,
            (context, intent) -> AddGroupManagerPageFragment.fromIntent(intent));

        // 发起群聊 / 新建会话。不回传结果，建完直接把会话压在本页上面。
        register(CreateConversationActivity.class,
            (context, intent) -> CreateConversationPageFragment.fromIntent(intent),
            intent -> CreateConversationActivity.class.getName());

        // 加群成员。同样是需要回传结果的选择器（RESULT_ADD_SUCCESS / RESULT_ADD_FAIL）。
        register(AddGroupMemberActivity.class,
            (context, intent) -> AddGroupMemberPageFragment.fromIntent(intent),
            PaneRegistry::groupInfoKey);

        // 「改一段文字然后保存」的五个页面，共用 TextEditPageFragment。
        // 都不去重：这类页面从来只有一层，且每次进来都该按最新的资料重新填。
        register(SetAliasActivity.class, (context, intent) -> SetAliasPageFragment.fromIntent(intent));
        register(SetNameActivity.class, (context, intent) -> SetNamePageFragment.fromIntent(intent));
        register(SetGroupNameActivity.class, (context, intent) -> SetGroupNamePageFragment.fromIntent(intent));
        register(SetGroupRemarkActivity.class, (context, intent) -> SetGroupRemarkPageFragment.fromIntent(intent));
        register(SetGroupAnnouncementActivity.class,
            (context, intent) -> SetGroupAnnouncementPageFragment.fromIntent(intent));

        // 频道列表。带 pick 时是选择器，只能全屏。
        register(ChannelListActivity.class, (context, intent) -> {
            if (intent.getBooleanExtra("pick", false)) {
                return null;
            }
            ChannelListFragment fragment = new ChannelListFragment();
            Bundle args = new Bundle();
            args.putBoolean("pick", false);
            fragment.setArguments(args);
            return fragment;
        }, intent -> ChannelListActivity.class.getName());

        // 新的朋友
        register(FriendRequestListActivity.class, (context, intent) -> new FriendRequestListFragment(),
            intent -> FriendRequestListActivity.class.getName());

        // 收藏、黑名单、聊天室、互联域、会议历史：全局唯一的列表页，栈内单例
        register(FavoriteListActivity.class, (context, intent) -> new FavoriteListFragment(),
            intent -> FavoriteListActivity.class.getName());
        register(BlacklistListActivity.class, (context, intent) -> new BlacklistListFragment(),
            intent -> BlacklistListActivity.class.getName());
        register(ChatRoomListActivity.class, (context, intent) -> new ChatRoomListFragment(),
            intent -> ChatRoomListActivity.class.getName());
        register(DomainListActivity.class, (context, intent) -> new DomainListFragment(),
            intent -> DomainListActivity.class.getName());
        register(ConferenceHistoryListActivity.class, (context, intent) -> new ConferenceHistoryListFragment(),
            intent -> ConferenceHistoryListActivity.class.getName());

        // 会议一族。入口页（发现 tab → 会议）与详情页都在右栏可达的路径上，登记后不再整屏跳。
        // 会议入口页：全局唯一。
        register(ConferencePortalActivity.class, (context, intent) -> ConferencePortalPageFragment.fromIntent(intent),
            intent -> ConferencePortalActivity.class.getName());
        // 会议详情：按会议去重（收藏列表/邀请消息反复点到同一个会议不该叠两层）。
        register(ConferenceInfoActivity.class, (context, intent) -> ConferenceInfoPageFragment.fromIntent(intent),
            intent -> "conferenceInfo:" + intent.getStringExtra("conferenceId"));
        // 发起会议 / 预定会议：一次性表单，不去重。
        register(CreateConferenceActivity.class, (context, intent) -> CreateConferencePageFragment.fromIntent(intent));
        register(OrderConferenceActivity.class, (context, intent) -> OrderConferencePageFragment.fromIntent(intent));

        // 投票一族。入口是会话加号面板（已在右栏），页面本体登记后不再整屏跳。
        // 投票首页 / 我的投票：按群去重（不同的群各自一份）。
        register(PollHomeActivity.class, (context, intent) -> PollHomePageFragment.fromIntent(intent),
            intent -> "pollHome:" + intent.getStringExtra("groupId"));
        register(PollListActivity.class, (context, intent) -> PollListPageFragment.fromIntent(intent),
            intent -> "pollList:" + intent.getStringExtra("groupId"));
        // 投票详情：按投票去重。
        register(PollDetailActivity.class, (context, intent) -> PollDetailPageFragment.fromIntent(intent),
            intent -> "pollDetail:" + intent.getLongExtra("pollId", 0));
        // 创建投票：一次性表单，不去重。
        register(CreatePollActivity.class, (context, intent) -> CreatePollPageFragment.fromIntent(intent));

        // 接龙一族。入口是会话加号面板（已在右栏），页面本体登记后不再整屏跳。
        // 创建接龙：一次性表单，不去重。
        register(CreateCollectionActivity.class, (context, intent) -> CreateCollectionPageFragment.fromIntent(intent));
        // 接龙详情：按 collectionId 去重（同一条接龙消息反复点开不该叠两层）。
        register(CollectionDetailActivity.class, (context, intent) -> CollectionDetailPageFragment.fromIntent(intent),
            intent -> {
                cn.wildfirechat.message.Message message = intent.getParcelableExtra("message");
                if (message == null || !(message.content instanceof cn.wildfirechat.message.CollectionMessageContent)) {
                    return null;
                }
                return "collectionDetail:"
                    + ((cn.wildfirechat.message.CollectionMessageContent) message.content).getCollectionId();
            });

        // 组织架构成员列表。带 pick 时是选择器，只能全屏；菜单里的搜索框要回调到页面本身。
        register(OrganizationMemberListActivity.class, (context, intent) -> {
            if (intent.getBooleanExtra("pick", false)) {
                return null;
            }
            OrganizationMemberListFragment fragment = new OrganizationMemberListFragment();
            Bundle args = new Bundle();
            args.putInt("organizationId", intent.getIntExtra("organizationId", 0));
            args.putBoolean("pick", false);
            fragment.setArguments(args);
            return fragment;
        }, intent -> OrganizationMemberListActivity.class.getName() + ':' + intent.getIntExtra("organizationId", 0));

        // 会话内的查找：按日期、链接记录、图片与视频、文件
        register(ConversationMessageByDateActivity.class, (context, intent) -> {
            Conversation conversation = intent.getParcelableExtra("conversation");
            return conversation == null ? null : ConversationMessageByDateFragment.newInstance(conversation);
        }, intent -> conversationKey("messageByDate", intent.getParcelableExtra("conversation")));
        register(ConversationLinkRecordActivity.class, (context, intent) -> {
            Conversation conversation = intent.getParcelableExtra("conversation");
            return conversation == null ? null : ConversationLinkRecordFragment.newInstance(conversation);
        }, intent -> conversationKey("linkRecord", intent.getParcelableExtra("conversation")));
        register(ConversationMediaActivity.class, (context, intent) -> {
            Conversation conversation = intent.getParcelableExtra("conversation");
            return conversation == null ? null : ConversationMediaFragment.newInstance(conversation);
        }, intent -> conversationKey("media", intent.getParcelableExtra("conversation")));
        register(FileRecordActivity.class, (context, intent) -> FileRecordFragment.newInstance(
                intent.getParcelableExtra("conversation"),
                intent.getStringExtra("fromUser"),
                intent.getBooleanExtra("isMyFiles", false)),
            intent -> "fileRecord:" + intent.getBooleanExtra("isMyFiles", false)
                + ':' + intent.getStringExtra("fromUser")
                + ':' + conversationKey("c", intent.getParcelableExtra("conversation")));

        // 搜索一族，共用 SearchPageFragment。这几页 providesOwnToolbar()==true，
        // 右栏不会再给一条标题栏，顶部就是它们自己的「搜索框 + 取消」。
        // 都不去重：每次进来都该是一张空搜索框，退回上次的搜索结果反而是错的。
        register(SearchPortalActivity.class,
            (context, intent) -> SearchPortalPageFragment.fromIntent(intent));
        register(SearchUserActivity.class,
            (context, intent) -> SearchUserPageFragment.fromIntent(intent));
        register(SearchMessageActivity.class,
            (context, intent) -> SearchMessagePageFragment.fromIntent(intent));
        register(SearchChannelActivity.class,
            (context, intent) -> SearchChannelPageFragment.fromIntent(intent));
        // @群成员：需要回传选中的人，调用方用 WfcPageCompat.startPageForResult 打开
        register(MentionGroupMemberActivity.class,
            (context, intent) -> MentionGroupMemberPageFragment.fromIntent(intent));

        // 转发一族。
        // 转发页不去重：每次转发的是不同的消息，退回上一次那张选择列表是错的。
        register(ForwardActivity.class, (context, intent) -> ForwardPageFragment.fromIntent(intent));
        // 转发页里的「新建会话」，以及所有「选一个人/群作为会话对象」的入口。
        // 回传 userInfo / groupInfo，调用方用 WfcPageCompat.startPageForResult 打开。
        register(PickOrCreateConversationTargetActivity.class,
            (context, intent) -> PickOrCreateConversationTargetPageFragment.fromIntent(intent));
        // 挑一个已有会话，回传 conversationInfo
        register(PickConversationActivity.class,
            (context, intent) -> new PickConversationPageFragment());
        // 「我」→ 文件。全局唯一，栈内单例。
        register(FileRecordListActivity.class, (context, intent) -> new FileRecordListFragment(),
            intent -> FileRecordListActivity.class.getName());
        // 会议邀请（ConferenceInviteActivity）不注册：它的入口是全屏的会议界面，
        // 不存在「开在右栏」的场景。

        // 设置一族里住在 uikit 的这几页。都是全局唯一的页面，栈内单例。
        // App 自己的设置页（SettingActivity、AboutActivity……）在 chat 模块，
        // uikit 看不见它们，由 App 侧的 AppPaneRegistry 自行登记。
        register(PrivacySettingActivity.class, (context, intent) -> new PrivacySettingFragment(),
            intent -> PrivacySettingActivity.class.getName());
        register(PrivacyFindMeSettingActivity.class, (context, intent) -> new PrivacyFindMeSettingFragment(),
            intent -> PrivacyFindMeSettingActivity.class.getName());
        register(MessageNotifySettingActivity.class, (context, intent) -> new MessageNotifySettingFragment(),
            intent -> MessageNotifySettingActivity.class.getName());
        register(ChatSettingActivity.class, (context, intent) -> new ChatSettingFragment(),
            intent -> ChatSettingActivity.class.getName());
        register(FontSizeActivity.class, (context, intent) -> new FontSizeFragment(),
            intent -> FontSizeActivity.class.getName());

        // 改自己的昵称。不去重：每次进来都该按最新的资料重新填，与 SetAliasActivity 那一族一致。
        register(ChangeMyNameActivity.class, (context, intent) -> new ChangeMyNameFragment());
        // 发好友申请。不去重：申请对象不同，退回上一次那张申请表是错的。
        register(InviteFriendActivity.class, (context, intent) -> InviteFriendFragment.fromIntent(intent));

        // ==================== 已接入族的叶子页 ====================
        // 下面这些页面本身不带下级，但它们挂在已经进了右栏的页面上。少登记一个，
        // 用户就会在一条右栏路径走到一半时突然被弹到全屏，返回还回不到原来那条栈。

        // 选联系人（会话里发名片）。不去重：每次挑的对象不同，且要回传结果。
        register(ContactListActivity.class,
            (context, intent) -> ContactListFragment.newPickInstance(intent));

        // 频道详情（搜索结果点进来、扫频道码）。按频道去重，同一个频道不重复压栈。
        register(ChannelInfoActivity.class, (context, intent) -> ChannelInfoFragment.fromIntent(intent),
            PaneRegistry::channelInfoKey);
        // 创建频道。demo 里没有入口，是留给 aar 集成方的。不去重：是一张一次性表单。
        register(CreateChannelActivity.class, (context, intent) -> new CreateChannelFragment());

        // 互联域详情。按域去重。
        register(DomainInfoActivity.class, (context, intent) -> DomainInfoFragment.fromIntent(intent),
            intent -> {
                DomainInfo domainInfo = intent.getParcelableExtra("domainInfo");
                return domainInfo == null ? null : "domainInfo:" + domainInfo.domainId;
            });

        // 群禁言设置，挂在群管理下面。按群去重。
        register(GroupMuteOrAllowActivity.class,
            (context, intent) -> GroupMuteOrAllowFragment.fromIntent(intent),
            PaneRegistry::groupInfoKey);

        // 群消息已读回执，挂在会话里。不去重：每条消息一份回执。
        register(GroupMessageReceiptActivity.class,
            (context, intent) -> GroupMessageReceiptFragment.fromIntent(intent));

        // 合并转发消息详情，挂在会话和收藏里。不去重：合并消息可以层层嵌套，
        // 每一层都是不同的内容，去重会让内层那条打不开。
        register(CompositeMessageContentActivity.class,
            (context, intent) -> CompositeMessageContentFragment.fromIntent(intent));

        // 多端登录页，挂在会话列表顶部那条在线横幅上。按全部 clientId 拼 key 去重：
        // 同一组设备只保留一页，设备列表变化（踢掉一台）后 key 变化，重新开一页。
        register(PCSessionActivity.class, (context, intent) -> PCSessionFragment.fromIntent(intent),
            intent -> {
                ArrayList<PCOnlineInfo> infos = intent.getParcelableArrayListExtra("pcOnlineInfos");
                if (infos == null || infos.isEmpty()) {
                    return null;
                }
                StringBuilder sb = new StringBuilder("pcSession:");
                for (PCOnlineInfo info : infos) {
                    sb.append(info.getClientId()).append(',');
                }
                return sb.toString();
            });

        // 扫群码之后的入群落地页（不是群设置页，那个是 ConversationInfoActivity）。按群去重。
        register(GroupInfoActivity.class, (context, intent) -> GroupInfoFragment.fromIntent(intent),
            intent -> {
                String groupId = intent.getStringExtra("groupId");
                return groupId == null ? null : "groupInfo:" + groupId;
            });

        // ==================== 网页、二维码、选人 ====================

        // 内嵌网页。全仓库被引用最多的一页（链接消息、图文消息、用户协议、工作台 H5……），
        // 调用方几乎都已经在右栏里。不去重：同一个 url 可以同时开两份，且网页自己有前进后退栈。
        register(WfcWebViewActivity.class, (context, intent) -> WfcWebViewFragment.fromIntent(intent));

        // 二维码展示（我的 / 群 / 频道 / 会议）。按二维码内容去重，反复点只退回原来那一层。
        register(QRCodeActivity.class, (context, intent) -> QRCodeFragment.fromIntent(intent),
            intent -> {
                String qrCodeValue = intent.getStringExtra("qrCodeValue");
                return qrCodeValue == null ? null : "qrcode:" + qrCodeValue;
            });

        // 选联系人（可多选，工作台 H5 和文件记录在用）。不去重：每次挑的对象不同，且要回传结果。
        register(PickContactActivity.class,
            (context, intent) -> PickContactPageFragment.fromIntent(intent));

        // 在组织架构里选人，挂在「发起群聊」和「加群成员」里点部门那一下。
        // 不去重：每次带着当前已勾选的人进来，是一次性的选择流程，且要回传结果。
        register(PickOrganizationMemberActivity.class,
            (context, intent) -> PickOrganizationMemberPageFragment.fromIntent(intent));

        // ==================== 位置 ====================

        // 查看一条位置消息。按坐标去重：同一条位置消息连点两下不该叠出两张地图。
        // 不同的位置消息（坐标不同）各自是一页，返回能逐个退回去。
        register(ShowLocationActivity.class, (context, intent) -> ShowLocationPageFragment.fromIntent(intent),
            intent -> "location:" + intent.getDoubleExtra("Lat", 0) + "," + intent.getDoubleExtra("Long", 0));

        // 发送位置（会话加号面板 →「位置」）。不去重：是一次性的选择流程，且要回传结果。
        register(MyLocationActivity.class, (context, intent) -> MyLocationPageFragment.fromIntent(intent));

        // ==================== 相册 ====================

        // 会话「+」→「相册」，选图片/视频发送。不去重：是一次性的选择流程，且要回传结果。
        // 拍照、预览两个子流程仍然全屏——它们是 ImageGridFragment 内部自己的
        // Fragment.startActivityForResult，不经过右栏机制，落在哪个宿主里都会正确回调。
        register(ImageGridActivity.class, (context, intent) -> ImagePickerPanePageFragment.fromIntent(intent));
    }

    @Nullable
    private static String channelInfoKey(Intent intent) {
        ChannelInfo channelInfo = intent.getParcelableExtra("channelInfo");
        if (channelInfo != null) {
            return "channelInfo:" + channelInfo.channelId;
        }
        String channelId = intent.getStringExtra("channelId");
        return channelId == null ? null : "channelInfo:" + channelId;
    }

    // ==================== 用户资料（UserInfoActivity / EmployeeInfoActivity 共用） ====================

    @Nullable
    private static Fragment createUserInfoPage(Context context, Intent intent) {
        UserInfo userInfo = intent.getParcelableExtra("userInfo");
        if (userInfo == null) {
            return null;
        }
        return UserInfoFragment.newInstance(userInfo, intent.getStringExtra("groupId"),
            intent.getParcelableExtra("groupMemberSource"), null);
    }

    @Nullable
    private static String userInfoKey(Intent intent) {
        UserInfo userInfo = intent.getParcelableExtra("userInfo");
        return userInfo == null ? null : "userInfo:" + userInfo.uid + ':' + intent.getStringExtra("groupId");
    }

    @Nullable
    private static String groupInfoKey(Intent intent) {
        GroupInfo groupInfo = intent.getParcelableExtra("groupInfo");
        if (groupInfo == null || intent.getComponent() == null) {
            return null;
        }
        return intent.getComponent().getClassName() + ':' + groupInfo.target;
    }
}
