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

### 阶段 2：宽屏资源适配（`-sw600dp`）

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

---

### 阶段 3：会话页宿主解耦（纯重构，行为等价）

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

---

### 阶段 4：主界面双栏

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

---

### 阶段 5：入口路由统一

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
| Q3 | 切到「联系人/发现/我」Tab 时右栏是否保留当前会话？ | 双栏核心交互手感 | 建议保留（对齐微信 Pad） |
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
