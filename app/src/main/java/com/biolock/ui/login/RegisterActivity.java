package com.biolock.ui.login;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.biolock.R;
import com.biolock.model.User;
import com.biolock.repository.Result;
import com.biolock.repository.UserRepository;

public class RegisterActivity extends AppCompatActivity {
    private EditText editTextUsername;
    private EditText editTextEmail;
    private EditText editTextPassword;
    private EditText editTextConfirmPassword;
    private UserRepository userRepository;
    private Button buttonRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initializeComponents();
    }

    private void initializeComponents() {
        initializeViews();

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

    private void initializeViews() {
        editTextUsername = findViewById(R.id.editTextUsername);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);
        buttonRegister = findViewById(R.id.buttonRegister);
        buttonRegister.setEnabled(false);
    }

    private void setupClickListeners() {
        buttonRegister.setOnClickListener(v -> handleRegistration());
    }

    private void handleRegistration() {
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
                            Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }
}