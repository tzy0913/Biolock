package com.biolock.repository;

import android.util.Log;
import com.biolock.database.DatabaseHelper;
import com.biolock.database.dao.AttendanceDao;
import com.biolock.model.Attendance;
import com.biolock.model.Session;
import com.biolock.model.Class;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AttendanceRepository {
    private static final String TAG = "AttendanceRepository";
    private final AttendanceDao attendanceDao;
    private final SessionRepository sessionRepository;
    private final ClassRepository classRepository;
    private final DatabaseHelper dbHelper;

    public AttendanceRepository() {
        this.dbHelper = DatabaseHelper.getInstance();
        this.attendanceDao = new AttendanceDao();
        this.sessionRepository = new SessionRepository();
        this.classRepository = new ClassRepository();
    }

    private void ensureInitialized() throws SQLException {
        if (!dbHelper.isInitialized()) {
            Log.d(TAG, "Initializing database for attendance operations");
            dbHelper.initialize();
        }
    }

    public Result<Long> markAttendance(long userId, int sessionId) {
        try {
            ensureInitialized();
            Log.d(TAG, "Marking attendance for user: " + userId + " session: " + sessionId);

            Attendance attendance = new Attendance();
            attendance.setUserId(userId);
            attendance.setSessionId(sessionId);
            attendance.setTimestamp(LocalTime.now());
            attendance.setStatus("pending"); // Status will be set by database trigger

            long attendanceId = attendanceDao.insert(attendance);
            Log.d(TAG, "Successfully marked attendance with ID: " + attendanceId);

            return Result.success(attendanceId);
        } catch (SQLException e) {
            Log.e(TAG, "Database error marking attendance", e);
            return Result.error(e);
        }
    }

    public Result<List<Attendance>> getTodayAttendance(long userId) {
        LocalDate today = LocalDate.now();
        return getAttendanceByDateRange(userId, today, today);
    }

    public Result<List<Attendance>> getAttendanceByDateRange(long userId, LocalDate startDate, LocalDate endDate) {
        try {
            ensureInitialized();
            Log.d(TAG, "Getting sessions from " + startDate + " to " + endDate + " for user: " + userId);

            // First get sessions within date range
            Result<List<Session>> sessionsResult = sessionRepository.getSessionsByDateRange(startDate, endDate);
            if (!sessionsResult.isSuccess()) {
                return Result.error(new Exception("Failed to fetch sessions"));
            }

            // Get all attendance records for this user
            List<Attendance> attendanceList = attendanceDao.getByUserId(userId);
            Map<Integer, Attendance> attendanceMap = attendanceList.stream()
                    .collect(Collectors.toMap(Attendance::getSessionId, a -> a));

            // Create attendance objects for all sessions
            List<Attendance> rangeAttendance = new ArrayList<>();
            for (Session session : sessionsResult.getData()) {
                Result<Class> classResult = classRepository.getClassById(session.getClassId());
                if (!classResult.isSuccess()) continue;
                Class classData = classResult.getData();

                // Create or get attendance record
                Attendance attendance;
                if (attendanceMap.containsKey(session.getSessionId())) {
                    // Use existing attendance record
                    attendance = attendanceMap.get(session.getSessionId());
                } else {
                    // Create new attendance object for session without record
                    attendance = new Attendance();
                    attendance.setUserId(userId);
                    attendance.setSessionId(session.getSessionId());
                    attendance.setStatus("Not Marked");
                }

                // Set additional info from session and class
                attendance.setSessionDate(session.getDate());
                attendance.setStartTime(session.getStartTime());
                attendance.setEndTime(session.getEndTime());
                attendance.setModuleCode(classData.getModuleCode());
                attendance.setModuleName(classData.getModuleName());
                attendance.setSection(classData.getSection());
                attendance.setRoom(classData.getRoom());

                rangeAttendance.add(attendance);
            }

            // Sort by date and time
            rangeAttendance.sort((a1, a2) -> {
                int dateCompare = a2.getSessionDate().compareTo(a1.getSessionDate()); // Latest first
                if (dateCompare == 0) {
                    return a2.getStartTime().compareTo(a1.getStartTime()); // Latest time first
                }
                return dateCompare;
            });

            Log.d(TAG, "Found " + rangeAttendance.size() + " sessions/attendance records");
            return Result.success(rangeAttendance);
        } catch (SQLException e) {
            Log.e(TAG, "Database error getting attendance by date range", e);
            return Result.error(e);
        }
    }
}