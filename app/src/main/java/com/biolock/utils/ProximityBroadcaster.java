/**
 * Utility class for handling Bluetooth LE proximity broadcasting and scanning.
 * Manages proximity-based session validation using Bluetooth LE advertising.
 */
package com.biolock.utils;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class ProximityBroadcaster {
    // Constants
    private static final String TAG = "ProximityBroadcaster";
    private static final ParcelUuid SERVICE_UUID =
            ParcelUuid.fromString("00001234-0000-1000-8000-00805F9B34FB");
    private static final long SCAN_PERIOD = 10000;

    // Components
    private final Context context;
    private final BluetoothAdapter bluetoothAdapter;
    private BluetoothLeAdvertiser advertiser;
    private BluetoothLeScanner scanner;
    private ScanCallback scanCallback;
    private AdvertiseCallback advertiseCallback;
    private ProximityListener proximityListener;

    // Listener Interface
    public interface ProximityListener {
        void onProximityDetected(Long sessionId);
        void onProximityTimeout();
        void onProximityError(String error);
    }

    // Constructor
    public ProximityBroadcaster(Context context) {
        this.context = context;
        BluetoothManager bluetoothManager = (BluetoothManager)
                context.getSystemService(Context.BLUETOOTH_SERVICE);
        this.bluetoothAdapter = bluetoothManager.getAdapter();
    }

    // Public Methods
    public void setProximityListener(ProximityListener listener) {
        this.proximityListener = listener;
    }

    public void startBroadcasting(Long sessionId) {
        if (sessionId == null) {
            notifyError("Invalid session ID");
            return;
        }

        String sessionIdString = String.valueOf(sessionId);

        try {
            if (!checkPrerequisites()) {
                return;
            }

            advertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
            if (advertiser == null) {
                notifyError("Proximity broadcasting not supported on this device");
                return;
            }

            AdvertiseSettings settings = createAdvertiseSettings();
            AdvertiseData data = createAdvertiseData(sessionIdString);
            advertiseCallback = createAdvertiseCallback(sessionIdString);

            advertiser.startAdvertising(settings, data, advertiseCallback);

        } catch (SecurityException e) {
            handleSecurityException("broadcasting", e);
        } catch (Exception e) {
            handleException("broadcasting", e);
        }
    }

    public void startScanning() {
        try {
            if (!checkPrerequisites()) {
                return;
            }

            scanner = bluetoothAdapter.getBluetoothLeScanner();
            if (scanner == null) {
                notifyError("Proximity scanning not supported on this device");
                return;
            }

            List<ScanFilter> filters = createScanFilters();
            ScanSettings settings = createScanSettings();
            scanCallback = createScanCallback();

            scanner.startScan(filters, settings, scanCallback);

            setupScanTimeout();

        } catch (SecurityException e) {
            handleSecurityException("scanning", e);
        } catch (Exception e) {
            handleException("scanning", e);
        }
    }

    public void stopBroadcasting() {
        try {
            if (advertiser != null && advertiseCallback != null) {
                advertiser.stopAdvertising(advertiseCallback);
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception while stopping broadcast", e);
        }
    }

    public void stopScanning() {
        try {
            if (scanner != null && scanCallback != null) {
                scanner.stopScan(scanCallback);
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception while stopping scan", e);
        }
    }

    // Private Helper Methods
    private boolean checkPrerequisites() {
        if (!ProximityValidator.hasRequiredPermissions(context)) {
            notifyError("Required permissions not granted");
            return false;
        }

        if (!ProximityValidator.isBleSupported(context)) {
            notifyError("Device does not support proximity validation");
            return false;
        }

        if (!ProximityValidator.isBluetoothEnabled(context)) {
            notifyError("Proximity validation is disabled");
            return false;
        }

        return true;
    }

    private AdvertiseSettings createAdvertiseSettings() {
        return new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(false)
                .build();
    }

    private AdvertiseData createAdvertiseData(String sessionIdString) {
        return new AdvertiseData.Builder()
                .addServiceUuid(SERVICE_UUID)
                .addServiceData(SERVICE_UUID, sessionIdString.getBytes())
                .build();
    }

    private AdvertiseCallback createAdvertiseCallback(String sessionIdString) {
        return new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                Log.d(TAG, "Started broadcasting proximity signal for session: " + sessionIdString);
            }

            @Override
            public void onStartFailure(int errorCode) {
                notifyError("Failed to start broadcasting: " + errorCode);
            }
        };
    }

    private List<ScanFilter> createScanFilters() {
        List<ScanFilter> filters = new ArrayList<>();
        filters.add(new ScanFilter.Builder()
                .setServiceUuid(SERVICE_UUID)
                .build());
        return filters;
    }

    private ScanSettings createScanSettings() {
        return new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();
    }

    private ScanCallback createScanCallback() {
        return new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                processScanResult(result);
            }

            @Override
            public void onScanFailed(int errorCode) {
                notifyError("Proximity scan failed: " + errorCode);
            }
        };
    }

    private void processScanResult(ScanResult result) {
        try {
            byte[] serviceData = result.getScanRecord().getServiceData(SERVICE_UUID);
            if (serviceData != null && proximityListener != null) {
                String sessionIdString = new String(serviceData);
                Long sessionId = Long.parseLong(sessionIdString);
                proximityListener.onProximityDetected(sessionId);
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error parsing session ID", e);
            notifyError("Invalid session ID format");
        }
    }

    private void setupScanTimeout() {
        new android.os.Handler().postDelayed(() -> {
            stopScanning();
            if (proximityListener != null) {
                proximityListener.onProximityTimeout();
            }
        }, SCAN_PERIOD);
    }

    private void handleSecurityException(String operation, SecurityException e) {
        Log.e(TAG, "Security exception while " + operation, e);
        notifyError("Security exception: " + e.getMessage());
    }

    private void handleException(String operation, Exception e) {
        Log.e(TAG, "Error while " + operation, e);
        notifyError("Error: " + e.getMessage());
    }

    private void notifyError(String error) {
        Log.e(TAG, error);
        if (proximityListener != null) {
            proximityListener.onProximityError(error);
        }
    }
}