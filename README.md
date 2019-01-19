## 说明
野火IM系统有4部分组成，服务器部分/iOS部分（ios-workspace）/Android部分（android-chat）/协议栈部分（mars）。其中iso和android都依赖于协议栈部分。此为协议栈工程。


### iOS的编译

在${mars}/mars/libraries/目录下执行下面命令，然后copy生成的```mars.framework```到```ios-workspace/wfchatclient/Framework```目录下
```
python build_apple.py
```

### Android的编译
在${mars}/mars/libraries/目录下执行
```
python build_android.py
```

等待编译完成后，在进入到${mars}/mars/libraries/mars_android_sdk目录下执行
```
./gradlew build
```

### 集成
1. client部分，支持快速集成，具体参考[jitpack-wildfire.chat](https://jitpack.io/#wildfirechat/android-chat/)
2. UI(chat)部分，目前不支持快速集成，需要你自行下载，并将代码移动到你自己的项目，且必须是application module，不能作为library module引入的原因是注解中使用了R.xx.yyyy，
而library module中，R.xx.yyy并不是一个常量。后续会采用butterKnife的方式，引入R2.xx.yyyy。

编译完成后就能在build/output/aar目录下得到mars-core-release.aar, copy到```android-chat/mars-core-release```目录下

#### 鸣谢
1. [LQRWeChat](https://github.com/GitLqr/LQRWeChat) 本项目中图片选择器、表情基于此开发
2. [butterKnife](https://github.com/JakeWharton/butterknife) 
3. OKHttp等一些其他优秀的开源项目

***对以上项目的作者衷心的感谢，世界因你们的分享变得更美好。***

## License

Under the MIT license. See the [LICENSE](https://github.com/wildfirechat/mars/blob/firechat/LICENSE) file for details.