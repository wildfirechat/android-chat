# Android 平板（Pad）适配调研与实施计划

> 目标：让本 App 在 Android 平板上具备原生平板体验（横屏、宽屏布局、会话双栏、平板身份多端在线）。
> **硬约束：手机端的功能与交互保持 100% 不变。**

---

## 一、约束与总原则

### 1.1 硬约束

手机端（含折叠屏折叠态、小屏设备）的**任何**页面、交互、动效、跳转路径都不允许发生变化。

### 1.2 保证手机端零回归的四条纪律

所有改动必须满足以下四条中的至少一条，否则不允许合入：

| # | 纪律 | 落地方式 |
|---|------|----------|
| D1 | **资源隔离** | 平板专属布局/尺寸只放进 `-sw600dp` 限定目录，默认 `res/layout`、`res/values` **零改动**。手机永远命中默认资源。 |
| D2 | **开关隔离** | 平板专属代码路径一律由 `WfcDeviceUtils.isTwoPaneLayout(context)` 包裹，该方法在手机上恒返回 `false`，`else` 分支保留**原样代码**。 |
| D3 | **行为等价重构** | 必须动的公共代码（如 `WfcBaseActivity`、`ConversationFragment`）只做"提取接口 / 抽出方法"式重构，手机端仍走原实现类，重构前后手机端字节级行为一致。 |
| D4 | **新增而非修改** | 平板的双栏宿主是**新增** Activity（`MainPadActivity`）/新增布局，`MainActivity`、`ConversationActivity` 在手机上的代码路径保持不变。 |

### 1.3 两个"是不是平板"的判定必须区分开

这是本次适配最容易出错的地方，务必分清：

| 判定 | 用途 | 依据 | 特点 |
|------|------|------|------|
| `isTwoPaneLayout()` | **布局决策**（是否双栏、是否用宽屏资源） | `res/values/bools.xml` + `res/values-sw600dp/bools.xml` 里的 `wfc_two_pane` | 跟随资源限定符，**随分屏/折叠实时变化**，与 `-sw600dp` 布局天然同步 |
| `isPadDevice()` | **平台身份决策**（登录时 platform 是否上报 9=APad） | `Configuration.smallestScreenWidthDp >= 600`，**首次启动时判定并持久化** | 必须**稳定不变**，因为 token 与 platform 绑定，中途变化会导致连接失败 |

> 反例警告：不要用 `isPadDevice()` 决定布局（分屏时窗口很窄仍会走双栏，布局错乱）；也不要用 `isTwoPaneLayout()` 决定登录 platform（用户拖动分屏就会改变平台身份，token 失效）。

---

## 二、现状调研结论

### 2.1 全局阻塞点（不解决则平板体验无从谈起）

| # | 问题 | 位置 | 影响 |
|---|------|------|------|
| B1 | **代码级强制竖屏** | `uikit/.../WfcBaseActivity.java:46`<br>`uikit/.../WfcBaseNoToolbarActivity.java:34`<br>`this.setRequestedOrientation(SCREEN_ORIENTATION_PORTRAIT)` | 全仓库 **85 个 Activity** 继承这两个基类，平板上全部被锁竖屏 |
| B2 | **Manifest 级强制竖屏** | `chat` 13 处、`uikit` 22 处、`push` 1 处，共 **36 处** `android:screenOrientation="portrait"` | 即使解掉 B1，这些页面仍锁竖屏 |
| B3 | **摄像头被声明为必需** | `chat/src/main/AndroidManifest.xml:26-27`<br>`uikit/src/main/AndroidManifest.xml:21-22`<br>`<uses-feature android:name="android.hardware.camera" />` 与 `camera.autofocus` 均未写 `required="false"` | 应用市场会把**无摄像头/无自动对焦的平板过滤掉**，装都装不上 |
| B4 | **完全没有宽屏资源** | 全仓库 `values-sw*dp`、`layout-sw*dp`、`layout-land` 目录数量为 **0** | 平板上就是一个被拉伸的手机界面 |
| B5 | **没有 configChanges** | 仅 `imagepicker` 3 个 Activity 声明了 `orientation|screenSize` | 解锁横屏后旋转会重建 Activity，状态丢失问题会集中暴露 |
| B6 | **朋友圈走独立基类，当前已可横屏** | `../android-momentkit`（sibling 仓库，经 `uikit/build.gradle` 的 `sourceSets` 引入）8 个 Activity 继承 `BaseTitleBarActivity → BaseStatusControlActivity → AppCompatActivity`，**不经过 `WfcBaseActivity`**，manifest 中也无 `screenOrientation` | ①这 8 个页面**在手机上现在就能横屏**（既有行为，非本次引入），回归基线必须如实记录，否则阶段 1 会误判为回归；②平板横屏解锁需单独覆盖这条链路，且改动要提交到**另一个仓库** |

> 好消息：`chat/src/main/AndroidManifest.xml:59` 已有 `android:resizeableActivity="true"`，分屏/自由窗口的基础具备。

### 2.2 协议层：SDK 已内置 Pad 平台能力

调研中最重要的发现——**不需要动 `mars-core-release.aar`，Pad 身份链路已经打通**：

| 能力 | 位置 | 说明 |
|------|------|------|
| 平台设置 API | `client/.../remote/ChatManager.java:1297` `setPlatform(boolean isPad)` | 需在 `connect()` **之前**调用 |
| 平台枚举 | `client/.../client/Platform.java` | `PlatformType_APad(9)`，`getPlatFormName()` 返回"Android 平板" |
| 设备上报 | `client/.../client/ClientService.java:4796-4800` | `isPad` 为 true 时 `info.platform = 9`，否则 2 |
| 登录透传 | `chat/.../app/AppService.java:205` | 已有注释「如果是 android pad，需要设置为 pad 类型」，且 `passwordLogin` 已用 `ChatManager.Instance().getPlatform().value()` 作为登录 platform 参数 |
| 多端在线模型 | `client/.../model/PCOnlineInfo.java` `PCOnlineType.Pad_Online` | 已定义 |
| 多端在线 UI | `uikit/.../conversationlist/notification/viewholder/PCOnlineNotificationViewHolder.java:45` | 已处理 `Pad_Online` 分支 |

**含义**：平板可以作为独立端（platform=9）与手机**同时在线**，而不是把手机踢下线。

**约束**（必须写进阶段 6 的前置条件）：
- token 与 `clientId` + `platform` 强绑定，**平台身份变更必须重新登录**；
- 需要 app server 的 `/login_pwd`、`/login`（短信）接口接受 `platform=9` 并签发对应 token；
- 需要 IM server 授权支持多端在线（专业版能力，需与后端确认）。

### 2.3 主界面与会话页结构

```
MainActivity (chat/.../main/MainActivity.java)
  └─ main_activity.xml: [toolbar] + [ViewPager2 contentViewPager] + [BottomNavigationView]
       ├─ ConversationListFragment   (uikit)
       ├─ ContactListFragment        (uikit)
       ├─ WebViewFragment            (workspace，可选，见 showWorkSpace())
       ├─ DiscoveryFragment          (chat)
       └─ MeFragment                 (chat)

ConversationActivity (uikit/.../conversation/ConversationActivity.java)
  └─ fragment_container_activity.xml: [toolbar] + [FrameLayout containerFrameLayout]
       └─ ConversationFragment (1696 行)
```

点击会话的跳转在 `MainActivity.java:462-466`：`startActivity(new Intent(this, ConversationActivity.class))`。

### 2.4 `ConversationFragment` 与宿主 Activity 的耦合点（双栏改造必须先解开）

`ConversationFragment` 目前假设"我独占一个 Activity"，共 6 处硬耦合：

| # | 位置 | 耦合内容 | 双栏下的问题 |
|---|------|----------|--------------|
| C1 | `:1038` `setActivityTitle()` | `(WfcBaseActivity) getActivity()` 强转，调 `activity.setTitle()` / `activity.getToolbar().setSubtitle()` | 双栏宿主的 toolbar 是主界面 toolbar，会话标题会写到全局标题栏 |
| C2 | `:1070` `applyTitleWithIcons()` | 直接操作 `activity.getToolbar()` 内部 TextView（静音/听筒图标） | 同上 |
| C3 | `:574` `checkAndHighlightMessage()` | `getActivity().getIntent().getLongExtra("highlightMessageId", 0)` | 双栏宿主的 Intent 里没有这个 extra |
| C4 | `:938`、`:1022` | `getActivity().finish()`（聊天室加入失败、密聊信息缺失） | 会把整个主界面关掉 |
| C5 | `ConversationActivity.java:55` `menu()` + `:64` `onOptionsItemSelected` | 「会话信息」菜单在 Activity 上，不在 Fragment 上 | 右栏切换会话时菜单不跟随 |
| C6 | `:1323` `onBackPressed()` 包级可见 | 由 `ConversationActivity.onBackPressed()` 调用 | 双栏宿主需要同样的返回拦截链 |

### 2.5 像素/屏幕尺寸耦合点清单

以下代码用**整块屏幕**尺寸算布局，在宽屏或双栏（窗口 ≠ 屏幕）下会出错：

| 位置 | 问题 | 宽屏后的表现 |
|------|------|--------------|
| `ConversationFragment.java:1573` | 多选操作栏按钮宽度 = `displayMetrics.widthPixels / actions.size()` | 双栏下按钮按整屏宽平分，溢出右栏 |
| `ConversationInputPanel.java:1018` | 图片推荐 popup 的 x = `displayMetrics.widthPixels - popupWidth - 8dp` | popup 贴到屏幕最右，脱离右栏 |
| `KeyboardAwareLinearLayout.java:254-256` | `getDeviceRotation()` 用 real display metrics 判断横竖屏 | 分屏/双栏时窗口是竖的但设备是横的，键盘高度缓存串 key（`keyboard_height_landscape` / `_portrait`） |
| `widget/selecttext/SelectUtils.java:187,191` | `Resources.getSystem().getDisplayMetrics()`（系统级，忽略多窗口） | 文本选择菜单定位偏移 |
| `menu/.../Display.java:15-16` | `getScreenMetrics()` 用整屏宽高，`PopupMenu` 据此定位 | 主界面「+」菜单在平板上定位异常 |
| `imagepicker/.../activity_image_grid.xml:23` | `numColumns="3"` 写死 | 平板上图片巨大，一屏放不下几张 |
| `conversation_ext_layout.xml` | 固定 8 格（`container_0`..`container_7`）等分 `match_parent` | 宽屏下扩展面板图标被拉得极稀疏 |
| `group_member_list.xml:20`、`conversation_info_group_fragment.xml:40`、`conversation_info_single_fragment.xml:29`、`conversation_receipt_fragment.xml:19`、`GroupMemberListFragment.java:61` | 头像宫格 `spanCount="5"` 写死 | 平板上头像过大、留白过多 |
| 各气泡布局 `maxWidth="240dp/250dp/200dp"` | 固定上限 | 本身**不算 bug**（气泡不该无限拉长），但宽栏下左右两侧留白巨大，需在列表层加内容区最大宽度 |

### 2.6 平板硬件差异

| 差异 | 涉及代码 | 处理方向 |
|------|----------|----------|
| 多数平板**无听筒、无距离传感器** | `audio/AudioPlayModeUtils.java`（听筒播放开关）、`audio/AudioPlayManager.java:228`（距离传感器息屏） | 平板上隐藏「听筒播放」设置项；`AudioPlayManager` 在无 `TYPE_PROXIMITY` 时跳过注册 |
| 无 SIM / telephony | `READ_PHONE_STATE` 权限、短信登录 | 短信登录本身走服务端，可用；权限申请需容错 |
| 摄像头方向与数量差异 | `cameraview`、`mm/TakePhotoActivity`（锁竖屏） | 解锁横屏后需实测预览方向 |
| 无摄像头机型 | B3 的 `uses-feature` | 改 `required="false"` + 运行时判断 |

---

## 三、总体方案：分 5 层，逐层可独立上线与回退

