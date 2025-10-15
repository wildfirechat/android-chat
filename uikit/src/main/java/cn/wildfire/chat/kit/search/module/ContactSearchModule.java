/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.search.module;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.contact.ContactViewModel;
import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfire.chat.kit.contact.viewholder.UserViewHolder;
import cn.wildfire.chat.kit.conversation.ConversationActivity;
import cn.wildfire.chat.kit.search.SearchableModule;
import cn.wildfire.chat.kit.utils.PinyinUtils;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.UserInfo;

public class ContactSearchModule extends SearchableModule<UserInfo, UserViewHolder> {
    @Override
    public UserViewHolder onCreateViewHolder(Fragment fragment, @NonNull ViewGroup parent, int viewType) {
        View itemView;
        itemView = LayoutInflater.from(fragment.getActivity()).inflate(R.layout.contact_item_contact, parent, false);
        return new UserViewHolder(fragment, null, itemView);
    }

    @Override
    public void onBind(Fragment fragment, UserViewHolder holder, UserInfo userInfo) {
        holder.onBind(new UIUserInfo(userInfo));
    }

    @Override
    public void onClick(Fragment fragment, UserViewHolder holder, View view, UserInfo userInfo) {
        Intent intent = new Intent(fragment.getActivity(), ConversationActivity.class);
        Conversation conversation = new Conversation(Conversation.ConversationType.Single, userInfo.uid, 0);
        intent.putExtra("conversation", conversation);
        fragment.startActivity(intent);
        fragment.getActivity().finish();
    }

    @Override
    public int getViewType(UserInfo userInfo) {
        return R.layout.contact_item_contact;
    }

    @Override
    public int priority() {
        return 100;
    }

    @Override
    public String category() {
        return WfcUIKit.getWfcUIKit().getApplication().getString(R.string.contact_category);
    }

    @Override
    public List<UserInfo> search(String keyword) {
        ContactViewModel contactViewModel = WfcUIKit.getAppScopeViewModel(ContactViewModel.class);
        List<UIUserInfo> contacts = contactViewModel.getContacts();
        List<UserInfo> results = new ArrayList<>();
        keyword = keyword.toLowerCase();
        boolean isEnglishOnly = isEnglishOnly(keyword);
        if (isEnglishOnly) {
            HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
            format.setCaseType(HanyuPinyinCaseType.UPPERCASE);
            format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        }
        for (UIUserInfo uiUserInfo : contacts) {
            UserInfo info = uiUserInfo.getUserInfo();
            if (info.displayName.toLowerCase().contains(keyword) || (info.friendAlias != null && info.friendAlias.toLowerCase().contains(keyword))) {
                results.add(info);
            } else if (isEnglishOnly) {
                String displayNamePinyin = PinyinUtils.getPinyin(info.displayName);
                String friendAliasPinyin = info.friendAlias == null ? "" : PinyinUtils.getPinyin(info.friendAlias);
                String displayNameFirstLetter = PinyinUtils.getPinyinFirstLetter(info.displayName);
                String friendAliasFirstLetter = info.friendAlias == null ? "" : PinyinUtils.getPinyinFirstLetter(info.friendAlias);
                if (displayNamePinyin.toLowerCase().contains(keyword)
                    || friendAliasPinyin.toLowerCase().contains(keyword)
                    || displayNameFirstLetter.toLowerCase().contains(keyword)
                    || friendAliasFirstLetter.toLowerCase().contains(keyword)) {
                    results.add(info);
                }
            }
        }

        return results;
    }

    private static boolean isEnglishOnly(String input) {
        return input.matches("^[a-zA-Z0-9]+$");
    }
}
