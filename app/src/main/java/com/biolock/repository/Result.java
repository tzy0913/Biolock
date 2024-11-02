package com.biolock.repository;

public abstract class Result<T> {
    private Result() {} // Prevent direct instantiation

    public static <T> Result<T> success(T data) {
        return new Success<>(data);
    }

    public static <T> Result<T> error(Exception error) {
        return new Error<>(error);
    }

    public abstract boolean isSuccess();

    public abstract T getData();

    public abstract Exception getError();

    public static final class Success<T> extends Result<T> {
        private final T data;

        private Success(T data) {
            this.data = data;
        }

        @Override
        public boolean isSuccess() {
            return true;
        }

        @Override
        public T getData() {
            return data;
        }

        @Override
        public Exception getError() {
            throw new IllegalStateException("Error is not available for Success result");
        }
    }

    public static final class Error<T> extends Result<T> {
        private final Exception error;

        private Error(Exception error) {
            this.error = error;
        }

        @Override
        public boolean isSuccess() {
            return false;
        }

        @Override
        public T getData() {
            throw new IllegalStateException("Data is not available for Error result");
        }

        @Override
        public Exception getError() {
            return error;
        }
    }
}