```
L4  平板身份（platform=9，与手机多端同时在线）        ← 依赖后端，可最后做/可不做
L3  会话双栏（左列表 + 右会话）                        ← 平板体验的核心
L2  宽屏资源适配（-sw600dp 布局/尺寸/宫格列数）        ← 收益/风险比最高
L1  横屏解锁 + 旋转不崩不丢状态                        ← 平板可用性的地基
L0  设备判定、构建与回归基线                            ← 前置
```

每层结束都是一个**可发布、可回退**的状态。若中途叫停，已完成的层不影响手机端。

---

## 四、分阶段实施计划

### 阶段 0：基础设施与回归基线

**目标**：建立开关、建立"手机端没变"的可验证基线。

**改动**

1. 新增 `uikit/src/main/res/values/bools.xml`：
   ```xml
   <bool name="wfc_two_pane">false</bool>
   ```
   新增 `uikit/src/main/res/values-sw600dp/bools.xml`：
   ```xml
   <bool name="wfc_two_pane">true</bool>
   ```

2. 新增 `uikit/src/main/java/cn/wildfire/chat/kit/utils/WfcDeviceUtils.java`：
   - `isTwoPaneLayout(Context)` → 读 `R.bool.wfc_two_pane`（布局决策，随窗口变化）
   - `isPadDevice(Context)` → 首启时按 `Configuration.smallestScreenWidthDp >= 600` 判定并写入 SP（平台身份，稳定不变）
   - `isLandscapeAllowed(Context)` → 供基类使用，等价于 `isPadDevice()`

3. 建立回归基线：在手机上录制/截图现有关键路径（见第六章手机必测清单），作为每阶段结束的比对基准。

**手机端保证**：本阶段不改任何既有代码，纯新增。

**验收**：手机 `isTwoPaneLayout()` == false；平板 == true；分屏窄窗口下平板也返回 false。

**工作量**：0.5 人日

---

### 阶段 1：横屏解锁 ✅ 代码已完成，待真机回归

**目标**：平板可横屏，旋转不崩溃、不丢状态；手机仍恒定竖屏。

**实施记录（与原计划的差异）**

- 解锁 31 个页面，**保留 5 个**竖屏锁：`SingleCallActivity`、`MultiCallActivity`（阶段 7）、`TakePhotoActivity`（阶段 7 相机方向）、`ShowLocationActivity`（地图页，横屏适配工作量另计）、push SDK 内的 `LinkProxyClientActivity`（三方 AAR，无源码）。
- 逐个核查了 36 个锁竖屏页面的基类，发现 **5 个不继承 `WfcBase*`**，删掉 manifest 锁后手机端会意外可横屏。其中 `SplashActivity`（`AppCompatActivity`）位于登录必经路径，已在其 `onCreate` 中补上与基类一致的条件锁定；其余 4 个正是上面保留竖屏的页面。
- **manifest merger 坑 1**：`avenginekit`（sibling 仓库 `../android-av`）声明了 `hardware.camera` / `camera.autofocus` 且未指定 `required`（默认 true），合并时 true 胜出。app 层只写 `required="false"` 无效，必须加 `tools:replace="android:required"`。
- **manifest merger 坑 2**：只要还有页面锁 portrait，aapt 就会隐式加上 `required=true` 的 `android.hardware.screen.portrait`，导致只支持横屏的设备被市场过滤。已显式声明 `screen.portrait` / `screen.landscape` 均为非必需。
- 朋友圈（B6）**本阶段无需改动**：其 8 个页面本就未锁竖屏，平板上已可横屏，手机端行为也未发生任何变化。宽屏布局适配留到阶段 2。

**改动**

1. `WfcBaseActivity.java:46` 与 `WfcBaseNoToolbarActivity.java:34` 改为条件锁定：
   ```java
   // 手机端行为与改动前完全一致：仍然强制竖屏
   if (WfcDeviceUtils.isPadDevice(this)) {
       setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
   } else {
       setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
   }
   ```

2. 36 处 manifest 的 `android:screenOrientation="portrait"`：**全部删除**，改由基类统一控制。
   - 例外：需要确认后再决定的 3 类页面，先保留竖屏锁，放到阶段 7 处理：
     - `SingleCallActivity`、`MultiCallActivity`（uikit manifest:99、258）
     - `TakePhotoActivity`（uikit manifest:74）+ `cameraview` 相关
     - `push` 模块内的 1 处（第三方推送落地页）

3. `uses-feature` 修正（B3）：`chat` 与 `uikit` 两个 manifest 中的 `android.hardware.camera`、`android.hardware.camera.autofocus` 加 `android:required="false"`，并在 app 层加 `tools:replace="android:required"` 覆盖 `avenginekit` 的默认值；同时显式声明 `screen.portrait` / `screen.landscape` 为非必需。

   > 验收方式：`aapt2 dump badging <apk> | grep feature`，这 4 项都必须出现在 `uses-feature-not-required` 中。

4. **朋友圈链路（B6）**：`../android-momentkit` 的 `BaseStatusControlActivity` 是这 8 个页面的统一基类，在其 `onCreate` 中加入与 `WfcBaseActivity` 相同的条件锁定逻辑（手机锁竖屏、平板放开）。
   - 注意：①改动落在**另一个仓库**，需同步提交与版本对齐；②该模块当前**无任何竖屏锁**，加上条件锁定后手机端会从「可横屏」变为「锁竖屏」——这是与其余 85 个页面对齐、消除既有不一致，但**属于手机端可见变化，需产品确认**；若不希望动，就在该基类中只对平板生效、手机保持现状（推荐，严格满足硬约束）。

5. **登录链路宽屏布局**（本属阶段 2，因不适配就无法登录、进而无法验证其余页面，故提前到本阶段）：新增 `chat/src/main/res/layout-sw600dp/` 下的 `login_activity_password.xml`、`login_activity_sms.xml`、`agreement_activity.xml`、`activity_splash.xml`。
   - 两个核心问题：原布局**没有 ScrollView**，横屏下可用高度骤减、键盘弹出会遮挡输入框和登录按钮；输入框 `match_parent` 会被拉伸到整个平板宽度。
   - 做法：外层包 `ScrollView`（`fillViewport=true`），表单用 `@dimen/wfc_form_max_width`（400dp）限宽居中，协议卡片用 `@dimen/wfc_card_max_width`（560dp）。控件 id 与手机版**完全一致**，`LoginActivity` / `SMSLoginActivity` / `AgreementActivity` 三个类**零改动**。
   - 手机端保证：新增的是 `-sw600dp` 限定资源，手机永远命中默认布局（纪律 D1），默认 `layout/` 目录零改动。

6. 旋转状态排查：解锁后逐页跑一遍，重点修 Activity 重建后状态丢失（列表位置、输入框草稿、已选中项）。
   - 原则：**优先用 `onSaveInstanceState` 修复，不要用 `configChanges` 绕过**，因为双栏阶段还要处理折叠/分屏，屏蔽重建会掩盖问题。

**手机端保证**：D2（`isPadDevice()` 在手机恒 false，走原 `SCREEN_ORIENTATION_PORTRAIT` 分支）。删 manifest 里的 `portrait` 不影响手机——基类已经锁死。`uses-feature` 只放宽安装条件，不改运行时行为。

**验收**
- 手机：全量页面仍无法横屏（把设备旋转 90°，界面不动）；
- 平板：全部页面可横屏，旋转后无崩溃、无白屏、无状态丢失；
- 无摄像头平板可正常安装。

**风险**：85 个 Activity 一次性解锁横屏，横屏下的布局挤压问题会集中暴露。缓解：本阶段只保证"不崩不丢"，横屏下**好不好看**留到阶段 2。

**工作量**：3~5 人日（主要在旋转回归）

---

### 阶段 2：宽屏资源适配（`-sw600dp`）✅ 代码已完成，待真机回归

**目标**：宽屏下不再是"拉伸的手机界面"。全部通过新增限定符资源实现，**不碰默认资源**。

**改动**（全部为新增 `-sw600dp` 资源目录下的文件）

| 项 | 做法 |
|----|------|
| 内容区最大宽度 | 新增 `values/dimens.xml` + `values-sw600dp/dimens.xml` 定义 `wfc_content_max_width`（手机 = `match_parent` 语义，平板 = 如 720dp），会话消息列表、设置类页面、表单类页面居中约束 |
| 头像宫格列数 | `values/integers.xml` `wfc_member_grid_span=5` / `values-sw600dp` = 8；改 `GroupMemberListFragment.java:61` 与 4 个布局的 `spanCount` 引用该 integer |
| 图片选择器 | 新增 `imagepicker/src/main/res/layout-sw600dp/activity_image_grid.xml`，`numColumns` 引用 integer（手机 3 / 平板 5） |
| 会话扩展面板 | 新增 `layout-sw600dp/conversation_ext_layout.xml`，给 8 格容器加最大宽度并居中，避免图标被拉稀疏 |
| 列表项高度/内边距 | `values-sw600dp/dimens.xml` 适度放大 |
| 底部导航 | 平板横屏下底部导航过宽，`values-sw600dp` 中限制其最大宽度并居中（阶段 4 会被侧边栏取代，此处为过渡） |

**同时修复 2.5 节的像素耦合**（这些是 bug 修复，手机端行为不变）：
- `ConversationFragment.java:1573`：`widthPixels` → 改用 `multiMessageActionContainerLinearLayout` 的实际宽度（`post {}` 里取 measured width）；
- `ConversationInputPanel.java:1018`：popup 定位改为基于 `extImageView` 的 `getLocationOnScreen` + 自身所在窗口宽度，不用屏幕宽；
- `menu/.../Display.java`：`PopupMenu` 定位改用锚点视图所在窗口尺寸；
- `KeyboardAwareLinearLayout.java:254`：`getDeviceRotation()` 改为按**自身视图宽高**判断横竖，而非设备 real metrics；
- `SelectUtils.java:187,191`：`Resources.getSystem()` → 改用传入 View 的 `context.getResources()`。

> 这 5 处在手机全屏场景下"窗口 == 屏幕"，修复前后取值相同，因此手机端行为等价（D3）。需在手机上逐项回归验证。

**验收**
- 手机：所有页面像素级与基线一致（截图比对）；上述 5 处交互（多选栏、图片推荐 popup、主界面「+」菜单、表情键盘高度、文本选择菜单）表现与改前完全一致；
- 平板：横竖屏下无异常拉伸，图片选择器、成员宫格、扩展面板排布合理。

**工作量**：5~8 人日

#### 实施记录

**对本节原计划的三处修正**

1. **`spanCount` 只改布局没用。** `group_member_list.xml` 等 4 个布局虽然写了 `app:spanCount="5"`，但 `GroupMemberListFragment`、`GroupConversationInfoFragment`、`SingleConversationInfoFragment` 都在 Java 里 `setLayoutManager(new GridLayoutManager(ctx, 5))`，**运行时覆盖了 XML**。原计划只点了 `GroupMemberListFragment.java:61` 一处，实际有 3 处，XML 与 Java 必须同改，否则宫格列数纹丝不动。
2. **`menu/.../Display.java` 不是问题，未改。** `getScreenMetrics(Context)` 走的是传入 Context 的 `Resources`，而全部 5 个调用点传的都是 Activity（或 Fragment 的 Activity）Context —— API 24 起 Activity 的 Resources 已按窗口边界配置，多窗口下拿到的本就是窗口尺寸。且 `PopupMenu` 用 `getLocationInWindow` 定位，以窗口为界本身就是对的。
3. **`SelectUtils.getDisplayWidth/getDisplayHeight` 是死代码，未改。** 全仓库零调用方。因 uikit 可作为 AAR 被外部集成，未删除，仅加 `@Deprecated` 说明其忽略窗口边界，引导后续改用锚点 View 自身尺寸。

> 结论：2.5 节列的 5 处像素耦合，**真正需要修的只有 3 处**。

**实际改动**

