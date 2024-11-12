/**
 * RecyclerView adapter for displaying attendance records.
 * Handles both student attendance and course class views with different layouts.
 */
package com.biolock.ui.attendance.adapter;

// Android Core & UI Components
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

// AndroidX Components
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

// Biolock Components
import com.biolock.R;
import com.biolock.model.Attendance;
import com.biolock.model.CourseClass;

// Java Utilities
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AttendanceAdapter extends RecyclerView.Adapter<AttendanceAdapter.AttendanceViewHolder> {
    // State
    private List<?> itemList = new ArrayList<>();
    private boolean showDate = false;
    private boolean isInstructorView = false;

    // Public Methods
    public void setShowDate(boolean showDate) {
        this.showDate = showDate;
        notifyDataSetChanged();
    }

    public void setInstructorView(boolean isInstructorView) {
        this.isInstructorView = isInstructorView;
        notifyDataSetChanged();
    }

    public void setItems(List<?> items) {
        this.itemList = items;
        notifyDataSetChanged();
    }

    // RecyclerView.Adapter Methods
    @NonNull
    @Override
    public AttendanceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_attendance, parent, false);
        return new AttendanceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AttendanceViewHolder holder, int position) {
        Object item = itemList.get(position);
        if (item instanceof Attendance) {
            holder.bind((Attendance) item, showDate, isInstructorView);
        } else if (item instanceof CourseClass) {
            holder.bindCourseClass((CourseClass) item, showDate);
        }
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    // ViewHolder Class
    static class AttendanceViewHolder extends RecyclerView.ViewHolder {
        // UI Components
        private final TextView studentName;
        private final TextView studentEmail;
        private final TextView status;
        private final TextView moduleCode;
        private final TextView moduleName;
        private final TextView section;
        private final TextView time;

        // Date Formatters
        private final SimpleDateFormat timeFormat;
        private final SimpleDateFormat dateFormat;

        public AttendanceViewHolder(@NonNull View itemView) {
            super(itemView);
            // Initialize all UI Components in constructor
            studentName = itemView.findViewById(R.id.studentName);
            studentEmail = itemView.findViewById(R.id.studentEmail);
            status = itemView.findViewById(R.id.status);
            moduleCode = itemView.findViewById(R.id.moduleCode);
            moduleName = itemView.findViewById(R.id.moduleName);
            section = itemView.findViewById(R.id.section);
            time = itemView.findViewById(R.id.time);

            // Initialize Date Formatters in constructor
            timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
            dateFormat = new SimpleDateFormat("MMM d", Locale.getDefault());
        }

        // Binding Methods
        void bind(Attendance attendance, boolean showDate, boolean isInstructorView) {
            if (isInstructorView) {
                bindInstructorView(attendance);
            } else {
                bindStudentView(attendance, showDate);
            }
            setStatusWithColor(status, attendance.getStatus());
        }

        void bindCourseClass(CourseClass courseClass, boolean showDate) {
            setStudentViewsVisibility(false);
            setClassViewsVisibility(true);
            bindClassDetails(courseClass);
            bindDateTime(courseClass.getSessionDate(), courseClass.getStartTime(),
                    courseClass.getEndTime(), showDate);
            setStatusWithColor(status, courseClass.getStatus());
        }

        // Helper Methods
        private void bindInstructorView(Attendance attendance) {
            setStudentViewsVisibility(true);
            setClassViewsVisibility(false);
            studentName.setText(attendance.getStudentName());
            studentEmail.setText(attendance.getStudentEmail());
        }

        private void bindStudentView(Attendance attendance, boolean showDate) {
            setStudentViewsVisibility(false);
            setClassViewsVisibility(true);
            bindClassDetails(attendance);
            bindDateTime(attendance.getSessionDate(), attendance.getStartTime(),
                    attendance.getEndTime(), showDate);
        }

        private void bindClassDetails(Attendance attendance) {
            moduleCode.setText(attendance.getModuleCode());
            moduleName.setText(attendance.getModuleName());
            section.setText(String.format("Section %s - %s",
                    attendance.getSection(),
                    attendance.getRoom()));
        }

        private void bindClassDetails(CourseClass courseClass) {
            moduleCode.setText(courseClass.getModuleCode());
            moduleName.setText(courseClass.getModuleName());
            section.setText(String.format("Section %s - %s",
                    courseClass.getSection(),
                    courseClass.getRoom()));
        }

        private void bindDateTime(java.util.Date sessionDate, java.util.Date startTime,
                                  java.util.Date endTime, boolean showDate) {
            if (startTime != null && endTime != null) {
                String timeString;
                if (showDate && sessionDate != null) {
                    timeString = String.format("%s, %s - %s",
                            dateFormat.format(sessionDate),
                            timeFormat.format(startTime),
                            timeFormat.format(endTime));
                } else {
                    timeString = String.format("%s - %s",
                            timeFormat.format(startTime),
                            timeFormat.format(endTime));
                }
                time.setText(timeString);
                time.setVisibility(View.VISIBLE);
            } else {
                time.setVisibility(View.GONE);
            }
        }

        private void setStudentViewsVisibility(boolean visible) {
            int visibility = visible ? View.VISIBLE : View.GONE;
            studentName.setVisibility(visibility);
            studentEmail.setVisibility(visibility);
        }

        private void setClassViewsVisibility(boolean visible) {
            int visibility = visible ? View.VISIBLE : View.GONE;
            moduleCode.setVisibility(visibility);
            moduleName.setVisibility(visibility);
            section.setVisibility(visibility);
            status.setVisibility(visibility);
        }

        private void setStatusWithColor(TextView statusView, String status) {
            if (status == null) {
                statusView.setVisibility(View.GONE);
                return;
            }

            statusView.setText(status);
            int textColor = getStatusColor(statusView.getContext(), status);
            statusView.setTextColor(textColor);
            statusView.setVisibility(View.VISIBLE);
        }

        private int getStatusColor(Context context, String status) {
            switch (status) {
                case "PRESENT":
                case "COMPLETED":
                    return ContextCompat.getColor(context, android.R.color.holo_green_dark);
                case "ONGOING":
                    return ContextCompat.getColor(context, android.R.color.holo_blue_dark);
                case "UPCOMING":
                    return ContextCompat.getColor(context, android.R.color.darker_gray);
                case "LATE":
                case "ABSENT":
                    return ContextCompat.getColor(context, android.R.color.holo_red_dark);
                default:
                    return ContextCompat.getColor(context, android.R.color.darker_gray);
            }
        }
    }
}