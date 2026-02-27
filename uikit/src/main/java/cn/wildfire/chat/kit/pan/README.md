# Android 网盘功能集成文档

## 一、概述

Android端网盘功能与iOS端保持一致，提供以下功能：

1. **发现页面入口** - 在发现页面显示网盘入口（可配置显示/隐藏）
2. **输入框扩展** - 在聊天输入面板添加网盘扩展按钮
3. **网盘空间列表** - 显示三个空间：全局公共空间、我的公共空间、我的私有空间
4. **文件列表** - 显示空间内的文件和文件夹
5. **文件操作** - 创建文件夹、重命名、移动、复制、删除
6. **保存到网盘** - 从文件消息长按菜单保存到网盘
7. **从网盘发送文件** - 在输入面板中选择网盘文件发送

## 二、功能入口

### 1. 在 Config.java 中配置网盘服务地址

```java
// uikit/src/main/java/cn/wildfire/chat/kit/Config.java

/**
 * 网盘服务地址，如果需要网盘功能，请部署网盘服务，然后这里填上网盘服务地址；如果不需要网盘功能，请置为 null
 * 示例：http://192.168.1.81:8081
 */
public static String PAN_SERVER_ADDRESS = "http://192.168.1.81:8081";
```

### 2. 在 MyApp.java 中初始化网盘服务

```java
// chat/src/main/java/cn/wildfire/chat/app/MyApp.java

// 导入网盘服务
import cn.wildfire.chat.app.pan.PanServiceProvider;
import cn.wildfire.chat.app.pan.PanServiceImpl;

// 在 onCreate 方法中初始化网盘服务：
// 初始化网盘服务
if (!TextUtils.isEmpty(Config.PAN_SERVER_ADDRESS)) {
    PanServiceImpl.getInstance().setBaseUrl(Config.PAN_SERVER_ADDRESS);
    PanServiceProvider.init(Config.PAN_SERVER_ADDRESS);
}
```

## 三、集成步骤

### 1. 配置网盘服务地址

编辑 `uikit/src/main/java/cn/wildfire/chat/kit/Config.java`：

```java
public static String PAN_SERVER_ADDRESS = "http://your-pan-server:8081";
// 如果不需要网盘功能，设置为 null
// public static String PAN_SERVER_ADDRESS = null;
```

### 2. 注册 Activity

在 `AndroidManifest.xml` 中注册网盘相关 Activity：

```xml
<!-- 网盘空间列表 -->
<activity
    android:name="cn.wildfire.chat.kit.pan.PanSpaceListActivity"
    android:label="@string/pan_title" />

<!-- 网盘文件列表 -->
<activity
    android:name="cn.wildfire.chat.kit.pan.PanFileListActivity"
    android:label="@string/pan_title" />

<!-- 保存到网盘 -->
<activity
    android:name="cn.wildfire.chat.kit.pan.PanSaveActivity"
    android:label="@string/pan_save_to" />

<!-- 目标选择（移动/复制） -->
<activity
    android:name="cn.wildfire.chat.kit.pan.PanTargetSelectActivity"
    android:label="@string/pan_title" />

<!-- 网盘文件选择器（从网盘发送文件） -->
<activity
    android:name="cn.wildfire.chat.kit.pan.PanFilePickerActivity"
    android:label="@string/pan_select_file" />
```

### 3. 打开网盘

```java
// 打开网盘空间列表
PanSpaceListActivity.start(context);
```

### 4. 添加网盘入口

在设置页面或其他合适的位置添加网盘入口：

```java
// 示例：在设置页面添加网盘入口
Preference panPreference = new Preference(context);
panPreference.setTitle(R.string.pan_title);
panPreference.setOnPreferenceClickListener(preference -> {
    PanSpaceListActivity.start(context);
    return true;
});
```

## 四、功能说明

### 网盘空间

| 空间类型 | 名称 | 访问权限 | 管理权限 |
|---------|------|---------|---------|
| GLOBAL_PUBLIC | 全局公共空间 | 所有人可读 | 全局管理员可写 |
| USER_PUBLIC | 我的公共空间 | 所有人可读 | 自己可管理 |
| USER_PRIVATE | 我的私有空间 | 仅自己可访问 | 自己可管理 |