| 项 | 落点 |
|----|------|
| 消息列表留白 | `values/dimens.xml` 加 `wfc_message_list_padding_horizontal`（手机 **0dp**）、`values-sw600dp` 48dp；`conversation_fragment.xml` 的 `msgRecyclerView` 引用之 |
| 扩展面板留白 | 同上，`wfc_ext_panel_padding_horizontal`；`conversation_ext_layout.xml` 的 8 格容器引用之 |
| 头像宫格列数 | `values/integers.xml` `wfc_member_grid_span`=5 / `-sw600dp`=8；4 个布局 + **3 个** Fragment 同改 |
| 图片选择器列数 | imagepicker 模块**不依赖 uikit**，须自建 `values/integers.xml` `ip_image_grid_span`=3 / `-sw600dp`=5，不能复用 uikit 的 integer |
| 底部导航 | 新增 `chat/layout-sw600dp/main_activity.xml`，导航条限宽 560dp 居中，外套一层铺满的 FrameLayout 承载底色（否则收窄后两侧露底、底栏看着是断的） |

> 未采用原计划的"最大宽度 + 复制整份 `layout-sw600dp` 布局"：`LinearLayout`/`RecyclerView` 没有 `maxWidth` 属性，而复制 246 行的 `conversation_ext_layout.xml` 只为改一个宽度，维护成本和漏同步风险都更高。改用**手机取值为 0dp 的 padding dimen**，默认布局里加一行引用即可，手机端解析结果与改造前逐像素一致。

**3 处像素耦合的具体改法**

- `ConversationFragment.setupMultiMessageAction()`：改为 `setWeightSum(actions.size())` + 子 View `LayoutParams(0, WRAP_CONTENT, 1f)`。**注意 weightSum 必须取 `actions.size()` 而不是实际渲染出的按钮数** —— 密聊会跳过转发按钮，原代码此时按钮总宽小于容器宽、由容器 `gravity="center"` 居中；若按实际数量分权重，按钮会填满整行，密聊下手机端就变样了。
- `ConversationInputPanel` 图片推荐 popup：x 改为 `输入面板 getLocationOnScreen()[0] + getWidth() - popupWidth - 8dp`。y 的键盘避让仍用 `heightPixels` —— 双栏是横向切分，纵向没有窗口≠屏幕的问题，动它反而有回归风险。
- `KeyboardAwareLinearLayout.getDeviceRotation()`：改为按自身 `getWidth()/getHeight()` 判断，宽高为 0（布局未完成）时退回 `Configuration.orientation`。**这是 3 处里唯一在手机上也可能有可见变化的**：手机分屏时原逻辑取物理屏幕方向，横屏设备上分屏出竖窗口会读错键盘高度缓存 key，改后才对。手机全屏场景取值不变。

**朋友圈（用户已授权放宽约束）**

代码不在本仓库：位于**同级独立仓库 `../android-momentkit`**（remote 指向 wildfirechat 官方 github/gitee），经 `uikit/build.gradle` 的 `sourceSets` 挂进 uikit 编译，资源并入 `cn.wildfire.chat.kit.R` 命名空间。且该模块**仅在你本地未提交的 `settings.gradle` / `uikit/build.gradle` 改动下才会被编进来**，仓库提交态是关闭的。

- 留白用 **`w` 限定符而非 `sw600dp`**：朋友圈横竖屏都要看，`sw` 是屏幕最小宽度、旋转时不变，跟不上；`w` 跟随当前窗口可用宽度，折叠屏展开与分屏也能自动适配。断点 `w600/w840/w1080/w1280dp`，正文列稳定在 470~550dp。
- 留白加在 **feed 条目**而非列表 —— 朋友圈封面图是 RecyclerView 的 header，给列表加 padding 会把封面一起缩进去。
- 三种条目原本用了两套内边距（图文 16dp、纯文字/链接 10dp），保留差异各自外扩，不做统一，避免动到手机端现有排版。
- `Utils.getScreenWidth()` 取的是 **Application** 的 DisplayMetrics（永远是整屏），新增 `getFeedContentWidth()` 替代。`calculateShowCheckAllText`（决定是否显示"全文"）与两个九宫格 Glide 解码尺寸改用之；其中 `calculateShowCheckAllText` 的常量由 74dp 调为 42dp，使手机上合计仍等于"屏宽 − 74dp"，判定结果不变。

**验证方式**：`aapt2 dump resources <apk>`，确认每个新资源的**无限定符取值**与改造前写死的数值完全一致（`wfc_*_padding_horizontal` = 0dp、`wfc_member_grid_span` = 5、`ip_image_grid_span` = 3、`moment_feed_item_*` = 16dp/10dp）。这是"手机端零回归"的静态证据。

---

### 阶段 3：会话页宿主解耦（纯重构，行为等价）✅ 代码已完成，待真机回归

**目标**：让 `ConversationFragment` 能被"主界面右栏"和"独立 Activity"两种宿主承载。**本阶段不改变任何可见行为**，手机端跑的还是 `ConversationActivity`。

**改动**

1. 新增接口 `uikit/.../conversation/ConversationHost.java`：
   ```java
   public interface ConversationHost {
       void setConversationTitle(CharSequence title, CharSequence subTitle);
       void setConversationTitleWithIcons(CharSequence title, boolean silent, boolean earpiece);
       void closeConversation();          // 取代 getActivity().finish()
       long getHighlightMessageId();      // 取代 getActivity().getIntent().getLongExtra(...)
   }
   ```

2. `ConversationActivity implements ConversationHost`：四个方法的实现体就是**把现在 `ConversationFragment` 里的代码原样搬过来**（C1/C2 的 toolbar 操作、C3 的 intent 读取、C4 的 `finish()`）。

3. `ConversationFragment` 改为通过 `(ConversationHost) getActivity()` 调用，删除对 `WfcBaseActivity` 的强转（C1:1038、C2:1070、C3:574、C4:938/1022）。

4. 会话菜单下沉（C5）：把 `ConversationActivity.menu()`（`R.menu.conversation`）与 `onOptionsItemSelected` 中的「会话信息」逻辑迁移到 `ConversationFragment`，用 `MenuProvider` 注册。`ConversationActivity` 只保留转发。

5. 返回拦截（C6）：`onBackPressed()` 由包级可见改为 `public boolean onBackPressed()`，宿主统一调用。

**手机端保证**：D3。重构后 `ConversationActivity` 承担了原来 `ConversationFragment` 直接做的事，调用链多一跳但语义完全相同。**本阶段必须做一次完整的会话页回归**（见第六章），确认标题、静音/听筒图标、副标题、菜单、返回键、聊天室加入失败、密聊异常等路径与改前一致。

**验收**：手机端会话页全路径回归通过，行为与基线无差异。平板此时**仍是单栏**，无可见变化。

**风险**：`ConversationFragment` 有 1696 行且逻辑密集，重构易引入隐性回归。缓解：本阶段**只做搬运，不做优化**；每个耦合点单独 commit，便于二分定位。

**工作量**：3~5 人日

#### 实施记录

**对本节原计划的一处调整：`ConversationActivity` 没有 `implements ConversationHost`。**

原计划让 `ConversationActivity` 实现接口、把 Fragment 里的四段代码搬进去。实际改为新增一个通用适配器
`WfcBaseActivityConversationHost`，对**任意** `WfcBaseActivity` 提供"会话页独占一个 Activity"这一经典形态的实现。
好处有二：

1. `ConversationActivity` 在 C1~C4 上**一行都不用改**（只改了 C5 菜单转发），diff 更小、回归面更窄，更符合本阶段"只做搬运"的要求；
2. uikit 作为 AAR 被集成时，宿主可能是集成方自己的 `WfcBaseActivity` 而并未实现 `ConversationHost`，
   `ConversationFragment.conversationHost()` 会自动回退到该适配器 —— 本次改造对集成方**不是破坏性变更**。

**新增文件**

| 文件 | 职责 |
|------|------|
| `conversation/ConversationHost.java` | 宿主接口，4 个方法（设标题 / 读标题 / 关会话 / 取高亮消息 id） |
| `conversation/ConversationTitleHelper.java` | 把标题（含静音/听筒图标）画到**指定的** toolbar 上。代码原样搬自 Fragment，只把"画到哪个 toolbar"变成构造参数，独立会话页与双栏右栏共用 |
| `conversation/WfcBaseActivityConversationHost.java` | 上述经典形态的宿主实现 |

**接口签名与原计划的差异**

- `setConversationTitle(title, subTitle, silent, earpiece)` 合成一个方法（原计划拆成两个）。判定"是否免打扰/是否听筒"的**策略**留在 Fragment（它才知道会话状态），**渲染**交给宿主，职责边界更清晰。
- 增加了 `getConversationTitle()`。原计划漏了 `resetConversationTitle()`（"对方正在输入"结束后还原标题）里的 `getActivity().getTitle()` —— 这也是一处宿主耦合。

**逐点落实**

| 耦合点 | 改法 |
|--------|------|
| C1 `setActivityTitle` | 改为 `host.setConversationTitle(...)`；`applyTitleWithIcons` / `appendTitleIcon` / `findToolbarTitleView` / `CenteredImageSpan` 与 `toolbarTitleView` 缓存整体迁入 `ConversationTitleHelper` |
| C2 `applyTitleWithIcons` | 同上 |
| C3 `checkAndHighlightMessage` | `host.getHighlightMessageId()` |
| C4 两处 `getActivity().finish()` | 新增私有 `closeConversation()` → `host.closeConversation()` |
| C5 会话菜单 | `menu_conversation_info` 的处理下沉为 `ConversationFragment.onConversationMenuItemSelected(MenuItem)`；`ConversationActivity.onOptionsItemSelected` 只做转发，其 `showConversationInfo()` 与 `conversation` 字段一并删除（字段改为方法内局部变量） |
| C6 `onBackPressed()` | 包级可见 → `public` |
| — | `onDestroyView()` 中原来的 `toolbarTitleView = null` 改为 `wfcBaseActivityHost = null`，等价地丢弃标题 TextView 缓存 |

**行为差异（均为更宽容，不构成回归）**

- 原 `(WfcBaseActivity) getActivity()` 在宿主类型不符时会抛 `ClassCastException`，现在 `conversationHost()` 返回 null、调用点直接 return。
- `showConversationInfo()` 增加了 `conversation == null` 的保护（原来在 Activity 上，会话未初始化时点菜单会崩）。

**验证**：`./gradlew :chat:assembleDebug` 通过；未新增/修改任何资源，手机端无资源变更。**行为等价只能靠真机回归确认**，见第六章。

---

### 阶段 4：主界面双栏 ✅ 代码已完成，待真机回归

**目标**：平板上左侧会话/联系人列表 + 右侧会话内容，类似微信 Pad / WhatsApp Pad。

**方案**：**新增** `MainPadActivity`（D4），不改 `MainActivity` 的手机路径。

**改动**

1. `SplashActivity.showMain()` 分流：
   ```java
   Intent intent = new Intent(this,
       WfcDeviceUtils.isTwoPaneLayout(this) ? MainPadActivity.class : MainActivity.class);
   ```
   `MainActivity` 的 `${applicationId}.main` 与分享 intent-filter 需要同步复制到 `MainPadActivity`，或统一由一个 trampoline 分发（推荐后者，避免两处维护）。

2. `MainPadActivity` 布局（`layout-sw600dp/main_pad_activity.xml`）：
   ```
   ┌──────┬───────────────┬──────────────────────────────┐
   │ 侧边 │  左栏 (320dp)  │  右栏 (剩余)                  │
   │ 导航 │  列表 Fragment │  ConversationFragment 容器    │
   │ Rail │  + 独立 toolbar│  + 独立 toolbar               │
   └──────┴───────────────┴──────────────────────────────┘
   ```
   - 侧边导航用 `NavigationRailView` 取代 `BottomNavigationView`（菜单资源复用 `@menu/main_bottom_navigation`，未读角标逻辑从 `MainActivity.java:293-348` 抽成公共工具类复用）；
   - 左栏复用**同一批 Fragment**：`ConversationListFragment`、`ContactListFragment`、`WebViewFragment`、`DiscoveryFragment`、`MeFragment`；
   - 右栏是 `FrameLayout`，装 `ConversationFragment`；
   - `MainPadActivity implements ConversationHost`，标题写到**右栏自己的 toolbar**（这正是阶段 3 抽接口的目的）。

3. 双栏交互规则（需产品确认，建议默认值）：

