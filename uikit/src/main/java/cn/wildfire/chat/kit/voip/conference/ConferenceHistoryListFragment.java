/*
 * Copyright (c) 2022 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.voip.conference;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.joda.time.DateTime;

import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.third.utils.TimeConvertUtils;
import cn.wildfire.chat.kit.voip.conference.model.ConferenceInfo;
import cn.wildfirechat.remote.ChatManager;

public class ConferenceHistoryListFragment extends Fragment {
    private RecyclerView recyclerView;
    private ConferenceHistoryListAdapter adapter;
    private List<ConferenceInfo> conferenceHistoryList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.av_conference_history_list, container, false);
        initView(view);
        return view;
    }

    private void initView(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        adapter = new ConferenceHistoryListAdapter();
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));
        loadAndShowConferenceHistory();
    }

    private void loadAndShowConferenceHistory() {
        conferenceHistoryList = ConferenceManager.getManager().getHistoryConference();
        adapter.notifyDataSetChanged();
    }

    class ConferenceHistoryListAdapter extends RecyclerView.Adapter<ConferenceHistoryViewHolder> {

        @NonNull
        @Override
        public ConferenceHistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            View view = layoutInflater.inflate(R.layout.av_conference_history_list_item, parent, false);
            return new ConferenceHistoryViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ConferenceHistoryViewHolder holder, int position) {
            holder.onBind(conferenceHistoryList.get(position));
        }

        @Override
        public int getItemCount() {
            return conferenceHistoryList == null ? 0 : conferenceHistoryList.size();
        }
    }

    static class ConferenceHistoryViewHolder extends RecyclerView.ViewHolder {
        private TextView titleTextView;
        private TextView descTextView;

        public ConferenceHistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            descTextView = itemView.findViewById(R.id.descTextView);
        }

        public void onBind(ConferenceInfo conferenceInfo) {
            titleTextView.setText(conferenceInfo.getConferenceTitle());
            descTextView.setText(buildConferenceDesc(conferenceInfo));
        }

        private String buildConferenceDesc(ConferenceInfo conferenceInfo) {
            DateTime dateTime = new DateTime(conferenceInfo.getStartTime() * 1000);
            String startDateStr = dateTime.toString("yyyy-MM-dd HH:mm");
            String hostDisplayName = ChatManager.Instance().getUserDisplayName(conferenceInfo.getOwner());
            String duration;
            if (conferenceInfo.getEndTime() > conferenceInfo.getStartTime()) {
                duration = TimeConvertUtils.formatLongTime((conferenceInfo.getEndTime() - conferenceInfo.getStartTime()) * 1000);
            } else {
                duration = "-";
            }

            String desc = String.format("时间: %s 发起人: %s 时长: %s", startDateStr, hostDisplayName, duration);
            return desc;
        }
    }

}
