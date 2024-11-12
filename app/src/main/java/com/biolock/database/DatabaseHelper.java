/**
 * DatabaseHelper manages database connections through an SSH tunnel with connection pooling.
 * Implements singleton pattern and provides thread-safe database operations.
 */
package com.biolock.database;

// Android Core
import android.util.Log;

// SSH Components
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

// Java SQL
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

// Java Utilities
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
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

    // Connection pool configuration
    private static final int INITIAL_POOL_SIZE = 3;
    private static final int MAX_POOL_SIZE = 10;
    private static final long CONNECTION_TIMEOUT_MS = 5000;
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000;

    private static volatile DatabaseHelper instance;
    private final ReentrantLock lock = new ReentrantLock();
    private final AtomicBoolean isInitialized = new AtomicBoolean(false);
    private final BlockingQueue<PooledConnection> connectionPool;
    private Properties connectionProps;
    private Session sshSession;
    private String jdbcUrl;

    // Inner class for managing pooled connections
    private static class PooledConnection {
        Connection connection;
        long lastValidated;

        PooledConnection(Connection connection) {
            this.connection = connection;
            this.lastValidated = System.currentTimeMillis();
        }

        boolean isValid() {
            try {
                if (connection == null || connection.isClosed()) {
                    return false;
                }
                // Only validate if more than 30 seconds have passed
                if (System.currentTimeMillis() - lastValidated > 30000) {
                    if (!connection.isValid(1)) {
                        return false;
                    }
                    lastValidated = System.currentTimeMillis();
                }
                return true;
            } catch (SQLException e) {
                return false;
            }
        }
    }

    // Singleton and initialization methods
    private DatabaseHelper() {
        connectionPool = new ArrayBlockingQueue<>(MAX_POOL_SIZE);
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
        connectionProps.setProperty("connectTimeout", "5000");
        connectionProps.setProperty("socketTimeout", "30000");
        connectionProps.setProperty("useSSL", "false");
        connectionProps.setProperty("allowPublicKeyRetrieval", "true");
        connectionProps.setProperty("tcpKeepAlive", "true");
    }

    // SSH and Database initialization methods
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
            initializeConnectionPool();
            isInitialized.set(true);
            Log.i(TAG, "Database initialized successfully with connection pool");
        } catch (Exception e) {
            cleanup();
            throw new SQLException("Failed to initialize database: " + e.getMessage(), e);
        } finally {
            lock.unlock();
        }
    }

    private void setupSSHTunnel() throws JSchException {
        Log.d(TAG, "Setting up SSH tunnel...");

        if (sshSession != null && sshSession.isConnected()) {
            return;
        }

        JSch jsch = new JSch();
        sshSession = jsch.getSession(SSH_USER, SSH_HOST, SSH_PORT);
        sshSession.setPassword(SSH_PASSWORD);

        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        config.put("PreferredAuthentications", "password");
        config.put("TCPKeepAlive", "yes");
        config.put("ServerAliveInterval", "30000");
        sshSession.setConfig(config);

        sshSession.connect(30000);
        sshSession.setPortForwardingL(LOCAL_PORT, DB_HOST, DB_PORT);
        jdbcUrl = String.format("jdbc:mysql://%s:%d/%s", DB_HOST, LOCAL_PORT, DB_NAME);
        Log.i(TAG, "SSH tunnel established successfully");
    }

    // Connection management methods
    private Connection createConnection() throws SQLException {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            Connection conn = DriverManager.getConnection(jdbcUrl, connectionProps);
            if (!conn.isValid(1)) {
                conn.close();
                throw new SQLException("Invalid connection");
            }
            return conn;
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL driver not found", e);
        }
    }

    private void initializeConnectionPool() throws SQLException {
        Log.d(TAG, "Initializing connection pool...");

        for (int i = 0; i < INITIAL_POOL_SIZE; i++) {
            Connection conn = createConnection();
            connectionPool.offer(new PooledConnection(conn));
        }

        Log.i(TAG, "Connection pool initialized with " + connectionPool.size() + " connections");
    }

    // Public connection handling methods
    public Connection getConnection() throws SQLException {
        if (!isInitialized.get()) {
            initialize();
        }

        SQLException lastException = null;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                PooledConnection pooledConn = connectionPool.poll(CONNECTION_TIMEOUT_MS, TimeUnit.MILLISECONDS);

                if (pooledConn == null) {
                    // Create new connection if pool is empty
                    if (connectionPool.size() < MAX_POOL_SIZE) {
                        Log.d(TAG, "Creating new connection as pool is empty");
                        return createConnection();
                    }
                    throw new SQLException("Connection pool exhausted");
                }

                if (!pooledConn.isValid()) {
                    Log.d(TAG, "Retrieved invalid connection, creating new one");
                    try {
                        pooledConn.connection.close();
                    } catch (SQLException e) {
                        Log.e(TAG, "Error closing invalid connection", e);
                    }
                    pooledConn.connection = createConnection();
                    pooledConn.lastValidated = System.currentTimeMillis();
                }

                return pooledConn.connection;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new SQLException("Interrupted while waiting for connection", e);
            } catch (SQLException e) {
                lastException = e;
                if (attempt < MAX_RETRIES) {
                    Log.w(TAG, "Failed to get connection, attempt " + attempt + " of " + MAX_RETRIES);
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new SQLException("Interrupted while retrying connection", ie);
                    }
                }
            }
        }

        throw new SQLException("Failed to get valid connection after " + MAX_RETRIES +
                " attempts. Last error: " + (lastException != null ? lastException.getMessage() : "Unknown"));
    }

    public void releaseConnection(Connection conn) {
        if (conn != null) {
            try {
                if (!conn.isClosed() && conn.isValid(1)) {
                    if (!connectionPool.offer(new PooledConnection(conn))) {
                        conn.close();
                    }
                } else {
                    conn.close();
                }
            } catch (SQLException e) {
                Log.e(TAG, "Error releasing connection", e);
                try {
                    conn.close();
                } catch (SQLException ex) {
                    Log.e(TAG, "Error closing invalid connection", ex);
                }
            }
        }
    }

    // Cleanup and status methods
    public synchronized void cleanup() {
        Log.d(TAG, "Cleaning up database resources...");

        PooledConnection pooledConn;
        while ((pooledConn = connectionPool.poll()) != null) {
            try {
                if (pooledConn.connection != null) {
                    pooledConn.connection.close();
                }
            } catch (SQLException e) {
                Log.e(TAG, "Error closing connection", e);
            }
        }

        if (sshSession != null) {
            try {
                if (sshSession.isConnected()) {
                    sshSession.delPortForwardingL(LOCAL_PORT);
                }
                sshSession.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Error closing SSH session", e);
            }
            sshSession = null;
        }

        isInitialized.set(false);
        Log.i(TAG, "Database resources cleaned up");
    }

    public boolean isInitialized() {
        return isInitialized.get();
    }
}