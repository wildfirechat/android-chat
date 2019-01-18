package com.lqr.imagepicker.view;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;

import com.lqr.imagepicker.R;

public class FolderPopUpWindow extends PopupWindow implements View.OnClickListener {

    private View rootView;
    private ListView listView;
    private OnItemClickListener onItemClickListener;
    private final View masker;
    private final View marginView;
    private int marginPx;

    public FolderPopUpWindow(Context context, BaseAdapter adapter) {
        super(context);

        rootView = View.inflate(context, R.layout.pop_folder, null);
        masker = rootView.findViewById(R.id.masker);
        masker.setOnClickListener(this);
        marginView = rootView.findViewById(R.id.margin);
        marginView.setOnClickListener(this);
        listView = (ListView) rootView.findViewById(R.id.listView);
        listView.setAdapter(adapter);

        setContentView(rootView);
        setWidth(ViewGroup.LayoutParams.MATCH_PARENT);  //如果不设置，就是 AnchorView 的宽度
        setHeight(ViewGroup.LayoutParams.MATCH_PARENT);
        setFocusable(true);
        setOutsideTouchable(true);
        setBackgroundDrawable(new ColorDrawable(0));
        setAnimationStyle(0);
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                rootView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                int maxHeight = rootView.getHeight() * 6 / 8;
                int realHeight = listView.getHeight();
                ViewGroup.LayoutParams listParams = listView.getLayoutParams();
                listParams.height = realHeight > maxHeight ? maxHeight : realHeight;
                listView.setLayoutParams(listParams);
                LinearLayout.LayoutParams marginParams = (LinearLayout.LayoutParams) marginView.getLayoutParams();
                marginParams.height = marginPx;
                marginView.setLayoutParams(marginParams);
                enterAnimator();
            }
        });
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                if (onItemClickListener != null)
                    onItemClickListener.onItemClick(adapterView, view, position, l);
            }
        });
    }

    private void enterAnimator() {
        ObjectAnimator alpha = ObjectAnimator.ofFloat(masker, "alpha", 0, 1);
        ObjectAnimator translationY = ObjectAnimator.ofFloat(rootView, "translationY", rootView.getHeight(), 0);
        AnimatorSet set = new AnimatorSet();
        set.setDuration(300);
        set.playTogether(alpha, translationY);
        set.setInterpolator(new AccelerateDecelerateInterpolator());
        set.start();
    }

    @Override
    public void dismiss() {
        exitAnimator();
    }

    private void exitAnimator() {
        ObjectAnimator alpha = ObjectAnimator.ofFloat(masker, "alpha", 1, 0);
        ObjectAnimator translationY = ObjectAnimator.ofFloat(rootView, "translationY", 0, rootView.getHeight());
        AnimatorSet set = new AnimatorSet();
        set.setDuration(300);
        set.playTogether(alpha, translationY);
        set.setInterpolator(new AccelerateDecelerateInterpolator());
        set.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                rootView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                FolderPopUpWindow.super.dismiss();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
        set.start();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    public void setSelection(int selection) {
        listView.setSelection(selection);
    }

    public void setMargin(int marginPx) {
        this.marginPx = marginPx;
    }

    @Override
    public void onClick(View v) {
        dismiss();
    }

    public interface OnItemClickListener {
        void onItemClick(AdapterView<?> adapterView, View view, int position, long l);
    }
}
