/*
 * Copyright (c) 2022 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.workspace;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import cn.wildfire.chat.kit.R;
import wendu.dsbridge.DWebView;

public class WebViewFragment extends Fragment {
    private String url;
    private String htmlContent;
    private JsApi jsApi;

    DWebView webView;

    public static WebViewFragment loadUrl(String url) {
        WebViewFragment fragment = new WebViewFragment();
        Bundle bundle = new Bundle();
        bundle.putString("url", url);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_webview, container, false);
        bindViews(view);
        init();
        return view;
    }

    private void bindViews(View view) {
        webView = view.findViewById(R.id.webview);
    }

    private void init() {
        Bundle bundle = getArguments();
        url = bundle.getString("url");
        htmlContent = bundle.getString("htmlContent");
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
            }
        });

        this.jsApi = new JsApi(this, webView, url);
        webView.addJavascriptObject(this.jsApi, null);
        if (!TextUtils.isEmpty(htmlContent)) {
            webView.loadDataWithBaseURL("", htmlContent, "text/html", "UTF-8", "");
        } else {
            webView.loadUrl(url);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (!jsApi.onActivityResult(requestCode, resultCode, data)){
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
