/**
 * Fragment for displaying weekly attendance records with navigation.
 * Shows attendance data for a selected week with next/previous week navigation.
 */
package com.biolock.ui.attendance.fragments;

// Android Core & UI Components
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

// AndroidX Fragment & RecyclerView
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

// Biolock Models
import com.biolock.R;
import com.biolock.model.User;

// Biolock Repositories
import com.biolock.repository.AttendanceRepository;
import com.biolock.repository.Result;

// Biolock UI Components
import com.biolock.ui.attendance.adapter.AttendanceAdapter;

// Biolock Utilities
import com.biolock.utils.SessionManager;

// Java Time & Util
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class WeekAttendanceFragment extends Fragment {
    // Constants
    private static final String ARG_USER_ROLE = "user_role";
    private static final String ARG_CLASS_ID = "class_id";

    // UI Components
    private RecyclerView recyclerView;
    private TextView textNoClasses;
    private TextView textWeekRange;
    private ImageButton buttonPrevWeek;
    private ImageButton buttonNextWeek;

    // Dependencies
    private AttendanceRepository attendanceRepository;
    private SessionManager sessionManager;
    private AttendanceAdapter adapter;

    // State
    private LocalDate weekStartDate;
    private String userRole;
    private Long classId;

    // Factory Method
    public static WeekAttendanceFragment newInstance(String userRole, Long classId) {
        WeekAttendanceFragment fragment = new WeekAttendanceFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USER_ROLE, userRole);
        if (classId != null) {
            args.putLong(ARG_CLASS_ID, classId);
        }
        fragment.setArguments(args);
        return fragment;
    }

    // Lifecycle Methods
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userRole = getArguments().getString(ARG_USER_ROLE);
            if (getArguments().containsKey(ARG_CLASS_ID)) {
                classId = getArguments().getLong(ARG_CLASS_ID);
            }
        }
        weekStartDate = LocalDate.now().with(DayOfWeek.MONDAY);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_week_attendance, container, false);
        initializeViews(view);
        setupClickListeners();
        loadWeekAttendance();
        return view;
    }

    // Initialization Methods
    private void initializeViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerViewWeek);
        textNoClasses = view.findViewById(R.id.textNoClasses);
        textWeekRange = view.findViewById(R.id.textWeekRange);
        buttonPrevWeek = view.findViewById(R.id.buttonPrevWeek);
        buttonNextWeek = view.findViewById(R.id.buttonNextWeek);

        attendanceRepository = new AttendanceRepository();
        sessionManager = new SessionManager(requireContext());

        adapter = new AttendanceAdapter();
        adapter.setInstructorView(User.ROLE_INSTRUCTOR.equals(userRole));
        adapter.setShowDate(true);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        updateWeekRange();
    }

    // Event Handling Methods
    private void setupClickListeners() {
        buttonPrevWeek.setOnClickListener(v -> {
            weekStartDate = weekStartDate.minusWeeks(1);
            updateWeekRange();
            loadWeekAttendance();
        });

        buttonNextWeek.setOnClickListener(v -> {
            weekStartDate = weekStartDate.plusWeeks(1);
            updateWeekRange();
            loadWeekAttendance();
        });
    }

    // Date Management Methods
    private void updateWeekRange() {
        LocalDate weekEndDate = weekStartDate.plusDays(6);
        String range = String.format("%s - %s",
                weekStartDate.format(DateTimeFormatter.ofPattern("MMM dd")),
                weekEndDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
        textWeekRange.setText(range);
    }

    // Data Loading Methods
    private void loadWeekAttendance() {
        new Thread(() -> {
            try {
                boolean isInstructor = User.ROLE_INSTRUCTOR.equals(userRole);
                Long id = isInstructor ? sessionManager.getUserId() : sessionManager.getUserId();

                LocalDate start = weekStartDate;
                LocalDate end = weekStartDate.plusDays(6);

                Log.d("WeekAttendanceFragment", String.format("Loading week: %s to %s",
                        start.toString(), end.toString()));

                Result<List<?>> result = attendanceRepository.getWeekAttendance(id, isInstructor, start, end);

                requireActivity().runOnUiThread(() -> {
                    if (result.isSuccess()) {
                        List<?> items = result.getData();
                        updateUI(items);
                    } else {
                        showError(result.getError());
                    }
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() ->
                        showError("Error loading week attendance: " + e.getMessage()));
            }
        }).start();
    }

    // UI Update Methods
    private void updateUI(List<?> items) {
        if (items == null || items.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            textNoClasses.setVisibility(View.VISIBLE);
            textNoClasses.setText("No classes this week");
            return;
        }

        recyclerView.setVisibility(View.VISIBLE);
        textNoClasses.setVisibility(View.GONE);
        adapter.setItems(items);
    }

    private void showError(String message) {
        recyclerView.setVisibility(View.GONE);
        textNoClasses.setVisibility(View.VISIBLE);
        textNoClasses.setText(message != null ? message : "Error loading classes");
    }
}