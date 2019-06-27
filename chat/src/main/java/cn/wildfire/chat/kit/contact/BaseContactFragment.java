package cn.wildfire.chat.kit.contact;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.contact.model.FooterValue;
import cn.wildfire.chat.kit.contact.model.HeaderValue;
import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfire.chat.kit.contact.viewholder.footer.FooterViewHolder;
import cn.wildfire.chat.kit.contact.viewholder.header.HeaderViewHolder;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfire.chat.kit.utils.PinyinUtils;
import cn.wildfire.chat.kit.widget.QuickIndexBar;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.model.UserInfo;

public abstract class BaseContactFragment extends Fragment implements QuickIndexBar.OnLetterUpdateListener, ContactAdapter.OnContactClickListener, ContactAdapter.OnHeaderClickListener, ContactAdapter.OnFooterClickListener {

    @Bind(R.id.contactRecyclerView)
    RecyclerView contactRecyclerView;
    @Bind(R.id.quickIndexBar)
    QuickIndexBar quickIndexBar;
    @Bind(R.id.indexLetterTextView)
    TextView indexLetterTextView;

    protected ContactAdapter contactAdapter;

    private LinearLayoutManager linearLayoutManager;
    protected ContactViewModel contactViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        contactViewModel = WfcUIKit.getAppScopeViewModel(ContactViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(getContentLayoutResId(), container, false);
        ButterKnife.bind(this, view);
        initView();
        return view;
    }

    private void initView() {

        contactAdapter = onCreateContactAdapter();
        contactAdapter.setOnContactClickListener(this);
        contactAdapter.setOnHeaderClickListener(this);
        contactAdapter.setOnFooterClickListener(this);

        initHeaderViewHolders();
        initFooterViewHolders();

        contactRecyclerView.setAdapter(contactAdapter);
        linearLayoutManager = new LinearLayoutManager(getActivity());
        contactRecyclerView.setLayoutManager(linearLayoutManager);
        quickIndexBar.setOnLetterUpdateListener(this);
    }

    public void initHeaderViewHolders() {
        // no header
    }

    public void initFooterViewHolders() {
        // no footer
    }

    /**
     * 一定要包含一个id为contactRecyclerView的RecyclerView
     *
     * @return
     */
    protected int getContentLayoutResId() {
        return R.layout.contact_contacts_fragment;
    }

    /**
     * the data for this contactAdapter should be set here
     *
     * @return
     */
    protected ContactAdapter onCreateContactAdapter() {
        contactAdapter = new ContactAdapter(this);
        contactAdapter.setContacts(userInfoToUIUserInfo(contactViewModel.getContacts(true)));
        return contactAdapter;
    }

    protected void addHeaderViewHolder(Class<? extends HeaderViewHolder> clazz, HeaderValue value) {
        contactAdapter.addHeaderViewHolder(clazz, value);
        // to do notify header changed
    }

    protected void addFooterViewHolder(Class<? extends FooterViewHolder> clazz, FooterValue value) {
        contactAdapter.addFooterViewHolder(clazz, value);
    }

    @Override
    public void onLetterUpdate(String letter) {
        indexLetterTextView.setVisibility(View.VISIBLE);
        indexLetterTextView.setText(letter);

        if ("↑".equalsIgnoreCase(letter)) {
            linearLayoutManager.scrollToPositionWithOffset(0, 0);
        } else if ("☆".equalsIgnoreCase(letter)) {
            linearLayoutManager.scrollToPositionWithOffset(contactAdapter.headerCount(), 0);
        } else if ("#".equalsIgnoreCase(letter)) {
            List<UIUserInfo> data = contactAdapter.getContacts();
            for (int i = 0; i < data.size(); i++) {
                UIUserInfo friend = data.get(i);
                if (friend.getCategory().equals("#")) {
                    linearLayoutManager.scrollToPositionWithOffset(contactAdapter.headerCount() + i, 0);
                    break;
                }
            }
        } else {
            List<UIUserInfo> data = contactAdapter.getContacts();
            for (int i = 0; i < data.size(); i++) {
                UIUserInfo friend = data.get(i);
                if (friend.getCategory().compareTo(letter) >= 0) {
                    linearLayoutManager.scrollToPositionWithOffset(i + contactAdapter.headerCount(), 0);
                    break;
                }
            }
        }
    }

    @Override
    public void onLetterCancel() {
        indexLetterTextView.setVisibility(View.GONE);
    }

    /**
     * 是否显示快速导航条
     *
     * @param show
     */
    public void showQuickIndexBar(boolean show) {
        if (quickIndexBar != null) {
            quickIndexBar.setVisibility(show ? View.VISIBLE : View.GONE);
            quickIndexBar.invalidate();
        }
    }

    @Override
    public void onContactClick(UIUserInfo userInfo) {

    }

    @Override
    public void onHeaderClick(int index) {

    }

    @Override
    public void onFooterClick(int index) {

    }

    private UIUserInfo userInfoToUIUserInfo(UserInfo userInfo) {
        UIUserInfo info = new UIUserInfo(userInfo);
        String indexLetter;
        UserViewModel userViewModel = WfcUIKit.getAppScopeViewModel(UserViewModel.class);
        String displayName = userViewModel.getUserDisplayName(userInfo);
        if (!TextUtils.isEmpty(displayName)) {
            String pinyin = PinyinUtils.getPinyin(displayName);
            char c = pinyin.toUpperCase().charAt(0);
            if (c >= 'A' && c <= 'Z') {
                indexLetter = c + "";
                info.setSortName(pinyin);
            } else {
                indexLetter = "#";
                // 为了让排序排到最后
                info.setSortName("{" + pinyin);
            }
            info.setCategory(indexLetter);
        } else {
            info.setSortName("");
        }
        return info;
    }

    protected List<UIUserInfo> userInfoToUIUserInfo(List<UserInfo> userInfos) {
        if (userInfos != null) {
            List<UIUserInfo> uiUserInfos = new ArrayList<>(userInfos.size());
            String indexLetter;
            for (UserInfo userInfo : userInfos) {
                uiUserInfos.add(userInfoToUIUserInfo(userInfo));
            }
            Collections.sort(uiUserInfos, (o1, o2) -> o1.getSortName().compareToIgnoreCase(o2.getSortName()));

            String preIndexLetter = null;
            for (UIUserInfo info : uiUserInfos) {
                indexLetter = info.getCategory();
                if (preIndexLetter == null || !preIndexLetter.equals(indexLetter)) {
                    info.setShowCategory(true);
                }
                preIndexLetter = indexLetter;
            }
            return uiUserInfos;
        } else {
            return null;
        }
    }
}
