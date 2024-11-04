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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MonthFragment extends Fragment {
    private CalendarView calendarView;
    private RecyclerView recyclerView;
    private TextView textNoClasses;
    private TextView textSelectedDate;
    private AttendanceRepository attendanceRepository;
    private SessionManager sessionManager;
    private AttendanceAdapter adapter;
    private LocalDate selectedDate = LocalDate.now();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_month, container, false);

        calendarView = view.findViewById(R.id.calendarView);
        recyclerView = view.findViewById(R.id.recyclerViewClasses);
        textNoClasses = view.findViewById(R.id.textNoClasses);
        textSelectedDate = view.findViewById(R.id.textSelectedDate);

        initializeComponents();
        setupCalendarListener();
        updateSelectedDateText();
        loadDateAttendance();

        return view;
    }

    private void initializeComponents() {
        attendanceRepository = new AttendanceRepository();
        sessionManager = new SessionManager(requireContext());

        adapter = new AttendanceAdapter();
        adapter.setShowDate(true); // Show dates in month view
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupCalendarListener() {
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            selectedDate = LocalDate.of(year, month + 1, dayOfMonth);
            updateSelectedDateText();
            loadDateAttendance();
        });
    }

    private void updateSelectedDateText() {
        textSelectedDate.setText(selectedDate.format(
                DateTimeFormatter.ofPattern("MMMM dd, yyyy"))
        );
    }

    private void loadDateAttendance() {
        new Thread(() -> {
            Result<List<Attendance>> result = attendanceRepository.getAttendanceByDateRange(
                    sessionManager.getUserId(),
                    selectedDate,
                    selectedDate
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