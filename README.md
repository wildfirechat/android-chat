## 说明
本工程为野火IM Android平台的SDK和Demo。野火IM作为一个通用的即时通讯SDK，可以集成到各种应用中。详情可以阅读[docs](http://docs.wildfirechat.cn).


开发一套IM系统真的很艰辛，请路过的朋友们给点个star，支持我们坚持下去🙏🙏🙏🙏🙏


### 联系我们
问题讨论请加群：822762829

<img src="http://static.wildfirechat.cn/qr_qqgroup.jpeg" width = 50% height = 50% />


## 体验Demo
我们提供了体验demo，请使用微信扫码下载安装体验

![野火IM](http://static.wildfirechat.cn/download_qrcode.png)

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
1. client部分，支持快速集成，具体参考[jitpack-wildfire.chat](https://jitpack.io/#wildfirechat/android-chat/)
2. UI(chat)部分，目前不支持快速集成，需要你自行下载，并将代码移动到你自己的项目，且必须是application module，不能作为library module引入的原因是注解中使用了R.xx.yyyy，
而library module中，R.xx.yyy并不是一个常量。后续会采用butterKnife的方式，引入R2.xx.yyyy。

## 贡献
欢迎提交pull request，一起打造一个更好的开源IM。

## 鸣谢
1. [LQRWeChat](https://github.com/GitLqr/LQRWeChat) 本项目中图片选择器、表情基于此开发
2. [butterKnife](https://github.com/JakeWharton/butterknife)
3. OKHttp等一些其他优秀的开源项目

***对以上项目的作者衷心的感谢，世界因你们的分享变得更美好。***

## License

Under the MIT license. See the [LICENSE](https://github.com/wildfirechat/mars/blob/firechat/LICENSE) file for details.
