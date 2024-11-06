package com.biolock.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.biolock.ui.login.LoginActivity;

import java.util.HashMap;

public class SessionManager {
    private static final String TAG = "SessionManager";

    // Shared preferences file name
    private static final String PREF_NAME = "BiolockPref";

    // Shared preferences keys
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_LAST_USER_ID = "lastUserId";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_NAME = "name";
    private static final String KEY_ROLE = "role";

    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;
    private final Context context;

    public SessionManager(Context context) {
        this.context = context;
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public void createLoginSession(Long userId, String email, String name, String role) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putLong(KEY_USER_ID, userId);
        editor.putLong(KEY_LAST_USER_ID, userId);
        editor.putString(KEY_EMAIL, email);
        editor.putString(KEY_NAME, name);
        editor.putString(KEY_ROLE, role);
        editor.commit();
        Log.d(TAG, "User session created for: " + email + ", userId: " + userId + ", lastUserId: " + userId);
    }

    public HashMap<String, String> getUserDetails() {
        HashMap<String, String> user = new HashMap<>();
        user.put(KEY_EMAIL, pref.getString(KEY_EMAIL, null));
        user.put(KEY_NAME, pref.getString(KEY_NAME, null));
        user.put(KEY_ROLE, pref.getString(KEY_ROLE, null));
        return user;
    }

    public Long getUserId() {
        long id = pref.getLong(KEY_USER_ID, -1);
        return id == -1 ? null : id;
    }

    public Long getLastUserId() {
        long lastId = pref.getLong(KEY_LAST_USER_ID, -1);
        Log.d(TAG, "Retrieved lastUserId: " + (lastId == -1 ? "null" : lastId));
        return lastId == -1 ? null : lastId;
    }


    public String getUserRole() {
        return pref.getString(KEY_ROLE, null);
    }

    public String getUserName() {
        return pref.getString(KEY_NAME, null);
    }

    public String getUserEmail() {
        return pref.getString(KEY_EMAIL, null);
    }

    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public void clearCurrentLogin() {
        // Clear current login but keep the last user ID
        Long lastUserId = getLastUserId();
        editor.clear();
        if (lastUserId != null) {
            editor.putLong(KEY_LAST_USER_ID, lastUserId);
        }
        editor.commit();
    }

    public void logoutUser() {
        // Store the last user ID before clearing
        Long lastUserId = getLastUserId();

        // Clear all data from shared preferences
        editor.clear();

        // Restore the last user ID
        if (lastUserId != null) {
            editor.putLong(KEY_LAST_USER_ID, lastUserId);
        }

        editor.commit();

        // After logout redirect user to Login Activity
        Intent i = new Intent(context, LoginActivity.class);
        // Closing all the activities
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        // Add new Flag to start new Activity
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        // Starting Login Activity
        context.startActivity(i);

        Log.d(TAG, "User logged out, preserved lastUserId: " + lastUserId);
    }

    public void checkLogin() {
        // Check login status
        if (!this.isLoggedIn()) {
            // user is not logged in redirect him to Login Activity
            Intent i = new Intent(context, LoginActivity.class);
            // Closing all the Activities
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            // Add new Flag to start new Activity
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            // Starting Login Activity
            context.startActivity(i);
        }
    }
}