package com.biolock.ui.attendance.adapter;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.biolock.ui.attendance.fragments.MonthFragment;
import com.biolock.ui.attendance.fragments.TodayFragment;
import com.biolock.ui.attendance.fragments.WeekFragment;

public class AttendancePagerAdapter extends FragmentStateAdapter {
    public AttendancePagerAdapter(FragmentActivity activity) {
        super(activity);
    }

    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new TodayFragment();
            case 1:
                return new WeekFragment();
            case 2:
                return new MonthFragment();
            default:
                return new TodayFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}