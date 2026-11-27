package hu.unideb.inf.core.exception;

public class ForbiddenOperationException extends WmsBusinessException {

    public static final String ERROR_CODE = "forbidden-operation";
    public static final String TITLE = "Forbidden Operation";

    public ForbiddenOperationException(String message) {
        super(ERROR_CODE, TITLE, 403, message);
    }
}
