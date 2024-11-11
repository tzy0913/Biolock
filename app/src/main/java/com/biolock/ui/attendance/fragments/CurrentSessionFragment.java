/**
 * Fragment for displaying current session attendance data and statistics.
 * Shows real-time attendance status and analytics for instructors.
 */
package com.biolock.ui.attendance.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ProgressBar;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.biolock.R;
import com.biolock.model.Attendance;
import com.biolock.model.CourseClass;
import com.biolock.repository.AttendanceRepository;
import com.biolock.repository.Result;
import com.biolock.ui.attendance.adapter.AttendanceAdapter;

import java.util.Calendar;
import java.util.List;

public class CurrentSessionFragment extends Fragment {
    // Constants
    private static final String TAG = "CurrentSession";
    private static final String ARG_CLASS_ID = "class_id";

    // Core Data
    private Long classId;
    private AttendanceRepository attendanceRepository;
    private AttendanceAdapter adapter;

    // UI Components - Main
    private RecyclerView recyclerView;
    private TextView textNoSession;
    private View cardStats;

    // UI Components - Statistics
    private TextView textTotalStudents;
    private TextView textPresentCount;
    private TextView textLateCount;
    private TextView textNotMarkedCount;
    private ProgressBar progressPresent;
    private ProgressBar progressLate;
    private ProgressBar progressNotMarked;

    // Factory Method
    public static CurrentSessionFragment newInstance(Long classId) {
        CurrentSessionFragment fragment = new CurrentSessionFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_CLASS_ID, classId);
        fragment.setArguments(args);
        return fragment;
    }

    // Lifecycle Methods
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            classId = getArguments().getLong(ARG_CLASS_ID);
            Log.d(TAG, "Initialized with class ID: " + classId);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_current_session, container, false);
        initializeViews(view);
        initializeComponents();
        loadCurrentSessionData();
        return view;
    }

    // Initialization Methods
    private void initializeViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        textNoSession = view.findViewById(R.id.textNoSession);
        cardStats = view.findViewById(R.id.cardStats);

        // Stats views
        textTotalStudents = view.findViewById(R.id.textTotalStudents);
        textPresentCount = view.findViewById(R.id.textPresentCount);
        textLateCount = view.findViewById(R.id.textLateCount);
        textNotMarkedCount = view.findViewById(R.id.textNotMarkedCount);
        progressPresent = view.findViewById(R.id.progressPresent);
        progressLate = view.findViewById(R.id.progressLate);
        progressNotMarked = view.findViewById(R.id.progressNotMarked);
    }

    private void initializeComponents() {
        attendanceRepository = new AttendanceRepository();
        adapter = new AttendanceAdapter();
        adapter.setInstructorView(true);
        adapter.setShowDate(false);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    // Data Loading Methods
    private void loadCurrentSessionData() {
        new Thread(() -> {
            try {
                CourseClass currentClass = CourseClass.getCurrentClass();
                if (currentClass == null) {
                    Log.e(TAG, "No current class set");
                    requireActivity().runOnUiThread(() ->
                            displayError("Error: No class information available"));
                    return;
                }

                Long instructorId = currentClass.getInstructorId();
                Log.d(TAG, "Loading session data for instructor ID: " + instructorId);

                Result<List<?>> result = attendanceRepository.getTodayAttendance(instructorId, true);

                requireActivity().runOnUiThread(() -> {
                    if (result.isSuccess()) {
                        List<?> items = result.getData();
                        Log.d(TAG, "Got items: " + (items != null ? items.size() : "null"));
                        if (items == null || items.isEmpty()) {
                            displayNoSession();
                        } else {
                            CourseClass currentActiveClass = findCurrentSession((List<CourseClass>) items);
                            if (currentActiveClass != null) {
                                loadAttendanceForSession(currentActiveClass);
                            } else {
                                displayNoSession();
                            }
                        }
                    } else {
                        Log.e(TAG, "Error in result: " + result.getError());
                        displayError("Error loading session data: " + result.getError());
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Exception in loadCurrentSessionData", e);
                requireActivity().runOnUiThread(() ->
                        displayError("Error loading session data: " + e.getMessage()));
            }
        }).start();
    }

    private void loadAttendanceForSession(CourseClass currentClass) {
        if (currentClass == null || currentClass.getSessionId() == null) {
            displayError("Invalid session data");
            return;
        }

        new Thread(() -> {
            try {
                Log.d(TAG, "Loading attendance for session: " + currentClass.getSessionId());
                Result<List<Attendance>> result =
                        attendanceRepository.getCurrentSessionAttendance(currentClass.getSessionId());

                requireActivity().runOnUiThread(() -> {
                    if (result.isSuccess()) {
                        displayAttendance(result.getData());
                    } else {
                        displayError("Error loading attendance data: " + result.getError());
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Exception loading attendance", e);
                requireActivity().runOnUiThread(() ->
                        displayError("Error loading attendance: " + e.getMessage()));
            }
        }).start();
    }

    // Session Management Methods
    private CourseClass findCurrentSession(List<CourseClass> classes) {
        Calendar now = Calendar.getInstance();
        Log.d(TAG, "Finding current session at time: " + now.getTime());

        for (CourseClass classObj : classes) {
            Calendar startTime = Calendar.getInstance();
            Calendar endTime = Calendar.getInstance();

            startTime.setTime(classObj.getSessionDate());
            endTime.setTime(classObj.getSessionDate());

            startTime.set(Calendar.HOUR_OF_DAY, classObj.getStartTime().getHours());
            startTime.set(Calendar.MINUTE, classObj.getStartTime().getMinutes());

            endTime.set(Calendar.HOUR_OF_DAY, classObj.getEndTime().getHours());
            endTime.set(Calendar.MINUTE, classObj.getEndTime().getMinutes());
            endTime.add(Calendar.MINUTE, 30); // 30 min buffer

            if (now.after(startTime) && now.before(endTime)) {
                Log.d(TAG, "Found ongoing class");
                return classObj;
            }
        }
        return null;
    }

    // Display Methods
    private void displayAttendance(List<Attendance> attendances) {
        if (attendances == null || attendances.isEmpty()) {
            displayNoSession();
            return;
        }

        // Calculate stats
        int total = attendances.size();
        int present = 0, late = 0;

        for (Attendance a : attendances) {
            String status = a.getStatus();
            if (status != null) {
                switch (status.toLowerCase()) {
                    case "present": present++; break;
                    case "late": late++; break;
                }
            }
        }

        int notMarked = total - (present + late);

        // Calculate percentages
        float presentPercent = (present * 100f) / total;
        float latePercent = (late * 100f) / total;
        float notMarkedPercent = (notMarked * 100f) / total;

        // Update UI
        textTotalStudents.setText(String.format("Total Students: %d", total));
        textPresentCount.setText(String.format("%d (%.1f%%)", present, presentPercent));
        textLateCount.setText(String.format("%d (%.1f%%)", late, latePercent));
        textNotMarkedCount.setText(String.format("%d (%.1f%%)", notMarked, notMarkedPercent));

        progressPresent.setProgress(Math.round(presentPercent));
        progressLate.setProgress(Math.round(latePercent));
        progressNotMarked.setProgress(Math.round(notMarkedPercent));

        cardStats.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.VISIBLE);
        textNoSession.setVisibility(View.GONE);

        adapter.setItems(attendances);
    }

    private void displayNoSession() {
        cardStats.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        textNoSession.setVisibility(View.VISIBLE);
        textNoSession.setText("No active session");
    }

    private void displayError(String message) {
        cardStats.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        textNoSession.setVisibility(View.VISIBLE);
        textNoSession.setText(message);
    }
}
