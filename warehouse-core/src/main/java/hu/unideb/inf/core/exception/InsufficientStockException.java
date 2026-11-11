package hu.unideb.inf.core.exception;

public class InsufficientStockException extends RuntimeException {

    private final String productSku;
    private final int requestedQuantity;
    private final int availableQuantity;

    public InsufficientStockException(int requestedQuantity, int availableQuantity) {
        this(null, requestedQuantity, availableQuantity);
    }

    public InsufficientStockException(String productSku, int requestedQuantity, int availableQuantity) {
        super(buildMessage(productSku, requestedQuantity, availableQuantity));
        this.productSku = productSku;
        this.requestedQuantity = requestedQuantity;
        this.availableQuantity = availableQuantity;
    }

    private static String buildMessage(String productSku, int requestedQuantity, int availableQuantity) {
        if (productSku != null && !productSku.isBlank()) {
            return String.format(
                    "Nincs elegendő elérhető készlet a termékből (%s). Kért mennyiség: %d, elérhető: %d.",
                    productSku, requestedQuantity, availableQuantity
            );
        }
        return String.format(
                "Nincs elegendő elérhető készlet. Kért mennyiség: %d, elérhető: %d.",
                requestedQuantity, availableQuantity
        );
    }

    public String getProductSku() {
        return productSku;
    }

    public int getRequestedQuantity() {
        return requestedQuantity;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }
}

