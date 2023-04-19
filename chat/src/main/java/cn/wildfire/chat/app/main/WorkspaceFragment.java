/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.main;

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

import cn.wildfire.chat.kit.workspace.JsApi;
import cn.wildfirechat.chat.R;

public class WorkspaceFragment extends Fragment {
    private String url;
    private String htmlContent;
    private JsApi jsApi;

    WebView webView;

    public static WorkspaceFragment loadUrl(String url) {
        WorkspaceFragment fragment = new WorkspaceFragment();
        Bundle bundle = new Bundle();
        bundle.putString("url", url);
        fragment.setArguments(bundle);
        return fragment;
    }

    public static WorkspaceFragment loadHtmlContent(String htmlContent) {
        WorkspaceFragment fragment = new WorkspaceFragment();
        Bundle bundle = new Bundle();
        bundle.putString("htmlContent", htmlContent);
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

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                jsApi.setCurrentUrl(url);
                return super.shouldOverrideUrlLoading(view, url);
            }
        });

        if (!TextUtils.isEmpty(htmlContent)) {
            webView.loadDataWithBaseURL("", htmlContent, "text/html", "UTF-8", "");
        } else {
            webView.loadUrl(url);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
}
