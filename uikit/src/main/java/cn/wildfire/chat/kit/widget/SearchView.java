/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.widget;

import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import cn.wildfire.chat.kit.R;

public class SearchView extends FrameLayout {

    EditText mEditText;
    View mCancelView;

    public void onCancelClick() {
        mEditText.setText("");
    }

    private OnQueryTextListener mListener;

    public SearchView(@NonNull Context context) {
        this(context, null);
    }

    public SearchView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SearchView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.search_view, this);
        bindViews();
        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                search(s.toString());
                handleCancelView(s.toString());
            }
        });
    }

    private void bindViews() {
        mEditText = findViewById(R.id.editText);
        mCancelView = findViewById(R.id.search_cancel);
        mCancelView.setOnClickListener(_v -> onCancelClick());
    }

    private void search(String s) {
        if (mListener != null) {
            mListener.onQueryTextChange(s);
        }
    }

    private void handleCancelView(String s) {
        if (TextUtils.isEmpty(s)) {
            mCancelView.setVisibility(View.GONE);
        } else {
            mCancelView.setVisibility(View.VISIBLE);
        }
    }

    public void setQuery(String s) {
        mEditText.setText(s);
    }

    public void setOnQueryTextListener(OnQueryTextListener listener) {
        mListener = listener;
    }

    public interface OnQueryTextListener {
        void onQueryTextChange(String s);
    }
}
