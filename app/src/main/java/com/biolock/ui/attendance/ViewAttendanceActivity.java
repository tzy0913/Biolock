/**
 * Activity for viewing attendance records with different time period filters.
 * Shows different tabs based on user role (instructor/student).
 */
package com.biolock.ui.attendance;

// Android Core
import android.os.Bundle;

// AndroidX Libraries
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

// Biolock Components
import com.biolock.R;
import com.biolock.model.User;
import com.biolock.ui.attendance.adapter.AttendancePagerAdapter;
import com.biolock.utils.SessionManager;

// Google Material Design
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class ViewAttendanceActivity extends AppCompatActivity {
    // Constants
    public static final String EXTRA_CLASS_ID = "extra_class_id";

    // UI Components
    private ViewPager2 viewPager;
    private TabLayout tabLayout;

    // Dependencies
    private SessionManager sessionManager;

    // Lifecycle Methods
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_attendance);

        sessionManager = new SessionManager(this);
        initializeViews();
    }

    // Initialization Methods
    private void initializeViews() {
        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);

        setupViewPager();
        setupTabLayout();
    }

    private void setupViewPager() {
        String userRole = sessionManager.getUserRole();
        Long classId = getIntent().getLongExtra(EXTRA_CLASS_ID, -1);

        AttendancePagerAdapter pagerAdapter = new AttendancePagerAdapter(this, userRole, classId);
        viewPager.setAdapter(pagerAdapter);
    }

    private void setupTabLayout() {
        String userRole = sessionManager.getUserRole();

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (User.ROLE_INSTRUCTOR.equals(userRole)) {
                setupInstructorTabs(tab, position);
            } else {
                setupStudentTabs(tab, position);
            }
        }).attach();
    }

    private void setupInstructorTabs(TabLayout.Tab tab, int position) {
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
    }

    private void setupStudentTabs(TabLayout.Tab tab, int position) {
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
}