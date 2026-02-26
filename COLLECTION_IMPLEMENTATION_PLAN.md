# Android端接龙功能实施计划

## 一、功能概述

接龙（Collection）是一种群聊互动功能，允许群成员围绕一个主题进行报名、登记等接龙活动。

### 特性
- **消息类型**: 17 (与iOS MESSAGE_CONTENT_TYPE_COLLECTION 保持一致)
- **仅支持群聊**: 单聊不支持接龙功能
- **核心功能**: 创建接龙、参与接龙、编辑参与内容、删除参与、关闭接龙

### 与iOS的互操作性
- 消息类型与iOS完全一致（type=17）
- 数据模型字段与iOS保持一致
- 后端API使用相同的接口规范

---

## 二、后端API接口

基于iOS实现，接龙服务提供以下REST API：

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 创建接龙 | POST | `/api/collections` | 创建新的接龙 |
| 获取详情 | POST | `/api/collections/{id}/detail` | 获取接龙详情 |
| 参与接龙 | POST | `/api/collections/{id}/join` | 参与或更新参与内容 |
| 删除参与 | POST | `/api/collections/{id}/delete` | 删除自己的参与 |
| 关闭接龙 | POST | `/api/collections/{id}/close` | 关闭接龙（创建者） |

### 认证方式
使用`authCode`进行身份验证（通过`ChatManager.getAuthCode`获取）

---

## 三、实施阶段

### 阶段1: Client模块 - 数据模型（第1-2天）

#### 1.1 添加消息类型常量
- **文件**: `client/src/main/java/cn/wildfirechat/message/core/MessageContentType.java`
- **位置**: 在第41行后添加
- **内容**:
```java
int ContentType_Collection = 17;  // 接龙消息，与iOS MESSAGE_CONTENT_TYPE_COLLECTION 保持一致
```

#### 1.2 创建接龙参与条目类
- **文件**: `client/src/main/java/cn/wildfirechat/message/CollectionEntry.java`
- **字段**:
  - `entryId`: long
  - `collectionId`: long
  - `userId`: String
  - `content`: String
  - `createdAt`: long
  - `updatedAt`: long
  - `deleted`: int

#### 1.3 创建接龙消息内容类
- **文件**: `client/src/main/java/cn/wildfirechat/message/CollectionMessageContent.java`
- **注解**: `@ContentTag(type = ContentType_Collection, flag = PersistFlag.Persist_And_Count)`
- **字段**:
  - `collectionId`: String
  - `groupId`: String
  - `creatorId`: String
  - `title`: String
  - `desc`: String
  - `template`: String
  - `expireType`: int (0=无限期, 1=有限期)
  - `expireAt`: long
  - `maxParticipants`: int
  - `status`: int (0=进行中, 1=已结束, 2=已取消)
  - `entries`: List<CollectionEntry>
  - `createdAt`: long
  - `updatedAt`: long
- **方法**:
  - `encode()`: 将title放入searchableContent，其他数据JSON编码放入binaryContent
  - `decode()`: 从payload解析数据
  - `digest()`: 返回"[接龙] " + title
  - Parcelable序列化方法

---

### 阶段2: UIKit模块 - 网络服务（第2-3天）

#### 2.1 创建接龙数据模型
- **文件**: `uikit/src/main/java/cn/wildfire/chat/kit/collection/model/Collection.java`
- **文件**: `uikit/src/main/java/cn/wildfire/chat/kit/collection/model/CollectionEntry.java`
- **说明**: UI层使用的数据模型，与客户端模型区分

#### 2.2 创建接龙服务接口
- **文件**: `uikit/src/main/java/cn/wildfire/chat/kit/collection/CollectionService.java`
- **方法**:
```java
void createCollection(String groupId, String title, String desc, String template,
    int expireType, long expireAt, int maxParticipants,
    Callback<Collection> successCallback, Callback<Error> errorCallback);

void getCollection(long collectionId, String groupId,
    Callback<Collection> successCallback, Callback<Error> errorCallback);

void joinCollection(long collectionId, String groupId, String content,
    Callback<Void> successCallback, Callback<Error> errorCallback);

void deleteEntry(long collectionId, String groupId,
    Callback<Void> successCallback, Callback<Error> errorCallback);

void closeCollection(long collectionId, String groupId,
    Callback<Void> successCallback, Callback<Error> errorCallback);
```

#### 2.3 创建接龙服务实现
- **文件**: `chat/src/main/java/cn/wildfire/chat/app/collection/CollectionServiceImpl.java`
- **功能**:
  - 封装HTTP请求到接龙后端服务
  - 使用`OKHttpHelper`进行网络请求
  - 通过`ChatManager.getAuthCode`获取认证码
  - 基础URL可配置

---

