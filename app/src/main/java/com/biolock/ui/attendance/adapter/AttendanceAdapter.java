package com.biolock.ui.attendance.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.biolock.R;
import com.biolock.model.Attendance;
import com.biolock.model.CourseClass;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AttendanceAdapter extends RecyclerView.Adapter<AttendanceAdapter.AttendanceViewHolder> {
    private List<?> itemList = new ArrayList<>();
    private boolean showDate = false;
    private boolean isInstructorView = false;

    public void setShowDate(boolean showDate) {
        this.showDate = showDate;
        notifyDataSetChanged();
    }

    public void setInstructorView(boolean isInstructorView) {
        this.isInstructorView = isInstructorView;
        notifyDataSetChanged();
    }

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

    public void setItems(List<?> items) {
        this.itemList = items;
        notifyDataSetChanged();
    }

    static class AttendanceViewHolder extends RecyclerView.ViewHolder {
        private final TextView studentName;
        private final TextView studentEmail;
        private final TextView status;
        private final TextView moduleCode;
        private final TextView moduleName;
        private final TextView section;
        private final TextView time;
        private final SimpleDateFormat timeFormat;

        public AttendanceViewHolder(@NonNull View itemView) {
            super(itemView);
            studentName = itemView.findViewById(R.id.studentName);
            studentEmail = itemView.findViewById(R.id.studentEmail);
            status = itemView.findViewById(R.id.status);
            moduleCode = itemView.findViewById(R.id.moduleCode);
            moduleName = itemView.findViewById(R.id.moduleName);
            section = itemView.findViewById(R.id.section);
            time = itemView.findViewById(R.id.time);
            timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
        }

        void bind(Attendance attendance, boolean showDate, boolean isInstructorView) {
            if (isInstructorView) {
                // Show student details
                studentName.setVisibility(View.VISIBLE);
                studentEmail.setVisibility(View.VISIBLE);
                studentName.setText(attendance.getStudentName());
                studentEmail.setText(attendance.getStudentEmail());

                // Hide class details
                moduleCode.setVisibility(View.GONE);
                moduleName.setVisibility(View.GONE);
                section.setVisibility(View.GONE);
            } else {
                // Hide student details
                studentName.setVisibility(View.GONE);
                studentEmail.setVisibility(View.GONE);

                // Show class details
                moduleCode.setVisibility(View.VISIBLE);
                moduleName.setVisibility(View.VISIBLE);
                section.setVisibility(View.VISIBLE);

                moduleCode.setText(attendance.getModuleCode());
                moduleName.setText(attendance.getModuleName());
                section.setText(String.format("Section %s - %s",
                        attendance.getSection(),
                        attendance.getRoom()));
            }

            // Show time if available
            if (attendance.getStartTime() != null && attendance.getEndTime() != null) {
                String timeText = String.format("%s - %s",
                        timeFormat.format(attendance.getStartTime()),
                        timeFormat.format(attendance.getEndTime()));
                time.setText(timeText);
                time.setVisibility(View.VISIBLE);
            } else {
                time.setVisibility(View.GONE);
            }

            String statusText = attendance.getStatus();
            if (attendance.getTimestamp() != null) {
                statusText += " (" + timeFormat.format(attendance.getTimestamp()) + ")";
            }
            status.setText(statusText);

            Context context = itemView.getContext();
            int textColor;
            switch (attendance.getStatus().toLowerCase()) {
                case "present":
                    textColor = ContextCompat.getColor(context, android.R.color.holo_green_dark);
                    break;
                case "late":
                case "absent":
                    textColor = ContextCompat.getColor(context, android.R.color.holo_red_dark);
                    break;
                default:
                    textColor = ContextCompat.getColor(context, android.R.color.darker_gray);
                    break;
            }
            status.setTextColor(textColor);
        }

        void bindCourseClass(CourseClass courseClass, boolean showDate) {
            // Hide student views
            studentName.setVisibility(View.GONE);
            studentEmail.setVisibility(View.GONE);
            status.setVisibility(View.GONE);

            // Show class details
            moduleCode.setVisibility(View.VISIBLE);
            moduleName.setVisibility(View.VISIBLE);
            section.setVisibility(View.VISIBLE);

            moduleCode.setText(courseClass.getModuleCode());
            moduleName.setText(courseClass.getModuleName());
            section.setText(String.format("Section %s - %s",
                    courseClass.getSection(),
                    courseClass.getRoom()));

            if (courseClass.getStartTime() != null && courseClass.getEndTime() != null) {
                String timeText = String.format("%s - %s",
                        timeFormat.format(courseClass.getStartTime()),
                        timeFormat.format(courseClass.getEndTime()));
                time.setText(timeText);
                time.setVisibility(View.VISIBLE);
            } else {
                time.setVisibility(View.GONE);
            }
        }
    }
}