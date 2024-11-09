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
    private static final String TAG = "ProximityBroadcaster";
    private static final ParcelUuid SERVICE_UUID =
            ParcelUuid.fromString("00001234-0000-1000-8000-00805F9B34FB");
    private static final long SCAN_PERIOD = 10000;

    private final Context context;
    private final BluetoothAdapter bluetoothAdapter;
    private BluetoothLeAdvertiser advertiser;
    private BluetoothLeScanner scanner;
    private ScanCallback scanCallback;
    private AdvertiseCallback advertiseCallback;
    private ProximityListener proximityListener;

    public interface ProximityListener {
        void onProximityDetected(Long sessionId); // Change to Long instead of String
        void onProximityTimeout();
        void onProximityError(String error);
    }

    public ProximityBroadcaster(Context context) {
        this.context = context;
        BluetoothManager bluetoothManager = (BluetoothManager)
                context.getSystemService(Context.BLUETOOTH_SERVICE);
        this.bluetoothAdapter = bluetoothManager.getAdapter();
    }

    public void setProximityListener(ProximityListener listener) {
        this.proximityListener = listener;
    }

    public void startBroadcasting(Long sessionId) {
        if (sessionId == null) {
            notifyError("Invalid session ID");
            return;
        }

        // Convert Long to String before broadcasting
        String sessionIdString = String.valueOf(sessionId);

        try {
            // Rest of your existing broadcasting code
            if (!ProximityValidator.hasRequiredPermissions(context)) {
                notifyError("Required permissions not granted");
                return;
            }

            if (!ProximityValidator.isBleSupported(context)) {
                notifyError("Device does not support proximity validation");
                return;
            }

            if (!ProximityValidator.isBluetoothEnabled(context)) {
                notifyError("Proximity validation is disabled");
                return;
            }

            advertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
            if (advertiser == null) {
                notifyError("Proximity broadcasting not supported on this device");
                return;
            }

            AdvertiseSettings settings = new AdvertiseSettings.Builder()
                    .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                    .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                    .setConnectable(false)
                    .build();

            AdvertiseData data = new AdvertiseData.Builder()
                    .addServiceUuid(SERVICE_UUID)
                    .addServiceData(SERVICE_UUID, sessionIdString.getBytes())
                    .build();

            advertiseCallback = new AdvertiseCallback() {
                @Override
                public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                    Log.d(TAG, "Started broadcasting proximity signal for session: " + sessionIdString);
                }

                @Override
                public void onStartFailure(int errorCode) {
                    notifyError("Failed to start broadcasting: " + errorCode);
                }
            };

            advertiser.startAdvertising(settings, data, advertiseCallback);

        } catch (SecurityException e) {
            Log.e(TAG, "Security exception while broadcasting", e);
            notifyError("Security exception: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error while broadcasting", e);
            notifyError("Error: " + e.getMessage());
        }
    }

    public void startScanning() {
        try {
            // Check all prerequisites first
            if (!ProximityValidator.hasRequiredPermissions(context)) {
                notifyError("Required permissions not granted");
                return;
            }

            if (!ProximityValidator.isBleSupported(context)) {
                notifyError("Device does not support proximity validation");
                return;
            }

            if (!ProximityValidator.isBluetoothEnabled(context)) {
                notifyError("Proximity validation is disabled");
                return;
            }

            scanner = bluetoothAdapter.getBluetoothLeScanner();
            if (scanner == null) {
                notifyError("Proximity scanning not supported on this device");
                return;
            }

            List<ScanFilter> filters = new ArrayList<>();
            filters.add(new ScanFilter.Builder()
                    .setServiceUuid(SERVICE_UUID)
                    .build());

            ScanSettings settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();

            scanCallback = new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
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

                @Override
                public void onScanFailed(int errorCode) {
                    notifyError("Proximity scan failed: " + errorCode);
                }
            };

            scanner.startScan(filters, settings, scanCallback);

            // Stop scanning after SCAN_PERIOD
            new android.os.Handler().postDelayed(() -> {
                stopScanning();
                if (proximityListener != null) {
                    proximityListener.onProximityTimeout();
                }
            }, SCAN_PERIOD);

        } catch (SecurityException e) {
            Log.e(TAG, "Security exception while scanning", e);
            notifyError("Security exception: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error while scanning", e);
            notifyError("Error: " + e.getMessage());
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

    private void notifyError(String error) {
        Log.e(TAG, error);
        if (proximityListener != null) {
            proximityListener.onProximityError(error);
        }
    }
}