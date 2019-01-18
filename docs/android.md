## 说明
本系统有4部分组成，服务器部分（moquette）/iOS部分（ios-workspace）/Android部分（android-chat）/协议栈部分（mars）。其中iso和android都依赖于协议栈部分。本文档介绍Android的相关知识，其他三部分的需要到各个对应工程中查看。


### 编译

下载之后要先编译一遍协议栈，编译方法参考协议栈文档。然后把编译完成后的mars-core-release.aar 放到${android-chat}/mars-core-release/目录下，用AndroidStudio打开整个工程编译就可以了

### 工程说明

工程中有6个module，client库是IM的通讯能力，avenginekit是1对1的音视频库，依赖于client。avdemo是音视频的demo。chat是IM的demo。app是client的开发demo（很不完备，可以用来测试client使用的。他与chat的区别是在于，chat目标是个可以正式使用的软件，而app是用来调试client的）。push是用来处理推送的SDK。其中avdemo/app/chat三个应用需要设置服务器配置。

### 配置

在项目的Config或者EnvDefine文件中，修改IM服务器地址${server.ip}:1983。也可以使用公网地址http://www.liyufan.win:1884
媒体服务器使用公网的http://www.liyufan.win:3478

### 登陆
使用服务器注册脚本注册的用户名密码登陆
