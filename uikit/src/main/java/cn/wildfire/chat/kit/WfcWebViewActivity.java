package cn.wildfire.chat.kit;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import butterknife.BindView;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.chat.R2;

public class WfcWebViewActivity extends WfcBaseActivity {
    private String url;

    @BindView(R2.id.webview)
    WebView webView;

    public static void loadUrl(Context context, String title, String url) {
        Intent intent = new Intent(context, WfcWebViewActivity.class);
        intent.putExtra("url", url);
        intent.putExtra("title", title);
        context.startActivity(intent);
    }

    public static void loadHtmlContent(Context context, String title, String htmlContent) {
        Intent intent = new Intent(context, WfcWebViewActivity.class);
        intent.putExtra("title", title);
        intent.putExtra("content", htmlContent);
        context.startActivity(intent);
    }

    @Override
    protected int contentLayout() {
        return R.layout.activity_webview;
    }

    @Override
    protected void afterViews() {
        url = getIntent().getStringExtra("url");
        String htmlContent = getIntent().getStringExtra("content");
        if (!TextUtils.isEmpty(htmlContent)) {
            webView.loadDataWithBaseURL("", htmlContent, "text/html", "UTF-8", "");
        } else {
            webView.loadUrl(url);
        }

        String title = getIntent().getStringExtra("title");
        if (!TextUtils.isEmpty(title)) {
            setTitle(title);
        }
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                String webTitle = view.getTitle();
                if (!TextUtils.isEmpty(webTitle)) {
                    if (TextUtils.isEmpty(title) || !TextUtils.equals(webTitle, "about:blank")) {
                        setTitle(webTitle);
                    }
                }
            }
        });
    }
}
