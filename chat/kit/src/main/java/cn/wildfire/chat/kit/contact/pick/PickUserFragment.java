package cn.wildfire.chat.kit.contact.pick;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import butterknife.BindView;
import butterknife.OnFocusChange;
import butterknife.OnTextChanged;
import cn.wildfire.chat.kit.contact.BaseUserListFragment;
import cn.wildfire.chat.kit.contact.UserListAdapter;
import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfire.chat.kit.widget.QuickIndexBar;
import cn.wildfirechat.chat.R;

public abstract class PickUserFragment extends BaseUserListFragment implements QuickIndexBar.OnLetterUpdateListener {
    private SearchAndPickUserFragment searchAndPickUserFragment;
    protected PickUserViewModel pickUserViewModel;

    @BindView(R.id.pickedUserRecyclerView)
    RecyclerView pickedUserRecyclerView;
    @BindView(R.id.searchEditText)
    EditText searchEditText;
    @BindView(R.id.searchFrameLayout)
    FrameLayout searchUserFrameLayout;

    private boolean isSearchFragmentShowing = false;

    private Observer<UIUserInfo> contactCheckStatusUpdateLiveDataObserver = userInfo -> {
        ((CheckableUserListAdapter) userListAdapter).updateUserStatus(userInfo);
        hideSearchContactFragment();
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
        RecyclerView.LayoutManager pickedContactRecyclerViewLayoutManager = new GridLayoutManager(getActivity(), 1, GridLayoutManager.HORIZONTAL, false);
        pickedUserRecyclerView.setLayoutManager(pickedContactRecyclerViewLayoutManager);
    }

    @OnFocusChange(R.id.searchEditText)
    void onSearchEditTextFocusChange(View view, boolean focus) {
        if (getActivity() == null || getActivity().isFinishing()) {
            return;
        }
        if (focus) {
            showSearchContactFragment();
        } else {
            hideSearchContactFragment();
        }
    }

    @OnTextChanged(value = R.id.searchEditText, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
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
}
