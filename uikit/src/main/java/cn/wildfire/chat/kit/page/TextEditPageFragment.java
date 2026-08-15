/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.page;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import cn.wildfire.chat.kit.widget.SimpleTextWatcher;

/**
 * 「改一段文字然后保存」这一类页面的公共部分：一个输入框 + 一个保存菜单，空文本时保存不可用。
 * <p>
 * 设置备注名、改昵称、群名称、我在本群的昵称、群公告，改造前是五个各写一遍这段逻辑的
 * {@code WfcBaseActivity}。写在 Activity 上的页面进不了平板右栏（右栏里没有 Activity），
 * 所以整页下沉到 Fragment，手机端由一个空壳 Activity 装着，两端共用同一份。
 * <p>
 * 子类只需回答四件事：布局、输入框 id、保存菜单、按下保存做什么。初值在
 * {@link #onPageViewCreated} 里填。
 */
public abstract class TextEditPageFragment extends Fragment implements WfcPage {

    protected EditText editText;
    private MenuItem confirmMenuItem;

    @LayoutRes
    protected abstract int contentLayout();

    @IdRes
    protected abstract int editTextId();

    /**
     * 保存菜单项的 id，须存在于 {@link #pageMenu()} 里。
     */
    protected abstract int confirmMenuItemId();

    /**
     * 按下保存。{@code text} 已 trim。保存成功后自行调用 {@link #finishPage()}。
     */
    protected abstract void onConfirm(String text);

    /**
     * 输入为空时保存是否仍然可用。默认否 —— 只有「清空备注」这类页面才需要 true。
     */
    protected boolean allowEmptyText() {
        return false;
    }

    /**
     * 视图与输入框都已就绪，子类在这里填初值（{@link #setText}）、设提示、加输入过滤器。
     */
    protected void onPageViewCreated(@NonNull View view) {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(contentLayout(), container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        editText = view.findViewById(editTextId());
        if (editText != null) {
            editText.addTextChangedListener(new SimpleTextWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                    onEditTextChanged();
                }
            });
        }
        onPageViewCreated(view);
        updateConfirmState();
    }

    // ==================== WfcPage ====================

    @Override
    public void onPreparePageMenu(Menu menu) {
        confirmMenuItem = menu.findItem(confirmMenuItemId());
        updateConfirmState();
    }

    @Override
    public boolean onPageMenuItemSelected(MenuItem item) {
        if (item.getItemId() == confirmMenuItemId()) {
            onConfirm(text());
            return true;
        }
        return false;
    }

    // ==================== 给子类用的小工具 ====================

    /**
     * 当前输入内容（已 trim）。视图还没建好时返回空串。
     */
    protected String text() {
        return editText == null ? "" : editText.getText().toString().trim();
    }

    /**
     * 填入初值并把光标移到末尾。
     */
    protected void setText(@Nullable CharSequence value) {
        if (editText == null || TextUtils.isEmpty(value)) {
            return;
        }
        editText.setText(value);
        editText.setSelection(value.length());
    }

    protected void setHint(@Nullable CharSequence hint) {
        if (editText != null && !TextUtils.isEmpty(hint)) {
            editText.setHint(hint);
        }
    }

    /**
     * 输入变化。子类可覆写以加入自己的判断（群公告：与服务端当前公告相同则不允许保存）。
     * <p>
     * 不叫 {@code onTextChanged} —— 那个名字会和 {@link SimpleTextWatcher} 继承来的
     * {@code TextWatcher.onTextChanged(CharSequence,int,int,int)} 撞在一起。
     */
    protected void onEditTextChanged() {
        updateConfirmState();
    }

    /**
     * 重算保存菜单的可用状态。
     * <p>
     * 菜单与视图谁先就绪并不确定（手机端菜单可能在内容 Fragment 的视图之前 inflate 出来），
     * 所以两条路径都会调到这里，且两个字段都要判空。
     */
    protected void updateConfirmState() {
        if (confirmMenuItem != null) {
            confirmMenuItem.setEnabled(isConfirmEnabled());
        }
    }

    protected boolean isConfirmEnabled() {
        return allowEmptyText() || !TextUtils.isEmpty(text());
    }

    /**
     * 关掉本页：手机端 finish，右栏里出栈。
     */
    protected void finishPage() {
        WfcPageCompat.finishPage(this);
    }
}
