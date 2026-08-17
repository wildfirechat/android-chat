/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit;

import android.content.Context;
import android.content.Intent;

import androidx.fragment.app.Fragment;

import cn.wildfire.chat.kit.page.WfcPageCompat;

/**
 * 内嵌网页页面的空壳。
 * <p>
 * 页面本体是 {@link WfcWebViewFragment}：手机端由本壳装着，平板上同一份实现直接进右栏，
 * 标题栏、菜单、返回都由宿主提供。
 */
public class WfcWebViewActivity extends WfcBaseActivity {

    /**
     * 打开一个网址。<strong>调用方手上有 Fragment 时优先用
     * {@link #loadUrl(Fragment, String, String)}</strong>——那一版在平板上会把网页压到发起页
     * 所在的那条右栏栈上；本方法只有 Context，右栏只能靠上一次点击落在哪一栏去猜。
     */
    public static void loadUrl(Context context, String title, String url) {
        context.startActivity(buildUrlIntent(context, title, url));
    }

    public static void loadUrl(Fragment from, String title, String url) {
        WfcPageCompat.startPage(from, buildUrlIntent(from.requireContext(), title, url));
    }

    public static void loadHtmlContent(Context context, String title, String htmlContent) {
        context.startActivity(buildHtmlContentIntent(context, title, htmlContent));
    }

    public static void loadHtmlContent(Fragment from, String title, String htmlContent) {
        WfcPageCompat.startPage(from, buildHtmlContentIntent(from.requireContext(), title, htmlContent));
    }

    public static Intent buildUrlIntent(Context context, String title, String url) {
        Intent intent = new Intent(context, WfcWebViewActivity.class);
        intent.putExtra("url", url);
        intent.putExtra("title", title);
        return intent;
    }

    public static Intent buildHtmlContentIntent(Context context, String title, String htmlContent) {
        Intent intent = new Intent(context, WfcWebViewActivity.class);
        intent.putExtra("title", title);
        intent.putExtra("content", htmlContent);
        return intent;
    }

    @Override
    protected int contentLayout() {
        return R.layout.fragment_container_activity;
    }

    @Override
    protected void afterViews() {
        // 配置变化后 FragmentManager 已经把页面恢复出来了，无条件 add 会再叠一层
        if (getSupportFragmentManager().findFragmentById(R.id.containerFrameLayout) != null) {
            return;
        }
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.containerFrameLayout, WfcWebViewFragment.fromIntent(getIntent()))
            .commit();
    }
}
