/**
 * Generic wrapper class for operation results.
 * Provides a standardized way to handle both successful results and errors.
 *
 * @param <T> The type of data being wrapped
 */
package com.biolock.repository;

public class Result<T> {
    // ============================
    // Instance Fields
    // ============================
    private final T data;            // The result data (null if error)
    private final String error;      // Error message (null if success)
    private final boolean success;   // Whether the operation succeeded

    // ============================
    // Constructor
    // ============================

    /**
     * Private constructor for creating Result instances
     * Use static factory methods success() or error() instead
     *
     * @param data Result data
     * @param error Error message
     * @param success Success status
     */
    private Result(T data, String error, boolean success) {
        this.data = data;
        this.error = error;
        this.success = success;
    }

    // ============================
    // Static Factory Methods
    // ============================

    /**
     * Creates a successful result containing data
     *
     * @param data The result data
     * @param <T> Type of the result data
     * @return A successful Result instance containing the data
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(data, null, true);
    }

    /**
     * Creates an error result with an error message
     *
     * @param error The error message
     * @param <T> Type of the result data (will be null)
     * @return A failed Result instance containing the error message
     */
    public static <T> Result<T> error(String error) {
        return new Result<>(null, error, false);
    }

    // ============================
    // Getters
    // ============================

    /**
     * Gets the result data
     * @return The data if successful, null if error
     */
    public T getData() {
        return data;
    }

    /**
     * Gets the error message
     * @return The error message if failed, null if successful
     */
    public String getError() {
        return error;
    }

    /**
     * Checks if the operation was successful
     * @return true if successful, false if error
     */
    public boolean isSuccess() {
        return success;
    }
}