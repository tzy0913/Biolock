package com.biolock.utils;

import android.view.View;
import com.google.mlkit.vision.face.Face;

public class LivenessDetector {
    private static final float HEAD_ROTATION_THRESHOLD = 10f;
    private static final float SMILE_THRESHOLD = 0.8f;

    public enum LivenessState {
        WAITING,        // Initial state
        MOVE_LEFT,      // User should turn head left
        MOVE_RIGHT,     // User should turn head right
        SMILE,          // User should smile
        LOOK_STRAIGHT,  // User should look straight
        COMPLETED      // All checks passed
    }

    private LivenessState currentState = LivenessState.WAITING;
    private boolean leftChecked = false;
    private boolean rightChecked = false;
    private boolean smileChecked = false;
    private boolean straightChecked = false;

    public void reset() {
        currentState = LivenessState.WAITING;
        leftChecked = false;
        rightChecked = false;
        smileChecked = false;
        straightChecked = false;
    }

    public LivenessResult processFrame(Face face) {
        float rotY = face.getHeadEulerAngleY();  // Head rotation Y (left/right)
        float rotZ = face.getHeadEulerAngleZ();  // Head rotation Z (tilt)

        if (Math.abs(rotZ) > HEAD_ROTATION_THRESHOLD) {
            return new LivenessResult(false, "Please keep your head straight", currentState);
        }

        switch (currentState) {
            case WAITING:
                if (isLookingStraight(rotY)) {
                    currentState = LivenessState.MOVE_LEFT;
                    return new LivenessResult(false, "Turn your head left slowly", currentState);
                }
                break;

            case MOVE_LEFT:
                if (rotY > HEAD_ROTATION_THRESHOLD) {
                    leftChecked = true;
                    currentState = LivenessState.MOVE_RIGHT;
                    return new LivenessResult(false, "Now turn your head right slowly", currentState);
                }
                break;

            case MOVE_RIGHT:
                if (rotY < -HEAD_ROTATION_THRESHOLD) {
                    rightChecked = true;
                    currentState = LivenessState.SMILE;
                    return new LivenessResult(false, "Please smile", currentState);
                }
                break;

            case SMILE:
                if (face.getSmilingProbability() != null &&
                        face.getSmilingProbability() > SMILE_THRESHOLD) {
                    smileChecked = true;
                    currentState = LivenessState.LOOK_STRAIGHT;
                    return new LivenessResult(false, "Now look straight at the camera", currentState);
                }
                break;

            case LOOK_STRAIGHT:
                if (isLookingStraight(rotY)) {
                    straightChecked = true;
                    currentState = LivenessState.COMPLETED;
                    return new LivenessResult(true, "Liveness check completed", currentState);
                }
                break;

            case COMPLETED:
                return new LivenessResult(true, "Liveness check completed", currentState);
        }

        return new LivenessResult(false, getInstructionForState(currentState), currentState);
    }

    private boolean isLookingStraight(float rotY) {
        return Math.abs(rotY) < HEAD_ROTATION_THRESHOLD/2;
    }

    private String getInstructionForState(LivenessState state) {
        switch (state) {
            case WAITING: return "Look straight at the camera";
            case MOVE_LEFT: return "Turn your head left slowly";
            case MOVE_RIGHT: return "Turn your head right slowly";
            case SMILE: return "Please smile";
            case LOOK_STRAIGHT: return "Look straight at the camera";
            case COMPLETED: return "Liveness check completed";
            default: return "Follow the instructions";
        }
    }

    public static class LivenessResult {
        public final boolean isCompleted;
        public final String message;
        public final LivenessState state;

        public LivenessResult(boolean isCompleted, String message, LivenessState state) {
            this.isCompleted = isCompleted;
            this.message = message;
            this.state = state;
        }
    }
}