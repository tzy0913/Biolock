package com.biolock.utils;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.List;

public class ProximityValidator {
    private static final String TAG = "ProximityValidator";
    public static final int PERMISSION_REQUEST_CODE = 123;

    public static boolean hasRequiredPermissions(Context context) {
        List<String> requiredPermissions = getRequiredPermissions();

        for (String permission : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(context, permission) !=
                    PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public static List<String> getRequiredPermissions() {
        List<String> permissions = new ArrayList<>();

        // Basic Bluetooth permissions
        permissions.add(Manifest.permission.BLUETOOTH);
        permissions.add(Manifest.permission.BLUETOOTH_ADMIN);

        // Location permissions required for BLE scanning
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);

        // Android 12+ specific permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN);
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE);
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
        }

        return permissions;
    }

    public static List<String> getMissingPermissions(Context context) {
        List<String> missingPermissions = new ArrayList<>();
        List<String> requiredPermissions = getRequiredPermissions();

        for (String permission : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(context, permission) !=
                    PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }

        return missingPermissions;
    }

    public static boolean isBleSupported(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    public static boolean isBluetoothEnabled(Context context) {
        try {
            BluetoothManager bluetoothManager = (BluetoothManager)
                    context.getSystemService(Context.BLUETOOTH_SERVICE);
            BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
            return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
        } catch (SecurityException e) {
            return false;
        }
    }
}