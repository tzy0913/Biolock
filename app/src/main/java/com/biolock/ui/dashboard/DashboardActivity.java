/**
 * Main dashboard activity handling both instructor and student functionalities.
 * Manages class schedules, attendance marking with face recognition and proximity validation,
 * session management for instructors, and real-time updates.
 */
package com.biolock.ui.dashboard;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import com.biolock.R;
import com.biolock.model.Attendance;
import com.biolock.model.CourseClass;
import com.biolock.model.SecuritySettings;
import com.biolock.model.User;
import com.biolock.repository.AttendanceRepository;
import com.biolock.repository.FaceAuthenticationRepository;
import com.biolock.repository.Result;
import com.biolock.repository.SessionRepository;
import com.biolock.repository.UserRepository;
import com.biolock.ui.attendance.ViewAttendanceActivity;
import com.biolock.ui.login.LoginActivity;
import com.biolock.ui.settings.SettingsActivity;
import com.biolock.utils.FacePreprocessor;
import com.biolock.utils.LivenessDetector;
import com.biolock.utils.ProximityBroadcaster;
import com.biolock.utils.ProximityValidator;
import com.biolock.utils.SessionManager;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DashboardActivity extends AppCompatActivity {
    // Constants
    private static final String TAG = "DashboardActivity";
    private static final int REFRESH_INTERVAL = 30000; // 30 seconds

    // UI Components
    private TextView textGreeting;
    private TextView textClassInfo;
    private Button buttonMarkAttendance;
    private Button buttonViewAttendance;
    private LinearLayout sessionButtons;
    private Button buttonStartSession;
    private Button buttonEndSession;
    private Button buttonViewClassAttendance;

    // Face Authentication UI
    private View faceAuthLayout;
    private PreviewView previewView;
    private TextView statusText;
    private View overlayView;

    // Dependencies
    private AttendanceRepository attendanceRepository;
    private SessionRepository sessionRepository;
    private SessionManager sessionManager;
    private UserRepository userRepository;
    private SimpleDateFormat timeFormat;
    private FaceAuthenticationRepository faceAuthenticationRepository;
    private LivenessDetector livenessDetector;
    private FaceDetector faceDetector;
    private ProcessCameraProvider cameraProvider;
    private final ExecutorService cameraExecutor = Executors.newSingleThreadExecutor();
    private ProximityBroadcaster proximityBroadcaster;
    private ActivityResultLauncher<Intent> bluetoothEnableLauncher;

    // State Management
    private List<Attendance> attendanceList;
    private boolean livenessCheckPassed = false;
    private boolean isAuthenticationSuccessful = false;
    private boolean isAuthenticating = false;
    private boolean isAuthInProgress = false;
    private boolean isProximityValidated = false;
    private int currentAttendanceAttempt = 0;
    private SecuritySettings securitySettings;
    private Attendance currentPendingAttendance;
    private Handler refreshHandler;
    private Runnable refreshRunnable;

    // Lifecycle Methods
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        initializeViews();
        setupDashboard();
        setupRefreshHandler();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboardData(User.ROLE_INSTRUCTOR.equals(sessionManager.getUserRole()));
        refreshHandler.postDelayed(refreshRunnable, REFRESH_INTERVAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        refreshHandler.removeCallbacks(refreshRunnable);
        proximityBroadcaster.stopScanning();
        isProximityValidated = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
        proximityBroadcaster.stopBroadcasting();
        proximityBroadcaster.stopScanning();
        cameraExecutor.shutdown();
    }

    // Permission Handling Methods
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == ProximityValidator.PERMISSION_REQUEST_CODE) {
            if (ProximityValidator.hasRequiredPermissions(this)) {
                boolean isInstructor = User.ROLE_INSTRUCTOR.equals(sessionManager.getUserRole());
                checkPermissionsAndStartProximity(isInstructor);
            } else {
                Toast.makeText(this,
                        "Proximity validation requires all permissions to be granted",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void checkPermissionsAndStartProximity(boolean isInstructor) {
        if (!ProximityValidator.hasRequiredPermissions(this)) {
            List<String> missingPermissions = ProximityValidator.getMissingPermissions(this);
            requestPermissions(
                    missingPermissions.toArray(new String[0]),
                    ProximityValidator.PERMISSION_REQUEST_CODE
            );
            return;
        }

        if (!ProximityValidator.isBleSupported(this)) {
            Toast.makeText(this,
                    "This device doesn't support proximity validation",
                    Toast.LENGTH_LONG).show();
            return;
        }

        if (!ProximityValidator.isBluetoothEnabled(this)) {
            showEnableProximityDialog();
            return;
        }

        if (isInstructor) {
            proceedWithStartSession();
        } else {
            proceedWithMarkAttendance();
        }
    }

    // Initialization Methods
    private void initializeViews() {
        textGreeting = findViewById(R.id.textGreeting);
        textClassInfo = findViewById(R.id.textClassInfo);
        buttonMarkAttendance = findViewById(R.id.buttonMarkAttendance);
        buttonViewAttendance = findViewById(R.id.buttonViewAttendance);
        sessionButtons = findViewById(R.id.sessionButtons);
        buttonStartSession = findViewById(R.id.buttonStartSession);
        buttonEndSession = findViewById(R.id.buttonEndSession);
        buttonViewClassAttendance = findViewById(R.id.buttonViewClassAttendance);

        timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
        sessionManager = new SessionManager(this);
        attendanceRepository = new AttendanceRepository();
        sessionRepository = new SessionRepository();
        userRepository = new UserRepository();

        faceAuthLayout = findViewById(R.id.faceAuthLayout);
        previewView = findViewById(R.id.previewView);
        statusText = findViewById(R.id.statusTextView);
        overlayView = findViewById(R.id.overlayView);

        proximityBroadcaster = new ProximityBroadcaster(this);
        setupBluetoothLauncher();

        textGreeting.setText(String.format("Hi, %s!", sessionManager.getUserName()));

        findViewById(R.id.buttonSettings).setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));

        findViewById(R.id.buttonLogout).setOnClickListener(v -> {
            proximityBroadcaster.stopBroadcasting();
            proximityBroadcaster.stopScanning();
            sessionManager.logoutUser();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        findViewById(R.id.buttonBackToChoiceFromFace).setOnClickListener(v -> {
            faceAuthLayout.setVisibility(View.GONE);
            if (cameraProvider != null) {
                cameraProvider.unbindAll();
            }
            resetFaceAuthState();
        });
    }

    private void setupBluetoothLauncher() {
        bluetoothEnableLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        boolean isInstructor = User.ROLE_INSTRUCTOR.equals(sessionManager.getUserRole());
                        checkPermissionsAndStartProximity(isInstructor);
                    } else {
                        Toast.makeText(this,
                                "Proximity validation requires Bluetooth to be enabled",
                                Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    private void setupDashboard() {
        boolean isInstructor = User.ROLE_INSTRUCTOR.equals(sessionManager.getUserRole());
        setupViewVisibility(isInstructor);
        setupClickListeners(isInstructor);
        loadDashboardData(isInstructor);
    }

    private void setupViewVisibility(boolean isInstructor) {
        buttonMarkAttendance.setVisibility(isInstructor ? View.GONE : View.VISIBLE);
        buttonViewAttendance.setVisibility(isInstructor ? View.GONE : View.VISIBLE);
        sessionButtons.setVisibility(isInstructor ? View.VISIBLE : View.GONE);
        buttonViewClassAttendance.setVisibility(isInstructor ? View.VISIBLE : View.GONE);
    }

    private void setupClickListeners(boolean isInstructor) {
        if (isInstructor) {
            buttonViewClassAttendance.setOnClickListener(v -> {
                Intent intent = new Intent(this, ViewAttendanceActivity.class);
                CourseClass currentClass = CourseClass.getCurrentClass();
                if (currentClass != null) {
                    intent.putExtra(ViewAttendanceActivity.EXTRA_CLASS_ID, currentClass.getClassId());
                }
                startActivity(intent);
            });

            buttonStartSession.setOnClickListener(v -> startSession());
            buttonEndSession.setOnClickListener(v -> showEndSessionDialog());
        } else {
            buttonMarkAttendance.setOnClickListener(v -> markAttendance());
            buttonViewAttendance.setOnClickListener(v ->
                    startActivity(new Intent(this, ViewAttendanceActivity.class)));
        }
    }

    private void setupRefreshHandler() {
        refreshHandler = new Handler();
        refreshRunnable = () -> {
            loadDashboardData(User.ROLE_INSTRUCTOR.equals(sessionManager.getUserRole()));
            refreshHandler.postDelayed(refreshRunnable, REFRESH_INTERVAL);
        };
    }

    // Dashboard Data Methods
    private void loadDashboardData(boolean isInstructor) {
        new Thread(() -> {
            try {
                Result<List<?>> result = attendanceRepository.getTodayAttendance(
                        sessionManager.getUserId(),
                        isInstructor
                );

                if (!result.isSuccess()) {
                    showError("Failed to load class information");
                    return;
                }

                if (isInstructor) {
                    handleInstructorData((List<CourseClass>) result.getData());
                } else {
                    handleStudentData((List<Attendance>) result.getData());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading dashboard data", e);
                showError("Error loading data");
            }
        }).start();
    }

    private void handleInstructorData(List<CourseClass> classes) {
        runOnUiThread(() -> {
            CourseClass currentClass = findCurrentOrUpcomingClass(classes);
            if (currentClass == null) {
                showNoClasses();
                return;
            }

            CourseClass.setCurrentClass(currentClass);
            SpannableStringBuilder builder = new SpannableStringBuilder();
            appendClassInfo(builder, currentClass);
            textClassInfo.setText(builder);
            buttonViewClassAttendance.setEnabled(true);

            boolean isOngoing = "ONGOING".equals(currentClass.getStatus());
            boolean hasValidationCode = currentClass.getValidationCode() != null;

            if (isOngoing && hasValidationCode) {
                proximityBroadcaster.startBroadcasting(currentClass.getSessionId());
                buttonStartSession.setEnabled(false);
                buttonEndSession.setEnabled(true);
            } else {
                boolean canStart = isOngoing && !hasValidationCode;
                buttonStartSession.setEnabled(canStart);
                buttonEndSession.setEnabled(false);
            }
        });
    }

    private void handleStudentData(List<Attendance> attendances) {
        runOnUiThread(() -> {
            this.attendanceList = attendances;
            Attendance currentAttendance = findCurrentOrUpcomingClass(attendances);
            if (currentAttendance == null) {
                showNoClasses();
                return;
            }

            SpannableStringBuilder builder = new SpannableStringBuilder();
            appendClassInfo(builder, currentAttendance);
            appendAttendanceStatus(builder, currentAttendance.getStatus());
            textClassInfo.setText(builder);

            boolean isOngoing = "ONGOING".equals(currentAttendance.getStatus());
            boolean hasCode = currentAttendance.getValidationCode() != null;
            boolean notMarked = currentAttendance.getAttendanceId() == null ||
                    currentAttendance.getAttendanceId() == 0;

            Log.d(TAG, "Current Status: " + currentAttendance.getStatus());
            Log.d(TAG, "Validation Code: " + currentAttendance.getValidationCode());
            Log.d(TAG, "Attendance ID: " + currentAttendance.getAttendanceId());
            Log.d(TAG, "isOngoing: " + isOngoing);
            Log.d(TAG, "hasCode: " + hasCode);
            Log.d(TAG, "notMarked: " + notMarked);

            buttonMarkAttendance.setEnabled(isOngoing && hasCode && notMarked);
            buttonMarkAttendance.setText("Mark Attendance");
        });
    }

    private <T> T findCurrentOrUpcomingClass(List<T> classes) {
        if (classes == null || classes.isEmpty()) return null;

        Calendar now = Calendar.getInstance();

        for (T classObj : classes) {
            Calendar endTime = Calendar.getInstance();

            if (classObj instanceof Attendance) {
                Attendance attendance = (Attendance) classObj;
                endTime.setTime(attendance.getSessionDate());
                endTime.set(Calendar.HOUR_OF_DAY, attendance.getEndTime().getHours());
                endTime.set(Calendar.MINUTE, attendance.getEndTime().getMinutes());
            } else if (classObj instanceof CourseClass) {
                CourseClass courseClass = (CourseClass) classObj;
                endTime.setTime(courseClass.getSessionDate());
                endTime.set(Calendar.HOUR_OF_DAY, courseClass.getEndTime().getHours());
                endTime.set(Calendar.MINUTE, courseClass.getEndTime().getMinutes());
            } else {
                continue;
            }

            if (endTime.after(now)) {
                return classObj;
            }
        }
        return null;
    }

    // Session Management Methods
    private void startSession() {
        CourseClass currentClass = CourseClass.getCurrentClass();
        if (currentClass == null || currentClass.getSessionId() == null) {
            Toast.makeText(this, "No active class session", Toast.LENGTH_SHORT).show();
            return;
        }

        checkPermissionsAndStartProximity(true);
    }

    private void proceedWithStartSession() {
        CourseClass currentClass = CourseClass.getCurrentClass();
        if (currentClass == null || currentClass.getSessionId() == null) {
            Toast.makeText(this, "No active class session", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            Result<String> result = sessionRepository.startSession(currentClass.getSessionId());
            runOnUiThread(() -> {
                if (result.isSuccess()) {
                    proximityBroadcaster.startBroadcasting(currentClass.getSessionId());
                    showSessionCodeDialog(result.getData());
                    buttonStartSession.setEnabled(false);
                    buttonEndSession.setEnabled(true);
                    loadDashboardData(true);
                } else {
                    Toast.makeText(this, result.getError(), Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }

    private void showEndSessionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("End Session Early")
                .setMessage("Are you sure you want to end the session now? This will update the end time to current time.")
                .setPositiveButton("End Session", (dialog, which) -> endSession())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void endSession() {
        CourseClass currentClass = CourseClass.getCurrentClass();
        if (currentClass == null || currentClass.getSessionId() == null) {
            Toast.makeText(this, "No active session", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            Result<Void> result = sessionRepository.endSession(currentClass.getSessionId());
            runOnUiThread(() -> {
                if (result.isSuccess()) {
                    proximityBroadcaster.stopBroadcasting();
                    Toast.makeText(this, "Session ended", Toast.LENGTH_SHORT).show();
                    buttonStartSession.setEnabled(true);
                    buttonEndSession.setEnabled(false);
                    loadDashboardData(true);
                } else {
                    Toast.makeText(this, result.getError(), Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }

    // Attendance Marking Methods
    private void markAttendance() {
        Attendance currentAttendance = findCurrentOrUpcomingClass(attendanceList);
        if (currentAttendance == null || currentAttendance.getSessionId() == null) {
            Toast.makeText(this, "No active session found", Toast.LENGTH_SHORT).show();
            return;
        }

        isProximityValidated = false;
        currentPendingAttendance = currentAttendance;
        checkPermissionsAndStartProximity(false);
    }

    private void proceedWithMarkAttendance() {
        if (currentPendingAttendance == null) {
            Toast.makeText(this, "Session information not found", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isProximityValidated) {
            return;
        }

        proximityBroadcaster.setProximityListener(new ProximityBroadcaster.ProximityListener() {
            @Override
            public void onProximityDetected(Long sessionId) {
                if (isProximityValidated) return;

                if (sessionId.equals(currentPendingAttendance.getSessionId())) {
                    isProximityValidated = true;
                    proximityBroadcaster.stopScanning();

                    runOnUiThread(() -> {
                        Toast.makeText(DashboardActivity.this,
                                "Proximity validation successful",
                                Toast.LENGTH_SHORT).show();
                        checkFaceEnrollmentAndProceed(currentPendingAttendance);
                    });
                }
            }

            @Override
            public void onProximityTimeout() {
                if (isProximityValidated) return;

                runOnUiThread(() -> {
                    Toast.makeText(DashboardActivity.this,
                            "Could not validate proximity. Please ensure you are in the classroom.",
                            Toast.LENGTH_LONG).show();
                });
                isProximityValidated = false;
            }

            @Override
            public void onProximityError(String error) {
                if (isProximityValidated) return;

                runOnUiThread(() -> {
                    Toast.makeText(DashboardActivity.this,
                            "Proximity validation error: " + error,
                            Toast.LENGTH_LONG).show();
                });
                isProximityValidated = false;
            }
        });

        proximityBroadcaster.startScanning();
    }

    // Face Authentication Methods
    private void checkFaceEnrollmentAndProceed(Attendance attendance) {
        if (faceAuthenticationRepository == null) {
            faceAuthenticationRepository = new FaceAuthenticationRepository(this);
        }

        new Thread(() -> {
            Result<Boolean> hasEnrollment = faceAuthenticationRepository.hasFaceEnrolled(sessionManager.getUserId());

            runOnUiThread(() -> {
                if (!hasEnrollment.isSuccess() || !hasEnrollment.getData()) {
                    Toast.makeText(this, "Face not enrolled. Please enroll face first.",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                showOtpDialog(attendance);
            });
        }).start();
    }

    private void startFaceAuthentication(Attendance currentAttendance) {
        if (faceAuthenticationRepository == null) {
            faceAuthenticationRepository = new FaceAuthenticationRepository(this);
        }

        if (livenessDetector == null) {
            livenessDetector = new LivenessDetector();
        }

        if (faceDetector == null) {
            FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                    .build();
            faceDetector = FaceDetection.getClient(options);
        }

        new Thread(() -> {
            Result<SecuritySettings> settingsResult = userRepository.getSecuritySettings(sessionManager.getUserId());

            runOnUiThread(() -> {
                if (settingsResult.isSuccess()) {
                    securitySettings = settingsResult.getData();
                } else {
                    securitySettings = new SecuritySettings();
                    securitySettings.setMaxFailedAttempts(3);
                }

                faceAuthLayout.setVisibility(View.VISIBLE);
                startFaceAuthCamera(previewView, statusText, overlayView, currentAttendance);
            });
        }).start();
    }

    private void startFaceAuthCamera(PreviewView previewView, TextView statusText,
                                     View overlayView, Attendance attendance) {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindFaceAuthCamera(cameraProvider, previewView, statusText, overlayView, attendance);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera", e);
                Toast.makeText(this, "Error starting camera: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindFaceAuthCamera(ProcessCameraProvider cameraProvider,
                                    PreviewView previewView,
                                    TextView statusText,
                                    View overlayView,
                                    Attendance attendance) {
        this.cameraProvider = cameraProvider;
        Preview preview = new Preview.Builder().build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, image -> {
            if (isAuthenticationSuccessful) {
                image.close();
                return;
            }

            Image mediaImage = image.getImage();
            if (mediaImage != null) {
                InputImage inputImage = InputImage.fromMediaImage(
                        mediaImage,
                        image.getImageInfo().getRotationDegrees()
                );

                faceDetector.process(inputImage)
                        .addOnSuccessListener(faces -> {
                            if (faces.isEmpty()) {
                                updateStatus("No face detected", statusText);
                                setNormalOverlay(overlayView);
                            } else if (faces.size() > 1) {
                                updateStatus("Multiple faces detected", statusText);
                                setNormalOverlay(overlayView);
                            } else {
                                Face face = faces.get(0);
                                processAttendanceLivenessAndAuth(face, mediaImage,
                                        image.getImageInfo().getRotationDegrees(),
                                        statusText, overlayView, attendance);
                            }
                            image.close();
                        })
                        .addOnFailureListener(e -> {
                            updateStatus("Detection failed", statusText);
                            setNormalOverlay(overlayView);
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
            Toast.makeText(this, "Error starting camera", Toast.LENGTH_SHORT).show();
        }
    }

    private void validateCodeAndProceed(String code, Attendance attendance,
                                        EditText input, Button submitButton, AlertDialog dialog) {
        new Thread(() -> {
            try {
                Result<Boolean> validationResult = sessionRepository.validateCode(
                        attendance.getSessionId(),
                        code
                );

                if (!validationResult.isSuccess() || !validationResult.getData()) {
                    showError("Invalid code. Please try again.", input, submitButton);
                    return;
                }

                runOnUiThread(() -> {
                    dialog.dismiss();
                    startFaceAuthentication(attendance);
                });

            } catch (Exception e) {
                Log.e(TAG, "Error in code validation", e);
                showError("An error occurred. Please try again.", input, submitButton);
            }
        }).start();
    }

    private void processAttendanceLivenessAndAuth(Face face, Image image, int rotation,
                                                  TextView statusText, View overlayView,
                                                  Attendance attendance) {
        if (isAuthenticationSuccessful || isAuthInProgress) {
            return;
        }

        checkFaceQuality(face, statusText, overlayView);

        if (!isAuthenticating) {
            return;
        }

        if (!livenessCheckPassed) {
            LivenessDetector.LivenessResult result = livenessDetector.processFrame(face);
            updateStatus(result.message, statusText);

            if (result.state == LivenessDetector.LivenessState.COMPLETED) {
                livenessCheckPassed = true;
                setScanningOverlay(overlayView);
                updateStatus("Verifying identity...", statusText);

                Bitmap faceBitmap = FacePreprocessor.extractFace(image, face.getBoundingBox(), rotation);
                if (faceBitmap != null && !isAuthInProgress) {
                    isAuthInProgress = true;
                    authenticateAndMarkAttendance(faceBitmap, statusText, attendance);
                }
            } else {
                setNormalOverlay(overlayView);
            }
        }
    }

    private void checkFaceQuality(Face face, TextView statusText, View overlayView) {
        float rotY = face.getHeadEulerAngleY();
        float rotZ = face.getHeadEulerAngleZ();

        if (Math.abs(rotY) < 15 && Math.abs(rotZ) < 15) {
            setScanningOverlay(overlayView);
            isAuthenticating = true;
        } else {
            isAuthenticating = false;
            setNormalOverlay(overlayView);
            StringBuilder guidance = new StringBuilder("Please ");
            if (Math.abs(rotY) >= 15) {
                guidance.append("face the camera directly");
            }
            if (Math.abs(rotZ) >= 15) {
                if (guidance.length() > 7) guidance.append(" and ");
                guidance.append("keep your head level");
            }
            updateStatus(guidance.toString(), statusText);
        }
    }

    private void authenticateAndMarkAttendance(Bitmap faceBitmap, TextView statusText, Attendance attendance) {
        Long userId = sessionManager.getUserId();
        if (userId == null || isAuthenticationSuccessful) {
            isAuthInProgress = false;
            return;
        }

        currentAttendanceAttempt++;

        new Thread(() -> {
            try {
                Thread.sleep(800);

                Result<Boolean> authResult = faceAuthenticationRepository.authenticate(
                        userId,
                        faceBitmap,
                        FaceAuthenticationRepository.AuthPurpose.ATTENDANCE
                );

                if (!authResult.isSuccess() || !authResult.getData()) {
                    handleFailedAuthentication(statusText);
                    return;
                }

                Result<Void> markResult = attendanceRepository.markAttendance(
                        userId,
                        attendance.getSessionId()
                );

                if (!markResult.isSuccess()) {
                    handleFailedAttendanceMarking();
                    return;
                }

                handleSuccessfulAttendance(statusText);

            } catch (Exception e) {
                handleAuthenticationError(e);
            }
        }).start();
    }

    private void handleFailedAuthentication(TextView statusText) {
        int maxAttempts = securitySettings != null ? securitySettings.getMaxFailedAttempts() : 3;

        runOnUiThread(() -> {
            String failMessage = String.format("Authentication failed (%d/%d)\nPlease try again",
                    currentAttendanceAttempt, maxAttempts);
            updateStatus(failMessage, statusText);

            if (currentAttendanceAttempt >= maxAttempts) {
                handleMaxAttemptsReached();
            } else {
                livenessCheckPassed = false;
                isAuthenticating = false;
            }
        });

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            isAuthInProgress = false;
        }, 2000);
    }

    private void handleMaxAttemptsReached() {
        isAuthenticating = false;
        updateStatus("Max attempts reached", statusText);
        setNormalOverlay(overlayView);

        Toast.makeText(this, "Too many failed attempts. Please try again later.",
                Toast.LENGTH_LONG).show();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            faceAuthLayout.setVisibility(View.GONE);
            if (cameraProvider != null) {
                cameraProvider.unbindAll();
            }
            resetFaceAuthState();
        }, 2000);
    }

    private void handleFailedAttendanceMarking() {
        showError("Failed to mark attendance. Please try again.");
        runOnUiThread(() -> {
            faceAuthLayout.setVisibility(View.GONE);
            if (cameraProvider != null) {
                cameraProvider.unbindAll();
            }
        });
    }

    private void handleSuccessfulAttendance(TextView statusText) {
        isAuthenticationSuccessful = true;
        runOnUiThread(() -> {
            updateStatus("Authentication successful!", statusText);
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                faceAuthLayout.setVisibility(View.GONE);
                if (cameraProvider != null) {
                    cameraProvider.unbindAll();
                }
                Toast.makeText(DashboardActivity.this,
                        "Attendance marked successfully!", Toast.LENGTH_LONG).show();
                loadDashboardData(false);
            }, 2000);
        });
    }

    private void handleAuthenticationError(Exception e) {
        Log.e(TAG, "Authentication error", e);
        showError("Authentication error: " + e.getMessage());
        runOnUiThread(() -> {
            isAuthenticating = false;
            livenessCheckPassed = false;
            faceAuthLayout.setVisibility(View.GONE);
            if (cameraProvider != null) {
                cameraProvider.unbindAll();
            }
            resetFaceAuthState();
        });
        isAuthInProgress = false;
    }

    // UI Helper Methods
    private void appendClassInfo(SpannableStringBuilder builder, CourseClass courseClass) {
        builder.append(courseClass.getModuleCode()).append("\n");
        builder.append(courseClass.getModuleName()).append("\n");
        builder.append(String.format("Section %s - %s",
                courseClass.getSection(),
                courseClass.getRoom()
        )).append("\n");

        if (courseClass.getStartTime() != null && courseClass.getEndTime() != null) {
            builder.append(String.format("%s - %s",
                    timeFormat.format(courseClass.getStartTime()),
                    timeFormat.format(courseClass.getEndTime())
            )).append("\n");
        }

        builder.append("Status: ");
        String statusText = courseClass.getStatus() != null ? courseClass.getStatus() : "NOT MARKED";
        SpannableString spannable = new SpannableString(statusText);

        int color;
        switch (statusText) {
            case "COMPLETED":
                color = ContextCompat.getColor(this, android.R.color.holo_green_dark);
                break;
            case "ONGOING":
                color = ContextCompat.getColor(this, android.R.color.holo_blue_dark);
                break;
            case "UPCOMING":
                color = ContextCompat.getColor(this, android.R.color.darker_gray);
                break;
            default:
                color = ContextCompat.getColor(this, android.R.color.darker_gray);
                break;
        }

        spannable.setSpan(
                new ForegroundColorSpan(color),
                0,
                statusText.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        builder.append(spannable);
    }

    private void appendClassInfo(SpannableStringBuilder builder, Attendance attendance) {
        builder.append(attendance.getModuleCode()).append("\n");
        builder.append(attendance.getModuleName()).append("\n");
        builder.append(String.format("Section %s - %s",
                attendance.getSection(),
                attendance.getRoom()
        )).append("\n");

        if (attendance.getStartTime() != null && attendance.getEndTime() != null) {
            builder.append(String.format("%s - %s",
                    timeFormat.format(attendance.getStartTime()),
                    timeFormat.format(attendance.getEndTime())
            )).append("\n");
        }
    }

    private void appendAttendanceStatus(SpannableStringBuilder builder, String status) {
        builder.append("Status: ");
        String statusText = status != null ? status.toUpperCase() : "NOT MARKED";
        SpannableString spannable = new SpannableString(statusText);

        int color;
        switch (statusText) {
            case "PRESENT":
                color = ContextCompat.getColor(this, android.R.color.holo_green_dark);
                break;
            case "LATE":
            case "ABSENT":
                color = ContextCompat.getColor(this, android.R.color.holo_red_dark);
                break;
            default:
                color = ContextCompat.getColor(this, android.R.color.darker_gray);
                break;
        }

        spannable.setSpan(
                new ForegroundColorSpan(color),
                0,
                statusText.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        builder.append(spannable);
    }

    private void showNoClasses() {
        textClassInfo.setText("No classes scheduled for today");
        buttonMarkAttendance.setEnabled(false);
        buttonStartSession.setEnabled(false);
        buttonEndSession.setEnabled(false);
    }

    private void showSessionCodeDialog(String code) {
        new AlertDialog.Builder(this)
                .setTitle("Session Started")
                .setMessage(String.format("Share this code with your students:\n\n%s", code))
                .setPositiveButton("Copy Code", (dialog, which) -> {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Session Code", code);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(this, "Code copied to clipboard", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void showOtpDialog(Attendance attendance) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Step 2: Enter Attendance Code");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setFilters(new InputFilter[] { new InputFilter.LengthFilter(6) });
        input.setGravity(Gravity.CENTER);
        input.setHint("Enter 6-digit code");

        LinearLayout container = new LinearLayout(this);
        container.setPadding(60, 40, 60, 20);
        container.addView(input);
        builder.setView(container);

        builder.setPositiveButton("Submit", null);
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> {
            Button submitButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            submitButton.setOnClickListener(view -> {
                String code = input.getText().toString().trim();
                if (!code.matches("\\d{6}")) {
                    input.setError("Please enter a valid 6-digit code");
                    return;
                }

                submitButton.setEnabled(false);
                submitButton.setText("Verifying...");

                validateCodeAndProceed(code, attendance, input, submitButton, dialog);
            });
        });

        dialog.show();
        input.requestFocus();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }

    private void showEnableProximityDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Proximity Validation Required")
                .setMessage("Proximity validation requires Bluetooth. Would you like to enable it now?")
                .setPositiveButton("Enable", (dialog, which) -> {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    bluetoothEnableLauncher.launch(enableBtIntent);
                })
                .setNegativeButton("Cancel", (dialog, which) ->
                        Toast.makeText(this,
                                "Proximity validation requires Bluetooth to be enabled",
                                Toast.LENGTH_LONG).show())
                .setCancelable(false)
                .show();
    }

    // Utility Methods
    private void updateStatus(String message, TextView statusText) {
        runOnUiThread(() -> {
            if (statusText != null) {
                statusText.setText(message);
            }
        });
    }

    private void setNormalOverlay(View overlayView) {
        runOnUiThread(() -> {
            if (overlayView != null) {
                overlayView.setBackgroundResource(R.drawable.normal_overlay);
            }
        });
    }

    private void setScanningOverlay(View overlayView) {
        runOnUiThread(() -> {
            if (overlayView != null) {
                overlayView.setBackgroundResource(R.drawable.scanning_overlay);
            }
        });
    }

    private void resetFaceAuthState() {
        livenessCheckPassed = false;
        isAuthenticationSuccessful = false;
        isAuthenticating = false;
        isAuthInProgress = false;
        isProximityValidated = false;
        currentAttendanceAttempt = 0;
        if (livenessDetector != null) {
            livenessDetector.reset();
        }
    }

    private void showError(String message, Object... params) {
        runOnUiThread(() -> {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();

            if (params != null && params.length >= 2 && params[0] instanceof EditText && params[1] instanceof Button) {
                EditText input = (EditText) params[0];
                Button submitButton = (Button) params[1];
                input.setError(message);
                submitButton.setEnabled(true);
                submitButton.setText("Submit");
            } else {
                textClassInfo.setText("Error loading class information");
                buttonMarkAttendance.setEnabled(false);
                buttonViewClassAttendance.setEnabled(false);
            }
        });
    }
}

