package cn.wildfire.chat.kit.contact.pick;

import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import butterknife.BindView;
import butterknife.OnFocusChange;
import butterknife.OnTextChanged;
import cn.wildfire.chat.kit.contact.BaseUserListFragment;
import cn.wildfire.chat.kit.contact.UserListAdapter;
import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfire.chat.kit.third.utils.UIUtils;
import cn.wildfire.chat.kit.widget.QuickIndexBar;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;

public abstract class PickUserFragment extends BaseUserListFragment implements QuickIndexBar.OnLetterUpdateListener {
    private SearchAndPickUserFragment searchAndPickUserFragment;
    protected PickUserViewModel pickUserViewModel;

    @BindView(R2.id.pickedUserRecyclerView)
    protected RecyclerView pickedUserRecyclerView;
    @BindView(R2.id.searchEditText)
    EditText searchEditText;
    @BindView(R2.id.searchFrameLayout)
    FrameLayout searchUserFrameLayout;
    @BindView(R2.id.hint_view)
    protected View hintView;

    private boolean isSearchFragmentShowing = false;
    private PickedUserAdapter pickedUserAdapter;

    private Observer<UIUserInfo> contactCheckStatusUpdateLiveDataObserver = userInfo -> {
        ((CheckableUserListAdapter) userListAdapter).updateUserStatus(userInfo);
        hideSearchContactFragment();
        updatePickedUserView(userInfo);
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pickUserViewModel = ViewModelProviders.of(getActivity()).get(PickUserViewModel.class);
        pickUserViewModel.userCheckStatusUpdateLiveData().observeForever(contactCheckStatusUpdateLiveDataObserver);
    }

    @Override
    protected void afterViews(View view) {
        super.afterViews(view);
        initView();
        setupPickFromUsers();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        pickUserViewModel.userCheckStatusUpdateLiveData().removeObserver(contactCheckStatusUpdateLiveDataObserver);
    }

    private void initView() {
        configPickedUserRecyclerView();
        pickedUserAdapter = getPickedUserAdapter();
        pickedUserRecyclerView.setAdapter(pickedUserAdapter);
    }

    protected void configPickedUserRecyclerView() {
        RecyclerView.LayoutManager pickedContactRecyclerViewLayoutManager = new LinearLayoutManager(getActivity(), GridLayoutManager.HORIZONTAL, false);
        pickedUserRecyclerView.setLayoutManager(pickedContactRecyclerViewLayoutManager);
        pickedUserRecyclerView.addItemDecoration(new Decoration());
    }

    protected PickedUserAdapter getPickedUserAdapter() {
        return new PickedUserAdapter();
    }

    @OnFocusChange(R2.id.searchEditText)
    void onSearchEditTextFocusChange(View view, boolean focus) {
        if (getActivity() == null || getActivity().isFinishing()) {
            return;
        }
        if (focus) {
            showSearchContactFragment();
        } else {
            hideSearchContactFragment();
        }
        handleHintView(focus);
    }

    protected void handleHintView(boolean focus) {
        if (pickedUserAdapter.getItemCount() == 0 && !focus) {
            hintView.setVisibility(View.VISIBLE);
        } else {
            hintView.setVisibility(View.GONE);
        }
    }

    @OnTextChanged(value = R2.id.searchEditText, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    void search(Editable editable) {
        // restore view state
        if (searchAndPickUserFragment == null) {
            return;
        }
        String key = editable.toString();
        if (!TextUtils.isEmpty(key)) {
            searchAndPickUserFragment.search(key);
        } else {
            searchAndPickUserFragment.rest();
        }
    }

    @Override
    public int getContentLayoutResId() {
        return R.layout.contact_pick_fragment;
    }

    @Override
    public UserListAdapter onCreateUserListAdapter() {
        return new CheckableUserListAdapter(this);
    }

    abstract protected void setupPickFromUsers();

    private void showSearchContactFragment() {
        if (searchAndPickUserFragment == null) {
            searchAndPickUserFragment = new SearchAndPickUserFragment();
            searchAndPickUserFragment.setPickUserFragment(this);
        }
        searchUserFrameLayout.setVisibility(View.VISIBLE);
        getChildFragmentManager().beginTransaction()
                .replace(R.id.searchFrameLayout, searchAndPickUserFragment)
                .commit();
        isSearchFragmentShowing = true;
    }

    public void hideSearchContactFragment() {
        if (!isSearchFragmentShowing) {
            return;
        }

        searchEditText.setText("");
        searchEditText.clearFocus();
        searchUserFrameLayout.setVisibility(View.GONE);
        getChildFragmentManager().beginTransaction().remove(searchAndPickUserFragment).commit();
        isSearchFragmentShowing = false;
    }

    @Override
    public void onUserClick(UIUserInfo userInfo) {
        if (userInfo.isCheckable()) {
            if (!pickUserViewModel.checkUser(userInfo, !userInfo.isChecked())) {
                Toast.makeText(getActivity(), "选人超限", Toast.LENGTH_SHORT).show();
            }
        }
    }

    protected void handleEditText() {
        if (pickedUserAdapter.getItemCount() == 0) {
            searchEditText.setHint("");
        } else {
            searchEditText.setHint("搜索");
        }
    }

    private void updatePickedUserView(UIUserInfo userInfo) {
        if (userInfo.isChecked()) {
            pickedUserAdapter.addUser(userInfo);
        } else {
            pickedUserAdapter.removeUser(userInfo);
        }
        handleHintView(false);
        handleEditText();
    }

    private static class Decoration extends RecyclerView.ItemDecoration {
        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);
            if (position < parent.getAdapter().getItemCount() - 1) {
                outRect.right = UIUtils.dip2Px(4);
            } else {
                outRect.right = 0;
            }
        }
    }
}