| 场景 | 建议行为 |
|------|----------|
| 点击会话列表项 | 右栏切换会话，**不 startActivity**；左栏保持选中态高亮 |
| 首次进入 / 无会话 | 右栏显示空状态占位（Logo + "选择一个会话开始聊天"） |
| 右栏有会话时按返回 | 先交给 `ConversationFragment.onBackPressed()`（收起表情/扩展面板、退出多选）；未消费则 `moveTaskToBack`（与手机 `MainActivity.onBackPressed():361` 语义一致） |
| 切到「联系人/发现/我」Tab | 右栏保持当前会话不变（微信 Pad 行为），或清空——**待定** |
| 会话被删除/退群 | 右栏回到空状态 |
| 旋转 / 分屏变窄至 < 600dp | 由于 Activity 会重建且 `wfc_two_pane` 变 false，需在 `MainPadActivity` 中检测并转跳 `MainActivity` + `ConversationActivity`（保留当前会话）。**这是双栏最容易出问题的路径，需单独设计与测试** |

4. `ConversationActivity` 在平板上**依然保留**：供通知点击、外部分享、深链等场景兜底（阶段 5 统一收口）。

**手机端保证**：D4。`MainActivity`、`main_activity.xml`、`ConversationActivity` 均不改（仅 `SplashActivity` 增加一个分支，手机走原分支）。

**验收**
- 手机：启动仍进 `MainActivity`，点会话仍 `startActivity(ConversationActivity)`，全流程与基线一致；
- 平板：双栏切换流畅，未读数/角标正确，旋转与分屏窄化不崩溃、会话不丢。

**工作量**：8~12 人日

#### 实施记录

**对本节原计划的核心调整：没有新增 `MainPadActivity`，双栏做在 `MainActivity` 内部（D2 开关隔离，而非 D4 新增文件）。**

原计划"新增 `MainPadActivity`"的最大代价，本节第 1 条自己已经点出来了：manifest 里 `${applicationId}.main`、
`ACTION_SEND` 分享等 intent-filter 与 `singleTask` 语义要复制一份，还要处理两个主界面并存。而
`${applicationId}.main` 在 uikit 里有 **7 处**调用方（通知点击、退群后返回主界面等），一旦分叉必然踩坑。

改法是让 `MainActivity` 自己按窗口宽度选布局：

```java
protected int contentLayout() {
    return WfcDeviceUtils.isTwoPaneLayout(this) ? R.layout.main_pad_activity : R.layout.main_activity;
}
```

配套地，本节第 3 条表格最后一行"旋转/分屏变窄至 < 600dp 需转跳"这个**最容易出问题的路径也随之消失**：
Activity 重建时 `contentLayout()` 自然重新求值，无需任何跨 Activity 跳转编排。

手机端保证由 **D2** 提供，且有静态证据：`bool/wfc_two_pane` 的无限定符取值为 `false`（`aapt2 dump` 已确认），
`MainActivity` 中所有新增分支都在 `twoPaneController != null` 之下，手机上恒不成立。

**双栏布局：左栏是手机布局的原样搬运**

`layout/main_pad_activity.xml` 的左栏与 `main_activity.xml` 结构、控件 id 完全一致
（`toolbar` / `contentLinearLayout` / `contentViewPager` / `bottomNavigationView` / `startingTextView`），
只是宽度固定为 `wfc_pad_left_pane_width`（sw600dp 320dp，sw840dp 360dp）。

因此 `MainActivity` 里 appbar 折叠动画（`updateToolbar`）、底部导航未读角标
（`BottomNavigationMenuView.getChildAt(n)`）、ViewPager 切换、「+」菜单锚点等逻辑**一行都不用改**。

> 未采用原计划的 `NavigationRailView` 侧边导航：它与 `BottomNavigationView` 虽同为 `NavigationBarView` 子类，
> 但角标代码依赖 `getChildAt(0)` 返回 `BottomNavigationMenuView`，Rail 的子 View 结构不同（不同 material 版本还会变），
> 会把三处角标逻辑全部拖下水。先用"左栏底部导航"这一低风险形态跑通双栏，侧边 Rail 可作为后续独立的视觉优化。

**新增文件**

| 文件 | 职责 |
|------|------|
| `chat/.../main/TwoPaneConversationController.java` | 右栏全部逻辑，`implements ConversationHost`。手机上**不会被实例化** |
| `chat/res/layout/main_pad_activity.xml` | 双栏布局（无限定符，仅由代码在双栏时选用） |
| `chat/res/values/dimens.xml`、`values-sw840dp/dimens.xml` | `wfc_pad_left_pane_width` |
| `uikit/res/drawable/selector_conversation_item_two_pane.xml` | 会话列表项多一个 `state_activated` 选中态。**新建而非改 `selector_common_item`**，后者被多处复用，改它可能波及手机端 |

**双栏交互规则的落地情况**

| 场景 | 实现 |
|------|------|
| 点击会话列表项 | 右栏切换，不 `startActivity`；左栏高亮（`ConversationListAdapter.setSelectedConversation`） |
| 首次进入 / 无会话 | 右栏显示占位（Logo + `pad_select_a_conversation`），右栏 toolbar 一并隐藏 |
| 右栏有会话时按返回 | 先给 `ConversationFragment.onBackPressed()`，未消费则 `moveTaskToBack(true)`，与手机语义一致。**返回键不清空右栏**（微信 Pad 行为） |
| 切到其他 Tab | ~~右栏保持当前会话~~ → **阶段 5.5 改为每个 tab 一条独立导航栈** |
| 会话被删除/退群 | 右栏回到空状态，见下方"两个易踩的坑" |
| 旋转 / 分屏变窄 | 会话存进 `onSaveInstanceState`；仍是双栏则恢复到右栏，变成单栏则 `startActivity(ConversationActivity)` 交还给独立会话页 |

**两个易踩的坑（都已处理）**

1. **`commitNow()` 之后不能立刻 `setupConversation()`。** `commitNow` 只把 Fragment 推进到"**宿主当前的**"生命周期状态；
   分屏/旋转重建后恢复右栏是在 `Activity.onCreate` 里调用的，此时 `onCreateView` 还没跑，直接
   `setupConversation` 会因 `adapter`/`inputPanel` 未创建而 NPE。改为 `container.post(...)`，并用局部变量持有 Fragment，
   避免快速连点两个会话时把参数应用到后一个 Fragment 上。
   （**阶段 5 又进一步改造**：事务改异步 `commitAllowingStateLoss()`，时机改为观察 `getViewLifecycleOwnerLiveData()`，
   原因见阶段 5 的"顺带修掉阶段 4 的一个 Fragment 事务隐患"。）
2. **"会话从列表消失就清空右栏"是错的。** 新建的空会话在发出第一条消息前本来就不在会话列表里，
   直接判"不在列表 → 关闭"会让刚点开的新会话立刻被关掉。改为只有**曾经出现在列表里、之后又消失**才算删除
   （`conversationSeenInList` 标志位）。

**会话切换用新建 Fragment 而不是复用**

`ConversationActivity` 是 `singleTask` + `onNewIntent` + 复用同一个 Fragment 再 `setupConversation`；右栏改为**每次换会话新建一个
`ConversationFragment`**。多选模式、输入框草稿、展开中的表情/扩展面板、密聊退出时的临时文件清理等状态都挂在 Fragment 上，
复用极易串会话；平板上会话切换远比手机频繁，这个风险不能留。

**已知差距（留待后续）**

- ~~右栏内的入口（点头像进私聊、搜索定位、通知点击等）目前仍走 `startActivity(ConversationActivity)`，
  即在双栏之上再压一个全屏会话页。这正是**阶段 5** 要收口的 31 处启动点，`ConversationHost.getHighlightMessageId()`
  在双栏下暂时返回 0，也要到阶段 5 才由 `ConversationRouter` 按参数传入。~~ → **阶段 5 已收口**。
- `updateToolbar()` 在「发现/我」页会把状态栏染成白色，双栏下这会影响整个窗口（含右栏）。属平板端视觉瑕疵，非功能问题。
- `chat/res/layout-sw600dp/main_activity.xml`（阶段 2 产物）在双栏开启后不再被使用，但**予以保留**：
  它与 `wfc_two_pane` 共用 `sw600dp` 限定符，若将来把 `wfc_two_pane` 改回 false 以关闭双栏，
  平板会平滑退回到那份单栏布局，是一个现成的 kill-switch。

**验证**：`./gradlew :chat:assembleDebug` 通过；`aapt2 dump resources` 确认
`bool/wfc_two_pane` = `() false` / `(sw600dp) true`，`layout/main_pad_activity` 只有无限定符一份，
`dimen/wfc_pad_left_pane_width` = `() 320dp` / `(sw840dp) 360dp`，未改动任何手机端既有资源。

---

### 阶段 5：入口路由统一 ✅ 代码已完成，待真机回归

**目标**：全仓库 **31 处** `ConversationActivity` 启动点在平板上正确落到双栏，手机上路径不变。

**改动**

1. 新增 `uikit/.../conversation/ConversationRouter.java`：
   ```java
   public static void open(Context ctx, Conversation conv, String title,
                           long focusMessageId, String channelPrivateChatUser) {
       if (WfcDeviceUtils.isTwoPaneLayout(ctx) && ctx instanceof ConversationHost) {
           ((ConversationHost) ctx).showConversation(...);   // 右栏切换
       } else {
           ctx.startActivity(ConversationActivity.buildConversationIntent(...)); // 原逻辑
       }
   }
   ```

2. 逐个替换 31 处启动点（分布见下），每处替换后手机端行为完全不变（走 else 分支）：
   - 会话列表：`conversationlist/viewholder/ConversationViewHolder.java:233`
   - 通知点击：`WfcNotificationManager.java:190` ← **重点**，平板上应打开 `MainPadActivity` 并定位到该会话
   - 搜索：`search/module/` 下 4 个 module
   - 群/频道/联系人/收藏/文件助手：`GroupListFragment:90`、`GroupInfoActivity:162,176`、`ChannelListFragment:95`、`UserInfoFragment:288`、`FavContentViewHolder:66`、`PCSessionActivity:188`
   - 会议、聊天室、按日期查找、链接记录等其余入口

3. 外部入口（分享 `ACTION_SEND`、`WfcScheme` 深链、`${applicationId}.main`）在平板上统一进 `MainPadActivity`。

**手机端保证**：D2。`ConversationRouter` 在手机端等价于原来的 `startActivity`。

**验收**：手机端 31 条入口逐条回归；平板端每条入口都落在双栏右栏且左栏选中态正确。

**工作量**：4~6 人日

#### 实施记录（与计划的差异）

**核心差异：路由的入参是 Intent，不是拆开的会话参数**

计划里 `open(ctx, conv, title, focusMessageId, channelPrivateChatUser)` 要求每个调用点把已经构造好的 Intent
拆成 5 个参数再传进来。实际改成：

```java
Intent intent = ConversationActivity.buildConversationIntent(...);  // 原样不动
intent.putExtra("toFocusMessageId", ...);                           // 原样不动
ConversationRouter.open(context, intent);                           // 只改这一行
```

这样做的收益：

1. **每个调用点只改一行**，`new Intent(...)` / `buildConversationIntent(...)` / 各种 `putExtra` 全部保持原样，
   29 处改动的 diff 一眼可核对；拆参数的写法则要逐处判断"这个入口原本传了哪几个 extra"，极易漏传。
2. **手机端逐字节等价**：手机路径就是 `context.startActivity(conversationIntent)`，intent 对象与改造前完全相同
   （flags 也保留，比如 `GroupInfoActivity` 之后紧跟的 `finish()` 语义不变）。
3. **新增 extra 时两条路径自动同步**：右栏解析的键名与 `ConversationActivity` 完全一致，
   将来加参数不必改路由。

**判定顺序：必须先判双栏，再找宿主**

计划写的是 `isTwoPaneLayout(ctx) && ctx instanceof ConversationHost`，实现时这两个条件的**顺序**是关键：
双栏主界面在手机上**依然是同一个 `MainActivity` 类、依然实现 `ConversationPaneHost`**，只是没有右栏。
若先做 `instanceof` 再判宽度，手机端点会话会调到一个空实现上，表现为"点了没反应"。
`ConversationRouter.open()` 里对此有显式注释，`MainActivity.showConversationInPane()` 另有一层兜底
（控制器为 null 时退回独立会话页）。

