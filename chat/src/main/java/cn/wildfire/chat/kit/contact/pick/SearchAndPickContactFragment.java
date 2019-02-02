package cn.wildfire.chat.kit.contact.pick;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.wildfire.chat.kit.contact.ContactAdapter;
import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfirechat.chat.R;

public class SearchAndPickContactFragment extends Fragment implements ContactAdapter.OnContactClickListener {
    private CheckableContactAdapter contactAdapter;
    private PickContactViewModel pickContactViewModel;
    private PickContactFragment pickContactFragment;

    @Bind(R.id.contactRecyclerView)
    RecyclerView contactRecyclerView;
    @Bind(R.id.tipTextView)
    TextView tipTextView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.contact_search_fragment, container, false);
        ButterKnife.bind(this, view);
        init();
        return view;
    }

    public void setPickContactFragment(PickContactFragment pickContactFragment) {
        this.pickContactFragment = pickContactFragment;
    }


    @OnClick(R.id.tipTextView)
    void onTipTextViewClick() {
        pickContactFragment.hideSearchContactFragment();
    }

    private void init() {
        pickContactViewModel = ViewModelProviders.of(getActivity()).get(PickContactViewModel.class);
        contactAdapter = new CheckableContactAdapter(this);
        contactAdapter.setOnContactClickListener(this);
        contactRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        contactRecyclerView.setAdapter(contactAdapter);
    }

    public void search(String keyword) {
        if (TextUtils.isEmpty(keyword)) {
            return;
        }
        List<UIUserInfo> result = pickContactViewModel.searchContact(keyword);
        if (result == null || result.isEmpty()) {
            contactRecyclerView.setVisibility(View.GONE);
            tipTextView.setVisibility(View.VISIBLE);
        } else {
            contactRecyclerView.setVisibility(View.VISIBLE);
            tipTextView.setVisibility(View.GONE);
        }
        contactAdapter.setContacts(result);
        contactAdapter.notifyDataSetChanged();
    }

    public void rest() {
        tipTextView.setVisibility(View.VISIBLE);
        contactRecyclerView.setVisibility(View.GONE);
        contactAdapter.setContacts(null);
    }

    @Override
    public void onContactClick(UIUserInfo userInfo) {
        if (userInfo.isCheckable()) {
            pickContactViewModel.checkContact(userInfo, !userInfo.isChecked());
            // the checked status has already changed by checkContact method
            contactAdapter.updateContactStatus(userInfo);
        }
    }
}
