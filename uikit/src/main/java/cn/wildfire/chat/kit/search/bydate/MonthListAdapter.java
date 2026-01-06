package cn.wildfire.chat.kit.search.bydate;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Map;

public class MonthListAdapter extends RecyclerView.Adapter<MonthListAdapter.ViewHolder> {

    public static class MonthItem {
        public int year;
        public int month;
        public Map<Integer, Integer> dayMessageCount;

        public MonthItem(int year, int month, Map<Integer, Integer> dayMessageCount) {
            this.year = year;
            this.month = month;
            this.dayMessageCount = dayMessageCount;
        }
    }

    private List<MonthItem> months;
    private MessageCountCalendarView.OnDateClickListener onDateClickListener;

    public MonthListAdapter(List<MonthItem> months, MessageCountCalendarView.OnDateClickListener listener) {
        this.months = months;
        this.onDateClickListener = listener;
    }

    public void updateMonthData(int position, Map<Integer, Integer> dayMessageCount) {
        if (position >= 0 && position < months.size()) {
            months.get(position).dayMessageCount = dayMessageCount;
            notifyItemChanged(position);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        MessageCountCalendarView messageCountCalendarView = new MessageCountCalendarView(parent.getContext());
        RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = (int) (16 * parent.getContext().getResources().getDisplayMetrics().density);
        messageCountCalendarView.setLayoutParams(params);
        return new ViewHolder(messageCountCalendarView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MonthItem item = months.get(position);
        holder.messageCountCalendarView.setOnDateClickListener(onDateClickListener);
        holder.messageCountCalendarView.setMonthData(item.year, item.month, item.dayMessageCount);
    }

    @Override
    public int getItemCount() {
        return months != null ? months.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        MessageCountCalendarView messageCountCalendarView;

        public ViewHolder(@NonNull MessageCountCalendarView itemView) {
            super(itemView);
            messageCountCalendarView = itemView;
        }
    }
}
