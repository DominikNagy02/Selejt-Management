package hu.unideb.inf.backend.controller;

import hu.unideb.inf.core.exception.ShelfCapacityExceededException;
import hu.unideb.inf.core.exception.WmsBusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
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

    private final Clock clock;

    public GlobalExceptionHandler() {
        this(Clock.systemUTC());
    }

    @Autowired
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

