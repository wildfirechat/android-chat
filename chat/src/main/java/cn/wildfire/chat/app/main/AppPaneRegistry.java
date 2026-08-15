/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.main;

import cn.wildfire.chat.app.misc.DiagnoseActivity;
import cn.wildfire.chat.app.misc.DiagnoseFragment;
import cn.wildfire.chat.app.setting.AboutActivity;
import cn.wildfire.chat.app.setting.AboutFragment;
import cn.wildfire.chat.app.setting.AccountActivity;
import cn.wildfire.chat.app.setting.AccountFragment;
import cn.wildfire.chat.app.setting.ChangePasswordActivity;
import cn.wildfire.chat.app.setting.ChangePasswordFragment;
import cn.wildfire.chat.app.setting.ResetPasswordActivity;
import cn.wildfire.chat.app.setting.ResetPasswordFragment;
import cn.wildfire.chat.app.setting.SettingActivity;
import cn.wildfire.chat.app.setting.SettingFragment;
import cn.wildfire.chat.kit.pane.PaneRegistry;

/**
 * 本 App 自己那些页面的右栏登记表。
 * <p>
 * {@code PaneRegistry} 住在 uikit 里，看不见 chat 模块的类，所以 App 侧的页面只能反过来自己登记
 * ——这正是 {@code PaneRegistry.register} 是 public 的原因，把 uikit 当 aar 集成的第三方 App
 * 登记自己的页面走的也是这条路。启动时调一次 {@link #register()} 即可，见 {@code MyApp.onCreate}。
 * <p>
 * 没登记的页面不是错误：{@code TwoPaneNavigator} 找不到登记项时原样 {@code startActivity}，
 * 仍然是全屏页面。登录、闪屏、备份恢复这些<strong>本来就不该出现在右栏</strong>的页面永远不要登记。
 */
public final class AppPaneRegistry {

    private AppPaneRegistry() {
    }

    public static void register() {
        // 「我 → 设置」这棵子树。都是全局唯一的页面，栈内单例：反复点进来只退回原来那一层，
        // 不会在右栏叠出一摞设置页。
        PaneRegistry.register(SettingActivity.class, (context, intent) -> new SettingFragment(),
            intent -> SettingActivity.class.getName());
        PaneRegistry.register(AboutActivity.class, (context, intent) -> new AboutFragment(),
            intent -> AboutActivity.class.getName());
        PaneRegistry.register(DiagnoseActivity.class, (context, intent) -> new DiagnoseFragment(),
            intent -> DiagnoseActivity.class.getName());

        // 「我 → 账号与安全」及其下的两个改密页
        PaneRegistry.register(AccountActivity.class, (context, intent) -> new AccountFragment(),
            intent -> AccountActivity.class.getName());
        PaneRegistry.register(ChangePasswordActivity.class,
            (context, intent) -> new ChangePasswordFragment(),
            intent -> ChangePasswordActivity.class.getName());
        // 重置密码不去重：登录页「忘记密码」会带 resetCode 进来，与设置里进来的是两种不同的填法
        PaneRegistry.register(ResetPasswordActivity.class,
            (context, intent) -> ResetPasswordFragment.fromIntent(intent));

        // 登录、闪屏、用户协议、备份与恢复、PC 登录确认都不登记：
        // 它们要么发生在双栏主界面存在之前，要么是自带多步进度的独立流程，全屏才是对的。
    }
}
