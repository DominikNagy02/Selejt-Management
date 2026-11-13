package hu.unideb.inf.core.exception;

import java.util.Objects;

public class ResourceNotFoundException extends WmsBusinessException {

    public static final String ERROR_CODE = "resource-not-found";
    public static final String TITLE = "Resource Not Found";

    private final String resourceType;
    private final String identifier;

    public ResourceNotFoundException(String resourceType, String identifier) {
        super(
                ERROR_CODE,
                TITLE,
                404,
                buildMessage(resourceType, identifier)
        );
        this.resourceType = resourceType;
        this.identifier = identifier;
    }

    public static ResourceNotFoundException of(String resourceType, Object identifier) {
        Objects.requireNonNull(identifier, "identifier must not be null");
        return new ResourceNotFoundException(resourceType, String.valueOf(identifier));
    }

    private static String buildMessage(String resourceType, String identifier) {
        Objects.requireNonNull(resourceType, "resourceType must not be null");
        Objects.requireNonNull(identifier, "identifier must not be null");
        return String.format(
                "A keresett %s erőforrás nem található (azonosító: %s).",
                resourceType, identifier
        );
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getIdentifier() {
        return identifier;
    }
}

