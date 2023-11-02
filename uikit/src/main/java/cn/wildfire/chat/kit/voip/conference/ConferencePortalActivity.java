package cn.wildfire.chat.kit.voip.conference;

import android.content.Intent;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
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
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.voip.conference.model.ConferenceInfo;

public class ConferencePortalActivity extends WfcBaseActivity {

    RecyclerView recyclerView;
    LinearLayout emptyLinearLayout;

    private FavConferenceAdapter adapter;
    private List<ConferenceInfo> favConferenceList;

    protected void bindEvents() {
        super.bindEvents();
        findViewById(R.id.startConferenceLinearLayout).setOnClickListener(v -> startConference());
        findViewById(R.id.joinConferenceLinearLayout).setOnClickListener(v -> joinConference());
        findViewById(R.id.orderConferenceLinearLayout).setOnClickListener(v -> orderConference());
        findViewById(R.id.conferenceHistoryButton).setOnClickListener(v -> showConferenceHistory());
    }

    protected void bindViews() {
        super.bindViews();
        recyclerView = findViewById(R.id.conferenceListRecyclerView);
        emptyLinearLayout = findViewById(R.id.emptyLinearLayout);
    }

    @Override
    protected int contentLayout() {
        return R.layout.av_conference_portal_activity;
    }

    @Override
    protected void afterViews() {
        adapter = new FavConferenceAdapter();
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAndShowFavConference();
    }

    void startConference() {
        Intent intent = new Intent(this, CreateConferenceActivity.class);
        startActivity(intent);
    }

    void joinConference() {
        View view = LayoutInflater.from(this).inflate(R.layout.av_conference_join_dialog, null);
        new MaterialDialog.Builder(this)
            .customView(view, false)
            .cancelable(false)
            .negativeText("取消")
            .positiveText("确认")
            .onPositive(new MaterialDialog.SingleButtonCallback() {
                @Override
                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                    EditText callIdEditText = view.findViewById(R.id.callIdEditText);
                    EditText passwordEditText = view.findViewById(R.id.passwordEditText);
                    Intent intent = new Intent(ConferencePortalActivity.this, ConferenceInfoActivity.class);
                    intent.putExtra("conferenceId", callIdEditText.getText().toString());
                    intent.putExtra("password", passwordEditText.getText().toString());
                    startActivity(intent);
                }
            })
            .build()
            .show();
    }

    void orderConference() {
        Intent intent = new Intent(this, OrderConferenceActivity.class);
        startActivity(intent);
    }

    void showConferenceHistory() {
        Intent intent = new Intent(this, ConferenceHistoryListActivity.class);
        startActivity(intent);
    }


    private void loadAndShowFavConference() {
        WfcUIKit.getWfcUIKit().getAppServiceProvider().getFavConferences(new AppServiceProvider.FavConferenceCallback() {
            @Override
            public void onSuccess(List<ConferenceInfo> infos) {
                if (infos == null || infos.isEmpty()) {
                    emptyLinearLayout.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    recyclerView.setVisibility(View.VISIBLE);
                    emptyLinearLayout.setVisibility(View.GONE);
                    favConferenceList = infos;
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFail(int code, String msg) {
                emptyLinearLayout.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);

            }
        });

    }

    class FavConferenceAdapter extends RecyclerView.Adapter<FavConferenceViewHolder> {

        @NonNull
        @Override
        public FavConferenceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(ConferencePortalActivity.this);
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

    static class FavConferenceViewHolder extends RecyclerView.ViewHolder {
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
                v.getContext().startActivity(intent);
            });

        }

        public void onBind(ConferenceInfo info) {
            titleTextView.setText(info.getConferenceTitle());
            startDateTimeTextView.setText(buildStartDateTimeDesc(info));
            this.conferenceInfo = info;
        }

    }

    private static String buildStartDateTimeDesc(ConferenceInfo info) {
        long now = System.currentTimeMillis() / 1000;
        String desc;
        if (now > info.getEndTime()) {
            desc = "会议已结束";
        } else if (now > info.getStartTime()) {
            desc = "会议已开始，请尽快加入";
        } else {
            Calendar date = Calendar.getInstance();
            date.setTime(new Date(now * 1000));
            Calendar startDate = Calendar.getInstance();
            startDate.setTime(new Date(info.getStartTime() * 1000));
            if (date.get(Calendar.YEAR) == startDate.get(Calendar.YEAR) && date.get(Calendar.DAY_OF_YEAR) == startDate.get(Calendar.DAY_OF_YEAR)) {
                desc = "今天";
            } else {
                desc = (String) DateFormat.format("MM月dd日", startDate);
            }
            desc += " ";
            desc += DateFormat.format("HH:mm", startDate);
            desc += " 开始会议";
        }

        return desc;
    }
}