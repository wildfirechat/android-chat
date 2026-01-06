package cn.wildfire.chat.kit.search.bydate;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Calendar;
import java.util.Map;

import cn.wildfire.chat.kit.R;

public class MessageCountCalendarView extends LinearLayout {
    private int year;
    private int month;
    private Map<Integer, Integer> dayMessageCount;
    private OnDateClickListener onDateClickListener;

    public interface OnDateClickListener {
        void onDateClick(int year, int month, int day);
    }

    public MessageCountCalendarView(Context context) {
        super(context);
        init();
    }

    public MessageCountCalendarView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MessageCountCalendarView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setOrientation(VERTICAL);
        setPadding(16, 16, 16, 16);
        setBackgroundColor(Color.WHITE);
    }

    public void setMonthData(int year, int month, Map<Integer, Integer> dayMessageCount) {
        this.year = year;
        this.month = month;
        this.dayMessageCount = dayMessageCount;
        renderCalendar();
    }

    public String getMonthTitle() {
        return String.format("%d年%d月", year, month);
    }

    public void setOnDateClickListener(OnDateClickListener listener) {
        this.onDateClickListener = listener;
    }

    private void renderCalendar() {
        removeAllViews();

        // 月份标题
        TextView titleView = new TextView(getContext());
        titleView.setText(getMonthTitle());
        titleView.setTextSize(18);
        titleView.setTextColor(Color.BLACK);
        titleView.setGravity(Gravity.CENTER);
        titleView.setPadding(0, 0, 0, 24);
        addView(titleView);

        // 星期标题
        GridLayout weekHeader = getGridLayout();
        addView(weekHeader);

        // 日期网格
        GridLayout dateGrid = new GridLayout(getContext());
        dateGrid.setColumnCount(7);

        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month - 1, 1);
        int firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        // 空白填充
        for (int i = 0; i < firstDayOfWeek; i++) {
            View emptyView = new View(getContext());
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = (int) (40 * getResources().getDisplayMetrics().density);
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            dateGrid.addView(emptyView, params);
        }

        // 日期
        Calendar today = Calendar.getInstance();
        for (int day = 1; day <= daysInMonth; day++) {
            TextView dayView = new TextView(getContext());
            dayView.setText(String.valueOf(day));
            dayView.setTextSize(14);
            dayView.setGravity(Gravity.CENTER);

            int size = (int) (40 * getResources().getDisplayMetrics().density);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = size;
            params.height = size;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setGravity(Gravity.CENTER);
            params.setMargins(4, 4, 4, 4);
            dayView.setLayoutParams(params);

            // 检查是否是今天
            boolean isToday = (today.get(Calendar.YEAR) == year
                    && today.get(Calendar.MONTH) == month - 1
                    && today.get(Calendar.DAY_OF_MONTH) == day);

            // 检查是否有消息
            int messageCount = dayMessageCount != null ? dayMessageCount.getOrDefault(day, 0) : 0;
            boolean hasMessage = messageCount > 0;

            // 设置样式
            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            drawable.setColor(Color.TRANSPARENT);

            if (isToday) {
                int primaryColor = getResources().getColor(R.color.colorPrimary);
                drawable.setStroke(2, primaryColor);
                dayView.setTextColor(Color.BLACK);
            } else if (hasMessage) {
                dayView.setTextColor(Color.BLACK);
            } else {
                dayView.setTextColor(Color.LTGRAY);
            }

            dayView.setBackground(drawable);

            if (hasMessage) {
                final int finalDay = day;
                dayView.setOnClickListener(v -> {
                    if (onDateClickListener != null) {
                        onDateClickListener.onDateClick(year, month, finalDay);
                    }
                });
            } else {
                dayView.setClickable(false);
                dayView.setEnabled(false);
            }

            dateGrid.addView(dayView, params);
        }

        addView(dateGrid);
    }

    @NonNull
    private GridLayout getGridLayout() {
        GridLayout weekHeader = new GridLayout(getContext());
        weekHeader.setColumnCount(7);
        weekHeader.setPadding(0, 0, 0, 8);
        String[] weeks = {"日", "一", "二", "三", "四", "五", "六"};
        for (String week : weeks) {
            TextView weekView = new TextView(getContext());
            weekView.setText(week);
            weekView.setTextSize(12);
            weekView.setGravity(Gravity.CENTER);
            weekView.setTextColor(Color.GRAY);
            LayoutParams params = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
            weekView.setLayoutParams(params);
            weekHeader.addView(weekView);
        }
        return weekHeader;
    }
}
