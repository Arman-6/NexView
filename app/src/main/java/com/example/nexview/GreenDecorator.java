package com.example.nexview;

import android.graphics.Color;
import android.text.style.ForegroundColorSpan;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import java.util.HashSet;
import java.util.Set;

public class GreenDecorator implements DayViewDecorator {
    private final Set<CalendarDay> dates = new HashSet<>();

    public void addDate(CalendarDay date) {
        dates.add(date);
    }

    @Override
    public boolean shouldDecorate(CalendarDay day) {
        return dates.contains(day);
    }

    @Override
    public void decorate(DayViewFacade view) {
        view.addSpan(new ForegroundColorSpan(Color.RED)); // Green for goal met
    }
}