### 阶段3: UIKit模块 - 消息展示（第3-4天）

#### 3.1 创建接龙消息ViewHolder
- **文件**: `uikit/src/main/java/cn/wildfire/chat/kit/conversation/message/viewholder/CollectionMessageContentViewHolder.java`
- **注解**:
  - `@MessageContentType(CollectionMessageContent.class)`
  - `@EnableContextMenu`（可选）
- **布局文件**:
  - `uikit/src/main/res/layout/conversation_item_collection_send.xml`
  - `uikit/src/main/res/layout/conversation_item_collection_receive.xml`
- **UI设计**:
  - 显示接龙图标
  - 显示标题
  - 显示参与人数
  - 显示前3条参与记录
  - 显示"参与接龙"操作按钮

#### 3.2 注册ViewHolder
- **文件**: `chat/src/main/java/cn/wildfire/chat/app/MyApp.java`
- **代码**:
```java
MessageViewHolderManager.getInstance().registerMessageViewHolder(
    CollectionMessageContentViewHolder.class,
    R.layout.conversation_item_collection_send,
    R.layout.conversation_item_collection_receive
);
```

---

### 阶段4: UIKit模块 - 创建接龙界面（第4-5天）

#### 4.1 创建接龙Activity
- **文件**: `uikit/src/main/java/cn/wildfire/chat/kit/collection/CreateCollectionActivity.java`
- **布局**: `uikit/src/main/res/layout/activity_create_collection.xml`
- **包含字段**:
  - 标题输入框（必填）
  - 描述输入框（选填）
  - 模板输入框（选填，如"姓名-电话"）
  - 过期时间设置（无限期/设置日期）
  - 最大参与人数（0表示无限制）

#### 4.2 在输入栏插件面板添加接龙按钮
- 找到Android项目中的输入插件面板实现
- 仅群聊时显示接龙按钮
- 点击跳转CreateCollectionActivity

---

### 阶段5: UIKit模块 - 接龙详情界面（第5-6天）

#### 5.1 创建接龙详情Activity
- **文件**: `uikit/src/main/java/cn/wildfire/chat/kit/collection/CollectionDetailActivity.java`
- **布局**: `uikit/src/main/res/layout/activity_collection_detail.xml`
- **功能**:
  - 显示接龙详细信息（标题、描述、发起人、参与人数）
  - 显示所有参与记录列表
  - 当前用户的参与输入/编辑框
  - 关闭接龙按钮（仅创建者）
  - 删除参与按钮

#### 5.2 创建参与记录列表Adapter
- **文件**: `uikit/src/main/java/cn/wildfire/chat/kit/collection/CollectionEntryAdapter.java`
- **单项布局**: `uikit/src/main/res/layout/item_collection_entry.xml`

#### 5.3 处理消息点击事件
- **文件**: `uikit/src/main/java/cn/wildfire/chat/kit/conversation/ConversationFragment.java`
- 在消息点击处理中添加接龙消息的判断，跳转到详情页

---

### 阶段6: 配置与资源（第6-7天）

#### 6.1 添加配置项
- **文件**: `chat/src/main/java/cn/wildfire/chat/app/Config.java`
- **代码**:
```java
public static String COLLECTION_SERVER_ADDRESS = "https://jielong.wildfirechat.net"; // null表示不启用
```

#### 6.2 添加字符串资源
- **文件**: `uikit/src/main/res/values/strings.xml`
- **文件**: `uikit/src/main/res/values-zh-rCN/strings.xml`
- **字符串列表**:
  - `collection` - "接龙"
  - `create_collection` - "创建接龙"
  - `collection_title` - "标题"
  - `collection_title_hint` - "请输入接龙标题"
  - `collection_desc` - "描述"
  - `collection_template` - "模板"
  - `collection_template_hint` - "请输入模板（如：姓名-电话）"
  - `collection_detail` - "接龙详情"
  - `collection_participant_count` - "%d人参与"
  - `collection_join` - "参与接龙"
  - `collection_close` - "关闭接龙"
  - `collection_only_for_group` - "接龙功能仅支持群聊使用"
  - `collection_empty_hint` - "暂无参与，快来抢沙发吧~"
  - `collection_status_ended` - "接龙已结束"
  - `collection_status_cancelled` - "接龙已取消"

#### 6.3 添加图标资源
- 接龙功能图标: `uikit/src/main/res/drawable-xxhdpi/ic_collection.png`

---

### 阶段7: 测试与优化（第7-8天）

#### 7.1 功能测试
- [ ] 创建接龙
- [ ] 参与接龙
- [ ] 编辑参与内容
- [ ] 删除参与
- [ ] 关闭接龙
- [ ] 消息同步（多端）

