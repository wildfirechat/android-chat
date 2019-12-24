package cn.wildfire.chat.app.main;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.CallSuper;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.wildfire.chat.app.main.model.MainModel;
import cn.wildfire.chat.kit.chatroom.ChatRoomListActivity;
import cn.wildfire.chat.kit.conversation.ConversationActivity;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.model.Conversation;

public class DiscoveryFragment extends Fragment {

    @BindView(R.id.webView)
    WebView webView;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.main_fragment_discovery, container, false);
        ButterKnife.bind(this, view);


        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        // 设置可以访问文件
        webSettings.setAllowFileAccess(true);
        // 设置支持缩放
        webSettings.setBuiltInZoomControls(true);
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        webSettings.setDatabaseEnabled(true);
        // 使用localStorage则必须打开
        webSettings.setDomStorageEnabled(true);
        webSettings.setGeolocationEnabled(true);
        webSettings.setDisplayZoomControls(false);

        webView.loadUrl(MainModel.clientConfig.getHomeUrl());
        webView.setWebViewClient(new WebViewClient(){
           @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                //使用WebView加载显示url
                view.loadUrl(url);
                //返回true
                return true;
            }
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
            }
/*
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith("weixin://")) {
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(url));
                    startActivity(intent);
                    return true;
                }
                if (url.startsWith("wxp://")) {
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(url));
                    startActivity(intent);
                    return true;
                }
                if (url.startsWith("alipay://")) {
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(url));
                    startActivity(intent);
                    return true;
                }
                return super.shouldOverrideUrlLoading(view, url);
            }*/
        });
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.e("A:","onResume");
    }
    @Override
    public void onPause() {
        super.onPause();
        Log.e("A:","onPause");
    }

    @OnClick(R.id.chatRoomOptionItemView)
    void chatRoom() {
        Intent intent = new Intent(getActivity(), ChatRoomListActivity.class);
        startActivity(intent);
    }

    @OnClick(R.id.robotOptionItemView)
    void robot() {
        Intent intent = ConversationActivity.buildConversationIntent(getActivity(), Conversation.ConversationType.Single, "FireRobot", 0);
        startActivity(intent);
    }

    @OnClick(R.id.channelOptionItemView)
    void channel() {

    }
}
