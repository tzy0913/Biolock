package com.biolock.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.biolock.R;
import com.biolock.database.DatabaseHelper;
import com.biolock.ui.settings.SettingsActivity;
import com.biolock.utils.SessionManager;
import com.biolock.ui.login.LoginActivity;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

// DashboardActivity.java
public class DashboardActivity extends AppCompatActivity {
    private SessionManager sessionManager;
    private TextView textClassInfo;
    private TextView textGreeting;
    private Button buttonMarkAttendance;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        sessionManager = new SessionManager(this);

        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        initializeDatabase();
        initializeViews();
        setupClickListeners();
        updateDashboard();
    }

    private void initializeDatabase() {
        new Thread(() -> {
            try {
                dbHelper = DatabaseHelper.getInstance();
                dbHelper.initialize();
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Database initialization failed", Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        }).start();
    }

    private void initializeViews() {
        textClassInfo = findViewById(R.id.textClassInfo);
        textGreeting = findViewById(R.id.textGreeting);
        buttonMarkAttendance = findViewById(R.id.buttonMarkAttendance);
    }

    private void setupClickListeners() {
        findViewById(R.id.buttonSettings).setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class));
        });

        findViewById(R.id.buttonLogout).setOnClickListener(v -> {
            sessionManager.logout();
            if (dbHelper != null) {
                dbHelper.cleanup();
            }
            startActivity(new Intent(this, LoginActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
            finish();
        });

        buttonMarkAttendance.setOnClickListener(v -> {
            // TODO: Will implement after face authentication is working
        });

        findViewById(R.id.buttonViewAttendance).setOnClickListener(v -> {
            // TODO: Will implement after attendance marking is working
        });
    }

    private void updateDashboard() {
        String userName = sessionManager.getUserName();
        textGreeting.setText(String.format("Hi, %s!", userName));
        checkCurrentClass();
    }

    private void checkCurrentClass() {
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String currentTime = timeFormat.format(new Date());

        // For now, just disable attendance marking until face enrollment is done
        textClassInfo.setText("Please enroll your face in Settings first");
        buttonMarkAttendance.setEnabled(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkCurrentClass();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.cleanup();
        }
    }
}