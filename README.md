## 野火IM解决方案

## 特别注意

1. ```com.android.tools.build:gradle:3.5.0``` 可能存在bug，会导致音视频crash，请勿升级到次版本，若已升级到此版本，请降级到```3.4.2```，对应gradle版本为```5.1.1```
2. 待修复这问题之后，野火IM会升级所使用的```gradle```和```Android Gradle plugin```的版本

野火IM是一套跨平台、核心功能开源的即时通讯解决方案，主要包含以下内容。

| 仓库                                                         | 说明                                                    | 备注 |
| ------------------------------------------------------------ | ------------------------------------------------------- | ---- |
| [android-chat](https://github.com/wildfirechat/android-chat) | 野火IM Android SDK源码和App源码                       |可以很方便地进行二次开发，或集成到现有应用当中      |
| [ios-chat](https://github.com/wildfirechat/ios-chat)         | 野火IM iOS SDK源码和App源码                            |可以很方便地进行二次开发，或集成到现有应用当中      |
| [pc-chat](https://github.com/wildfirechat/pc-chat)           | 基于[Electron](https://electronjs.org/)开发的PC平台应用 |      |
| [web-chat](https://github.com/wildfirechat/web-chat)          | Web平台的Demo, [体验地址](http://web.wildfirechat.cn)   |      |
| [wx-chat](https://github.com/wildfirechat/wx-chat)           | 微信小程序平台的Demo   |      |
| [server](https://github.com/wildfirechat/server)             | IM server                                               |      |
| [app server](https://github.com/wildfirechat/app_server)     | 应用服务端                                          |      |
| [robot_server](https://github.com/wildfirechat/robot_server) | 机器人服务端                                        |      |
| [push_server](https://github.com/wildfirechat/push_server)   | 推送服务器                                              |      |
| [docs](https://github.com/wildfirechat/docs)                 | 野火IM相关文档，包含设计、概念、开发、使用说明          |      | |


## 说明

本工程为野火IM Android App，开发过程中，充分考虑了二次开发和集成需求，可作为SDK集成到其他应用中，或者直接进行二次开发，详情可以阅读[docs](http://docs.wildfirechat.cn).


开发一套IM系统真的很艰辛，请路过的朋友们给点个star，支持我们坚持下去🙏🙏🙏🙏🙏

## 开发调试说明

我们采用最新稳定版Android Studio及对应的gradle进行开发，对于旧版本的IDE，我们没有测试，编译之类问题，需自行解决。

## 二次开发说明

野火IM采用bugly作为日志手机工具，大家二次开发时，务必将```MyApp.java```中的 ```bugly id``` 替换为你们自己的，否则错误日志都跑我们这儿来了，你们收集不到错误日志，我们也会收到干扰。

另外，如果可以请告知我们，我们会在案例参考把项目加上。

### 联系我们

> 商务合作请优先采用邮箱和我们联系。技术问题请到[野火IM论坛](http://bbs.wildfirechat.cn/)发帖交流。

1. heavyrain.lee  邮箱: heavyrain.lee@wildfirechat.cn  微信：wildfirechat
2. imndx  邮箱: imndx@wildfirechat.cn  微信：wfchat

### 问题交流

1. 如果大家发现bug，请在GitHub提issue
2. 其他问题，请到[野火IM论坛](http://bbs.wildfirechat.cn/)进行交流学习
3. 微信公众号

<img src="http://static.wildfirechat.cn/wx_wfc_qrcode.jpg" width = 50% height = 50% />

> 强烈建议关注我们的公众号。我们有新版本发布或者有重大更新会通过公众号通知大家，另外我们也会不定期的发布一些关于野火IM的技术介绍。

## 体验Demo
我们提供了体验demo，请使用微信扫码下载安装体验

![野火IM](http://static.wildfirechat.cn/download_qrcode.png)

#### 加入野火官方体验交流群
使用野火IM移动客户端扫码下面二维码（不是使用微信扫码），和野火IM的爱好者一起畅谈野火IM的问题和经验：

![野火IM官方交流群](http://static.wildfirechat.cn/wildfirechat_official_group.jpeg)


## 应用截图
![ios-demo1](http://static.wildfirechat.cn/android-deomo1.gif)

![ios-demo2](http://static.wildfirechat.cn/android-deomo1.gif)

<img src="http://static.wildfirechat.cn/android-view1.png" width = 50% height = 50% />

<img src="http://static.wildfirechat.cn/android-view2.png" width = 50% height = 50% />

<img src="http://static.wildfirechat.cn/android-view3.png" width = 50% height = 50% />

<img src="http://static.wildfirechat.cn/android-view4.png" width = 50% height = 50% />

<img src="http://static.wildfirechat.cn/android-view5.png" width = 50% height = 50% />

<img src="http://static.wildfirechat.cn/android-view6.png" width = 50% height = 50% />

<img src="http://static.wildfirechat.cn/android-view7.png" width = 50% height = 50% />

<img src="http://static.wildfirechat.cn/android-view8.png" width = 50% height = 50% />

<img src="http://static.wildfirechat.cn/android-view9.png" width = 50% height = 50% />

<img src="http://static.wildfirechat.cn/android-view10.png" width = 50% height = 50% />

<img src="http://static.wildfirechat.cn/android-view11.png" width = 50% height = 50% />

## 协议栈的编译
工程中已经包括了编译好的协议栈，你也可以自己编译[协议栈](https://github.com/wildfirechat/proto)，编译方法请参考协议栈工程。

## 集成
1. client部分，自行下载代码，并将client module引入你们自己的项目。
2. UI(chat)部分，需要你自行下载，并将代码移动到你自己的项目，且必须是application module，不能作为library module引入的原因是注解中使用了R.xx.yyyy，
而library module中，R.xx.yyy并不是一个常量。后续会采用butterKnife的方式，引入R2.xx.yyyy。

## 贡献
欢迎提交pull request，一起打造一个更好的开源IM。

## 鸣谢
1. [LQRWeChat](https://github.com/GitLqr/LQRWeChat) 本项目中图片选择器、表情基于此开发
2. [butterKnife](https://github.com/JakeWharton/butterknife)
3. OKHttp等一些其他优秀的开源项目
4. 本工程使用的Icon全部来源于[icons8](https://icons8.com)，对他们表示感谢。
5. Gif动态图来源于网络，对网友的制作表示感谢。

如果有什么地方侵犯了您的权益，请联系我们删除🙏🙏🙏

## 案例参考

todo

## License

1. Under the MIT license. See the [LICENSE](https://github.com/wildfirechat/mars/blob/firechat/LICENSE) file for details.
2. Under the 996ICU License. See the [LICENSE](https://github.com/996icu/996.ICU/blob/master/LICENSE) file for details.