**新增接口 `ConversationPaneHost`，没有复用 `ConversationHost`**

`ConversationHost`（阶段 3）是"**会话页对宿主的要求**"——设标题、关闭自己，独立会话页和右栏都要实现；
`ConversationPaneHost` 是"**路由对宿主的要求**"——换一个会话，只有双栏主界面才有意义。
如果按计划把 `showConversation` 塞进 `ConversationHost`，`WfcBaseActivityConversationHost`
（独立会话页的适配器）就得实现一个它无法履行的语义。

**对 AAR 集成方的保护：双栏主界面必须显式注册**

这是计划没有覆盖、但必须处理的一点：`isTwoPaneLayout()` 在**任何**平板上都返回 true，包括把 uikit 当 AAR
集成、自己写了主界面的第三方 App。若路由仅凭屏幕宽度就跳去 `${applicationId}.main`，
集成方的平板用户会因为"路由到一个并不支持双栏的主界面"而**打不开任何会话**。

因此双栏分支需要 `ConversationRouter.setTwoPaneHostActivity(MainActivity.class)` 显式注册
（demo 在 `MyApp.onCreate` 调用），默认 `null`。集成方不注册时，即使在平板上也只走
`startActivity(ConversationActivity)` 的原路径，行为与改造前完全一致。
同时路由直接 `new Intent(context, twoPaneHostActivity)` 指定目标类，不再依赖 `${applicationId}.main` action。

**改动清单（共 30 处启动点）**

| 位置 | 处数 |
|------|------|
| `uikit`：会话列表项、群列表/群信息(2)、频道列表/创建频道、联系人资料、收藏、文件助手(PC)、聊天室列表、会议、按日期查找、链接记录、4 个搜索 module、4 个会话信息页的"举报"、`CreateConversationActivity`(3)、会话页点头像进私聊、公众号消息私聊 | 26 |
| `chat`：`DiscoveryFragment`(FireRobot)、`MainActivity` 会话列表点击、`MainActivity` 密聊创建成功 | 3 |
| `WfcNotificationManager` 通知点击（走 `buildTaskIntents`，见下） | 1 |

`MainActivity` 里"分屏变窄后把右栏会话交还给独立会话页"那一处**故意不走路由**：它的前提就是当前已是单栏。

**通知点击：`buildTaskIntents`**

通知原本用 `PendingIntent.getActivities(new Intent[]{主界面, 会话页})` 起两个 Activity，双栏下同样会多压一层。
`ConversationRouter.buildTaskIntents()` 在双栏时返回长度为 1 的数组（只起主界面，会话参数放 extras），
其余情况原样返回两个 intent。通知只有 Application Context 可用，`isTwoPaneLayout()` 在多窗口窄窗口下可能判不准，
因此 `MainActivity.handleConversationLaunchIntent()` 有兜底：到了主界面发现是单栏，就退回独立会话页。

**`getHighlightMessageId()` 补齐**

阶段 4 遗留的"双栏下恒返回 0"已修复：路由把 `highlightMessageId` / `toFocusMessageId` 透传给右栏控制器。
配套地，`showConversation` 的"同一个会话直接返回"去重逻辑**在需要定位消息时不生效**——
否则从搜索结果点进"右栏当前已经打开的那个会话"不会跳转到目标消息。

**冷启动时序：`onImServiceReady()`**

通知点击冷启动 App 时，路由 intent 早于 IM 服务连接到达，此时 `ChatManager` 尚不可用。
控制器把 intent 暂存，等 `MainActivity.init()` 之后由 `onImServiceReady()` 冲掉。
另外 `onCreate` 里只在 `savedInstanceState == null` 时处理启动 intent，否则每次旋转都会重新打开并重新高亮一次。

**顺带修掉阶段 4 的一个 Fragment 事务隐患**

阶段 4 的右栏用 `commitNowAllowingStateLoss()` + `container.post()`。阶段 5 把"会话页点头像进私聊"接入路由后，
`showConversation` 可能**在右栏会话页自己的点击回调里被调用**——`commitNow` 会在回调执行到一半时同步销毁这个
Fragment；若外层正在执行事务，还会抛 `FragmentManager is already executing transactions`。已改为：

- 事务改成异步 `commitAllowingStateLoss()`；
- `setupConversation` 的时机从 `container.post()` 改为观察 `getViewLifecycleOwnerLiveData()`，
  视图创建完成才回调，且视图已就绪时立即回调——`post()` 在 `Activity.onCreate` / `onNewIntent` 里调用时不保证时序。

**平板端的一处交互补充**

从非主界面入口（通知、群信息、搜索）路由进右栏时，左栏一并切回"会话"Tab，否则列表里的选中态用户根本看不见。
限定在 `isInitialized` 之后执行（冷启动时左栏本来就在会话列表，且 ViewPager 的 adapter 尚未装好）。

**顺带发现的既有缺陷（未修，避免改变手机端行为）**

`ConversationActivity.buildConversationIntent(Context, type, target, line, channelPrivateChatUser)` 这个重载
**丢弃了 `channelPrivateChatUser` 参数**（内部转调时传了 `null`）。受影响的是"公众号消息长按 → 私聊"
（`NormalMessageContentViewHolder.startChanelPrivateChat`）。这是阶段 5 之前就存在的 bug，
本阶段按"手机端行为不变"的约束原样保留，建议单独修。

**验证**：`./gradlew :chat:assembleDebug` 通过。本阶段**没有任何资源改动**（纯 Java），
`git status -- '*/res/*'` 为空；`aapt2 dump resources` 复核 `bool/wfc_two_pane` 仍为 `() false` / `(sw600dp) true`，
手机端所有路由分支的门禁保持关闭。

---

### 阶段 5.5：右栏导航栈（每个 tab 一条）✅ 代码已完成，待真机回归

**背景**：阶段 4/5 的右栏只能装会话页，且全局只有一条"当前会话"。产品要求补齐为完整的两栏导航
（对齐 flutter 端 `chat/lib/pad/pad_home.dart`）：

1. 每个 tab 有自己的导航栈。左栏点开的页面压进**当前 tab** 的右栏栈；右栏内再点开的页面压在同一条栈上。
   例外：媒体预览、音视频通话保持全屏。
2. 切到新 tab，右栏默认展示欢迎页（该 tab 自己那条栈的栈底）。
3. 工作台：**左栏显示欢迎占位，右栏栈底就是工作台网页**（它没有"列表 → 详情"的层次）。

> 这条覆盖了阶段 4 遗留的 Q3（"切 tab 时右栏保持当前会话"）——改为每个 tab 各看各的栈。

#### 与 flutter 端的根本差异：Android 的"页面"是 Activity

flutter 端能一行收口（`app_navigator.openPage(context, widget)` 把任意 Widget 压进右栏那条
`Navigator`），是因为它的页面本来就是 Widget。Android 的页面是 Activity，**没有任何通用手段
把一个 Activity 塞进半个屏幕的 View 里**（`ActivityView` 已废弃；Jetpack 的 Activity Embedding
虽然能真的并排显示两个 Activity，但它的返回栈是 task 级的，表达不了"每个 tab 一条栈"）。

好在本仓库的页面绝大多数是 **`fragment_container_activity` + 一个 Fragment** 的壳（共 31 个），
把壳去掉直接用里面那个 Fragment 即可。于是方案是"**注册表 + 拦截**"：

| 关注点 | flutter | 本次 Android 实现 |
|---|---|---|
| 唯一导航入口 | `openPage(context, widget)` | 覆写 `MainActivity.startActivityForResult(intent, -1, options)` |
| 页面载体 | 任意 Widget | `PaneRegistry` 里登记的 Fragment 工厂 |
| 每 tab 一条栈 | 一叠 `Navigator` + `IndexedStack` | 一叠 `PaneStackFragment`（各自的 childFragmentManager）+ show/hide |
| 每页的标题栏 | 页面自带 `Scaffold`/`AppBar` | `PanePageFragment`（toolbar + 内容容器） |
| 未接入的页面 | 不存在这个概念 | 未登记 → 原样 `startActivity`，仍是全屏 |

#### 新增文件

| 文件 | 职责 |
|------|------|
| `uikit/.../pane/PaneRegistry.java` | `Activity 类 → Fragment 工厂` 注册表，附带菜单转发钩子 |
| `uikit/.../pane/PaneStackFragment.java` | 一个 tab 的导航栈（一个自带 childFragmentManager 的容器） |
| `uikit/.../pane/PanePageFragment.java` | 栈里的一页：本页专属 toolbar + 内容 Fragment，`implements ConversationHost` |
| `uikit/.../pane/PanePage.java` | 页面向右栏申报菜单/标题/返回键/启动参数的接口，全是 default 方法 |
| `uikit/.../pane/PaneWelcomeFragment.java` | 栈底欢迎页；工作台 tab 的左栏占位也复用它 |
| `uikit/.../conversation/ConversationPanePage.java` | `ConversationFragment` + `PanePage`，手机端永不实例化 |
| `uikit/.../user/UserInfoMenuController.java` | 从 `UserInfoActivity` 抽出的菜单逻辑，两端共用 |
| `chat/.../main/TwoPaneNavigator.java` | 右栏导航器（取代阶段 4 的 `TwoPaneConversationController`，该文件已删除） |
| `uikit/res/layout/pane_{page,stack,welcome}_fragment.xml` | 三个新布局，均为新增 |

#### 拦截点：为什么是 `startActivityForResult(intent, -1, options)`

`Activity.startActivity(intent)`、`Fragment.startActivity(intent)`、`context.startActivity(intent)`
最终**全部**汇到 `Activity.startActivityForResult(intent, -1, options)` 这一个虚方法
（`Fragment` 那条走 `FragmentActivity.startActivityFromFragment` → `ActivityCompat.startActivityForResult`）。
覆写它，等于一次性拿到全仓库所有"打开一个页面"的调用，不必再像阶段 5 那样逐点替换 30 处。

手机端零影响有三重保险：

1. `twoPaneNavigator` 手机端恒为 null（`WfcDeviceUtils.isTwoPaneLayout()` 为 false 时根本不创建）；
2. `requestCode == -1` 把所有等结果的跳转排除在外——选择器、拍照、扫码、`registerForActivityResult`
   全都带非负 requestCode，永远走原路径，也就不会出现"页面进了右栏但结果回不来"；
3. 未在 `PaneRegistry` 登记的页面一律放行。**媒体预览、音视频通话就是靠"不登记"保持全屏的**，
   不需要额外的例外判断。

#### "换内容"还是"往下钻"：按按下点所在的栏区分

同一个 `openPage` 要承载两种语义：左栏选了另一位联系人是**换内容**（该栈退回栈底再压入，
返回直接回欢迎页），右栏内从群资料点进成员资料是**开一层**（压栈，返回要回得去）。
flutter 靠"调用点的最近 Navigator 是不是根 Navigator"来分，Android 这边拿不到调用方身份
（`context` 就是 Activity 本身），改为在 `dispatchTouchEvent` 里记录 **ACTION_DOWN 落在哪一栏**。

导航几乎都由点击触发，这个信号可靠且零侵入；万一判错，后果也只是多压一层或少压一层，不会丢功能。

> 需求原文对左栏点击也写的是"压入"。这里按"重置后压入"实现：否则依次点 10 个联系人会攒出
> 10 层栈，返回键要按 10 次才回到欢迎页。flutter 端 `_setPaneRoot` 也是这个语义。

#### 页面的标题栏：每页一条，而不是右栏共用一条

`PanePageFragment` 的布局是 `toolbar + 内容容器`，相当于 `WfcBaseActivity` 在右栏里的等价物。
栈里每一层自带标题栏，于是**不需要"栈顶页面把标题交接给一条公用 toolbar"** 这种极易出错的编排。

