package cn.wildfire.chat.kit.contact.pick;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.Bind;
import butterknife.OnFocusChange;
import butterknife.OnTextChanged;
import cn.wildfire.chat.kit.contact.BaseContactFragment;
import cn.wildfire.chat.kit.contact.ContactAdapter;
import cn.wildfire.chat.kit.contact.ContactViewModel;
import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfire.chat.kit.widget.QuickIndexBar;
import cn.wildfirechat.chat.R;

public class PickContactFragment extends BaseContactFragment implements QuickIndexBar.OnLetterUpdateListener {
    private SearchAndPickContactFragment searchAndPickContactFragment;
    private PickContactViewModel pickContactViewModel;

    @Bind(R.id.pickedContactRecyclerView)
    RecyclerView pickedContactRecyclerView;
    @Bind(R.id.searchEditText)
    EditText searchEditText;
    @Bind(R.id.searchContactFrameLayout)
    FrameLayout searchContactFrameLayout;

    private boolean isSearchFragmentShowing = false;

    private Observer<UIUserInfo> contactCheckStatusUpdateLiveDataObserver = userInfo -> {
        ((CheckableContactAdapter) contactAdapter).updateContactStatus(userInfo);
        hideSearchContactFragment();
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pickContactViewModel = ViewModelProviders.of(getActivity()).get(PickContactViewModel.class);
        pickContactViewModel.contactCheckStatusUpdateLiveData().observeForever(contactCheckStatusUpdateLiveDataObserver);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        initView();
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        pickContactViewModel.contactCheckStatusUpdateLiveData().removeObserver(contactCheckStatusUpdateLiveDataObserver);
    }

    private void initView() {
        RecyclerView.LayoutManager pickedContactRecyclerViewLayoutManager = new GridLayoutManager(getActivity(), 1, GridLayoutManager.HORIZONTAL, false);
        pickedContactRecyclerView.setLayoutManager(pickedContactRecyclerViewLayoutManager);
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
        String key = editable.toString();
        if (!TextUtils.isEmpty(key)) {
            searchAndPickContactFragment.search(key);
        } else {
            searchAndPickContactFragment.rest();
        }
    }

    @Override
    public int getContentLayoutResId() {
        return R.layout.contact_pick_fragment;
    }

    @Override
    public ContactAdapter onCreateContactAdapter() {
        CheckableContactAdapter checkableContactAdapter = new CheckableContactAdapter(this);
        ContactViewModel contactViewModel = ViewModelProviders.of(getActivity()).get(ContactViewModel.class);

        List<UIUserInfo> contacts = userInfoToUIUserInfo(contactViewModel.getContacts(false));
        pickContactViewModel.setContacts(contacts);
        checkableContactAdapter.setContacts(contacts);
        return checkableContactAdapter;
    }

    private void showSearchContactFragment() {
        if (searchAndPickContactFragment == null) {
            searchAndPickContactFragment = new SearchAndPickContactFragment();
            searchAndPickContactFragment.setPickContactFragment(this);
        }
        searchContactFrameLayout.setVisibility(View.VISIBLE);
        getChildFragmentManager().beginTransaction()
                .replace(R.id.searchContactFrameLayout, searchAndPickContactFragment)
                .commit();
        isSearchFragmentShowing = true;
    }

    public void hideSearchContactFragment() {
        if (!isSearchFragmentShowing) {
            return;
        }

        searchEditText.setText("");
        searchEditText.clearFocus();
        searchContactFrameLayout.setVisibility(View.GONE);
        getChildFragmentManager().beginTransaction().remove(searchAndPickContactFragment).commit();
        isSearchFragmentShowing = false;
    }

    @Override
    public void onContactClick(UIUserInfo userInfo) {
        if (userInfo.isCheckable()) {
            if (!pickContactViewModel.checkContact(userInfo, !userInfo.isChecked())) {
                Toast.makeText(getActivity(), "选人超限", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
