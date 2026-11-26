package hu.unideb.inf.core.exception;

import java.util.Objects;

public class DuplicateResourceException extends WmsBusinessException {

    public static final String ERROR_CODE = "duplicate-resource";
    public static final String TITLE = "Duplicate Resource";

    private final String resourceType;
    private final String field;
    private final String value;

    public DuplicateResourceException(String resourceType, String field, String value) {
        super(
                ERROR_CODE,
                TITLE,
                409,
                buildMessage(resourceType, field, value)
        );
        this.resourceType = resourceType;
        this.field = field;
        this.value = value;
    }

    public static DuplicateResourceException of(String resourceType, String field, Object value) {
        Objects.requireNonNull(value, "value must not be null");
        return new DuplicateResourceException(resourceType, field, String.valueOf(value));
    }

    private static String buildMessage(String resourceType, String field, String value) {
        Objects.requireNonNull(resourceType, "resourceType must not be null");
        Objects.requireNonNull(field, "field must not be null");
        Objects.requireNonNull(value, "value must not be null");
        return String.format(
                "A(z) %s erőforrás már létezik (%s: %s).",
                resourceType, field, value
        );
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getField() {
        return field;
    }

    public String getValue() {
        return value;
    }
}
