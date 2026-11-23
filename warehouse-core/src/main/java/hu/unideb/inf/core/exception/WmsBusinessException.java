package hu.unideb.inf.core.exception;

import java.util.Objects;

public abstract class WmsBusinessException extends RuntimeException {

    private final String errorCode;
    private final String title;
    private final int httpStatus;

    protected WmsBusinessException(
            String errorCode,
            String title,
            int httpStatus,
            String message
    ) {
        super(Objects.requireNonNull(message, "message must not be null"));
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
        this.title = Objects.requireNonNull(title, "title must not be null");
        if (httpStatus < 100 || httpStatus > 599) {
            throw new IllegalArgumentException("httpStatus must be a valid HTTP status code");
        }
        this.httpStatus = httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getTitle() {
        return title;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}

