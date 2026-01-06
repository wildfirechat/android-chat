package cn.wildfire.chat.kit.search.bydate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Map;

import cn.wildfire.chat.kit.R;

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
    private CalendarView.OnDateClickListener onDateClickListener;

    public MonthListAdapter(List<MonthItem> months, CalendarView.OnDateClickListener listener) {
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
        CalendarView calendarView = new CalendarView(parent.getContext());
        RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = (int) (16 * parent.getContext().getResources().getDisplayMetrics().density);
        calendarView.setLayoutParams(params);
        return new ViewHolder(calendarView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MonthItem item = months.get(position);
        holder.calendarView.setOnDateClickListener(onDateClickListener);
        holder.calendarView.setMonthData(item.year, item.month, item.dayMessageCount);
    }

    @Override
    public int getItemCount() {
        return months != null ? months.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CalendarView calendarView;

        public ViewHolder(@NonNull CalendarView itemView) {
            super(itemView);
            calendarView = itemView;
        }
    }
}
