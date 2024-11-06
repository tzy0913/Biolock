package com.biolock.ui.attendance.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.biolock.R;
import com.biolock.model.Attendance;
import com.biolock.repository.AttendanceRepository;
import com.biolock.repository.Result;
import com.biolock.ui.attendance.adapter.AttendanceAdapter;
import com.biolock.utils.SessionManager;
import com.biolock.model.User;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MonthAttendanceFragment extends Fragment {
    private static final String ARG_USER_ROLE = "user_role";
    private static final String ARG_CLASS_ID = "class_id";

    private CalendarView calendarView;
    private RecyclerView recyclerView;
    private TextView textNoClasses;
    private TextView textSelectedDate;
    private AttendanceRepository attendanceRepository;
    private SessionManager sessionManager;
    private AttendanceAdapter adapter;
    private LocalDate selectedDate;
    private String userRole;
    private Long classId;

    public static MonthAttendanceFragment newInstance(String userRole, Long classId) {
        MonthAttendanceFragment fragment = new MonthAttendanceFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USER_ROLE, userRole);
        if (classId != null) {
            args.putLong(ARG_CLASS_ID, classId);
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userRole = getArguments().getString(ARG_USER_ROLE);
            if (getArguments().containsKey(ARG_CLASS_ID)) {
                classId = getArguments().getLong(ARG_CLASS_ID);
            }
        }
        selectedDate = LocalDate.now();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_month_attendance, container, false);
        initializeViews(view);
        setupCalendarListener();
        loadSelectedDateAttendance();
        return view;
    }

    private void initializeViews(View view) {
        calendarView = view.findViewById(R.id.calendarView);
        recyclerView = view.findViewById(R.id.recyclerViewClasses);
        textNoClasses = view.findViewById(R.id.textNoClasses);
        textSelectedDate = view.findViewById(R.id.textSelectedDate);

        attendanceRepository = new AttendanceRepository();
        sessionManager = new SessionManager(requireContext());

        adapter = new AttendanceAdapter();
        adapter.setInstructorView(User.ROLE_INSTRUCTOR.equals(userRole));
        adapter.setShowDate(false); // Single day view doesn't need dates

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        updateSelectedDateText();
    }

    private void setupCalendarListener() {
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            selectedDate = LocalDate.of(year, month + 1, dayOfMonth);
            updateSelectedDateText();
            loadSelectedDateAttendance();
        });
    }

    private void updateSelectedDateText() {
        textSelectedDate.setText(selectedDate.format(
                DateTimeFormatter.ofPattern("MMMM dd, yyyy")));
    }

    private void loadSelectedDateAttendance() {
        new Thread(() -> {
            try {
                boolean isInstructor = User.ROLE_INSTRUCTOR.equals(userRole);
                // Use instructor's ID instead of class ID
                Long id = isInstructor ? sessionManager.getUserId() : sessionManager.getUserId();

                Result<List<?>> result = attendanceRepository.getDateAttendance(id, isInstructor, selectedDate);

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
                        showError("Error loading date attendance: " + e.getMessage()));
            }
        }).start();
    }

    private void updateUI(List<?> items) {
        if (items == null || items.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            textNoClasses.setVisibility(View.VISIBLE);
            textNoClasses.setText("No classes on selected date");
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