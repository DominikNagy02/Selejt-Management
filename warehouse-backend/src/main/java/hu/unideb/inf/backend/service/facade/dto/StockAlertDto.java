package hu.unideb.inf.backend.service.facade.dto;

public record StockAlertDto(
        String title,
        String badgeText,
        String badgeSeverity,
        String location,
        String actionLabel
) {
    public String getTitle() {
        return title;
    }

    public String getBadgeText() {
        return badgeText;
    }

    public String getBadgeSeverity() {
        return badgeSeverity;
    }

    public String getLocation() {
        return location;
    }

    public String getActionLabel() {
        return actionLabel;
    }
}
