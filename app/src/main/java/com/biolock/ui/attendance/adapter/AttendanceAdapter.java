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
        private final SimpleDateFormat dateFormat;

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
            dateFormat = new SimpleDateFormat("MMM d", Locale.getDefault());
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

                // Format date and time
                if (attendance.getStartTime() != null && attendance.getEndTime() != null) {
                    String timeString;
                    if (showDate && attendance.getSessionDate() != null) {
                        timeString = String.format("%s, %s - %s",
                                dateFormat.format(attendance.getSessionDate()),
                                timeFormat.format(attendance.getStartTime()),
                                timeFormat.format(attendance.getEndTime()));
                    } else {
                        timeString = String.format("%s - %s",
                                timeFormat.format(attendance.getStartTime()),
                                timeFormat.format(attendance.getEndTime()));
                    }
                    time.setText(timeString);
                    time.setVisibility(View.VISIBLE);
                } else {
                    time.setVisibility(View.GONE);
                }
            }

            // Set status with color
            setStatusWithColor(status, attendance.getStatus());
        }

        void bindCourseClass(CourseClass courseClass, boolean showDate) {
            // Hide student views
            studentName.setVisibility(View.GONE);
            studentEmail.setVisibility(View.GONE);

            // Show class details
            moduleCode.setVisibility(View.VISIBLE);
            moduleName.setVisibility(View.VISIBLE);
            section.setVisibility(View.VISIBLE);
            status.setVisibility(View.VISIBLE);

            moduleCode.setText(courseClass.getModuleCode());
            moduleName.setText(courseClass.getModuleName());
            section.setText(String.format("Section %s - %s",
                    courseClass.getSection(),
                    courseClass.getRoom()));

            // Format date and time
            if (courseClass.getStartTime() != null && courseClass.getEndTime() != null) {
                String timeString;
                if (showDate && courseClass.getSessionDate() != null) {
                    timeString = String.format("%s, %s - %s",
                            dateFormat.format(courseClass.getSessionDate()),
                            timeFormat.format(courseClass.getStartTime()),
                            timeFormat.format(courseClass.getEndTime()));
                } else {
                    timeString = String.format("%s - %s",
                            timeFormat.format(courseClass.getStartTime()),
                            timeFormat.format(courseClass.getEndTime()));
                }
                time.setText(timeString);
                time.setVisibility(View.VISIBLE);
            } else {
                time.setVisibility(View.GONE);
            }

            // Set status with color
            setStatusWithColor(status, courseClass.getStatus());
        }

        private void setStatusWithColor(TextView statusView, String status) {
            if (status == null) {
                statusView.setVisibility(View.GONE);
                return;
            }

            statusView.setText(status);

            Context context = statusView.getContext();
            int textColor;

            switch (status) {
                case "PRESENT":
                case "COMPLETED":
                    textColor = ContextCompat.getColor(context, android.R.color.holo_green_dark);
                    break;
                case "ONGOING":
                    textColor = ContextCompat.getColor(context, android.R.color.holo_blue_dark);
                    break;
                case "UPCOMING":
                    textColor = ContextCompat.getColor(context, android.R.color.darker_gray);
                    break;
                case "LATE":
                    textColor = ContextCompat.getColor(context, android.R.color.holo_red_dark);
                    break;
                case "ABSENT":
                    textColor = ContextCompat.getColor(context, android.R.color.holo_red_dark);
                    break;
                default:
                    textColor = ContextCompat.getColor(context, android.R.color.darker_gray);
                    break;
            }

            statusView.setTextColor(textColor);
            statusView.setVisibility(View.VISIBLE);
        }
    }
}