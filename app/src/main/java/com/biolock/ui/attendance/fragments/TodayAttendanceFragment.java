/**
 * Fragment for displaying today's attendance records.
 * Shows different views for instructors and students.
 */
package com.biolock.ui.attendance.fragments;

// Android Core & UI Components
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

// AndroidX Fragment & RecyclerView
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

// Biolock Components
import com.biolock.R;
import com.biolock.model.User;
import com.biolock.repository.AttendanceRepository;
import com.biolock.repository.Result;
import com.biolock.ui.attendance.adapter.AttendanceAdapter;
import com.biolock.utils.SessionManager;

// Java Utilities
import java.util.List;

public class TodayAttendanceFragment extends Fragment {
    // Constants
    private static final String TAG = "TodayAttendanceFragment";
    private static final String ARG_USER_ROLE = "user_role";
    private static final String ARG_CLASS_ID = "class_id";

    // UI Components
    private RecyclerView recyclerView;
    private TextView textNoClasses;

    // Dependencies
    private AttendanceRepository attendanceRepository;
    private SessionManager sessionManager;
    private AttendanceAdapter adapter;

    // State
    private String userRole;
    private Long classId;

    // Factory Method
    public static TodayAttendanceFragment newInstance(String userRole, Long classId) {
        TodayAttendanceFragment fragment = new TodayAttendanceFragment();
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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_today_attendance, container, false);
        recyclerView = view.findViewById(R.id.recyclerViewToday);
        textNoClasses = view.findViewById(R.id.textNoClasses);
        initializeComponents();
        loadTodayAttendance();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTodayAttendance(); // Refresh when returning to fragment
    }

    // Initialization Methods
    private void initializeComponents() {
        attendanceRepository = new AttendanceRepository();
        sessionManager = new SessionManager(requireContext());
        adapter = new AttendanceAdapter();
        adapter.setInstructorView(User.ROLE_INSTRUCTOR.equals(userRole));
        adapter.setShowDate(false);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    // Data Loading Methods
    private void loadTodayAttendance() {
        new Thread(() -> {
            try {
                boolean isInstructor = User.ROLE_INSTRUCTOR.equals(userRole);
                Long id = isInstructor ? sessionManager.getUserId() : sessionManager.getUserId();

                logDebugInfo(id, isInstructor);

                Result<List<?>> result = attendanceRepository.getTodayAttendance(id, isInstructor);
                logQueryResult(result);

                requireActivity().runOnUiThread(() -> {
                    if (result.isSuccess()) {
                        List<?> items = result.getData();
                        updateUI(items);
                    } else {
                        showError(result.getError());
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Exception in loadTodayAttendance", e);
                requireActivity().runOnUiThread(() ->
                        showError("Error loading attendance: " + e.getMessage()));
            }
        }).start();
    }

    // UI Update Methods
    private void updateUI(List<?> items) {
        if (items == null || items.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            textNoClasses.setVisibility(View.VISIBLE);
            textNoClasses.setText("No classes for today");
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

    // Logging Methods
    private void logDebugInfo(Long id, boolean isInstructor) {
        Log.d(TAG, "Loading today's attendance");
        Log.d(TAG, "User Role: " + userRole);
        Log.d(TAG, "User ID: " + id);
        Log.d(TAG, "Is Instructor: " + isInstructor);
    }

    private void logQueryResult(Result<List<?>> result) {
        if (result.isSuccess()) {
            Log.d(TAG, "Query successful");
            List<?> items = result.getData();
            Log.d(TAG, "Items size: " + (items != null ? items.size() : "null"));
            if (items != null && !items.isEmpty()) {
                Log.d(TAG, "First item type: " + items.get(0).getClass().getSimpleName());
            }
        } else {
            Log.e(TAG, "Query failed: " + result.getError());
        }
    }
}