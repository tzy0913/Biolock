package com.biolock.ui.login;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.biolock.R;
import com.biolock.database.DatabaseHelper;
import com.biolock.model.User;
import com.biolock.repository.Result;
import com.biolock.repository.UserRepository;

public class RegisterActivity extends AppCompatActivity {
    private EditText editTextUsername;
    private EditText editTextEmail;
    private EditText editTextPassword;
    private EditText editTextConfirmPassword;
    private DatabaseHelper dbHelper;
    private UserRepository userRepository;
    private Button buttonRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initializeViews();
        initializeDatabase();
    }

    private void initializeViews() {
        editTextUsername = findViewById(R.id.editTextUsername);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);
        buttonRegister = findViewById(R.id.buttonRegister);

        // Disable register button until database is initialized
        buttonRegister.setEnabled(false);
    }

    private void initializeDatabase() {
        new Thread(() -> {
            try {
                dbHelper = DatabaseHelper.getInstance();
                dbHelper.initialize();
                userRepository = new UserRepository();

                runOnUiThread(() -> {
                    buttonRegister.setEnabled(true);
                    setupClickListeners();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Database initialization failed", Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        }).start();
    }

    private void setupClickListeners() {
        buttonRegister.setOnClickListener(v -> handleRegistration());
    }

    private void handleRegistration() {
        // Extra null check for safety
        if (userRepository == null) {
            Toast.makeText(this, "Please wait for database initialization", Toast.LENGTH_SHORT).show();
            return;
        }

        String username = editTextUsername.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString();
        String confirmPassword = editTextConfirmPassword.getText().toString();

        // Validation
        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                User newUser = new User();
                newUser.setName(username);
                newUser.setEmail(email);
                newUser.setRole("student");

                Result<Long> result = userRepository.register(newUser, password);

                runOnUiThread(() -> {
                    if (result.isSuccess()) {
                        Toast.makeText(RegisterActivity.this, "Registration successful", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(RegisterActivity.this,
                                "Registration failed: " + result.getError().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(RegisterActivity.this,
                        "Registration failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
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