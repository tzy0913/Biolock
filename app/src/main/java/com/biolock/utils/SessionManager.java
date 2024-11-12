/**
 * Manages user session data using SharedPreferences.
 * Handles login state, user information, and session persistence.
 * Provides methods for session creation, retrieval, and termination.
 */
package com.biolock.utils;

// Android Core Components
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

// Biolock UI Components
import com.biolock.ui.login.LoginActivity;

// Java Collections
import java.util.HashMap;

public class SessionManager {
    private static final String TAG = "SessionManager";

    // ============================
    // SharedPreferences Configuration
    // ============================
    private static final String PREF_NAME = "BiolockPref";

    // Preference Keys
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_LAST_USER_ID = "lastUserId";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_NAME = "name";
    private static final String KEY_ROLE = "role";

    // Class members
    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;
    private final Context context;

    // ============================
    // Constructor
    // ============================

    /**
     * Initializes session manager with application context
     * @param context Application context
     */
    public SessionManager(Context context) {
        this.context = context;
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    // ============================
    // Session Management Operations
    // ============================

    /**
     * Creates a new login session
     * @param userId User's unique identifier
     * @param email User's email
     * @param name User's display name
     * @param role User's role in the system
     */
    public void createLoginSession(Long userId, String email, String name, String role) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putLong(KEY_USER_ID, userId);
        editor.putLong(KEY_LAST_USER_ID, userId);
        editor.putString(KEY_EMAIL, email);
        editor.putString(KEY_NAME, name);
        editor.putString(KEY_ROLE, role);
        editor.commit();
        Log.d(TAG, "User session created for: " + email + ", userId: " + userId +
                ", lastUserId: " + userId);
    }

    /**
     * Ends current session but preserves last user ID
     */
    public void clearCurrentLogin() {
        Long lastUserId = getLastUserId();
        editor.clear();
        if (lastUserId != null) {
            editor.putLong(KEY_LAST_USER_ID, lastUserId);
        }
        editor.commit();
    }

    /**
     * Logs out user and redirects to login screen
     */
    public void logoutUser() {
        Long lastUserId = getLastUserId();
        editor.clear();

        if (lastUserId != null) {
            editor.putLong(KEY_LAST_USER_ID, lastUserId);
        }
        editor.commit();

        // Redirect to login activity
        Intent i = new Intent(context, LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);

        Log.d(TAG, "User logged out, preserved lastUserId: " + lastUserId);
    }

    /**
     * Verifies login status and redirects to login if needed
     */
    public void checkLogin() {
        if (!isLoggedIn()) {
            Intent i = new Intent(context, LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        }
    }

    // ============================
    // User Data Retrieval
    // ============================

    /**
     * Gets all user details as a map
     * @return HashMap containing user details
     */
    public HashMap<String, String> getUserDetails() {
        HashMap<String, String> user = new HashMap<>();
        user.put(KEY_EMAIL, pref.getString(KEY_EMAIL, null));
        user.put(KEY_NAME, pref.getString(KEY_NAME, null));
        user.put(KEY_ROLE, pref.getString(KEY_ROLE, null));
        return user;
    }

    /**
     * Gets current user ID
     * @return User ID or null if not logged in
     */
    public Long getUserId() {
        long id = pref.getLong(KEY_USER_ID, -1);
        return id == -1 ? null : id;
    }

    /**
     * Gets ID of last logged in user
     * @return Last user ID or null if none
     */
    public Long getLastUserId() {
        long lastId = pref.getLong(KEY_LAST_USER_ID, -1);
        Log.d(TAG, "Retrieved lastUserId: " + (lastId == -1 ? "null" : lastId));
        return lastId == -1 ? null : lastId;
    }

    /**
     * Gets user role
     * @return User's role or null if not set
     */
    public String getUserRole() {
        return pref.getString(KEY_ROLE, null);
    }

    /**
     * Gets user name
     * @return User's name or null if not set
     */
    public String getUserName() {
        return pref.getString(KEY_NAME, null);
    }

    /**
     * Gets user email
     * @return User's email or null if not set
     */
    public String getUserEmail() {
        return pref.getString(KEY_EMAIL, null);
    }

    /**
     * Checks if user is currently logged in
     * @return true if logged in, false otherwise
     */
    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }
}