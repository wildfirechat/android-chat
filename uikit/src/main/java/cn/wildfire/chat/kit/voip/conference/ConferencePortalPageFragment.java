/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.voip.conference;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import cn.wildfire.chat.kit.AppServiceProvider;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.page.WfcPage;
import cn.wildfire.chat.kit.page.WfcPageCompat;
import cn.wildfire.chat.kit.voip.conference.model.ConferenceInfo;

/**
 * 会议入口页（发现 tab → 会议）。
 * <p>
 * 逐行搬自 {@link ConferencePortalActivity}，那个类现在只是手机端的壳。入口是「发现」tab，
 * 平板上它本身就在左栏，点开会话/收藏列表应该落在右栏里，不迁的话要整屏跳出去再跳回来。
 */
public class ConferencePortalPageFragment extends Fragment implements WfcPage {

    RecyclerView recyclerView;
    LinearLayout emptyLinearLayout;

    private FavConferenceAdapter adapter;
    private List<ConferenceInfo> favConferenceList;

    public static ConferencePortalPageFragment fromIntent(Intent intent) {
        return new ConferencePortalPageFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.av_conference_portal_activity, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView = view.findViewById(R.id.conferenceListRecyclerView);
        emptyLinearLayout = view.findViewById(R.id.emptyLinearLayout);

        view.findViewById(R.id.startConferenceLinearLayout).setOnClickListener(v -> startConference());
        view.findViewById(R.id.joinConferenceLinearLayout).setOnClickListener(v -> joinConference());
        view.findViewById(R.id.orderConferenceLinearLayout).setOnClickListener(v -> orderConference());
        view.findViewById(R.id.conferenceHistoryButton).setOnClickListener(v -> showConferenceHistory());

        adapter = new FavConferenceAdapter();
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAndShowFavConference();
    }

    void startConference() {
        Intent intent = new Intent(getContext(), CreateConferenceActivity.class);
        WfcPageCompat.startPage(this, intent);
    }

    void joinConference() {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.av_conference_join_dialog, null);
        new MaterialDialog.Builder(requireContext())
            .customView(view, false)
            .cancelable(false)
            .negativeText(R.string.cancel)
            .positiveText(R.string.confirm)
            .onPositive(new MaterialDialog.SingleButtonCallback() {
                @Override
                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                    EditText callIdEditText = view.findViewById(R.id.callIdEditText);
                    EditText passwordEditText = view.findViewById(R.id.passwordEditText);
                    Intent intent = new Intent(getContext(), ConferenceInfoActivity.class);
                    intent.putExtra("conferenceId", callIdEditText.getText().toString());
                    intent.putExtra("password", passwordEditText.getText().toString());
                    WfcPageCompat.startPage(ConferencePortalPageFragment.this, intent);
                }
            })
            .build()
            .show();
    }

    void orderConference() {
        Intent intent = new Intent(getContext(), OrderConferenceActivity.class);
        WfcPageCompat.startPage(this, intent);
    }

    void showConferenceHistory() {
        Intent intent = new Intent(getContext(), ConferenceHistoryListActivity.class);
        WfcPageCompat.startPage(this, intent);
    }

    private void loadAndShowFavConference() {
        WfcUIKit.getWfcUIKit().getAppServiceProvider().getFavConferences(new AppServiceProvider.FavConferenceCallback() {
            @Override
            public void onSuccess(List<ConferenceInfo> infos) {
                if (getView() == null) {
                    return;
                }
                if (infos == null || infos.isEmpty()) {
                    emptyLinearLayout.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    recyclerView.setVisibility(View.VISIBLE);
                    emptyLinearLayout.setVisibility(View.GONE);
                    infos = infos.stream().filter(info -> info.getStartTime() > 0 && info.getEndTime() > 0).toList();
                    favConferenceList = infos;
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFail(int code, String msg) {
                if (getView() == null) {
                    return;
                }
                emptyLinearLayout.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            }
        });
    }

    class FavConferenceAdapter extends RecyclerView.Adapter<FavConferenceViewHolder> {

        @NonNull
        @Override
        public FavConferenceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            View view = inflater.inflate(R.layout.av_conference_fav_item, parent, false);
            return new FavConferenceViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull FavConferenceViewHolder holder, int position) {
            holder.onBind(favConferenceList.get(position));
        }

        @Override
        public int getItemCount() {
            return favConferenceList == null ? 0 : favConferenceList.size();
        }
    }

    class FavConferenceViewHolder extends RecyclerView.ViewHolder {
        private TextView titleTextView;
        private TextView startDateTimeTextView;
        private ConferenceInfo conferenceInfo;

        public FavConferenceViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            startDateTimeTextView = itemView.findViewById(R.id.startDateTimeTextView);

            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), ConferenceInfoActivity.class);
                intent.putExtra("conferenceId", conferenceInfo.getConferenceId());
                intent.putExtra("password", conferenceInfo.getPassword());
                // 右栏里 getContext 是双栏主界面，直接 startActivity 会把整个界面压成全屏。
                // 由外层 Fragment 路由，压到「发现」那条右栏栈上。
                WfcPageCompat.startPage(ConferencePortalPageFragment.this, intent);
            });
        }

        public void onBind(ConferenceInfo info) {
            titleTextView.setText(info.getConferenceTitle());
            startDateTimeTextView.setText(buildStartDateTimeDesc(itemView.getContext(), info));
            this.conferenceInfo = info;
        }
    }

    private static String buildStartDateTimeDesc(Context context, ConferenceInfo info) {
        long now = System.currentTimeMillis() / 1000;
        String desc;
        if (now > info.getEndTime()) {
            desc = context.getString(R.string.conf_ended);
        } else if (now > info.getStartTime()) {
            desc = context.getString(R.string.conf_started_join_prompt);
        } else {
            Calendar date = Calendar.getInstance();
            date.setTime(new Date(now * 1000));
            Calendar startDate = Calendar.getInstance();
            startDate.setTime(new Date(info.getStartTime() * 1000));
            if (date.get(Calendar.YEAR) == startDate.get(Calendar.YEAR) && date.get(Calendar.DAY_OF_YEAR) == startDate.get(Calendar.DAY_OF_YEAR)) {
                desc = context.getString(R.string.conf_today);
            } else {
                desc = (String) DateFormat.format(context.getString(R.string.conf_date_format), startDate);
            }
            desc += " ";
            desc += DateFormat.format("HH:mm", startDate);
            desc += " " + context.getString(R.string.conf_start_meeting);
        }

        return desc;
    }
}
