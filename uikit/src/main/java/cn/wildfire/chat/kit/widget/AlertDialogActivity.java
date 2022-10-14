/*
 * Copyright (c) 2022 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.widget;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.afollestad.materialdialogs.MaterialDialog;

import java.lang.ref.WeakReference;

public class AlertDialogActivity extends FragmentActivity {
    private static WeakReference<OnClickListener> negativeOnclickListerWr;
    private static WeakReference<OnClickListener> positiveOnclickListerWr;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        String title = intent.getStringExtra("title");
        boolean cancelable = intent.getBooleanExtra("cancelable", false);
        String negativeText = intent.getStringExtra("negativeText");
        String positiveText = intent.getStringExtra("positiveText");
        _showAlterDialog(title, cancelable, negativeText, positiveText, negativeOnclickListerWr.get(), positiveOnclickListerWr.get());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public static void showAlterDialog(Context context, String title, boolean cancelable, String negativeText, String positiveText, OnClickListener negativeListener, OnClickListener positiveListener) {
        Intent intent = new Intent(context, AlertDialogActivity.class);
        intent.putExtra("title", title);
        intent.putExtra("cancelable", cancelable);
        intent.putExtra("negativeText", negativeText);
        intent.putExtra("positiveText", positiveText);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        negativeOnclickListerWr = new WeakReference<>(negativeListener);
        positiveOnclickListerWr = new WeakReference<>(positiveListener);
        context.startActivity(intent);
    }

    private void _showAlterDialog(String content, boolean cancelable, String negativeText, String positiveText, OnClickListener negativeListener, OnClickListener positiveListener) {
        new MaterialDialog.Builder(this)
            .content(content)
            .negativeText(negativeText)
            .onNegative((dialog, which) -> {
                if (negativeListener != null) {
                    negativeListener.onClick();
                }
                finish();
            })
            .positiveText(positiveText)
            .onPositive((dialog, which) -> {
                if (positiveListener != null) {
                    positiveListener.onClick();
                }
                finish();
            })
            .content(content)
            .cancelListener(dialog -> finish())
            .cancelable(cancelable)
            .build()
            .show();
    }

    public interface OnClickListener {
        void onClick();
    }
}
