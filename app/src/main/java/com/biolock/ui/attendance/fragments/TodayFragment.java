package com.biolock.ui.attendance.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import java.util.List;

public class TodayFragment extends Fragment {
    private RecyclerView recyclerView;
    private TextView textNoClasses;
    private AttendanceRepository attendanceRepository;
    private SessionManager sessionManager;
    private AttendanceAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_today, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewToday);
        textNoClasses = view.findViewById(R.id.textNoClasses);

        initializeComponents();
        loadTodayAttendance();

        return view;
    }

    private void initializeComponents() {
        attendanceRepository = new AttendanceRepository();
        sessionManager = new SessionManager(requireContext());

        adapter = new AttendanceAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    private void loadTodayAttendance() {
        new Thread(() -> {
            Result<List<Attendance>> result = attendanceRepository.getTodayAttendance(
                    sessionManager.getUserId()
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