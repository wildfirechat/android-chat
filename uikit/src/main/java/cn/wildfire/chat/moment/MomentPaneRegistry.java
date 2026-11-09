package cn.wildfire.chat.moment;

import cn.wildfire.chat.kit.pane.PaneRegistry;
import cn.wildfirechat.model.UserInfo;

/**
 * 朋友圈五个页面在平板右栏的登记项。
 * <p>
 * <strong>为什么不直接写进 uikit 的 {@code PaneRegistry}</strong>：朋友圈是一份可选的源码目录
 * （{@code uikit/build.gradle} 里 {@code // moment start ... // moment end} 之间那两行
 * {@code srcDirs}），不集成朋友圈的项目会把它整段删掉。{@code PaneRegistry} 一旦 import
 * {@code cn.wildfire.chat.moment.*}，uikit 在那些项目里就编译不过了。所以登记项住在朋友圈自己这边，
 * 由 {@code PaneRegistry} 反射调用 {@link #register()} —— 找不到本类就当没有朋友圈，
 * 它的页面照旧全屏打开。
 */
public final class MomentPaneRegistry {

    private MomentPaneRegistry() {
    }

    public static void register() {
        // 朋友圈主页。按「看谁的朋友圈」去重：同一个人的朋友圈连点两下不该叠两层，
        // 但从张三的朋友圈点到李四的，那是新的一页，返回能逐层退回去。
        // key 里的 self 是「我自己的朋友圈」——它和别人的是两个不同的页面。
        PaneRegistry.register(FeedListActivity.class,
            (context, intent) -> FeedListPageFragment.fromIntent(intent),
            intent -> {
                UserInfo user = intent.getParcelableExtra(BaseFeedPageFragment.ARG_USER_INFO);
                return "moment:" + (user == null ? "self" : user.uid);
            });

        // 单条详情。按 feedId 去重。
        PaneRegistry.register(FeedDetailActivity.class,
            (context, intent) -> FeedDetailPageFragment.fromIntent(intent),
            intent -> "momentFeed:" + intent.getLongExtra("feedId", 0));

        // 朋友圈消息列表。全局唯一，栈内单例。
        PaneRegistry.register(FeedMessageActivity.class,
            (context, intent) -> FeedMessagePageFragment.fromIntent(intent),
            intent -> FeedMessageActivity.class.getName());

        // 发表。不去重：是一次性的编辑流程，且要回传结果，
        // 调用方（朋友圈主页）用 WfcPageCompat.startPageForResult 打开。
        PaneRegistry.register(PublishFeedActivity.class,
            (context, intent) -> PublishFeedPageFragment.fromIntent(intent));

        // 「谁可以看」。同样不去重、要回传结果，调用方是发表页。
        PaneRegistry.register(FeedVisibleScopeActivity.class,
            (context, intent) -> FeedVisibleScopePageFragment.fromIntent(intent));
    }
}
