package com.biolock.ui.attendance.adapter;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.biolock.ui.attendance.fragments.CurrentSessionFragment;
import com.biolock.ui.attendance.fragments.TodayAttendanceFragment;
import com.biolock.ui.attendance.fragments.WeekAttendanceFragment;
import com.biolock.ui.attendance.fragments.MonthAttendanceFragment;
import com.biolock.model.User;

public class AttendancePagerAdapter extends FragmentStateAdapter {
    private final String userRole;
    private final Long classId;

    public AttendancePagerAdapter(FragmentActivity fragmentActivity, String userRole, Long classId) {
        super(fragmentActivity);
        this.userRole = userRole;
        this.classId = classId;
    }

    @Override
    public int getItemCount() {
        return User.ROLE_INSTRUCTOR.equals(userRole) ? 4 : 3;
    }

    @Override
    public Fragment createFragment(int position) {
        if (User.ROLE_INSTRUCTOR.equals(userRole)) {
            switch (position) {
                case 0:
                    return CurrentSessionFragment.newInstance(classId);
                case 1:
                    return TodayAttendanceFragment.newInstance(userRole, classId);
                case 2:
                    return WeekAttendanceFragment.newInstance(userRole, classId);
                case 3:
                    return MonthAttendanceFragment.newInstance(userRole, classId);
                default:
                    throw new IllegalStateException("Unexpected position " + position);
            }
        } else {
            switch (position) {
                case 0:
                    return TodayAttendanceFragment.newInstance(userRole, null);
                case 1:
                    return WeekAttendanceFragment.newInstance(userRole, null);
                case 2:
                    return MonthAttendanceFragment.newInstance(userRole, null);
                default:
                    throw new IllegalStateException("Unexpected position " + position);
            }
        }
    }
}