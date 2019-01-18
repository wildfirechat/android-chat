# 这是什么？

这是火信IM解决方案的Android端，包含以下模块：

1. client库是IM的通讯能力，提供基本的通讯能力
2. chat是基于client的开发demo，是一个完整IM应用。提供了大量可重用的基础UI组件，比如会话列表、会话界面等，可在其基础上，快速新增功能，二次开发。
3. avenginekit是1对1的音视频库，依赖于client。
4. avdemo是音视频的demo。
5. push是用来处理推送的SDK。
6. emojilibarary是表情组件，提供emoji表情和动态表情支持，且可添加自定义表情。
7. imagepicker是图片选取组件
8. j

# 有什么用？
# 怎么用？

请参考[Android 开发](http://xxx)

0. 自定义消息
    1. 继承```MessageContent```，并添加```ContentTag```注解，用于标识消息类型和消息存储类型。自定义消息类型要求大于1000。消息类型，参考```MessageContentType```；消息存储类型，参考```PersistFlag```。
    2. 调用```ChatManager#registerMessageContent```注册新消息类型，或者直接修改```ClientService#onCreate```进行消息注册

1. 自定义消息UI

   1. 普通消息，靠边显示，显示发送者。

      继承```NormalMessageContentViewHolder```，并添加响应注解，参考```TextMessageContentViewHolder```

   2. 通知消息，居中显示，不显示消息发送者。

      1. 简单/小灰条通知，界面只显示一个小灰条

         自定义通知类消息后，将其加入到```SimpleNotificationMessageContentViewHolder```的```MessageContentType```里面即可

      2. 复杂通知，支持定义UI，显示比较复杂的界面

         继承```NotificationMessageContentViewHolder```，实现相关方法，并添加对应的```MessageContent```和布局注解，请参考```ExampleRichNotificationMessageContentViewHolder```

   3. 响应用户动作

      1. 单击

         用```@OnClick```注解单击的响应方法，用法和[*butterknife*](https://github.com/JakeWharton/butterknife)一致。参考```ExampleRichNotificationMessageContentViewHolder#onClick```，支持点击消息的不同UI作出不同的响应。

      2. 长按

         用```@MessageContextMenuItem```注解长按响应方法，当有多个长按响应时，会弹出选择对话框，让用户选择选择目标响应，可参考```ExampleRichNotificationMessageContentViewHolder#forwardMessage```。另外，支持根据不同的消息、会话，过滤长按菜单选项，可参考```ExampleRichNotificationMessageContentViewHolder#contextMenuItemFilter```

      3. 双击，目前尚未支持

   4. 注册自定义消息UI

      调用```MessageViewHolderManager#registerMessageViewHolder```注册新的消息UI，或者直接修改```MessageViewHolderManager#init```进行注册。

2. 自定义会话类型

   1. 火信根据会话类型和会话线路唯一标识一种会话类型，故可以采用相应会话类型(单聊、群里等)组合不同会话线路实现自定义会话

   2. 继承```ConversationViewHolder```自定义会话类型UI，添加```ConversationInfoType```和```@EnableContextMenu```注解，其中```@EnableContextMenu```可选，表示是否允许长按操作。

   3. 注册定义会话类型UI

      调动```ConversationViewHolderManager#registerConversationViewHolder```，或者直接修改```ConversationViewHolderManager```进行自定义会话类型UI的注册。

3. 自定义会话扩展

   会话扩展指的是：会话界面，点击+号，弹出的那些快捷功能。

   1. 自定义扩展

      继承```ConversationExt```，并实现设置icon等相关方法，用```ExtContextMenuItem```注解需要响应点击的方法，当有多个时，会弹出选择对话框。如果需要使用```startActivityForResult```方法，需用```ConversationExt#startActivityForResult```，以保证正确回调```onActivityResult```，可参考```FileExt```

   2. 如果需要实现类似微信语音输入的扩展，可参考```ExampleAudioInputExt```

   3. 注册扩展

      调用```ConversationExtManager#registerExt```，或直接修改```ConversationExtManager#init```进行扩展注册。

4. 自定义会话列表状态通知

   1. 继承```StatusNotification```，并实现其方法，
   2. 调用```ConversationListFragment#showNotification```显示通知，调用```ConversationListFragment#clearNotification```清除通知。

5. 修改会话列表长按菜单

   参考```ConversationViewHolder#removeConversation```和```ConversationViewHolder#contextMenuItemFilter```

6. 自定义搜索

   1. 继承```SearchableModule```，并实现其方法，参考```GroupSearchViewModule```
   2. 参考```SearchPortalActivity#initSearchModule```配置当前想进行的搜索项，参考```SearchPortalActivity#search```开始搜索

7. 推送继承

## 说明