- **标题**：页面实现 `PanePage.panePageTitle()` 时用它；否则读该页对应 Activity 在 manifest 里的
  `android:label`——与手机端 `WfcBaseActivity.updateActivityTitle()` 是同一份数据，两条路径标题必然一致。
- **菜单**：页面自己声明（`ConversationPanePage`），或由注册项提供（`ChannelListActivity` 的"查找频道"、
  `FriendRequestListActivity` 的"添加"这类三五行的，直接在注册表里再表达一次，比把 Activity 的菜单
  逻辑整体搬进 Fragment 风险小）。`UserInfoActivity` 的菜单有 120 行且状态相关，抽成了
  `UserInfoMenuController` 供两端共用（**这是本阶段唯一改动手机端页面逻辑的地方，属 D3 等价重构**）。
- **返回箭头**：栈深 > 0 时显示；栈底之下只有欢迎页时不显示（返回过去是一片空白）。
  工作台那条栈的栈底是真实页面，所以压在它上面的第一层也给箭头。

#### 已登记的页面（首批）

会话页、会话信息（单聊/群聊/聊天室/频道/密聊）、用户资料、群列表、群成员列表、群管理、
群管理员列表、入群申请、频道列表、新的朋友、收藏、黑名单、按日期查找、链接记录 —— 共 14 项。

未登记的仍是全屏，功能不受影响。后续可继续接入的：`chat` 模块的设置/账号/文件记录等页面、
搜索入口、组织架构。**永远不要登记**：媒体预览、音视频通话、扫码、各类选择器。

#### 五个易踩的坑（都已处理）

1. **`hide()` 不会让 Fragment 离开 RESUMED。** 被盖住的会话页、切走的那条栈上的会话页都会继续
   当自己在前台，把新到的消息标记为已读。必须配合 `setMaxLifecycle(..., STARTED)`，
   才等价于手机端被覆盖的 Activity；出栈/切回时事务反向执行会自动还原。
2. **入栈不能用 `replace`。** replace 会销毁被盖住那页的视图，返回时列表滚动位置、
   WebView 内容全部重来。改用 `hide(当前页) + add(新页)`。
3. **配置变化后必须认领 FragmentManager 自动恢复出来的那几条栈**（`adoptRestoredStacks`），
   否则会用同一个 tag 再 add 一条空栈，旋转一下右栏正在看的页面就没了。
4. **单栏布局下要主动清掉恢复出来的栈**（`removeRestoredStacks`）：`main_activity.xml` 里没有
   `paneContainerFrameLayout`，留着它们只会挂在一个不存在的容器上。
5. **栈内事务必须异步 commit。** 与阶段 5 同一个原因：`openPage` 可能在右栏页面自己的点击回调里
   被调用（会话页点头像进私聊），`commitNow` 会在回调执行到一半时同步动这条栈。

#### 会话页找宿主的方式扩了一条

`ConversationFragment.conversationHost()` 原来只认 `getActivity() instanceof ConversationHost`。
右栏里宿主是**父 Fragment**（`PanePageFragment`），因此在 Activity 判断之前加了一段父链遍历。
手机端会话页始终直接挂在 Activity 上，父链为空，这个循环一次都不会进。

同时 `MainActivity` 不再 `implements ConversationHost`（那是阶段 4 的单会话形态遗留），
`ConversationPaneHost` 保留——`ConversationRouter` 靠它找双栏宿主。

#### 与阶段 5 的关系

阶段 5 那 30 处 `ConversationRouter.open(...)` **全部保留且仍然有效**：它们最终调到
`MainActivity.showConversationInPane` → `TwoPaneNavigator.openInPane`。从非主界面页面
（全屏页）发起的路由仍走"起 `MainActivity` + `EXTRA_OPEN_CONVERSATION_IN_PANE`"那条，
落到**消息 tab** 的栈里。本阶段新增的拦截是它的超集，两者不冲突。

**验收**
- 手机：`bool/wfc_two_pane` 仍为 `() false`；`twoPaneNavigator` 恒为 null，
  `startActivityForResult` 覆写直接落到 `super`。仍需按 6.1 逐条回归。
- 平板：五个 tab 各自的栈互不干扰；切 tab 回来页面还在；工作台左欢迎右网页；
  返回键逐层出栈、栈空才退到后台；旋转/分屏后栈不丢。

**验证**：`./gradlew :chat:assembleDebug` 通过；资源改动仅 3 项——
`main_pad_activity.xml`（平板专用，由代码选用）、3 条新字符串（zh/en）、3 个新布局，
未修改任何手机端既有资源；`aapt2 dump resources` 复核 `bool/wfc_two_pane` = `() false` / `(sw600dp) true`。

**遗留**
- 右栏内的页面若用 `startActivityForResult` 打开子页（选择器），子页仍是全屏——设计如此。
- 左栏的 appbar 折叠动画（`updateToolbar`）在"发现/我"页会把状态栏染白，双栏下会影响整个窗口。
  阶段 4 已记录，仍未处理。

---

### 阶段 5.6：导航栈修补（首轮真机反馈）✅ 代码已完成，待真机回归

首轮真机试用暴露三个问题，本节逐个记录成因与修法。

#### 问题 1：右栏内点开的页面又变回全屏

两个独立成因，都已修：

**(a) 隐式 intent 匹配不到注册表。** 仓库里有 `new Intent(WfcIntent.ACTION_MOMENT)` 这种
只带 action、不带 component 的导航（朋友圈是唯一一处）。`PaneRegistry` 一切都以 component 为
索引，拿不到 component 就一路 fallthrough 到全屏。修法是在拦截入口先补齐：
`PaneRegistry.resolveComponent(context, intent)` 用 `PackageManager.resolveActivity` 把
**本应用内**的隐式 intent 解析成 component（按 action 缓存，导航是高频路径）。
补出来的 component 只在右栏内部用于判断；未登记时调用方启动的仍是**原始** intent。

**(b) 登记覆盖面不够。** 阶段 5.5 只登记了 14 个页面，右栏内往下钻常走的那些
（组织架构、图片与视频、文件记录、成员消息记录、成员权限、聊天室、互联域、会议记录、
组织成员详情）一个都没登记，点开自然是全屏。本轮把 uikit 里**所有** `fragment_container_activity`
形态的非选择器页面补齐，共 23 个。

登记项现在还带一个可选的 `PageKey`（见问题 2）。

#### 问题 2：会话页应当"栈内单例、跨栈可重复"

`会话A → 点头像 → 用户资料 → 发消息` 会在同一条栈上叠出第二层会话A；而
`消息 tab 开着会话A` 与 `通讯录 tab 也开着会话A` 是**合理的**，两条栈是两条独立的导航路径。

实现成 `singleTop` 的等价物，落在栈这一层，与手机端无关：

| 概念 | 手机端 | 右栏 |
|---|---|---|
| 页面身份 | `Intent` + `launchMode` | `PaneRegistry.PageKey`（`会话:type:target:line`） |
| 已在栈里 | `singleTop` / `singleTask` 复用 | `popBackStack(该页条目名, 0)` 退回到它 |
| 复用时的新参数 | `onNewIntent()` | `PanePage.onPanePageNewIntent()` |
| 关闭本页 | `finish()` | `popBackStack(该页条目名, INCLUSIVE)` |

要点：

- **key 只在本条栈内比较**，跨栈天然不互相影响——五条栈是五个独立的 `childFragmentManager`。
- **复用时不要无条件重建页面**。`ConversationPanePage.onPanePageNewIntent` 只在 intent 带
  `toFocusMessageId` / `highlightMessageId` 时才重跑 `setupConversation`（从按日期查找、
  链接记录点回本会话需要定位）；单纯的重复打开什么都不做，否则草稿和滚动位置全丢。
- **每一层一个 UUID 条目名**，页面据此精确定位自己那一层。不用自增序号：条目名会随返回栈
  一起保存/恢复，进程被杀后重建时自增计数器从 0 开始，会和恢复出来的条目重名。

#### 问题 3（顺带修掉的严重 bug）：右栏里 `getActivity().finish()` 会关掉整个主界面

页面 Fragment 手机端装在自己的 Activity 里，右栏里装在 `PanePageFragment` 里，
`getActivity()` 变成了**双栏主界面**——`finish()` 一下整个 App 就退了。
`用户资料 → 发消息`、`群组列表 → 点群`、`群信息 → 群已解散/已被移出` 都会踩到。

新增 `PaneCompat`（uikit，`pane` 包）收口这类"以 Activity 为单位"的动作：

| 场景 | 手机端 | 右栏 |
|---|---|---|
| `PaneCompat.finishPage(f)` 本页已失效，关掉 | `finish()` | 把本页连同压在其上的一起出栈 |
| `PaneCompat.finishAfterOpeningPage(f)` 打开下一页后关掉自己 | `finish()` | **什么都不做** |

第二种为什么什么都不做：右栏本来就有返回栈，下面那层留着才是对的（从会话返回到用户资料很自然）；
而且此刻新页面已经压在自己上面，真去"关自己"会把刚打开的页面一并带走。

手机端保证：这两个方法都先找父链上的 `PanePageFragment`，手机上不存在该类、父链一定为空，
逐字节等价于原来的 `getActivity().finish()`。改动点 7 处（用户资料 2、群信息 3、群组列表 1、
群成员列表 1、群管理 1），均为等价重构。

#### 关于官方「多返回栈」方案（`saveBackStack` / `restoreBackStack`）

Android 文档推荐的新 API 是在**一个** FragmentManager 上用 `saveBackStack(name)` /
`restoreBackStack(name)` 切换多条栈。这里没有采用，原因是它的语义是
**销毁被保存那条栈上的 Fragment、日后从 saved state 重建**：

- 工作台那条栈的栈底是 WebView，每次切走都会被销毁、切回来重新拉一遍远端页面；
- 会话页每次切 tab 都要重建，滚动位置与输入状态靠 saved state 恢复并不完整；
- 栈底那一页（欢迎页 / 工作台网页）不在返回栈里，`saveBackStack` 覆盖不到它。

本方案的"一个 tab 一个 `PaneStackFragment`，用 `show/hide` + `setMaxLifecycle` 切换"是
文档中的另一条路：切走的栈**活着**但降到 `STARTED`，等价于手机端被覆盖的 Activity。
项目也没有 Navigation graph，用不上 `NavigationUI` 那套自动接线。

**验收**（在 5.5 验收基础上追加）
- 右栏内点开的页面留在右栏：用户资料 → 组织架构 / 会话 → 图片与视频 / 文件记录 等。
- `会话A → 用户资料 → 发消息` 回到栈里原来那层会话A，栈深不增长，草稿还在。
- 消息 tab 与通讯录 tab 可以同时各开着同一个会话，互不干扰。
- `用户资料 → 发消息`、`群组列表 → 点群`、`群信息 →（被移出群）`都不再关掉主界面。
- 按日期查找 / 链接记录点回本会话，能定位到那条消息。

**仍然全屏（本轮未接入）**
- **朋友圈**：`FeedListActivity` 不是"壳 + Fragment"形态，而是自带沉浸式状态栏、自绘 TitleBar、
  自管窗口的独立 Activity（`BaseFeedActivity` / `BaseTitleBarActivity` / `BaseStatusControlActivity`
  共约 1500 行，且在同级仓库 `../android-momentkit`）。要进右栏必须先把它改造成 Fragment，
  是独立一块工作，未在本轮进行。修好隐式 intent 之后，改造完成时登记只需一行。
- 搜索类页面、短表单编辑页、选择器（`Pick*Activity`）：见下一节，已在阶段 5.7 全部接入。

---

### 阶段 5.7：页面契约下沉 + 逐族接入 ✅ 代码已完成，待真机回归

阶段 5.6 只解决了「页面能不能装进右栏」。真正卡住剩余页面的是另一件事：
**标题、菜单、返回键、`finish`、`setResult` 全写在 Activity 里，右栏没有 Activity**。
照搬的话，33 个带菜单的页面每个都要在右栏再表达一遍，两份实现必然各自漂移
（`EmployeeInfoActivity` 与改造前的 `UserInfoActivity` 就是逐行重复的两份）。

#### 契约：这些能力下沉到 Fragment，只写一份

