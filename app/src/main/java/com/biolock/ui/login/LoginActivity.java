package com.biolock.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.biolock.R;
import com.biolock.database.DatabaseHelper;
import com.biolock.model.User;
import com.biolock.repository.Result;
import com.biolock.repository.UserRepository;
import com.biolock.ui.dashboard.DashboardActivity;
import com.biolock.utils.SessionManager;

public class LoginActivity extends AppCompatActivity {
    private LinearLayout loginChoiceLayout;
    private LinearLayout manualLoginLayout;
    private ConstraintLayout faceLoginLayout;
    private EditText editTextEmail;
    private EditText editTextPassword;
    private DatabaseHelper dbHelper;
    private UserRepository userRepository;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        sessionManager = new SessionManager(this);

        if (sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, DashboardActivity.class));
            finish();
            return;
        }

        initializeDatabase();
        initializeViews();
        setupClickListeners();
    }

    private void initializeDatabase() {
        new Thread(() -> {
            try {
                dbHelper = DatabaseHelper.getInstance();
                dbHelper.initialize();
                userRepository = new UserRepository();
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Connection failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        }).start();
    }

    private void initializeViews() {
        loginChoiceLayout = findViewById(R.id.loginChoiceLayout);
        manualLoginLayout = findViewById(R.id.manualLoginLayout);
        faceLoginLayout = findViewById(R.id.faceLoginLayout);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
    }

    private void setupClickListeners() {
        findViewById(R.id.buttonFaceLogin).setOnClickListener(v ->
                Toast.makeText(this, "Face login not implemented yet", Toast.LENGTH_SHORT).show());

        findViewById(R.id.buttonManualLogin).setOnClickListener(v -> {
            loginChoiceLayout.setVisibility(View.GONE);
            manualLoginLayout.setVisibility(View.VISIBLE);
        });

        findViewById(R.id.buttonLogin).setOnClickListener(v -> handleLogin());

        findViewById(R.id.buttonRegister).setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));

        findViewById(R.id.buttonBackToChoice).setOnClickListener(v -> {
            manualLoginLayout.setVisibility(View.GONE);
            loginChoiceLayout.setVisibility(View.VISIBLE);
        });
    }

    private void handleLogin() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                Result<User> result = userRepository.login(email, password);
                runOnUiThread(() -> {
                    if (result.isSuccess()) {
                        User user = result.getData();
                        sessionManager.createLoginSession(
                                user.getUserId(),
                                user.getEmail(),
                                user.getName(),
                                user.getRole()
                        );
                        startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this, result.getError().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(LoginActivity.this,
                        "Login failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.cleanup();
        }
    }
}