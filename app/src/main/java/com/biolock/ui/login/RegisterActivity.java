/**
 * Activity handling user registration with input validation and error handling.
 * Manages user input, validation, and registration process.
 */
package com.biolock.ui.login;

// Android Core & Support Libraries
import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

// AndroidX Libraries
import androidx.appcompat.app.AppCompatActivity;

// Biolock Models & Repositories
import com.biolock.R;
import com.biolock.model.User;
import com.biolock.repository.Result;
import com.biolock.repository.UserRepository;

// Java Utilities
import java.util.regex.Pattern;

public class RegisterActivity extends AppCompatActivity {
    // Constants for validation
    private static final String TAG = "RegisterActivity";
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$");

    // UI Components
    private EditText editTextUsername;
    private EditText editTextEmail;
    private EditText editTextPassword;
    private EditText editTextConfirmPassword;
    private Button buttonRegister;

    // Dependencies
    private UserRepository userRepository;

    // Lifecycle Methods
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        initializeComponents();
    }

    // Initialization Methods
    private void initializeComponents() {
        initializeViews();
        initializeRepository();
    }

    private void initializeViews() {
        editTextUsername = findViewById(R.id.editTextUsername);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);
        buttonRegister = findViewById(R.id.buttonRegister);
        buttonRegister.setEnabled(false);
    }

    private void initializeRepository() {
        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("Initializing...");
        progress.setCancelable(false);
        progress.show();

        new Thread(() -> {
            try {
                userRepository = new UserRepository();
                runOnUiThread(() -> {
                    progress.dismiss();
                    buttonRegister.setEnabled(true);
                    setupClickListeners();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progress.dismiss();
                    Toast.makeText(this, "Initialization failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        }).start();
    }

    private void setupClickListeners() {
        buttonRegister.setOnClickListener(v -> handleRegistration());
    }

    // Input Validation Methods
    private boolean validateInputs(String username, String email, String password, String confirmPassword) {
        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (password.length() < MIN_PASSWORD_LENGTH) {
            Toast.makeText(this, "Password must be at least 8 characters long", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            Toast.makeText(this, "Password must contain at least one digit, lowercase, uppercase, and special character",
                    Toast.LENGTH_LONG).show();
            return false;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    // Registration Process Methods
    private void handleRegistration() {
        String username = editTextUsername.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString();
        String confirmPassword = editTextConfirmPassword.getText().toString();

        if (!validateInputs(username, email, password, confirmPassword)) {
            return;
        }

        performRegistration(username, email, password);
    }

    private void performRegistration(String username, String email, String password) {
        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("Registering...");
        progress.setCancelable(false);
        progress.show();

        new Thread(() -> {
            Result<User> result = userRepository.register(username, email, password);

            runOnUiThread(() -> {
                progress.dismiss();
                if (result.isSuccess()) {
                    Toast.makeText(RegisterActivity.this,
                            "Registration successful", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(RegisterActivity.this,
                            "Registration failed: " + result.getError(),
                            Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }
}