#### 7.2 与iOS互操作性测试
- [ ] iOS创建，Android查看和参与
- [ ] Android创建，iOS查看和参与

#### 7.3 边界情况处理
- [ ] 网络异常
- [ ] 接龙已过期
- [ ] 达到最大参与人数
- [ ] 重复参与（更新逻辑）

---

## 四、关键文件清单

| 模块 | 文件路径 | 说明 |
|------|----------|------|
| client | `client/src/main/java/cn/wildfirechat/message/core/MessageContentType.java` | 添加消息类型常量 |
| client | `client/src/main/java/cn/wildfirechat/message/CollectionEntry.java` | 接龙条目模型 |
| client | `client/src/main/java/cn/wildfirechat/message/CollectionMessageContent.java` | 接龙消息内容 |
| uikit | `uikit/src/main/java/cn/wildfire/chat/kit/collection/model/Collection.java` | UI层接龙模型 |
| uikit | `uikit/src/main/java/cn/wildfire/chat/kit/collection/model/CollectionEntry.java` | UI层接龙条目模型 |
| uikit | `uikit/src/main/java/cn/wildfire/chat/kit/collection/CollectionService.java` | 服务接口 |
| chat | `chat/src/main/java/cn/wildfire/chat/app/collection/CollectionServiceImpl.java` | 服务实现 |
| uikit | `uikit/src/main/java/cn/wildfire/chat/kit/collection/CreateCollectionActivity.java` | 创建接龙界面 |
| uikit | `uikit/src/main/java/cn/wildfire/chat/kit/collection/CollectionDetailActivity.java` | 接龙详情界面 |
| uikit | `uikit/src/main/java/cn/wildfire/chat/kit/collection/CollectionEntryAdapter.java` | 参与记录列表Adapter |
| uikit | `uikit/src/main/java/cn/wildfire/chat/kit/conversation/message/viewholder/CollectionMessageContentViewHolder.java` | 消息ViewHolder |
| uikit | `uikit/src/main/res/layout/conversation_item_collection_send.xml` | 发送布局 |
| uikit | `uikit/src/main/res/layout/conversation_item_collection_receive.xml` | 接收布局 |
| uikit | `uikit/src/main/res/layout/activity_create_collection.xml` | 创建接龙布局 |
| uikit | `uikit/src/main/res/layout/activity_collection_detail.xml` | 接龙详情布局 |
| uikit | `uikit/src/main/res/layout/item_collection_entry.xml` | 参与记录项布局 |
| chat | `chat/src/main/java/cn/wildfire/chat/app/Config.java` | 添加配置项 |
| chat | `chat/src/main/java/cn/wildfire/chat/app/MyApp.java` | 注册ViewHolder |

---

## 五、注意事项

### 5.1 消息类型一致性
Android使用17，与iOS的`MESSAGE_CONTENT_TYPE_COLLECTION`保持一致。

### 5.2 仅群聊限制
接龙功能仅在群聊中可用，单聊需要提示用户。

### 5.3 认证方式
使用`ChatManager.getAuthCode`获取`authCode`进行API认证，与iOS保持一致。

### 5.4 服务可配置
接龙服务地址应可配置，为null时隐藏接龙功能入口。

### 5.5 数据序列化
接龙消息的`title`字段放入`searchableContent`用于搜索，其他数据JSON编码后放入`binaryContent`。

### 5.6 权限控制
- 仅创建者可以关闭接龙
- 仅参与者自己可以删除/修改自己的参与内容

### 5.7 并发处理
参与接龙时考虑并发情况，后端会处理冲突。

---

## 六、iOS参考代码位置

| 功能 | iOS文件路径 |
|------|-------------|
| 消息内容模型 | `wfclient/WFChatClient/Messages/WFCCCollectionMessageContent.h/.m` |
| UI数据模型 | `wfuikit/WFChatUIKit/Collection/Model/WFCUCollection.h/.m` |
| 服务协议 | `wfuikit/WFChatUIKit/Collection/Model/WFCUCollectionService.h` |
| 创建接龙 | `wfuikit/WFChatUIKit/Collection/ViewController/WFCUCreateCollectionViewController.h/.m` |
| 接龙详情 | `wfuikit/WFChatUIKit/Collection/ViewController/WFCUCollectionDetailViewController.h/.m` |
| 消息Cell | `wfuikit/WFChatUIKit/MessageList/Cell/WFCUCollectionCell.h/.m` |
| 服务实现 | `wfchat/WildFireChat/CollectionService/CollectionService.h/.m` |
| 输入栏集成 | `wfuikit/WFChatUIKit/Vendor/ChatInputBar/WFCUChatInputBar.m` |
| 插件面板 | `wfuikit/WFChatUIKit/Vendor/ChatInputBar/WFCUPluginBoardView.m` |
