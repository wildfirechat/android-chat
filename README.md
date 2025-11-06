## WildFire IM Solution

WildFire IM is a professional-grade instant messaging and real-time audio/video complete solution, maintained and supported by Beijing WildFire Unlimited Network Technology Co., Ltd.

Main features include: secure and reliable private deployment, powerful performance, comprehensive functionality, full platform support, high open-source rate, simple deployment and maintenance, friendly for secondary development, and easy integration with third-party systems or embedding into existing systems. For detailed information, please refer to the [online documentation](https://docs.wildfirechat.cn).

Main projects include:

| [GitHub Repository (Main Site)](https://github.com/wildfirechat)      | [Gitee Repository (Mirror)](https://gitee.com/wfchat)        | Description                                                                                      | Notes                                           |
| ------------------------------------------------------------ | ----------------------------------------------------- | ----------------------------------------------------------------------------------------- | ---------------------------------------------- |
| [im-server](https://github.com/wildfirechat/im-server)       | [server](https://gitee.com/wfchat/im-server)          | IM Server                                                                                 |                                                |
| [android-chat](https://github.com/wildfirechat/android-chat) | [android-chat](https://gitee.com/wfchat/android-chat) | WildFire IM Android SDK source code and App source code                                                           | Easy for secondary development or integration into existing applications |
| [ios-chat](https://github.com/wildfirechat/ios-chat)         | [ios-chat](https://gitee.com/wfchat/ios-chat)         | WildFire IM iOS SDK source code and App source code                                                               | Easy for secondary development or integration into existing applications |
| [pc-chat](https://github.com/wildfirechat/vue-pc-chat)       | [pc-chat](https://gitee.com/wfchat/vue-pc-chat)       | PC client based on [Electron](https://electronjs.org/)                                        |                                                |
| [web-chat](https://github.com/wildfirechat/vue-chat)         | [web-chat](https://gitee.com/wfchat/vue-chat)         | WildFire IM Web client, [Demo URL](http://web.wildfirechat.cn)                                     |                                                |
| [wx-chat](https://github.com/wildfirechat/wx-chat)           | [wx-chat](https://gitee.com/wfchat/wx-chat)           | Mini program platform demo (supports WeChat, Baidu, Alipay, ByteDance, QQ and other mini program platforms)                             |                                                |
| [app server](https://github.com/wildfirechat/app_server)     | [app server](https://gitee.com/wfchat/app_server)     | Application server                                                                                |                                                |
| [robot_server](https://github.com/wildfirechat/robot_server) | [robot_server](https://gitee.com/wfchat/robot_server) | Robot server                                                                              |                                                |
| [push_server](https://github.com/wildfirechat/push_server)   | [push_server](https://gitee.com/wfchat/push_server)   | Push server                                                                                |                                                |
| [docs](https://github.com/wildfirechat/docs)                 | [docs](https://gitee.com/wfchat/docs)                 | WildFire IM related documentation, including design, concepts, development, and usage instructions, [View online](https://docs.wildfirechat.cn/) |                                                |


## Overview

This project is the WildFire IM Android App. During development, we fully considered secondary development and integration requirements. It can be integrated as an SDK into other applications or used directly for secondary development.

Developing an IM system is truly challenging. Please give us a star to support us in continuing this journey!

## About Package Name/ApplicationId
1. Developers should not directly use the package name/applicationId of this demo when developing specific products. We may modify the package name/applicationId from time to time.
2. It is prohibited to use this product for illegal purposes. If discovered, we will stop all forms of technical support.
3. When modifying the package name, compilation will fail. You need to synchronously modify the `package_name` field in both `google-services.json` and `agconnect-services.json` files. When integrating push notifications, you need to regenerate the corresponding `google-services.json` and `agconnect-services.json` files.
4. If you need to modify the package name of `client`, `mars-core-release`, or `avenginekit.aar`, please contact us.

## Development and Debugging Instructions
1. JDK: 17
2. We use the latest stable version of Android Studio and corresponding Gradle for development. For older versions of the IDE, we have not tested them, and compilation issues need to be resolved on your own.

## Special Notes About Audio/Video Calling When minSdkVersion is Set to 21 for Debug APK
1. When obfuscation is disabled, the debug version APK generated via command line using `./gradlew clean aDebug` or through Android Studio using `Build App Bundle(s)/APK(s) -> Build APK(s)` does not support audio/video calls. For specific reasons, please refer to [useFullClasspathForDexingTransform](https://issuetracker.google.com/issues/333107832)
2. When obfuscation is enabled, debug version APK works normally. Set `chat/build.gradle#buildTypes#debug#minifyEnabled` to true to enable obfuscation for debug version as well.
3. Release version APK generated via command line using `./gradlew clean aR` or through Android Studio using `Generate Signed App Bundle/APK...` works normally.

## Secondary Development Instructions
WildFire IM uses Bugly as a log collection tool. When doing secondary development, you must replace the `bugly id` in `MyApp.java` with your own. Otherwise, error logs will come to us, you won't be able to collect error logs, and we will be interfered with.

## Obfuscation Instructions
1. Ensure that the `lifecycle` dependency version is 2.2.0 or above.
2. Configure according to `chat/proguard-rules.pro`.

## Security Instructions
To facilitate developer deployment and testing, `HTTP` network requests are allowed by default. To improve security, please perform the following operations before going live:
1. Configure `HTTPS` support for `app-server` and set `APP_SERVER_ADDRESS` to an `HTTPS` address
2. If supporting an open platform, configure `HTTPS` support for the development platform and set `WORKSPACE_URL` to an `HTTPS` address
3. If supporting organizational structure, configure `HTTPS` support for the organizational structure service and set `ORG_SERVER_ADDRESS` to an `HTTPS` address
4. Set `android:usesCleartextTraffic` to `false` in `chat/src/main/AndroidManifest.xml`
5. For more security information, please refer to [Is WildFire Secure?](https://docs.wildfirechat.cn/blogs/%E9%87%8E%E7%81%AB%E5%AE%89%E5%85%A8%E5%90%97.html)

## Sensitive Permissions Instructions
1. `android.permission.PROCESS_OUTGOING_CALLS`, allows regular phone calls to interrupt audio/video calls, not requested by default
2. `android.permission.SYSTEM_ALERT_WINDOW`, allows audio/video call window to minimize and float above other windows
3. `android.permission.BLUETOOTH`, `android.permission.BLUETOOTH_ADMIN`, allows using Bluetooth headsets during audio/video calls

## Android 4.x Instructions
Please use the [api-19](https://github.com/wildfirechat/android-chat/tree/api-19) branch. If compilation fails, it may be due to the 4.x version protocol stack not being updated in time. Please contact `wfchat` via WeChat for updates.

### Contact Us

> For business cooperation, please contact us via email first. For technical issues, please post on the [WildFire IM Forum](http://bbs.wildfirechat.cn/).

1. heavyrain.lee  Email: heavyrain.lee@wildfirechat.cn  WeChat: wildfirechat
2. imndx  Email: imndx@wildfirechat.cn  WeChat: wfchat

### Issue Discussion

1. If you find bugs, please submit an issue on GitHub
2. For other questions, please discuss and learn on the [WildFire IM Forum](http://bbs.wildfirechat.cn/)
3. WeChat Official Account

<img src="http://static.wildfirechat.cn/wx_wfc_qrcode.jpg" width = 40% height = 40% />

> We strongly recommend following our official account. We will notify everyone through the official account when we release new versions or have major updates. We will also periodically publish technical introductions about WildFire IM.

## Demo Experience
We provide a demo for experience. Please scan the QR code with WeChat to download and install.

![WildFire IM](http://static.wildfirechat.cn/download_qrcode.png)

## Application Screenshots
[Click to view Android Demo video](https://static.wildfirechat.cn/wf-android-demo-live.mp4)

<img src="https://static.wildfirechat.cn/wf-android-demo-1.jpg" width = 40% height = 40% />

<img src="https://static.wildfirechat.cn/wf-android-demo-2.jpg" width = 40% height = 40% />

<img src="https://static.wildfirechat.cn/wf-android-demo-3.jpg" width = 40% height = 40% />

<img src="https://static.wildfirechat.cn/wf-android-demo-4.jpg" width = 40% height = 40% />

<img src="https://static.wildfirechat.cn/wf-android-demo-5.jpg" width = 40% height = 40% />

<img src="https://static.wildfirechat.cn/wf-android-demo-6.jpg" width = 40% height = 40% />

<img src="https://static.wildfirechat.cn/wf-android-demo-7.jpg" width = 40% height = 40% />

<img src="https://static.wildfirechat.cn/wf-android-demo-8.jpg" width = 40% height = 40% />

<img src="https://static.wildfirechat.cn/wf-android-demo-9.jpg" width = 40% height = 40% />

<img src="https://static.wildfirechat.cn/wf-android-demo-10.jpg" width = 40% height = 40% />

<img src="https://static.wildfirechat.cn/wf-android-demo-11.jpg" width = 40% height = 40% />

<img src="https://static.wildfirechat.cn/wf-android-demo-12.jpg" width = 40% height = 40% />

<img src="https://static.wildfirechat.cn/wf-android-demo-13.jpg" width = 40% height = 40% />

<img src="https://static.wildfirechat.cn/wf-android-demo-14.jpg" width = 40% height = 40% />

<img src="https://static.wildfirechat.cn/wf-android-demo-15.jpg" width = 40% height = 40% />

<img src="https://static.wildfirechat.cn/wf-android-demo-16.jpg" width = 40% height = 40% />

<img src="https://static.wildfirechat.cn/wf-android-demo-17.jpg" width = 40% height = 40% />

<img src="https://static.wildfirechat.cn/wf-android-demo-18.jpg" width = 40% height = 40% />

<img src="https://static.wildfirechat.cn/wf-android-demo-19.jpg" width = 40% height = 40% />


## Integration
1. For the client module, download the code yourself and import the client module into your own project.
2. For the uikit module, download the code yourself and import the uikit module into your own project.
3. For the push module, download the code yourself and import the push module into your own project.

## Push Notifications
When the application is in the background, different phone manufacturers have different background policies, and the app may be quickly or eventually frozen and killed. To receive messages in this case, vendor push notification services are required. Please deploy the push service. The push service code can be downloaded from [Github](https://github.com/wildfirechat/push_server) and [Gitee](https://gitee.com/wfchat/push_server). For specific usage, please refer to the instructions in the push service project.

## Contribution
Pull requests are welcome. Let's build a better open-source IM together.

## Acknowledgments
1. [LQRWeChat](https://github.com/GitLqr/LQRWeChat) The image selector and emoticons in this project are developed based on this
2. [butterKnife](https://github.com/JakeWharton/butterknife)
3. OKHttp and other excellent open-source projects
4. All icons used in this project are from [icons8](https://icons8.com), thanks to them.
5. Animated GIFs are from the internet, thanks to the creators.

If anything infringes on your rights, please contact us for removal.

## License

1. Under the Creative Commons Attribution-NoDerivs 3.0 Unported license. See the [LICENSE](https://github.com/wildfirechat/android-chat/blob/master/LICENSE) file for details.
