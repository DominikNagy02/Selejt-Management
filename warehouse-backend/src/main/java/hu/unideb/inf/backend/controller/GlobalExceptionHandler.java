package hu.unideb.inf.backend.controller;

import hu.unideb.inf.core.exception.ShelfCapacityExceededException;
import hu.unideb.inf.core.exception.WmsBusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@RestControllerAdvice
public class GlobalExceptionHandler {

    public static final String ERROR_URI_PREFIX = "https://smartwms.hu/errors/";
    public static final String VALIDATION_ERROR_CODE = "validation-failed";
    public static final String VALIDATION_ERROR_TITLE = "Validation Failed";
    public static final String VALIDATION_ERROR_DETAIL = "A kérés adatai érvénytelenek.";
    public static final String MALFORMED_REQUEST_CODE = "malformed-request";
    public static final String MALFORMED_REQUEST_TITLE = "Malformed Request";
    public static final String MALFORMED_REQUEST_DETAIL = "A kérés törzse nem olvasható vagy hibás JSON formátumú.";
    public static final String INTERNAL_ERROR_CODE = "internal-error";
    public static final String INTERNAL_ERROR_TITLE = "Internal Server Error";
    public static final String INTERNAL_ERROR_DETAIL = "Váratlan szerverhiba történt.";

    private final Clock clock;

    public GlobalExceptionHandler(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @ExceptionHandler(ShelfCapacityExceededException.class)
    public ProblemDetail handleShelfCapacityExceeded(
            ShelfCapacityExceededException ex,
            HttpServletRequest request
    ) {
        ProblemDetail pd = handleBusinessException(ex, request);
        pd.setProperty("shelfCode", ex.getShelfCode());
        pd.setProperty("violations", ex.getViolations());
        return pd;
    }

    @ExceptionHandler(WmsBusinessException.class)
    public ProblemDetail handleBusinessException(
            WmsBusinessException ex,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.valueOf(ex.getHttpStatus());
        return problem(status, ex.getErrorCode(), ex.getTitle(), ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationErrors(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(
                    fieldError.getField(),
                    fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "Invalid value"
            );
        }

        ProblemDetail pd = problem(
                HttpStatus.BAD_REQUEST,
                VALIDATION_ERROR_CODE,
                VALIDATION_ERROR_TITLE,
                VALIDATION_ERROR_DETAIL,
                request
        );
        pd.setProperty("errors", errors);
        return pd;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleMalformedRequest(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.BAD_REQUEST,
                MALFORMED_REQUEST_CODE,
                MALFORMED_REQUEST_TITLE,
                MALFORMED_REQUEST_DETAIL,
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpectedException(
            Exception ex,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                INTERNAL_ERROR_CODE,
                INTERNAL_ERROR_TITLE,
                INTERNAL_ERROR_DETAIL,
                request
        );
    }

    private ProblemDetail problem(
            HttpStatus status,
            String code,
            String title,
            String detail,
            HttpServletRequest request
    ) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setType(URI.create(ERROR_URI_PREFIX + code));
        pd.setTitle(title);
        pd.setInstance(URI.create(request.getRequestURI()));
        pd.setProperty("timestamp", Instant.now(clock));
        return pd;
    }
}
