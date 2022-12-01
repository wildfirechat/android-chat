/*
 * Copyright (c) 2022 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.voip;

import android.app.Dialog;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.third.utils.UIUtils;

public abstract class FullScreenBottomSheetDialogFragment extends BottomSheetDialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        BottomSheetDialog bottomSheet = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        View view = View.inflate(getContext(), R.layout.fullscreen_bottom_sheet_dialog_fragment, null);
        bottomSheet.setContentView(view);

        ViewStub contentViewStub = view.findViewById(R.id.contentViewStub);
        contentViewStub.setLayoutResource(contentLayout());
        contentViewStub.getLayoutParams().height = Resources.getSystem().getDisplayMetrics().heightPixels - getActionBarSize() - UIUtils.getStatusBarHeight(getContext());
        contentViewStub.inflate();

        BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from((View) (view.getParent()));
        //setting Peek at the 16:9 ratio keyline of its parent.
        bottomSheetBehavior.setPeekHeight(BottomSheetBehavior.PEEK_HEIGHT_AUTO);
        bottomSheetBehavior.setSkipCollapsed(skipCollapsed());
        bottomSheetBehavior.setState(bottomSheetState());

        AppBarLayout appBarLayout = view.findViewById(R.id.appBarLayout);

        bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View view, int i) {
                if (BottomSheetBehavior.STATE_EXPANDED == i) {
                    if (showAppBarLayout()) {
                        //appBarLayout.setVisibility(View.VISIBLE);
                        showView(appBarLayout, getActionBarSize());
                    }

                }
                if (BottomSheetBehavior.STATE_COLLAPSED == i) {
                    if (showAppBarLayout()) {
//                        appBarLayout.setVisibility(View.GONE);
                        hideAppBar(appBarLayout);
                    }
                }

                if (BottomSheetBehavior.STATE_HIDDEN == i) {
                    dismiss();
                }

            }

            @Override
            public void onSlide(@NonNull View view, float v) {

            }
        });

        //app bar cancel button clicked
        if (showAppBarLayout()) {
            TextView backTextView = view.findViewById(R.id.backTextView);
            if (!TextUtils.isEmpty(backText())) {
                backTextView.setText(backText());
                backTextView.setOnClickListener(view1 -> onBackClick());
            } else {
                backTextView.setVisibility(View.INVISIBLE);
            }

            TextView confirmTextView = view.findViewById(R.id.confirmTextView);
            if (!TextUtils.isEmpty(confirmText())) {
                confirmTextView.setText(confirmText());
                confirmTextView.setOnClickListener(view12 -> onConfirmClick());
            } else {
                confirmTextView.setVisibility(View.INVISIBLE);
            }
            TextView titleTextView = view.findViewById(R.id.titleTextView);
            titleTextView.setText(title());
        } else {
            //appBarLayout.setVisibility(showAppBarLayout() ? View.VISIBLE : View.GONE);
            hideAppBar(appBarLayout);
        }

        afterCreateDialogView(view);
        return bottomSheet;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    abstract protected int contentLayout();

    protected void afterCreateDialogView(View view) {

    }


    protected boolean showAppBarLayout() {
        return true;
    }

    protected String title() {
        return "Title";
    }

    protected String backText() {
        return getString(R.string.close);
    }

    protected String confirmText() {
        return getString(R.string.confirm);
    }

    protected void onBackClick() {
        dismiss();
    }

    protected void onConfirmClick() {
        dismiss();
    }

    protected int bottomSheetState() {
        return BottomSheetBehavior.STATE_EXPANDED;
    }

    protected boolean skipCollapsed() {
        return true;
    }

    protected void onCreateView() {

    }

    private void hideAppBar(View view) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.height = 0;
        view.setLayoutParams(params);

    }

    private void showView(View view, int size) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.height = size;
        view.setLayoutParams(params);
    }

    private int getActionBarSize() {
        final TypedArray array = getContext().getTheme().obtainStyledAttributes(new int[]{android.R.attr.actionBarSize});
        int size = (int) array.getDimension(0, 0);
        return size;
    }
}