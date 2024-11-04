package com.biolock.ui.attendance.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class WeekFragment extends Fragment {
    private RecyclerView recyclerView;
    private TextView textNoClasses;
    private TextView textWeekRange;
    private ImageButton buttonPrevWeek;
    private ImageButton buttonNextWeek;
    private AttendanceRepository attendanceRepository;
    private SessionManager sessionManager;
    private AttendanceAdapter adapter;
    private LocalDate weekStartDate = LocalDate.now().with(DayOfWeek.MONDAY);

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_week, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewWeek);
        textNoClasses = view.findViewById(R.id.textNoClasses);
        textWeekRange = view.findViewById(R.id.textWeekRange);
        buttonPrevWeek = view.findViewById(R.id.buttonPrevWeek);
        buttonNextWeek = view.findViewById(R.id.buttonNextWeek);

        initializeComponents();
        setupClickListeners();
        updateWeekRange();
        loadWeekAttendance();

        return view;
    }

    private void initializeComponents() {
        attendanceRepository = new AttendanceRepository();
        sessionManager = new SessionManager(requireContext());

        adapter = new AttendanceAdapter();
        adapter.setShowDate(true); // Show dates in week view
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

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

    private void updateWeekRange() {
        LocalDate weekEndDate = weekStartDate.plusDays(6);
        String range = String.format("%s - %s",
                weekStartDate.format(DateTimeFormatter.ofPattern("MMM dd")),
                weekEndDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
        );
        textWeekRange.setText(range);
    }

    private void loadWeekAttendance() {
        LocalDate weekEndDate = weekStartDate.plusDays(6);

        new Thread(() -> {
            Result<List<Attendance>> result = attendanceRepository.getAttendanceByDateRange(
                    sessionManager.getUserId(),
                    weekStartDate,
                    weekEndDate
            );

            requireActivity().runOnUiThread(() -> {
                if (result.isSuccess()) {
                    List<Attendance> attendanceList = result.getData();
                    if (attendanceList.isEmpty()) {
                        recyclerView.setVisibility(View.GONE);
                        textNoClasses.setVisibility(View.VISIBLE);
                    } else {
                        recyclerView.setVisibility(View.VISIBLE);
                        textNoClasses.setVisibility(View.GONE);
                        adapter.setAttendanceList(attendanceList);
                    }
                } else {
                    recyclerView.setVisibility(View.GONE);
                    textNoClasses.setVisibility(View.VISIBLE);
                    textNoClasses.setText("Error loading classes");
                }
            });
        }).start();
    }
}