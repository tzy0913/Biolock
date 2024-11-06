package com.biolock.ui.attendance;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.biolock.R;
import com.biolock.ui.attendance.adapter.AttendancePagerAdapter;
import com.biolock.utils.SessionManager;
import com.biolock.model.User;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class ViewAttendanceActivity extends AppCompatActivity {
    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private SessionManager sessionManager;
    public static final String EXTRA_CLASS_ID = "extra_class_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_attendance);

        sessionManager = new SessionManager(this);
        initializeViews();
    }

    private void initializeViews() {
        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);

        String userRole = sessionManager.getUserRole();
        Long classId = getIntent().getLongExtra(EXTRA_CLASS_ID, -1);

        AttendancePagerAdapter pagerAdapter = new AttendancePagerAdapter(this, userRole, classId);
        viewPager.setAdapter(pagerAdapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (User.ROLE_INSTRUCTOR.equals(userRole)) {
                switch (position) {
                    case 0:
                        tab.setText("Current Session");
                        break;
                    case 1:
                        tab.setText("Today");
                        break;
                    case 2:
                        tab.setText("Week");
                        break;
                    case 3:
                        tab.setText("Month");
                        break;
                }
            } else {
                switch (position) {
                    case 0:
                        tab.setText("Today");
                        break;
                    case 1:
                        tab.setText("Week");
                        break;
                    case 2:
                        tab.setText("Month");
                        break;
                }
            }
        }).attach();
    }
}