| 接口 | 位置 | 职责 |
|---|---|---|
| `WfcPage` | 内容 Fragment 实现 | `pageMenu` / `onPreparePageMenu` / `onPageMenuItemSelected` / `pageTitle` / `providesOwnToolbar` / `onPageBackPressed` / `onPageIntent` / `onNewPageIntent` |
| `WfcPageHost` | `WfcBaseActivity` 与 `PanePageFragment` 各实现一次 | `setPageTitle` / `invalidatePageMenu` / `finishPage` / `setPageResult` / `isPaneHost` |
| `WfcPageCompat` | 静态桥 | 先沿**父 Fragment 链**找宿主，找不到再看 Activity。手机端父链上不存在 `WfcPageHost`，一定落到 Activity，与改造前逐字节等价 |
| `WfcPageNavigator` | `MainActivity` 实现 | `openPageInPane` / `replacePageInPane`，唯一能同时拿到**发起者**与**原始 requestCode** 的通道 |

`WfcPage` **全部是 default 方法**：列表类页面什么都不用实现；老页面不实现本接口也照常工作。
`PaneCompat` 已被 `WfcPageCompat` 取代。

#### 页面迁移模板（每页三步）

1. 页面整体搬进 `XxxPageFragment`（`implements WfcPage`），提供 `fromIntent(Intent)` 静态工厂；
2. 原 Activity 缩成薄壳：`contentLayout()` 返回 `fragment_container_activity`，
   `afterViews()` 里 commit 那个 Fragment，别的什么都不做；
3. `PaneRegistry` 加一行 `register(XxxActivity.class, (c, i) -> XxxPageFragment.fromIntent(i))`。

抽象基类若因此没有子类了（`SearchActivity`、`BasePickGroupMemberActivity`、
`PickOrCreateConversationActivity`、`PickConversationTargetActivity`），保留 + `@Deprecated`
javadoc 指向新基类，供 AAR 集成方过渡，仓库内不再派生。

#### 三条必须换写法的调用

| 场景 | 改造前 | 现在 | 不换会怎样 |
|---|---|---|---|
| 打开下一页 | `startActivity` | `WfcPageCompat.startPage(f, i)` | 能进右栏，但拿不到发起者，只能靠「上次按下点落在哪一栏」猜压栈还是换内容 |
| 打开选择器 | `startActivityForResult` | `WfcPageCompat.startPageForResult(f, i, code)` | **结果永远送不回来**：`Fragment.startActivityForResult` 的 requestCode 在到达主界面前已被 FragmentManager 换成内部生成的码 |
| 用完即弃的页面收尾 | `startActivity(next); finish();` | `WfcPageCompat.replaceSelfWithPage(f, i)` | 右栏里 `getActivity()` 是双栏主界面，`finish()` = 整个界面退出 |

选择器在右栏里回传结果：页面内 `setPageResult` + `finishPage`，
`PaneStackFragment` 在该页**出栈时**投递回发起方的 `onActivityResult`，
调用方两端写法完全一致。

#### 新增能力：页面自带标题栏

`WfcPage.providesOwnToolbar()` 返回 true 时 `PanePageFragment` 把整条 `AppBarLayout` 收起来
（只藏 `Toolbar` 会在顶上留一道背景与阴影）。搜索页顶部是「搜索框 + 取消」而不是
「标题 + 返回箭头」，手机端它们继承 `WfcBaseNoToolbarActivity`，右栏对应的就是这个开关。
显隐必须在内容 commit 的**同一帧**决定，晚一帧会看到一条标题栏闪过去。

#### 已接入的页面族（`PaneRegistry` 登记项 14 → 50 + App 侧 6）

| 族 | 页面 | 要点 |
|---|---|---|
| 用户资料 | `UserInfoActivity` / `EmployeeInfoActivity` | 菜单随「是否好友/拉黑/星标」变化，逻辑收进 `UserInfoFragment`，两个入口共用 |
| 短表单编辑 | 设置别名 / 改名 / 群名 / 群备注 / 群公告 | 共用 `TextEditPageFragment` |
| 群成员选择器 | 发起群通话选人 / 移出成员 / 禁言 / 加管理员 | 共用 `BasePickGroupMemberPageFragment`，均回传结果 |
| 建会话 | `CreateConversationActivity` / `AddGroupMemberActivity` | 前者建完直接把会话压上来，后者回传成功/失败 |
| 搜索 | 搜索总入口 / 会话内查找 / 添加朋友 / 查找频道 / @群成员 | 共用 `SearchPageFragment` + `SearchShellActivity`，`providesOwnToolbar()==true` |
| 转发 | `ForwardActivity` / `PickOrCreateConversationTargetActivity` / `PickConversationActivity` / `FileRecordListActivity` | 共用 `PickOrCreateConversationPageFragment`；会议邀请 `ConferenceInviteActivity` 同样下沉但**不登记**（入口是全屏会议界面） |
| 设置 | 设置 / 关于 / 诊断 / 字体大小 / 隐私设置 / 找到我的方式 / 消息通知 / 账号与安全 / 修改密码 / 重置密码 / 修改昵称 / 发好友申请 | 这一族原来是「UI 直接长在 Activity 里」，见下文 |

**设置一族的不同之处：layout 不复制，而是把 toolbar 从原布局里摘掉**

前几族的页面本来就是「壳 Activity + 现成 Fragment」，登记项只有一行。设置一族不是：12 个页面的
UI 全部直接写在 Activity 的 `bindViews` / `afterViews` 里，布局文件自己 `<include>` 了 toolbar。

处理办法不是复制一份去掉 toolbar 的布局（那会留下 12 对迟早漂移的孪生文件），而是**把
`<include layout="@layout/toolbar"/>` 从原布局里删掉，原布局原地改由页面 Fragment 使用**——
Activity 变成薄壳后用的是 `fragment_container_activity`，那里已经有一条 toolbar 了，
原布局不再被任何 Activity 引用。每个页面仍然只有一个布局文件。

App 自己的 6 个页面（设置、关于、诊断、账号与安全、改密、重置密码）住在 `chat` 模块，
`PaneRegistry` 在 uikit 里看不见它们，由 `AppPaneRegistry.register()` 在 `MyApp.onCreate` 里
自行登记 —— 这也正是把 uikit 当 aar 集成的第三方 App 登记自己页面的方式。

顺带修掉的两类真 bug：

- **搜索结果的 `fragment.getActivity().finish()`**（5 个 `SearchableModule`）。搜索页一进右栏，
  这句就是整个界面退出。收口成 `SearchableModule.openConversationAndFinishSearch` /
  `openPageAndFinishSearch` 两个 helper，未来新增模块自动继承正确行为。
- **`afterViews()` 里无条件 `add` 子 Fragment**。配置变化后会在恢复出来的那个之上再叠一层
  （手机端锁竖屏所以从没暴露）。迁移到 Fragment 时统一改成「先查有没有，没有才 add，
  监听器无条件重挂」。
- **消息通知设置页的对讲开关挂错了控件**（`MessageNotifySettingActivity`）：
  `switchPtt` 的监听被写成 `switchSyncDraft.setOnCheckedChangeListener`，覆盖掉了同步草稿
  自己的监听。结果是拨「同步草稿」会去写 `pttEnabled`，而「对讲」开关拨了完全没反应。

---

#### 阶段 5.7b：底部面板在宽屏下改为居中对话框

底部面板（`BottomSheetDialog`）是手机的形态：拇指够得到、内容贴着屏幕底边。同一个面板到了平板上
会横着铺满整条屏幕宽度，内容拉成又扁又长的一带，视线焦点还落在屏幕最边缘。宽屏下的等价形态是
居中对话框（微信平板端的转发确认框也是这么做的）。

| 新增 | 作用 |
|---|---|
| `WfcSheetDialogCompat` | `isCentered(Context)` 判形态；`create(Context)` 造「手机 BottomSheet / 宽屏居中 Dialog」；`applyCenteredWindow(Dialog)` 限宽居中 |
| `WfcBottomSheetDialogFragment` | `BottomSheetDialogFragment` 的替代基类，子类不需要知道自己是哪种形态 |
| `R.style.WfcCenteredDialog` | 居中形态的主题 |
| `drawable-sw600dp/shape_bottom_sheet_bg.xml` | 居中形态下四角都要圆（默认那份只圆上面两个角） |

三个要点：

- **判定用 `WfcDeviceUtils.isTwoPaneLayout()`（sw600dp）而不是「是不是平板设备」**。平板分屏到
  窄窗口时窗口本身已经和手机一样窄，那时底部面板才是对的形态。drawable 的限定符与它是同一个
  条件，不会出现「圆角按居中排、形态还是贴底」的错配。
- **主题父类必须留在 AppCompat 一系**。MaterialComponents 的主题会把布局里的 `<Button>` 换成
  `MaterialButton`，而这些面板的按钮都是自带 shape 背景的，换掉就丢背景。
- **父类的 BottomSheet 专属逻辑都是 `instanceof BottomSheetDialog` 保护的**，`onCreateDialog`
  返回普通 Dialog 会自动退化成 `super.dismiss()`，不会出问题。子类只有直接操作
  `BottomSheetBehavior` 的地方需要用 `isCenteredDialog()` 跳过（转发框的「键盘弹起时收掉底部
  按钮行」就是一处：那是贴底形态的补救，居中对话框里按钮一直看得见）。

已接入：转发确认框（`ForwardBottomSheetDialogFragment`）、日期时间选择器
（`DateTimePickerHelper`，会议创建/预约用）。VoIP 会议里的几个面板不接入——它们的宿主是全屏
会议界面，贴底才是对的。

**验收（本阶段追加）**
- 左栏右上角放大镜 / 加号 → 搜索、添加朋友开在右栏，顶部只有搜索框 + 取消，没有多余标题栏。
- 搜索结果点联系人/群/聊天记录 → 会话**顶替**掉搜索页，返回不再回到搜索页。
- 会话内长按消息 → 转发 → 选择列表压在会话上面，发完退回会话。
- 转发页右上角「多选」→ 底部计数栏出现，选中若干个 → 发送 → 一次发完。
- 转发页 →「新建会话」→ 选人页压在转发页上面，选完退回转发页并直接弹确认框。
- 「我」→ 文件 → 按会话/按发送人，两个选择器都在右栏，选完在同一条栈上打开文件列表。
- 群会话输入框打 `@` → 选人页在右栏，选中后 @ 正确插入。
- 「我」→ 设置 → 关于 / 诊断 / 字体大小 / 隐私设置 → 找到我的方式，逐层压在同一条栈上，
  返回逐层退回；「我」→ 账号与安全 → 修改密码 / 重置密码同理。
- 「我」→ 个人资料 → 昵称 → 修改昵称页在右栏，改完退回资料页且昵称已更新。
- 陌生人资料 → 添加到通讯录 → 申请页在右栏，发完退回。
- 设置里切换语言 / 主题 / 字体大小 → 应用整体重启（不是只关掉右栏那一页）。
- 转发确认框、会议日期选择器在平板上是**居中对话框**，手机上仍是底部面板。
- 以上每一项在**手机端**的表现与改造前一致。

**已知取舍**
- `PaneStackFragment.pendingResults` 在配置变化时清空：旋转时正开着的选择器拿不到回调。
  要跨重建找回发起方需要给每个 Fragment 分配可持久化身份，代价远大于收益。
- 页面内的 observe 一律用 `getViewLifecycleOwner()`，旋转会中断进行中的操作。

**仍然全屏**
- 朋友圈（原因见 5.6）。
- 媒体预览、音视频通话、扫码：本来就该全屏，**永远不要登记**。

---

### 阶段 6：平板平台身份与多端在线（依赖后端）

**目标**：平板作为 `PlatformType_APad(9)` 独立端，与手机同时在线。

**前置条件（必须先确认，否则本阶段不启动）**
- [ ] IM server 已授权多端在线能力；
- [ ] app server `/login_pwd`、短信登录接口接受 `platform=9` 并签发对应 token；
- [ ] 产品确认：手机端会话列表顶部将出现「Pad 已登录」状态条（`PCOnlineNotificationViewHolder.java:45` 已支持）——**这是本次适配对手机端唯一的可见影响，必须产品拍板**。

