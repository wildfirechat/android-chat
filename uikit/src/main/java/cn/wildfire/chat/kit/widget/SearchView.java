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

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;

public class SearchView extends FrameLayout {

    @BindView(R2.id.editText)
    EditText mEditText;
    @BindView(R2.id.search_cancel)
    View mCancelView;

    @OnClick(R2.id.search_cancel)
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
        ButterKnife.bind(this);
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
