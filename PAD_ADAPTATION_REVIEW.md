# Pad 适配方案 Review

整体印象：方案文档 `PAD_ADAPTATION_PLAN.md` 非常完整，分层（横屏 → 宽屏 → 双栏 → 路由 → 导航栈）清晰，`isTwoPaneLayout / isPadDevice` 的区分、`WfcPage / WfcPageHost / WfcPageCompat` 的抽象方向都是对的。代码里也处理了很多容易踩的 Fragment 事务、`setMaxLifecycle`、返回栈恢复等坑。

但当前实现仍有一些**必须先处理的阻塞问题**，尤其是工作区里有一批未完成的半成品改动，直接会导致现有页面崩溃。

---

## P0：当前工作区存在会导致崩溃/无法构建的未完成改动

`git status` 里有一批布局被删掉了 `<include layout="@layout/toolbar"/>`，但对应的 Activity **还没有改成壳 Activity + `fragment_container_activity`**，这些 Activity 仍直接 inflate 这些布局。

受影响的布局和 Activity：

| 布局 | 仍直接使用它的 Activity |
|---|---|
| `channel_create_fragment.xml` | `CreateChannelActivity` |
| `channel_info_activity.xml` | `ChannelInfoActivity` |
| `composite_message_activity.xml` | `CompositeMessageContentActivity` |
| `conversation_receipt_activity.xml` | `GroupMessageReceiptActivity` |
| `domain_info_activity.xml` | `DomainInfoActivity` |
| `group_info_activity.xml` | `GroupInfoActivity` |
| `group_manage_mute_activity.xml` | `GroupMuteOrAllowActivity` |
| `pc_session_activity.xml` | `PCSessionActivity` |

这些 Activity 都继承 `WfcBaseActivity`，`WfcBaseActivity.bindViews()` 会找 `R.id.toolbar` / `R.id.appbar`，后面还会调用 `toolbar.setBackgroundResource(...)`。布局里没有 toolbar 后，**运行时必然 NPE**。

另外 `settings.gradle` 里出现了两行 `':pttclient'`，这是明显的合并错误，也可能导致 Gradle 报重复 include。这些本地依赖/朋友圈开关改动如果不需要随 Pad 方案提交，建议先还原。

> 结论：提交前必须把上述 Activity 全部壳化并注册到 `PaneRegistry`，或者恢复这些布局里的 toolbar；否则当前工作区不可用。

---

## P1：已接入右栏的页面仍存在“Activity 级操作”漏网

这几个是静态搜索到的真实问题，平板右栏里会表现成标题写错地方、甚至整个主界面被 finish。

### 1. `GroupMemberSearchModule` 会直接 finish 整个主界面

`uikit/.../conversation/mention/GroupMemberSearchModule.java:50-51`

```java
fragment.getActivity().setResult(Activity.RESULT_OK, intent);
fragment.getActivity().finish();
```

这个模块用于右栏的 `@` 选人搜索页。手机端没问题，但右栏里 `getActivity()` 是 `MainActivity`，`finish()` 会直接退出整个 App。

应该改成：

```java
WfcPageCompat.setPageResult(fragment, Activity.RESULT_OK, intent);
WfcPageCompat.finishPage(fragment);
```

或者复用 `MentionGroupMemberFragment.finishWithResult()` 的写法。

### 2. 组织架构列表会把标题写到左栏

`OrganizationMemberListFragment.java:107`

```java
getActivity().setTitle(organizationEx.organization.name);
```

该页面已注册进右栏，但右栏的标题应该写到 `PanePageFragment` 自己的 toolbar，而不是 `MainActivity` 的 ActionBar。应改成：

```java
WfcPageCompat.setPageTitle(this, organizationEx.organization.name);
```

### 3. 频道详情/聊天室详情同样会把标题写到左栏

- `ChannelConversationInfoFragment.java:78`
- `ChatRoomConversationInfoFragment.java:61`

```java
getActivity().setTitle(getString(R.string.channel_details));
```

这两个 Fragment 已通过 `ConversationInfoActivity` 进入右栏，同样需要改用 `WfcPageCompat.setPageTitle()`。

---

## P1：`ConversationActivity` 配置变化会重复添加 Fragment

`ConversationActivity.afterViews()` 目前是无条件 `add`：

```java
conversationFragment = new ConversationFragment();
getSupportFragmentManager().beginTransaction()
    .add(R.id.containerFrameLayout, conversationFragment, "content")
    .commit();
```

