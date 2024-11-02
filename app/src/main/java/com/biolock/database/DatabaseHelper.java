package com.biolock.database;

import android.util.Log;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public class DatabaseHelper {
    private static final String TAG = "DatabaseHelper";

    // Database configuration
    private static final String DB_HOST = "127.0.0.1";
    private static final int DB_PORT = 3306;
    private static final String DB_NAME = "biolock";
    private static final String DB_USER = "biolock";
    private static final String DB_PASSWORD = "!Passw0rd";

    // SSH tunnel configuration
    private static final String SSH_HOST = "192.168.1.118";
    private static final int SSH_PORT = 3307;
    private static final String SSH_USER = "biolock";
    private static final String SSH_PASSWORD = "!Passw0rd";
    private static final int LOCAL_PORT = 3306;

    // Instance management
    private static volatile DatabaseHelper instance;
    private final ReentrantLock lock = new ReentrantLock();
    private final AtomicBoolean isInitialized = new AtomicBoolean(false);

    // Connection properties
    private Properties connectionProps;
    private Session sshSession;

    private DatabaseHelper() {
        initializeConnectionProperties();
    }

    public static DatabaseHelper getInstance() {
        if (instance == null) {
            synchronized (DatabaseHelper.class) {
                if (instance == null) {
                    instance = new DatabaseHelper();
                }
            }
        }
        return instance;
    }

    private void initializeConnectionProperties() {
        connectionProps = new Properties();
        connectionProps.setProperty("user", DB_USER);
        connectionProps.setProperty("password", DB_PASSWORD);
        connectionProps.setProperty("autoReconnect", "true");
        connectionProps.setProperty("useSSL", "false");
        connectionProps.setProperty("allowPublicKeyRetrieval", "true");
    }

    public void initialize() throws SQLException {
        if (isInitialized.get()) {
            return;
        }

        lock.lock();
        try {
            if (isInitialized.get()) {
                return;
            }

            setupSSHTunnel();
            testConnection();
            isInitialized.set(true);
            Log.d(TAG, "SSH tunnel established successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize database helper", e);
            cleanup();
            throw new SQLException("Database initialization failed: " + e.getMessage(), e);
        } finally {
            lock.unlock();
        }
    }

    private void setupSSHTunnel() throws JSchException {
        if (sshSession != null && sshSession.isConnected()) {
            return;
        }

        try {
            JSch jsch = new JSch();
            sshSession = jsch.getSession(SSH_USER, SSH_HOST, SSH_PORT);
            sshSession.setPassword(SSH_PASSWORD);

            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            sshSession.setConfig(config);

            sshSession.connect(30000);
            sshSession.setPortForwardingL(LOCAL_PORT, DB_HOST, DB_PORT);
            Log.d(TAG, "SSH tunnel established successfully");
        } catch (JSchException e) {
            Log.e(TAG, "Failed to establish SSH tunnel", e);
            throw e;
        }
    }

    private void testConnection() throws SQLException {
        String url = String.format("jdbc:mysql://%s:%d/%s", DB_HOST, LOCAL_PORT, DB_NAME);
        try {
            Class.forName("com.mysql.jdbc.Driver");
            try (Connection conn = DriverManager.getConnection(url, connectionProps)) {
                if (!conn.isValid(5)) {
                    throw new SQLException("Invalid connection");
                }
                Log.d(TAG, "Database connection test successful");
            }
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL driver not found", e);
        }
    }

    public Connection getConnection() throws SQLException {
        if (!isInitialized.get()) {
            throw new SQLException("SSH tunnel not initialized. Call initialize() first.");
        }

        lock.lock();
        try {
            ensureSSHTunnelActive();
            String url = String.format("jdbc:mysql://%s:%d/%s", DB_HOST, LOCAL_PORT, DB_NAME);
            Connection conn = DriverManager.getConnection(url, connectionProps);

            if (!conn.isValid(1)) {
                conn.close();
                throw new SQLException("Invalid connection");
            }
            return conn;
        } finally {
            lock.unlock();
        }
    }

    private void ensureSSHTunnelActive() throws SQLException {
        try {
            if (sshSession == null || !sshSession.isConnected()) {
                setupSSHTunnel();
            }
        } catch (JSchException e) {
            throw new SQLException("Failed to ensure SSH tunnel: " + e.getMessage(), e);
        }
    }

    public void cleanup() {
        lock.lock();
        try {
            if (sshSession != null) {
                sshSession.disconnect();
                sshSession = null;
            }
            isInitialized.set(false);
            Log.d(TAG, "Database resources cleaned up");
        } finally {
            lock.unlock();
        }
    }

    public boolean isInitialized() {
        return isInitialized.get();
    }
}