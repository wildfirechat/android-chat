package cn.wildfire.chat.kit.widget;

import android.content.Context;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.aigestudio.wheelpicker.WheelPicker;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import cn.wildfire.chat.kit.R;

public class DateTimePickerHelper {
    public static void pickDateTime(Context context, PickDateTimeCallBack callBack) {
        if (callBack == null) {
            return;
        }
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context);
        View view = LayoutInflater.from(context).inflate(R.layout.date_time_picker, null);

        WheelPicker datePicker = view.findViewById(R.id.datePicker);
        List<String> dates = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());

        for (int i = 0; i < 365; i++) {
            int month = calendar.get(Calendar.MONTH) + 1;
            int day = calendar.get(Calendar.DAY_OF_MONTH);
            calendar.add(Calendar.DAY_OF_YEAR, 1);

            dates.add(month + "月" + day + "日");
        }

        datePicker.setData(dates);

        WheelPicker hourPicker = view.findViewById(R.id.hourPicker);
        List<String> hours = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            hours.add(String.format("%02d", i));
        }
        hourPicker.setData(hours);

        WheelPicker minutePicker = view.findViewById(R.id.minutePicker);
        List<String> minutes = new ArrayList<>();
        minutes.add("00");
        minutes.add("15");
        minutes.add("30");
        minutes.add("45");
        minutePicker.setData(minutes);

        TextView confirmTextView = view.findViewById(R.id.confirmTextView);
        TextView cancelTextView = view.findViewById(R.id.cancelTextView);
        confirmTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int dayP = datePicker.getCurrentItemPosition();
                int hourP = hourPicker.getCurrentItemPosition();
                int minuteP = minutePicker.getCurrentItemPosition();
                Calendar c = Calendar.getInstance();
                c.setTime(new Date());
                c.add(Calendar.DAY_OF_YEAR, dayP);
                c.set(Calendar.HOUR_OF_DAY, hourP);
                c.set(Calendar.MINUTE, minuteP * 15);
                c.set(Calendar.SECOND, 0);
                Date date = c.getTime();
                callBack.onPick(date);
                bottomSheetDialog.hide();
            }
        });
        cancelTextView.setOnClickListener(view1 -> {
            bottomSheetDialog.hide();
            callBack.onCancel();
        });

        bottomSheetDialog.setContentView(view);
        bottomSheetDialog.show();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        new Handler().postDelayed(() -> {
            hourPicker.setSelectedItemPosition(hour);
            minutePicker.setSelectedItemPosition((minute / 15 + 1) % 4);
        }, 100);

    }

    public interface PickDateTimeCallBack {
        void onPick(Date date);

        void onCancel();

    }
}