手机端锁竖屏所以很少暴露；但 Pad 解锁横屏后，`ConversationActivity` 一旦旋转/分屏重建，FragmentManager 会恢复旧的 `ConversationFragment`，这里又会 add 一个新的，导致两个会话页叠在一起。

建议改成先检查：

```java
if (getSupportFragmentManager().findFragmentById(R.id.containerFrameLayout) != null) {
    return;
}
```

同样的“`afterViews` 里无条件 `replace/add`”模式在仓库里还有不少，Pad 解锁旋转后会集中暴露状态丢失/重复添加。建议把 Pad 可旋转页面统一过一遍这个模式。

---

## P2：`isPadDevice` 用于横屏解锁，折叠屏逻辑有隐患

当前 `WfcBaseActivity` / `WfcBaseNoToolbarActivity` / `SplashActivity` 都用：

```java
WfcDeviceUtils.isLandscapeAllowed(this)
```

而它等价于 `isPadDevice()`，首次启动时持久化。

这会导致：

- 折叠屏**首次展开态**启动 → `isPadDevice() == true` → 折叠成手机后仍允许横屏，违反“折叠屏折叠态手机端不回归”的硬约束；
- 折叠屏**首次折叠态**启动 → `isPadDevice() == false` → 展开成平板后仍然锁竖屏，Pad 体验失效。

`isPadDevice()` 适合做 platform 身份（稳定、不随窗口变化），但**横屏解锁应该跟随当前窗口/设备形态动态判断**。建议 `isLandscapeAllowed()` 改用类似 `isTwoPaneLayout()` 的窗口维度判断，或者单独引入一个动态的大屏判定，避免和登录 platform 绑定。

---

## P2：冷启动/旋转时的一些状态丢失风险

### 1. `TwoPaneNavigator.pendingIntent` 没有保存

通知冷启动时，如果 IM 未就绪，`pendingIntent` 先暂存；但 `onSaveInstanceState` 没有保存它。此时如果用户旋转，Activity 重建后 `pendingIntent` 丢失，通知点击可能失效。

### 2. `openInPane()` 在 IM 未就绪时只验证“已注册”，没有验证“工厂是否真的能创建”

```java
if (!PaneRegistry.isRegistered(intent)) return false;
if (!imServiceReady) {
    pendingIntent = intent;
    return true;
}
```

对于 `GroupListActivity`、`ChannelListActivity`、`OrganizationMemberListActivity` 这类“带 pick 模式时工厂返回 null”的页面，冷启动阶段会先返回 true 并缓存，但之后 `openInTab()` 才发现不能进右栏，此时原始跳转已经被吞掉。建议未就绪时也先做一次 `PaneRegistry.createPage()` 校验，或 flush 失败后回退原 `startActivity`。

---

## 建议/后续

1. **右栏导航调用尽量收敛到 `WfcPageCompat.startPage/startPageForResult`**。目前很多左栏/页面内导航仍靠 `startActivity` + `lastTouchInPane` 兜底，虽然大部分点击场景可用，但异步回调、菜单、键盘触发的导航仍可能压栈/换内容判断错。
2. **统一处理 `afterViews` 的 Fragment 恢复**。Pad 旋转是这次新增的真实场景，不能只靠“手机锁竖屏所以没暴露”来忽略。
3. **未提交的 `CreateChannelFragment` / `ChannelInfoFragment` 还没有注册进 `PaneRegistry`**，也没有替换旧 Activity。如果目标是让频道创建/频道详情进右栏，需要把 Activity 壳化并补注册；如果暂不做，就不要改对应布局。
4. **手机端回归范围需要覆盖所有新实现 `WfcPage` 的页面**。`WfcBaseActivity` 全局接了 `WfcPage` 的菜单/标题/返回委托，虽然设计上等价，但毕竟是 85 个 Activity 的公共基类，建议按计划中的 6.1 清单逐项真机回归。

---

## 总结

方案架构是好的，但当前状态还不能算“可合入”：

- **必须先修**：未完成布局改动导致的 NPE、`GroupMemberSearchModule` finish 主界面、`ConversationActivity` 重复 Fragment、标题写到左栏。
- **需要决策**：折叠屏横屏策略、冷启动 pending intent 恢复。
- **建议补强**：全量 `afterViews` 旋转恢复审查、右栏导航调用点收敛。

如果先把 P0/P1 清掉，再跑一轮手机端和 Pad 真机回归，这套方案的基础是值得继续推进的。
