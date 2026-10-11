## 重要声明

**因反诈合规要求，本项目协议栈默认仅支持连接野火官方服务，不能连接到自行部署的服务。请添加野火微信（wfchat 或 wildfirechat）免费申请临时版本和无限制版本，欢迎使用！**

**严禁将本项目用于非法诈骗。**

> 联系官方获取到不受限版本后，请替换`./mars-core-release/mars-core-release.aar`文件，并 Clean 和 Rebuild

---------


## 野火IM解决方案

野火IM是专业级即时通讯和实时音视频整体解决方案，由北京野火无限网络科技有限公司维护和支持。

主要特性有：私有部署安全可靠，性能强大，功能齐全，全平台支持，开源率高，部署运维简单，二次开发友好，方便与第三方系统对接或者嵌入现有系统中。详细情况请参考[在线文档](https://docs.wildfirechat.cn)。

主要包括一下项目：

| [GitHub仓库地址(主站)](https://github.com/wildfirechat)      | [码云仓库地址(镜像)](https://gitee.com/wfchat)        | 说明                                                                                      | 备注                                           |
| ------------------------------------------------------------ | ----------------------------------------------------- | ----------------------------------------------------------------------------------------- | ---------------------------------------------- |
| [im-server](https://github.com/wildfirechat/im-server)       | [server](https://gitee.com/wfchat/im-server)          | IM Server                                                                                 |                                                |
| [android-chat](https://github.com/wildfirechat/android-chat) | [android-chat](https://gitee.com/wfchat/android-chat) | 野火IM Android SDK源码和App源码                                                           | 可以很方便地进行二次开发，或集成到现有应用当中 |
| [ios-chat](https://github.com/wildfirechat/ios-chat)         | [ios-chat](https://gitee.com/wfchat/ios-chat)         | 野火IM iOS SDK源码和App源码                                                               | 可以很方便地进行二次开发，或集成到现有应用当中 |
| [pc-chat](https://github.com/wildfirechat/vue-pc-chat)       | [pc-chat](https://gitee.com/wfchat/vue-pc-chat)       | 基于[Electron](https://electronjs.org/)开发的PC 端                                        |                                                |
| [web-chat](https://github.com/wildfirechat/vue-chat)         | [web-chat](https://gitee.com/wfchat/vue-chat)         | 野火IM Web 端, [体验地址](http://web.wildfirechat.cn)                                     |                                                |
| [wx-chat](https://github.com/wildfirechat/wx-chat)           | [wx-chat](https://gitee.com/wfchat/wx-chat)           | 小程序平台的Demo(支持微信、百度、阿里、字节、QQ 等小程序平台)                             |                                                |
| [app server](https://github.com/wildfirechat/app_server)     | [app server](https://gitee.com/wfchat/app_server)     | 应用服务端                                                                                |                                                |
| [robot_server](https://github.com/wildfirechat/robot_server) | [robot_server](https://gitee.com/wfchat/robot_server) | 机器人服务端                                                                              |                                                |
| [push_server](https://github.com/wildfirechat/push_server)   | [push_server](https://gitee.com/wfchat/push_server)   | 推送服务器                                                                                |                                                |
| [docs](https://github.com/wildfirechat/docs)                 | [docs](https://gitee.com/wfchat/docs)                 | 野火IM相关文档，包含设计、概念、开发、使用说明，[在线查看](https://docs.wildfirechat.cn/) |                                                |


## 说明

本工程为野火IM Android App，开发过程中，充分考虑了二次开发和集成需求，可作为SDK集成到其他应用中，或者直接进行二次开发。

开发一套IM系统真的很艰辛，请路过的朋友们给点个star，支持我们坚持下去🙏🙏🙏🙏🙏

## 关于包名/applicationId
1. 开发者开发具体产品时，请勿直接使用本 demo 的包名/applicationId，我们会不定期修改包名/applicationId
2. 禁止将本产品用于非法目的，一经发现，我们将停止任何形式的技术支持
3. 修改包名时，会导致编译失败，需同步修改`google-services.json`和`agconnect-services.json`文件中的`package_name`字段。对接推送时，需要重新生成对应的`google-services.json`和`agconnect-services.json`文件。
4. 如果需要修改`client`、`mars-core-release`或`avenginekit.aar`等的包名，请联系我们。

## 开发调试说明
1. JDK: 17
2. 我们采用最新稳定版Android Studio及对应的gradle进行开发，对于旧版本的IDE，我们没有测试，编译之类问题，需自行解决。

##  关于 minSdkVersion 设置为 21 时， debug 版 apk 可能不能进行音视频通话的特殊说明
1. 关闭混淆时，命令行下，通过`./gradlew clean aDebug` 或 Android Studio 里面，通过 `Build App Bundle(s)/APK(s) -> Build APK(s)` 生成的 debug 版本 apk，不支持音视频通话，具体原因请参考[useFullClasspathForDexingTransform](https://issuetracker.google.com/issues/333107832)
2. 开启混淆，debug 版 apk 一切正常，将`chat/build.gradle#buildTypes#debug#minifyEnabled`置为 true，即为 debug 版也开启混淆
3. 命令行下，通过`./gradlew clean aR`或 Android Studio 里面，通过`Generate Signed App Bundle/APK...`可生成 release 版 apk，release 版 apk，一切正常

## 二次开发说明
野火IM采用bugly作为日志手机工具，大家二次开发时，务必将```MyApp.java```中的 ```bugly id``` 替换为你们自己的，否则错误日志都跑我们这儿来了，你们收集不到错误日志，我们也会受到干扰。

## 混淆说明
1. 确保所依赖的```lifecycle```版本在2.2.0或以上。
2. 参考```chat/proguard-rules.pro```进行配置。

## 安全说明
为了方便开发者部署、测试，默认允许`HTTP`进行网络请求，为了提高安全性，上线之前，请进行以下操作：
1. 为`app-server`配置`HTTPS`支持，并将`APP_SERVER_ADDRESS`配置为`HTTPS`地址
2. 如果支持开放平台的话，为开发平台配置`HTTPS`支持，并将`WORKSPACE_URL`配置为`HTTPS`地址
3. 如果支持组织结构的话，为组织结构服务配置`HTTPS`支持，并将`ORG_SERVER_ADDRESS`配置为`HTTPS`地址
4. 将`chat/src/main/AndroidManifest.xml`中的`android:usesCleartextTraffic`置为`false`，并将`chat/src/main/res/xml/network_security_config.xml`中所有的`cleartextTrafficPermitted`置为`false`。配置了`network_security_config.xml`后，Android 7.0 及以上以它为准，`usesCleartextTraffic`不再生效
5. 更多安全说明，请参考[野火安全吗](https://docs.wildfirechat.cn/blogs/%E9%87%8E%E7%81%AB%E5%AE%89%E5%85%A8%E5%90%97.html)

## 自签证书说明
自建服务（IM 服务、应用服务、组织通讯录、对象存储等）使用自签名证书时，按以下步骤配置：

1. **放置证书**：将证书放到`client/src/main/assets/certs`目录，可以放多个，文件名不限，如`server.crt`
   - 放 CA 证书或服务端证书本身均可，只放证书，**不要放私钥**
   - 必须是 PEM 格式，即以`-----BEGIN CERTIFICATE-----`开头的文本文件。DER 格式的证书请先转换：`openssl x509 -inform der -in server.der -out server.crt`
   - 这一份证书由协议栈、OkHttp（应用服务、组织通讯录、投票、收藏、归档、上传、下载、语音识别等）、Glide 图片加载、WebView（工作台、链接）共用，不需要再放到其他地方。没有证书时，全部使用系统默认配置
   - 更换证书时，请使用新的文件名，或者卸载重装。协议栈使用的是首次运行时拷贝到 App 私有目录的证书，同名文件不会重新拷贝

2. **配置 network_security_config.xml**：修改`chat/src/main/res/xml/network_security_config.xml`中的`domain-config`，把使用自签证书的服务地址都列出来：
   ```xml
   <domain-config cleartextTrafficPermitted="true">
       <!-- 改成你自己的服务地址，域名或 IP 均可，不带 http(s):// 和端口，多个地址写多行 -->
       <domain includeSubdomains="true">example.com</domain>
       <domain includeSubdomains="false">192.168.1.100</domain>
       <trust-anchors>
           <certificates src="system" />
           <certificates src="@raw/wfc_self_signed_ca" />
       </trust-anchors>
   </domain-config>
   ```
   - `@raw/wfc_self_signed_ca`由`chat/build.gradle`中的`mergeSelfSignedCerts`任务在编译时根据`assets/certs`自动生成，不需要手工拷贝证书到`res/raw`
   - OkHttp、Glide、WebView 已经直接使用`assets/certs`中的证书，不依赖这里的地址列表。这里主要是给视频在线播放（`MediaPlayer`）等无法单独配置证书的组件用的
   - 修改服务地址后，这里要同步修改，否则上述组件会报`Trust anchor for certification path not found`

3. **协议栈**：IM 连接要使用 TLS 时，需要打开`ClientService#initProto`中被注释的代码，仅 2026.9.11 之后的 IM 服务支持：
   ```java
   String[] certs = copyCerts(SelfSignedCertUtils.CERT_DIR_NAME);
   ProtoLogic.setUseWebsocket(true);
   // 第一个参数是是否跳过证书验证，第二个参数是证书文件列表
   ProtoLogic.useTls(false, certs);
   ```

4. **以 SDK 方式集成时**：将证书放到自己 App 的`src/main/assets/certs`目录，自己创建的网络请求通过`SelfSignedCertUtils`信任这些证书：
   - OkHttp：`SelfSignedCertUtils.trustSelfSignedCerts(context, okHttpClientBuilder)`
   - HttpURLConnection：`SelfSignedCertUtils.trustSelfSignedCerts(context, connection)`，需要在连接之前调用
   - WebView：在`WebViewClient#onReceivedSslError`中，`SelfSignedCertUtils.isTrustedBySelfSignedCerts(context, error)`返回`true`时才调用`handler.proceed()`，否则按默认处理
   - 视频在线播放等组件也需要信任自签证书时，参考 demo 的`network_security_config.xml`和`mergeSelfSignedCerts`任务进行配置

5. **注意事项**
   - 只要证书链能追溯到`assets/certs`中的证书，OkHttp、Glide、WebView 都会跳过域名校验，以兼容没有写 subjectAltName 的自签证书。系统 CA 签发的证书仍然严格校验域名。请妥善保管自签 CA 的私钥
   - 推荐签发证书时把服务的域名和 IP 都写进 subjectAltName。`network_security_config.xml`不会放松域名校验，视频在线播放等组件要求证书的域名或 IP 必须匹配
   - WebView 只能拿到服务端证书本身，所以只支持服务端证书就是`assets/certs`中的证书，或者由其直接签发的情况

## 敏感权限说明
1. `android.permission.PROCESS_OUTGOING_CALLS`，音视频通话时，允许普通电话打断音视频通话，默认未申请
2. `android.permission.SYSTEM_ALERT_WINDOW`，允许音视频通话窗口最小化，并悬浮在其他窗口之上
3. `android.permission.BLUETOOTH`、`android.permission.BLUETOOTH_ADMIN`，音视频通话时，允许使用蓝牙耳机

## Android 4.x 说明
请使用[api-19](https://github.com/wildfirechat/android-chat/tree/api-19)分支，如果编译失败等，可能是4.x版本的协议栈版本没有及时更新所导致，请微信联系 `wfchat` 进行更新。

### 联系我们

> 商务合作请优先采用邮箱和我们联系。技术问题请到[野火IM论坛](http://bbs.wildfirechat.cn/)发帖交流。

1. heavyrain.lee  邮箱: heavyrain.lee@wildfirechat.cn  微信：wildfirechat
2. imndx  邮箱: imndx@wildfirechat.cn  微信：wfchat

### 问题交流

1. 如果大家发现bug，请在GitHub提issue
2. 其他问题，请到[野火IM论坛](http://bbs.wildfirechat.cn/)进行交流学习
3. 微信公众号

<img src="http://static.wildfirechat.cn/wx_wfc_qrcode.jpg" width = 40% height = 40% />

> 强烈建议关注我们的公众号。我们有新版本发布或者有重大更新会通过公众号通知大家，另外我们也会不定期的发布一些关于野火IM的技术介绍。

## 体验Demo
我们提供了体验demo，请使用微信扫码下载安装体验

![野火IM](http://static.wildfirechat.cn/download_qrcode.png)

## 应用截图
[点击查看 Android Demo 视频演示](https://static.wildfirechat.cn/wf-android-demo-live.mp4)

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


## 集成
1. client部分，自行下载代码，并将client module引入你们自己的项目。
2. uikit部分，自行下载代码，并将uikit module引入你们自己的项目。
3. push部分，自行下载代码，将push module引入你们自己的项目。

## 推送
当应用在后台后，不同手机厂家有着不同的后台策略，可能很快或者最终会被冻结和杀掉，此时收到消息需要厂商的推送通知服务。请部署推送服务，推送服务代码可以在[Github](https://github.com/wildfirechat/push_server)和[码云](https://gitee.com/wfchat/push_server)下载。具体使用方式，请参考推送服务项目上的说明。

## 贡献
欢迎提交pull request，一起打造一个更好的开源IM。

## 鸣谢
1. [LQRWeChat](https://github.com/GitLqr/LQRWeChat) 本项目中图片选择器、表情基于此开发
2. [butterKnife](https://github.com/JakeWharton/butterknife)
3. OKHttp等一些其他优秀的开源项目
4. 本工程使用的Icon全部来源于[icons8](https://icons8.com)，对他们表示感谢。
5. Gif动态图来源于网络，对网友的制作表示感谢。

如果有什么地方侵犯了您的权益，请联系我们删除🙏🙏🙏

## License

1. Under the Creative Commons Attribution-NoDerivs 3.0 Unported license. See the [LICENSE](https://github.com/wildfirechat/android-chat/blob/master/LICENSE) file for details.
