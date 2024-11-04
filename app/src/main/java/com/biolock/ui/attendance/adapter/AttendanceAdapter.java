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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class AttendanceAdapter extends RecyclerView.Adapter<AttendanceAdapter.AttendanceViewHolder> {
    private List<Attendance> attendanceList = new ArrayList<>();
    private boolean showDate = false; // Toggle for date visibility (week/month view)

    public void setShowDate(boolean showDate) {
        this.showDate = showDate;
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
        Attendance attendance = attendanceList.get(position);
        holder.bind(attendance, showDate);
    }

    @Override
    public int getItemCount() {
        return attendanceList.size();
    }

    public void setAttendanceList(List<Attendance> attendanceList) {
        this.attendanceList = attendanceList;
        notifyDataSetChanged();
    }

    static class AttendanceViewHolder extends RecyclerView.ViewHolder {
        private final TextView textDate;
        private final TextView textModuleCode;
        private final TextView textModuleName;
        private final TextView textSection;
        private final TextView textTime;
        private final TextView textStatus;

        public AttendanceViewHolder(@NonNull View itemView) {
            super(itemView);
            textDate = itemView.findViewById(R.id.textDate);
            textModuleCode = itemView.findViewById(R.id.textModuleCode);
            textModuleName = itemView.findViewById(R.id.textModuleName);
            textSection = itemView.findViewById(R.id.textSection);
            textTime = itemView.findViewById(R.id.textTime);
            textStatus = itemView.findViewById(R.id.textStatus);
        }

        public void bind(Attendance attendance, boolean showDate) {
            if (showDate) {
                textDate.setVisibility(View.VISIBLE);
                textDate.setText(attendance.getSessionDate().format(
                        DateTimeFormatter.ofPattern("EEEE, MMM dd")
                ));
            } else {
                textDate.setVisibility(View.GONE);
            }

            textModuleCode.setText(attendance.getModuleCode());
            textModuleName.setText(attendance.getModuleName());
            textSection.setText(String.format("Section %s - %s",
                    attendance.getSection(),
                    attendance.getRoom()));

            String timeText = String.format("%s - %s",
                    attendance.getStartTime().format(DateTimeFormatter.ofPattern("h:mm a")),
                    attendance.getEndTime().format(DateTimeFormatter.ofPattern("h:mm a")));
            textTime.setText(timeText);

            // Set status with color
            String status = attendance.getStatus().toUpperCase();
            textStatus.setText(status);

            Context context = itemView.getContext();
            int textColor;
            switch (status.toLowerCase()) {
                case "late":
                    textColor = ContextCompat.getColor(context, android.R.color.holo_red_dark);
                    break;
                case "present":
                    textColor = ContextCompat.getColor(context, android.R.color.holo_green_dark);
                    break;
                case "absent":
                    textColor = ContextCompat.getColor(context, android.R.color.holo_red_dark);
                    break;
                case "not marked":
                default:
                    textColor = ContextCompat.getColor(context, android.R.color.darker_gray);
                    break;
            }
            textStatus.setTextColor(textColor);

            // Optional: Add background tint based on status
            int bgColor;
            switch (status.toLowerCase()) {
                case "late":
                case "absent":
                    bgColor = ContextCompat.getColor(context, android.R.color.holo_red_light);
                    break;
                case "present":
                    bgColor = ContextCompat.getColor(context, android.R.color.holo_green_light);
                    break;
                default:
                    bgColor = ContextCompat.getColor(context, android.R.color.white);
                    break;
            }
            itemView.setBackgroundColor(Color.argb(20, Color.red(bgColor),
                    Color.green(bgColor), Color.blue(bgColor)));
        }
    }
}