**改动**

1. `MyApp.onCreate()`（`chat/.../app/MyApp.java`，`wfcUIKit.init(this)` 之后、`connect()` 之前）：
   ```java
   if (WfcDeviceUtils.isPadDevice(this)) {
       ChatManager.Instance().setPlatform(true);
   }
   ```
   登录侧无需改动——`AppService.passwordLogin` 已经在用 `ChatManager.Instance().getPlatform().value()`。

2. 平台身份变更保护：`isPadDevice()` 首启持久化后不再变；若检测到持久值与当前设备不符（如数据迁移到新设备），强制清 token 重新登录，避免 token/platform 不匹配导致连不上。

3. 平板上「PC 端管理」入口（`PCSessionActivity`）需支持展示与踢下线 Pad 端会话。

**手机端保证**：`isPadDevice()` 在手机恒 false，`setPlatform` 不会被调用，platform 仍为 2，登录链路完全不变。唯一影响是"当用户另有平板在线时，手机会话列表出现在线状态条"——这是既有 PC 在线能力的自然延伸，UI 代码已存在。

**验收**：平板登录后手机不掉线；双端消息同步；已读同步正确；两端都能在 PC 会话管理里看到对方。

**风险**：**高**，依赖服务端能力。若后端不支持，本阶段整体跳过——L0~L3 仍可独立发布，平板此时表现为"一个大屏手机端"（登录会踢掉手机），需产品接受。

**工作量**：2~3 人日（客户端）+ 后端联调

---

### 阶段 7：音视频与相机

**目标**：解决阶段 1 暂缓的 3 类锁竖屏页面。

**改动**
- `SingleCallActivity` / `MultiCallActivity`：解锁横屏，修 `MultiCallAudioFragment.java:99,207`、`MultiCallVideoFragment.java:128,284,415`、`ConferenceParticipantGridView.java:94-96`、`VideoConferenceMainView.java:99,108,314-315` 中基于 `widthPixels` 的宫格尺寸计算，改为按容器实际尺寸；
- `TakePhotoActivity` + `cameraview`：解锁横屏后校正预览方向与拍摄结果方向（平板默认摄像头方向常与手机相反）；
- 无听筒/无距离传感器：平板上隐藏「听筒播放」设置项，`AudioPlayManager.java:228` 在 `getDefaultSensor(TYPE_PROXIMITY)` 为 null 时跳过；
- `ConferenceActivity` 本就未锁竖屏，需在平板横屏下实测布局。

**手机端保证**：D2 —— 音视频页在手机上继续走 `SCREEN_ORIENTATION_PORTRAIT`；尺寸计算改为按容器取值，手机全屏下与 `widthPixels` 等值。

**验收**：手机音视频全流程与基线一致；平板横竖屏通话、会议九宫格布局正常。

**工作量**：5~8 人日

---

### 阶段 8：测试、灰度与文档

- 完整跑第六章测试矩阵；
- 平板单独渠道包灰度（或按 `isPadDevice()` 做远端开关，出问题可一键回退到单栏）；
- 更新 `CLAUDE.md` / `README.md`：新增"平板适配"章节，说明 `wfc_two_pane`、`WfcDeviceUtils`、`ConversationHost`、`ConversationRouter` 四个新概念。

**工作量**：3~5 人日

---

## 五、风险与回退

| 风险 | 等级 | 影响 | 缓解 / 回退 |
|------|------|------|-------------|
| 阶段 1 解锁横屏后旋转导致状态丢失/崩溃面广 | 高 | 平板可用性 | 分模块灰度；实在难修的个别页面**单独**保留 `screenOrientation="portrait"`，不影响整体 |
| 阶段 3 重构 `ConversationFragment`（1696 行）引入手机端回归 | 高 | **触碰硬约束** | 只搬运不优化；逐耦合点独立 commit；会话页全量回归 + 截图比对 |
| 双栏 ↔ 单栏动态切换（分屏拖动、折叠屏开合） | 中高 | 平板崩溃/丢会话 | 单独设计状态传递；`onSaveInstanceState` 保存当前会话；专项测试 |
| 阶段 6 依赖后端多端在线能力 | 中 | 阶段整体不可做 | L0~L3 与 L4 完全解耦，可只发布 L0~L3 |
| 平板出现「Pad 在线」状态条影响手机 UI | 中 | 硬约束的**唯一例外**，需产品确认 | 阶段 6 的前置卡点；不确认就不做阶段 6 |
| `mars-core-release.aar` 为官方服务锁定版 | 低 | 与网络相关的异常可能是授权限制而非 bug | 见 `CLAUDE.md`；排查网络问题时先排除该因素 |

**回退设计**：L1~L3 的所有平板行为都由 `wfc_two_pane` / `isPadDevice()` 控制。极端情况下把 `values-sw600dp/bools.xml` 的 `wfc_two_pane` 改回 `false`、`isPadDevice()` 恒返回 false，平板即退化为"大屏手机端"，手机端不受任何影响。

---

## 六、测试矩阵

### 6.1 手机端必测清单（每阶段结束都要跑，与基线截图比对）

1. 启动 → 闪屏 → 主界面，5 个 Tab 切换与 ViewPager 滑动、toolbar 颜色渐变（`MainActivity.java:885` `updateToolbar`）
2. 会话列表 → 进入会话 → 返回；未读角标、置顶、免打扰
3. 会话页：发文本/图片/语音/文件/位置、表情面板、扩展面板、@、引用、多选转发/删除、消息长按菜单
4. 会话页标题：普通标题、副标题、静音图标、听筒图标（阶段 3 重点）
5. 会话信息页、群管理、群成员宫格
6. 搜索（会话/联系人/群/消息）、按日期查找、链接记录、图片与视频
7. 通讯录、新朋友、用户详情、发起单聊
8. 音视频：单人音频/视频、多人、会议
9. 拍照/相册选择/图片预览
10. 分享进入（`ACTION_SEND` 文本/图片/文件）
11. 通知点击进入会话
12. 设置：字体大小、语言、深色主题、听筒播放、备份恢复
13. 朋友圈：列表、发布、详情、消息、可见范围
14. **全程确认无法横屏**——唯一例外是朋友圈的 8 个页面，它们改动前就可横屏（见 B6），基线中需如实记录，不要当成回归

### 6.2 平板测试设备矩阵

| 维度 | 覆盖 |
|------|------|
| 尺寸 | 8"（sw600~720dp）、11"（sw800dp+）、13" |
| 系统 | Android 9 / 11 / 13 / 14（`minSdk 24`、`targetSdk 34`） |
| 厂商 | 华为（HarmonyOS 兼容层）、小米、三星、联想 |
| 形态 | 纯平板、折叠屏展开/折叠、平板 + 键盘、分屏 1/2 与 1/3、自由窗口 |
| 特殊 | 无摄像头机型、无距离传感器机型、无 SIM 机型 |

### 6.3 平板专项

- 旋转 20 次不崩溃、不丢会话与草稿
- 分屏拖动跨越 600dp 阈值（双栏 ↔ 单栏来回切换）
- 折叠屏开合
- 外接键盘：Tab 焦点顺序、Enter 发送
- 多端在线（阶段 6）：平板 + 手机同时在线的消息/已读同步

---

## 七、工作量汇总

| 阶段 | 内容 | 人日 | 可独立发布 |
|------|------|------|-----------|
| 0 | 基础设施与基线 | 0.5 | — |
| 1 | 横屏解锁 | 3~5 | ✅ |
| 2 | 宽屏资源适配 | 5~8 | ✅ |
| 3 | 会话宿主解耦（重构） | 3~5 | ✅（无可见变化） |
| 4 | 主界面双栏 | 8~12 | ✅ |
| 5 | 入口路由统一 | 4~6 | ✅ |
| 6 | Pad 平台身份 | 2~3 + 后端 | ✅（可跳过） |
| 7 | 音视频与相机 | 5~8 | ✅ |
| 8 | 测试灰度与文档 | 3~5 | — |
| | **合计** | **34~53 人日** | |

**最小可用版本（MVP）**：阶段 0+1+2 ≈ 9~14 人日，平板即可横屏且不再是拉伸的手机界面。
**完整平板体验**：阶段 0~5 ≈ 24~37 人日。

---

## 八、待决策项

| # | 决策 | 影响 | 建议 |
|---|------|------|------|
| Q1 | 平板是否使用 `platform=9` 与手机多端同时在线？ | 决定阶段 6 是否启动；决定手机端是否出现「Pad 在线」状态条（硬约束的唯一例外） | 建议做，但需后端先确认能力 |
| Q2 | 双栏阈值取 `sw600dp` 还是 `sw720dp`？ | 600dp 会让大屏手机横屏、折叠屏展开态也进双栏 | 建议 **600dp**（Android 官方平板基线），若不希望折叠屏进双栏则用 720dp |
| ~~Q3~~ | 切到「联系人/发现/我」Tab 时右栏是否保留当前会话？ | 双栏核心交互手感 | **已定**：每个 tab 一条独立栈，切过去看到的是该 tab 自己的内容（阶段 5.5） |
| Q4 | 平板是同一个 APK 还是独立渠道包？ | 灰度与回退粒度 | 建议同一 APK + 运行时判定，便于折叠屏；灰度靠远端开关 |
| Q5 | 平板是否需要独立的应用图标/名称/启动页？ | 视觉资源工作量 | 建议复用 |
| Q6 | 阶段 1 是否接受"横屏可用但不够美观"作为中间态发布？ | 影响发布节奏 | 建议接受，阶段 2 补齐 |

---

## 附录：关键文件索引

| 用途 | 文件 |
|------|------|
| 平板判定与开关（阶段 0 已落地） | `uikit/src/main/java/cn/wildfire/chat/kit/utils/WfcDeviceUtils.java`<br>`uikit/src/main/res/values/bools.xml`（`wfc_two_pane=false`）<br>`uikit/src/main/res/values-sw600dp/bools.xml`（`wfc_two_pane=true`） |
| 全局竖屏锁 | `uikit/src/main/java/cn/wildfire/chat/kit/WfcBaseActivity.java:46`<br>`uikit/src/main/java/cn/wildfire/chat/kit/WfcBaseNoToolbarActivity.java:34` |
| 朋友圈独立基类（B6，另一仓库） | `../android-momentkit/src/main/java/cn/wildfire/chat/moment/thirdbar/BaseStatusControlActivity.java` |
| Manifest 竖屏锁 | `chat/src/main/AndroidManifest.xml`（13 处）<br>`uikit/src/main/AndroidManifest.xml`（22 处）<br>`push/src/main/AndroidManifest.xml`（1 处） |
| 摄像头 uses-feature | `chat/src/main/AndroidManifest.xml:26-27`<br>`uikit/src/main/AndroidManifest.xml:21-22` |
| 主界面 | `chat/src/main/java/cn/wildfire/chat/app/main/MainActivity.java`<br>`chat/src/main/res/layout/main_activity.xml` |
| 会话页 | `uikit/src/main/java/cn/wildfire/chat/kit/conversation/ConversationActivity.java`<br>`uikit/src/main/java/cn/wildfire/chat/kit/conversation/ConversationFragment.java`（1696 行）<br>`uikit/src/main/res/layout/fragment_container_activity.xml` |
| Pad 平台能力 | `client/src/main/java/cn/wildfirechat/remote/ChatManager.java:1297`（`setPlatform`）<br>`client/src/main/java/cn/wildfirechat/client/Platform.java`（`PlatformType_APad(9)`）<br>`client/src/main/java/cn/wildfirechat/client/ClientService.java:4796-4800`<br>`chat/src/main/java/cn/wildfire/chat/app/AppService.java:205` |
| 多端在线 UI | `client/src/main/java/cn/wildfirechat/model/PCOnlineInfo.java`<br>`uikit/.../conversationlist/notification/viewholder/PCOnlineNotificationViewHolder.java:45` |
| 应用初始化 | `chat/src/main/java/cn/wildfire/chat/app/MyApp.java`<br>`chat/src/main/java/cn/wildfire/chat/app/main/SplashActivity.java` |