### 文件操作

- **创建文件夹**：在文件列表页面点击右上角菜单
- **重命名**：长按文件/文件夹，选择重命名
- **移动**：长按文件/文件夹，选择移动到
- **复制**：长按文件/文件夹，选择复制到
- **删除**：长按文件/文件夹，选择删除

### 保存到网盘

在聊天界面长按文件消息，选择"保存到网盘"，然后选择目标空间即可。

### 从网盘发送文件

在输入面板的插件栏中点击"网盘"按钮，选择要发送的文件。

## 五、项目文件结构

```
android-chat/
├── chat/src/main/java/cn/wildfire/chat/app/pan/
│   ├── PanServiceImpl.java          # 服务实现
│   └── PanServiceProvider.java      # 服务提供者
│
├── uikit/src/main/java/cn/wildfire/chat/kit/pan/
│   ├── PanSpaceListActivity.java    # 空间列表
│   ├── PanSpaceListAdapter.java     # 空间适配器
│   ├── PanFileListActivity.java     # 文件列表
│   ├── PanFileListAdapter.java      # 文件适配器
│   ├── PanSaveActivity.java         # 保存到网盘
│   ├── PanSaveSpaceAdapter.java     # 保存适配器
│   ├── PanTargetSelectActivity.java # 目标选择
│   ├── PanTargetSelectAdapter.java  # 目标适配器
│   ├── model/
│   │   ├── PanSpace.java            # 空间模型
│   │   ├── PanFile.java             # 文件模型
│   │   ├── CreateFileRequest.java   # 创建请求
│   │   └── Result.java              # 响应结果
│   └── api/
│       └── PanService.java          # 服务接口
│
└── uikit/src/main/res/
    ├── layout/
    │   ├── activity_pan_space_list.xml
    │   ├── activity_pan_file_list.xml
    │   ├── activity_pan_save.xml
    │   ├── activity_pan_target_select.xml
    │   ├── item_pan_space.xml
    │   ├── item_pan_file.xml
    │   ├── item_pan_save_space.xml
    │   ├── item_pan_target_space.xml
    │   ├── item_pan_target_folder.xml
    │   └── layout_toolbar.xml
    ├── menu/
    │   └── menu_pan_file_list.xml
    └── values/
        └── pan_strings.xml
```

## 六、与iOS端一致性

Android端实现了与iOS端完全一致的交互逻辑：

1. **三个空间平铺展示** - 无分段控制器，直接显示三个空间
2. **长按菜单** - 文件消息长按可选择"保存到网盘"
3. **移动/复制限制** - 同空间内copy=false，跨空间copy=true
4. **删除逻辑** - 先删除记录，检查引用计数，无引用时删除OSS对象
5. **配置方式** - 在Config.java中配置PAN_SERVER_ADDRESS，与iOS的WFCConfig一致

## 七、注意事项

1. **图片资源**：需要自行添加以下图片资源到mipmap目录：
   - `ic_pan_space_global` - 全局公共空间图标
   - `ic_pan_space_public` - 我的公共空间图标
   - `ic_pan_space_private` - 我的私有空间图标
   - `ic_folder` - 文件夹图标
   - `ic_file` - 文件图标

2. **依赖检查**：确保以下依赖已添加到build.gradle：
   - Gson
   - OkHttp
   - Material Dialogs
   - RecyclerView

3. **权限**：确保有网络访问权限

4. **后端配置**：确保Pan服务URL配置正确，且后端服务正常运行

5. **配置优先级**：如果不需要网盘功能，将`PAN_SERVER_ADDRESS`设置为`null`

## 八、测试建议

1. 测试三个空间的正确显示
2. 测试文件列表的加载和展示
3. 测试创建文件夹功能
4. 测试重命名功能
5. 测试移动文件（同空间和跨空间）
6. 测试复制文件（同空间和跨空间）
7. 测试删除文件功能
8. 测试从文件消息保存到网盘
9. 测试从网盘选择文件发送
10. 测试网络异常情况
11. 测试无权限操作的情况
12. 测试关闭网盘功能（PAN_SERVER_ADDRESS设为null）
