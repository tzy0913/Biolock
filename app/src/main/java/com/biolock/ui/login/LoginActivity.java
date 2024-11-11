/**
 * Activity handling user authentication through both manual and face login.
 * Manages camera preview, face detection, liveness checks, and login flow.
 */
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
import com.biolock.repository.FaceAuthenticationRepository;
import com.biolock.repository.Result;
import com.biolock.repository.UserRepository;
import com.biolock.ui.dashboard.DashboardActivity;
import com.biolock.utils.FacePreprocessor;
import com.biolock.utils.LivenessDetector;
import com.biolock.utils.SessionManager;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import java.util.Calendar;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {
    // region Constants
    private static final String TAG = "LoginActivity";
    private static final int PERMISSION_REQUEST_CODE = 10;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA};
    private static final long WELCOME_DELAY = 1500;

    // UI Components
    private EditText editTextEmail;
    private EditText editTextPassword;
    private View welcomeOverlay;
    private TextView welcomeText;
    private View loginChoiceLayout;
    private View manualLoginLayout;
    private ConstraintLayout faceLoginLayout;
    private PreviewView previewView;
    private TextView statusText;
    private View overlayView;

    // Dependencies
    private UserRepository userRepository;
    private FaceAuthenticationRepository faceAuthenticationRepository;
    private SecuritySettings securitySettings;
    private SessionManager sessionManager;
    private LivenessDetector livenessDetector;

    // Camera Components
    private FaceDetector faceDetector;
    private ProcessCameraProvider cameraProvider;
    private final ExecutorService cameraExecutor = Executors.newSingleThreadExecutor();

    // State Management
    private boolean livenessCheckPassed = false;
    private boolean isAuthenticationSuccessful = false;
    private boolean isAuthenticating = false;
    private boolean isAuthInProgress = false;
    private int currentAttempt = 0;

    // Lifecycle Methods
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        sessionManager = new SessionManager(this);
        Long currentUserId = sessionManager.getLastUserId();
        Log.d(TAG, "onCreate - Current User ID: " + currentUserId);

        if (sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, DashboardActivity.class));
            finish();
            return;
        }

        initializeComponents();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }

        // Reset views' alpha values
        if (previewView != null) previewView.setAlpha(1f);
        if (overlayView != null) overlayView.setAlpha(1f);
        if (findViewById(R.id.statusLayout) != null) findViewById(R.id.statusLayout).setAlpha(1f);
        if (findViewById(R.id.buttonBackToChoiceFromFace) != null) {
            findViewById(R.id.buttonBackToChoiceFromFace).setAlpha(1f);
        }
        if (welcomeOverlay != null) welcomeOverlay.setAlpha(1f);

        resetFaceAuthState();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
        cameraExecutor.shutdown();
    }

    // Initialization Methods
    private void initializeComponents() {
        initializeViews();

        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("Initializing...");
        progress.setCancelable(false);
        progress.show();

        new Thread(() -> {
            try {
                userRepository = new UserRepository();
                faceAuthenticationRepository = new FaceAuthenticationRepository(this);
                livenessDetector = new LivenessDetector();

                // Initialize ML Kit face detector
                FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                        .build();
                faceDetector = FaceDetection.getClient(options);

                runOnUiThread(() -> {
                    progress.dismiss();
                    setupClickListeners();

                    // Check if there's a last logged in user
                    Long lastUserId = sessionManager.getLastUserId();
                    if (lastUserId == null || lastUserId == -1) {
                        loginChoiceLayout.setVisibility(View.GONE);
                        manualLoginLayout.setVisibility(View.VISIBLE);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Initialization failed", e);
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
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        welcomeOverlay = findViewById(R.id.welcomeOverlay);
        welcomeText = findViewById(R.id.welcomeText);
        previewView = findViewById(R.id.previewView);
        statusText = findViewById(R.id.statusTextView);
        overlayView = findViewById(R.id.overlayView);
        welcomeOverlay.setVisibility(View.GONE);
    }

    private void setupClickListeners() {
        findViewById(R.id.buttonManualLogin).setOnClickListener(v -> {
            loginChoiceLayout.setVisibility(View.GONE);
            manualLoginLayout.setVisibility(View.VISIBLE);
        });

        findViewById(R.id.buttonBackToChoice).setOnClickListener(v -> {
            Long lastUserId = sessionManager.getLastUserId();
            if (lastUserId != null && lastUserId != -1) {
                manualLoginLayout.setVisibility(View.GONE);
                loginChoiceLayout.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(this, "Please login first to enable face login",
                        Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.buttonFaceLogin).setOnClickListener(v -> startFaceLogin());
        findViewById(R.id.buttonLogin).setOnClickListener(v -> handleLogin());
        findViewById(R.id.buttonRegister).setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));

        findViewById(R.id.buttonBackToChoiceFromFace).setOnClickListener(v -> {
            faceLoginLayout.setVisibility(View.GONE);
            loginChoiceLayout.setVisibility(View.VISIBLE);
            if (cameraProvider != null) {
                cameraProvider.unbindAll();
            }
            resetFaceAuthState();
        });
    }

    // Face Login Methods
    private void startFaceLogin() {
        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE);
            return;
        }

        Long lastUserId = sessionManager.getLastUserId();
        if (lastUserId == null || lastUserId == -1) {
            Toast.makeText(this, "Please login manually first to enable face login",
                    Toast.LENGTH_LONG).show();
            loginChoiceLayout.setVisibility(View.GONE);
            manualLoginLayout.setVisibility(View.VISIBLE);
            return;
        }

        // Check lockout status first
        new Thread(() -> {
            Result<Boolean> isLockedResult = userRepository.isUserLocked(lastUserId);

            if (isLockedResult.isSuccess() && isLockedResult.getData()) {
                Result<User> userResult = userRepository.getUserById(lastUserId);

                runOnUiThread(() -> {
                    Toast.makeText(this,
                            "Face login temporarily locked. Please use manual login or try again later.",
                            Toast.LENGTH_LONG).show();
                    loginChoiceLayout.setVisibility(View.GONE);
                    manualLoginLayout.setVisibility(View.VISIBLE);

                    if (userResult.isSuccess()) {
                        editTextEmail.setText(userResult.getData().getEmail());
                    }
                });
                return;
            }

            Result<Boolean> hasEnrollment = faceAuthenticationRepository.hasFaceEnrolled(lastUserId);
            Result<SecuritySettings> settingsResult = userRepository.getSecuritySettings(lastUserId);

            runOnUiThread(() -> {
                if (!hasEnrollment.isSuccess() || !hasEnrollment.getData()) {
                    Toast.makeText(this, "Face not enrolled. Please enroll face first.",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                if (settingsResult.isSuccess()) {
                    securitySettings = settingsResult.getData();
                } else {
                    securitySettings = new SecuritySettings();
                    securitySettings.setMaxFailedAttempts(3);
                    securitySettings.setLockoutDurationMins(15);
                }

                resetFaceAuthState();
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

                faceDetector.process(inputImage)
                        .addOnSuccessListener(faces -> {
                            if (faces.isEmpty()) {
                                updateStatus("No face detected");
                                setNormalOverlay();
                            } else if (faces.size() > 1) {
                                updateStatus("Multiple faces detected");
                                setNormalOverlay();
                            } else {
                                Face face = faces.get(0);
                                processLivenessAndAuthentication(face, mediaImage,
                                        image.getImageInfo().getRotationDegrees());
                            }
                            image.close();
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Face detection failed", e);
                            updateStatus("Detection failed");
                            setNormalOverlay();
                            image.close();
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
        if (isAuthenticationSuccessful || isAuthInProgress) {
            return;
        }

        checkFaceQuality(face);

        if (!isAuthenticating) {
            return;
        }

        if (!livenessCheckPassed) {
            LivenessDetector.LivenessResult result = livenessDetector.processFrame(face);
            updateStatus(result.message);

            if (result.state == LivenessDetector.LivenessState.COMPLETED) {
                livenessCheckPassed = true;
                setScanningOverlay();
                updateStatus("Verifying identity...");

                Bitmap faceBitmap = FacePreprocessor.extractFace(image, face.getBoundingBox(), rotation);
                if (faceBitmap != null && !isAuthInProgress) {
                    isAuthInProgress = true;
                    authenticateUser(faceBitmap);
                }
            } else {
                setNormalOverlay();
            }
        }
    }

    private void checkFaceQuality(Face face) {
        float rotY = face.getHeadEulerAngleY();
        float rotZ = face.getHeadEulerAngleZ();

        if (Math.abs(rotY) < 15 && Math.abs(rotZ) < 15) {
            setScanningOverlay();
            isAuthenticating = true;
        } else {
            isAuthenticating = false;
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

    // Authentication Methods
    private void authenticateUser(Bitmap faceBitmap) {
        Long lastUserId = sessionManager.getLastUserId();
        if (lastUserId == null || lastUserId == -1 || isAuthenticationSuccessful) {
            isAuthInProgress = false;
            return;
        }

        currentAttempt++;

        new Thread(() -> {
            try {
                Thread.sleep(800); // Show "Verifying..." message

                Result<Boolean> authResult = faceAuthenticationRepository.authenticate(
                        lastUserId,
                        faceBitmap,
                        FaceAuthenticationRepository.AuthPurpose.LOGIN
                );

                if (authResult.isSuccess() && authResult.getData()) {
                    isAuthenticationSuccessful = true;
                    runOnUiThread(() -> {
                        updateStatus("Authentication successful!");
                        if (cameraProvider != null) {
                            cameraProvider.unbindAll();
                        }
                    });

                    Result<User> userResult = userRepository.getUserById(lastUserId);
                    if (userResult.isSuccess()) {
                        handleSuccessfulLogin(userResult.getData());
                    }
                } else {
                    int maxAttempts = securitySettings != null ?
                            securitySettings.getMaxFailedAttempts() : 3;

                    runOnUiThread(() -> {
                        String failMessage = String.format("Authentication failed (%d/%d)\nPlease try again",
                                currentAttempt, maxAttempts);
                        updateStatus(failMessage);

                        if (currentAttempt >= maxAttempts) {
                            handleMaxAttemptsReached(lastUserId);
                        } else {
                            livenessCheckPassed = false;
                            isAuthenticating = false;
                        }
                    });

                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        isAuthInProgress = false;
                    }, 2000);
                }

            } catch (Exception e) {
                Log.e(TAG, "Authentication error", e);
                runOnUiThread(() -> {
                    updateStatus("Authentication error: " + e.getMessage());
                    isAuthenticating = false;
                    livenessCheckPassed = false;
                });
                isAuthInProgress = false;
            }
        }).start();
    }

    private void handleMaxAttemptsReached(Long userId) {
        isAuthenticating = false;
        updateStatus("Max attempts reached. Account temporarily locked.");
        setNormalOverlay();

        Toast.makeText(LoginActivity.this,
                "Account locked due to too many failed attempts. Redirecting to manual login...",
                Toast.LENGTH_LONG).show();

        new Thread(() -> {
            Result<User> userResult = userRepository.getUserById(userId);

            runOnUiThread(() -> {
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    faceLoginLayout.setVisibility(View.GONE);
                    manualLoginLayout.setVisibility(View.VISIBLE);
                    editTextPassword.setText("");

                    if (userResult.isSuccess()) {
                        editTextEmail.setText(userResult.getData().getEmail());
                    }

                    if (cameraProvider != null) {
                        cameraProvider.unbindAll();
                    }
                    resetFaceAuthState();
                }, 2000);
            });
        }).start();
    }

    private void handleLogin() {
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

                if (result.isSuccess()) {
                    User user = result.getData();
                    runOnUiThread(() -> {
                        progress.dismiss();
                        handleSuccessfulLogin(user);
                    });
                } else {
                    runOnUiThread(() -> {
                        progress.dismiss();
                        showLoginErrorDialog("Login Failed",
                                "Invalid email or password. Please try again.");
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Login error", e);
                runOnUiThread(() -> {
                    progress.dismiss();
                    showLoginErrorDialog("Login Error",
                            "Error during login: " + e.getMessage());
                });
            }
        }).start();
    }

    private void showLoginErrorDialog(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void handleSuccessfulLogin(User user) {
        runOnUiThread(() -> {
            if (faceLoginLayout.getVisibility() == View.VISIBLE) {
                fadeOutFaceLoginViews();
            } else {
                fadeOutManualLoginLayout();
            }
            showWelcomeAndTransition(user);
        });
    }

    // UI Helper Methods
    private void fadeOutFaceLoginViews() {
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
    }

    private void fadeOutManualLoginLayout() {
        manualLoginLayout.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction(() -> manualLoginLayout.setVisibility(View.GONE))
                .start();
    }

    private void showWelcomeAndTransition(User user) {
        welcomeOverlay.setVisibility(View.VISIBLE);
        welcomeOverlay.setAlpha(0f);
        welcomeText.setText(getTimeBasedGreeting(user.getName()));

        welcomeOverlay.animate()
                .alpha(1f)
                .setDuration(300)
                .withEndAction(() -> {
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        sessionManager.createLoginSession(
                                user.getUserId(),
                                user.getEmail(),
                                user.getName(),
                                user.getRole()
                        );
                        Log.d(TAG, "Setting User ID in session: " + user.getUserId());

                        Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
                        startActivity(intent);
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                        finish();
                    }, WELCOME_DELAY);
                })
                .start();
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

    private void updateStatus(String message) {
        runOnUiThread(() -> statusText.setText(message));
    }

    private void setNormalOverlay() {
        runOnUiThread(() -> overlayView.setBackgroundResource(R.drawable.normal_overlay));
    }

    private void setScanningOverlay() {
        runOnUiThread(() -> overlayView.setBackgroundResource(R.drawable.scanning_overlay));
    }

    // State Management Methods
    private void resetFaceAuthState() {
        livenessCheckPassed = false;
        isAuthenticationSuccessful = false;
        isAuthenticating = false;
        isAuthInProgress = false;
        currentAttempt = 0;
        livenessDetector.reset();
    }

    // Permission Handling
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
}