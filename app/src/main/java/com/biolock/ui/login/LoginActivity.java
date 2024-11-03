package com.biolock.ui.login;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.biolock.R;
import com.biolock.model.SecuritySettings;
import com.biolock.model.User;
import com.biolock.repository.FaceEmbeddingRepository;
import com.biolock.repository.Result;
import com.biolock.repository.SecuritySettingsRepository;
import com.biolock.repository.UserRepository;
import com.biolock.ui.dashboard.DashboardActivity;
import com.biolock.utils.FaceAuthenticator;
import com.biolock.utils.FacePreprocessor;
import com.biolock.utils.FaceRecognition;
import com.biolock.utils.LivenessDetector;
import com.biolock.utils.SessionManager;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.nio.ByteBuffer;
import java.util.Calendar;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {
    // Constants
    private static final String TAG = "LoginActivity";
    private static final int PERMISSION_REQUEST_CODE = 10;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA};
    private static final long WELCOME_DELAY = 1500; // 1.5 seconds

    // UI Components
    private LinearLayout loginChoiceLayout;
    private LinearLayout manualLoginLayout;
    private ConstraintLayout faceLoginLayout;
    private PreviewView previewView;
    private TextView statusText;
    private EditText editTextEmail;
    private EditText editTextPassword;
    private View overlayView;
    private View welcomeOverlay;
    private TextView welcomeText;

    // Camera and Detection Components
    private FaceDetector faceDetector;
    private ProcessCameraProvider cameraProvider;
    private final ExecutorService cameraExecutor = Executors.newSingleThreadExecutor();

    // Repository and Utility Components
    private UserRepository userRepository;
    private FaceEmbeddingRepository faceEmbeddingRepository;
    private FaceRecognition faceRecognition;
    private FaceAuthenticator faceAuthenticator;
    private LivenessDetector livenessDetector;
    private SessionManager sessionManager;
    private SecuritySettingsRepository securitySettingsRepository;

    // Security and State Management
    private SecuritySettings securitySettings;
    private boolean hasFaceEnrollment = false;
    private boolean livenessCheckPassed = false;
    private boolean isAuthenticationSuccessful = false;
    private Long lastUserId;
    private boolean isAuthenticating = false;
    private int currentAttempt = 0;
    private long lastAuthAttemptTime = 0;
    private static final long AUTH_ATTEMPT_DELAY = 500;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        sessionManager = new SessionManager(this);
        securitySettingsRepository = new SecuritySettingsRepository();

        // Get the lastUserId from sharedPreferences
        lastUserId = sessionManager.getUserId();
        if (lastUserId == -1) {
            lastUserId = null;
        }

        if (sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, DashboardActivity.class));
            finish();
            return;
        }

        initializeComponents();
        loadSecuritySettings();
    }

    private void loadSecuritySettings() {
        if (lastUserId != null) {
            new Thread(() -> {
                Result<SecuritySettings> result = securitySettingsRepository.getSettings(lastUserId);
                if (result.isSuccess()) {
                    securitySettings = result.getData();
                } else {
                    // Use defaults if can't load settings
                    securitySettings = new SecuritySettings();
                    securitySettings.setMaxFailedAttempts(SecuritySettingsRepository.DEFAULT_MAX_ATTEMPTS);
                    securitySettings.setLockoutDurationMins(SecuritySettingsRepository.DEFAULT_LOCKOUT_DURATION);
                }
            }).start();
        }
    }

    private void initializeComponents() {
        initializeViews();

        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("Initializing...");
        progress.setCancelable(false);
        progress.show();

        new Thread(() -> {
            try {
                // Initialize repositories and utilities
                userRepository = new UserRepository();
                faceEmbeddingRepository = new FaceEmbeddingRepository();
                faceRecognition = new FaceRecognition(this);
                faceAuthenticator = new FaceAuthenticator(faceEmbeddingRepository, faceRecognition);
                livenessDetector = new LivenessDetector();

                // Configure face detector options
                FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                        .build();
                faceDetector = FaceDetection.getClient(options);

                // Check face enrollment status
                Result<Boolean> hasFaceResult = faceEmbeddingRepository.hasFaceEmbedding(lastUserId);
                if (hasFaceResult.isSuccess()) {
                    hasFaceEnrollment = hasFaceResult.getData();
                    if (!hasFaceEnrollment) {
                        runOnUiThread(() -> {
                            updateStatus("Face not enrolled. Please enroll face first.");
                            setNormalOverlay();
                        });
                    }
                }

                // Complete initialization on UI thread
                runOnUiThread(() -> {
                    progress.dismiss();
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
        loginChoiceLayout = findViewById(R.id.loginChoiceLayout);
        manualLoginLayout = findViewById(R.id.manualLoginLayout);
        faceLoginLayout = findViewById(R.id.faceLoginLayout);
        previewView = findViewById(R.id.previewView);
        statusText = findViewById(R.id.statusTextView);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        overlayView = findViewById(R.id.overlayView);

        welcomeOverlay = findViewById(R.id.welcomeOverlay);
        welcomeText = findViewById(R.id.welcomeText);
        welcomeOverlay.setVisibility(View.GONE);
    }

    private String getTimeBasedGreeting(String name) {
        Calendar c = Calendar.getInstance();
        int timeOfDay = c.get(Calendar.HOUR_OF_DAY);

        if (timeOfDay >= 0 && timeOfDay < 12) {
            return String.format("Good morning,\n%s", name);
        } else if (timeOfDay >= 12 && timeOfDay < 16) {
            return String.format("Good afternoon,\n%s", name);
        } else if (timeOfDay >= 16 && timeOfDay < 21) {
            return String.format("Good evening,\n%s", name);
        } else {
            return String.format("Good night,\n%s", name);
        }
    }

    private void setupClickListeners() {
        findViewById(R.id.buttonFaceLogin).setOnClickListener(v -> startFaceLogin());

        findViewById(R.id.buttonManualLogin).setOnClickListener(v -> {
            loginChoiceLayout.setVisibility(View.GONE);
            manualLoginLayout.setVisibility(View.VISIBLE);
        });

        findViewById(R.id.buttonLogin).setOnClickListener(v -> handleManualLogin());

        findViewById(R.id.buttonRegister).setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));

        // Back button from manual login
        findViewById(R.id.buttonBackToChoice).setOnClickListener(v -> {
            manualLoginLayout.setVisibility(View.GONE);
            loginChoiceLayout.setVisibility(View.VISIBLE);
        });

        // Back button from face login
        findViewById(R.id.buttonBackToChoiceFromFace).setOnClickListener(v -> {
            faceLoginLayout.setVisibility(View.GONE);
            loginChoiceLayout.setVisibility(View.VISIBLE);
            // Additional cleanup for face login
            if (faceRecognition != null) {
                faceRecognition.close();
            }
            livenessCheckPassed = false;
            livenessDetector.reset();
        });
    }

    // Reset state when starting face login
    private void startFaceLogin() {
        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE);
            return;
        }

        if (lastUserId == null || lastUserId == -1) {
            Toast.makeText(this, "Please login manually first to enable face login",
                    Toast.LENGTH_LONG).show();
            loginChoiceLayout.setVisibility(View.GONE);
            manualLoginLayout.setVisibility(View.VISIBLE);
            return;
        }

        // Check if user is locked out before starting
        new Thread(() -> {
            Result<Boolean> lockedResult = securitySettingsRepository.isUserLocked(lastUserId);
            runOnUiThread(() -> {
                if (lockedResult.isSuccess() && lockedResult.getData()) {
                    Toast.makeText(LoginActivity.this,
                            "Account temporarily locked. Please try again later.",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                // Reset all state flags
                livenessCheckPassed = false;
                isAuthenticating = false;
                isAuthenticationSuccessful = false;
                currentAttempt = 0;

                loginChoiceLayout.setVisibility(View.GONE);
                faceLoginLayout.setVisibility(View.VISIBLE);
                startCamera();
            });
        }).start();
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera", e);
                Toast.makeText(this, "Error starting camera: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases(ProcessCameraProvider cameraProvider) {
        this.cameraProvider = cameraProvider;
        Preview preview = new Preview.Builder().build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, image -> {
            Image mediaImage = image.getImage();
            if (mediaImage != null) {
                InputImage inputImage = InputImage.fromMediaImage(
                        mediaImage,
                        image.getImageInfo().getRotationDegrees()
                );

                // Don't close the image here - moved to after processing
                faceDetector.process(inputImage)
                        .addOnSuccessListener(faces -> {
                            if (faces.isEmpty()) {
                                updateStatus("No face detected");
                                setNormalOverlay();
                                image.close(); // Close if no face detected
                            } else if (faces.size() > 1) {
                                updateStatus("Multiple faces detected");
                                setNormalOverlay();
                                image.close(); // Close if multiple faces
                            } else {
                                Face face = faces.get(0);
                                processLivenessAndAuthentication(face, mediaImage,
                                        image.getImageInfo().getRotationDegrees());
                                image.close(); // Close after processing
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Face detection failed", e);
                            updateStatus("Detection failed");
                            setNormalOverlay();
                            image.close(); // Close on failure
                        });
            } else {
                image.close();
            }
        });

        try {
            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalysis
            );
            preview.setSurfaceProvider(previewView.getSurfaceProvider());
        } catch (Exception e) {
            Log.e(TAG, "Error binding camera uses cases", e);
            Toast.makeText(this, "Error binding camera uses cases",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void processLivenessAndAuthentication(Face face, Image image, int rotation) {
        // Skip processing if authentication was successful
        if (isAuthenticationSuccessful) {
            image.close();
            return;
        }

        // Skip if no face enrollment
        if (!hasFaceEnrollment) {
            updateStatus("Face not enrolled. Please enroll face first.");
            setNormalOverlay();
            return;
        }

        // Check lockout
        if (securitySettings != null) {
            new Thread(() -> {
                Result<Boolean> lockedResult = securitySettingsRepository.isUserLocked(lastUserId);
                if (lockedResult.isSuccess() && lockedResult.getData()) {
                    runOnUiThread(() -> {
                        updateStatus("Account temporarily locked. Please try again later.");
                        setNormalOverlay();
                    });
                    return;
                }
            }).start();
        }

        checkFaceQuality(face);

        if (!livenessCheckPassed) {
            LivenessDetector.LivenessResult result = livenessDetector.processFrame(face);
            updateStatus(result.message);

            if (result.state == LivenessDetector.LivenessState.COMPLETED) {
                livenessCheckPassed = true;
                setScanningOverlay();
                isAuthenticating = true;
            } else {
                setNormalOverlay();
            }
        } else if (isAuthenticating &&
                (securitySettings == null || currentAttempt < securitySettings.getMaxFailedAttempts())) {

            // Check if enough time has passed since last attempt
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastAuthAttemptTime < AUTH_ATTEMPT_DELAY) {
                return;
            }
            lastAuthAttemptTime = currentTime;

            setScanningOverlay();
            Bitmap faceBitmap = FacePreprocessor.extractFace(image, face.getBoundingBox(), rotation);
            if (faceBitmap != null) {
                authenticateUser(faceBitmap);
            }
        }
    }

    private void checkFaceQuality(Face face) {
        float rotY = face.getHeadEulerAngleY();  // Side-to-side rotation
        float rotZ = face.getHeadEulerAngleZ();  // Tilt

        if (Math.abs(rotY) < 15 && Math.abs(rotZ) < 15) {
            setScanningOverlay();
            isAuthenticating = true;  // Enable authentication when face is in position
        } else {
            isAuthenticating = false;  // Disable authentication when face is out of position
            setNormalOverlay();
            StringBuilder guidance = new StringBuilder("Please ");
            if (Math.abs(rotY) >= 15) {
                guidance.append("face the camera directly");
            }
            if (Math.abs(rotZ) >= 15) {
                if (guidance.length() > 7) guidance.append(" and ");
                guidance.append("keep your head level");
            }
            updateStatus(guidance.toString());
        }
    }

    private void authenticateUser(Bitmap faceBitmap) {
        if (lastUserId == null || lastUserId == -1 || isAuthenticationSuccessful) {
            return; // Skip if already authenticated or no user ID
        }

        currentAttempt++;

        new Thread(() -> {
            try {
                float[] newEmbedding = faceRecognition.generateEmbedding(faceBitmap);

                if (faceAuthenticator.authenticate(lastUserId, newEmbedding)) {
                    // Set flag to prevent further authentication attempts
                    isAuthenticationSuccessful = true;

                    // Stop camera immediately after successful authentication
                    runOnUiThread(() -> {
                        if (cameraProvider != null) {
                            cameraProvider.unbindAll();
                        }
                    });

                    // Reset failed attempts on successful login
                    securitySettingsRepository.resetFailedAttempts(lastUserId);

                    Result<User> userResult = userRepository.getUserById(lastUserId);
                    if (userResult.isSuccess()) {
                        handleSuccessfulLogin(userResult.getData());
                        return;
                    }
                } else {
                    // Increment failed attempts in database
                    securitySettingsRepository.incrementFailedAttempts(lastUserId);
                }

                int maxAttempts = securitySettings != null ?
                        securitySettings.getMaxFailedAttempts() :
                        SecuritySettingsRepository.DEFAULT_MAX_ATTEMPTS;

                // Handle failed attempt
                if (currentAttempt >= maxAttempts) {
                    isAuthenticating = false;
                    updateStatus("Max attempts reached. Account temporarily locked.");
                    setNormalOverlay();
                } else {
                    updateStatus(String.format("Authentication failed (Attempt %d/%d). Please try again.",
                            currentAttempt, maxAttempts));
                }

            } catch (Exception e) {
                Log.e(TAG, "Authentication error", e);
                updateStatus("Authentication error: " + e.getMessage());
            }
        }).start();
    }

    private void handleManualLogin() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("Logging in...");
        progress.setCancelable(false);
        progress.show();

        new Thread(() -> {
            try {
                Result<User> result = userRepository.login(email, password);
                runOnUiThread(() -> {
                    progress.dismiss();
                    if (result.isSuccess()) {
                        handleSuccessfulLogin(result.getData());
                    } else {
                        Toast.makeText(LoginActivity.this, result.getError().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progress.dismiss();
                    Toast.makeText(LoginActivity.this,
                            "Login failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void handleSuccessfulLogin(User user) {
        // Ensure we only show the welcome screen once
        if (!isAuthenticationSuccessful) {
            return;
        }

        runOnUiThread(() -> {
            // First fade out the face login views
            View[] viewsToHide = {
                    previewView,
                    overlayView,
                    findViewById(R.id.statusLayout),
                    findViewById(R.id.buttonBackToChoiceFromFace)
            };

            for (View view : viewsToHide) {
                if (view != null) {
                    view.animate()
                            .alpha(0f)
                            .setDuration(200)
                            .start();
                }
            }

            // After camera views fade out, show welcome screen
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                // Hide camera views completely
                for (View view : viewsToHide) {
                    if (view != null) {
                        view.setVisibility(View.GONE);
                    }
                }

                // Show and animate welcome overlay
                welcomeOverlay.setVisibility(View.VISIBLE);
                welcomeOverlay.setAlpha(0f);
                welcomeText.setText(getTimeBasedGreeting(user.getName()));

                welcomeOverlay.animate()
                        .alpha(1f)
                        .setDuration(300)
                        .withEndAction(() -> {
                            // After welcome delay, transition to dashboard
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                sessionManager.createLoginSession(
                                        user.getUserId(),
                                        user.getEmail(),
                                        user.getName(),
                                        user.getRole()
                                );

                                // Start dashboard with fade transition
                                Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
                                startActivity(intent);
                                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                                finish();
                            }, WELCOME_DELAY);
                        })
                        .start();
            }, 200); // Wait for camera views to fade out
        });
    }

    private void updateStatus(String message) {
        runOnUiThread(() -> statusText.setText(message));
    }

    private void setNormalOverlay() {
        runOnUiThread(() -> overlayView.setBackgroundResource(R.drawable.normal_overlay));
    }

    private void setScanningOverlay() {
        runOnUiThread(() -> overlayView.setBackgroundResource(R.drawable.scanning_overlay));
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (allPermissionsGranted()) {
                startFaceLogin();
            } else {
                Toast.makeText(this, "Camera permissions are required",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unbind camera use cases
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }

        // Reset all views' alpha values
        if (previewView != null) previewView.setAlpha(1f);
        if (overlayView != null) overlayView.setAlpha(1f);
        if (findViewById(R.id.statusLayout) != null) findViewById(R.id.statusLayout).setAlpha(1f);
        if (findViewById(R.id.buttonBackToChoiceFromFace) != null) {
            findViewById(R.id.buttonBackToChoiceFromFace).setAlpha(1f);
        }
        if (welcomeOverlay != null) welcomeOverlay.setAlpha(1f);

        // Reset all state flags
        livenessDetector.reset();
        livenessCheckPassed = false;
        isAuthenticating = false;
        isAuthenticationSuccessful = false;
        currentAttempt = 0;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
        if (faceRecognition != null) {
            faceRecognition.close();
        }
        cameraExecutor.shutdown();
    }
}