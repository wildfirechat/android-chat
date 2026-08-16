/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import cn.wildfire.chat.kit.conversation.forward.ForwardActivity;
import cn.wildfire.chat.kit.page.WfcPage;
import cn.wildfire.chat.kit.page.WfcPageCompat;
import cn.wildfire.chat.kit.workspace.JsApi;
import cn.wildfirechat.client.ConnectionStatus;
import cn.wildfirechat.message.LinkMessageContent;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.remote.ChatManager;
import wendu.dsbridge.DWebView;

/**
 * 内嵌网页页面：链接消息、图文消息、用户协议、工作台里的 H5 应用都落到这里。
 * <p>
 * 逐行搬自 {@link WfcWebViewActivity}，那个类现在只是手机端的壳。本页是全仓库被引用最多的一个
 * 页面（17 处），其中大部分调用方（消息 viewholder、收藏、关于、发现）本身已经在右栏里，
 * 不迁的话每点一个链接都会被弹回全屏。
 */
public class WfcWebViewFragment extends Fragment implements WfcPage {
    private String url;
    private String htmlContent;
    private String initialTitle;

    private DWebView webView;
    private JsApi jsApi;
    private static final String dsBridgeAgentTag = "WF-DSBridge";

    public static WfcWebViewFragment fromIntent(Intent intent) {
        WfcWebViewFragment fragment = new WfcWebViewFragment();
        Bundle args = new Bundle();
        args.putString("url", intent.getStringExtra("url"));
        args.putString("content", intent.getStringExtra("content"));
        args.putString("title", intent.getStringExtra("title"));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        url = args == null ? null : args.getString("url");
        htmlContent = args == null ? null : args.getString("content");
        initialTitle = args == null ? null : args.getString("title");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_webview, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        webView = view.findViewById(R.id.webview);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        String ua = settings.getUserAgentString();
        settings.setUserAgentString(ua + " " + dsBridgeAgentTag);
        // 用 Fragment 版构造：JsApi 里的选联系人、关闭本页因此能走右栏，见 JsApi 内的说明
        jsApi = new JsApi(this, webView, url);
        webView.addJavascriptObject(jsApi, null);
        webView.setDownloadListener((downloadUrl, userAgent, contentDisposition, mimetype, contentLength) -> {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(downloadUrl));
            startActivity(i);
            WfcPageCompat.finishPage(this);
        });
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                String webTitle = view.getTitle();
                if (!TextUtils.isEmpty(webTitle)) {
                    if (TextUtils.isEmpty(initialTitle) || !TextUtils.equals(webTitle, "about:blank")) {
                        // 标题是网页加载完才知道的，只能运行时回写给宿主
                        WfcPageCompat.setPageTitle(WfcWebViewFragment.this, webTitle);
                    }
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.equals(WfcWebViewFragment.this.url)) {
                    jsApi.setCurrentUrl(url);
                }
                return false;
            }
        });
        if (!TextUtils.isEmpty(htmlContent)) {
            webView.loadDataWithBaseURL("", htmlContent, "text/html", "UTF-8", "");
            settings.setTextZoom(400);
        } else {
            webView.loadUrl(url);
        }
    }

    @Override
    public void onDestroyView() {
        // WebView 是视图树的一部分，跟着视图一起销毁；右栏里本页出栈走的就是这条路径，
        // 放到 onDestroy 里会漏掉「视图销毁但 Fragment 还在」的情形
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroyView();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (jsApi == null || !jsApi.onActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    // ==================== WfcPage ====================

    @Nullable
    @Override
    public CharSequence pageTitle() {
        return TextUtils.isEmpty(initialTitle) ? null : initialTitle;
    }

    @Override
    public int pageMenu() {
        return R.menu.web;
    }

    @Override
    public void onPreparePageMenu(Menu menu) {
        MenuItem forward = menu.findItem(R.id.forward);
        if (forward != null) {
            forward.setEnabled(ChatManager.Instance().getConnectionStatus() == ConnectionStatus.ConnectionStatusConnected);
        }
    }

    @Override
    public boolean onPageMenuItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.forward) {
            LinkMessageContent content = new LinkMessageContent(webView.getTitle(), webView.getUrl());
            Message message = new Message();
            message.content = content;
            Intent intent = new Intent(getActivity(), ForwardActivity.class);
            intent.putExtra("message", message);
            // 转发完就该回到原来的地方，本页没有再回来的必要，让选择会话页把它顶掉
            if (WfcPageCompat.replaceSelfWithPage(this, intent)) {
                return true;
            }
            WfcPageCompat.startPage(this, intent);
        } else if (item.getItemId() == R.id.openWithDefaultBrowser) {
            // 交给系统浏览器：隐式 intent 没有 component，右栏登记表查不到，必然全屏，正合预期
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(webView.getUrl())));
        } else if (item.getItemId() != R.id.close) {
            return false;
        }
        WfcPageCompat.finishPage(this);
        return true;
    }

    @Override
    public boolean onPageBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return false;
    }
}
