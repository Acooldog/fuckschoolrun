package com.acooldog.toolbox.share.domain.model;

public final class StartupDialogConfig {
    private final String id;
    private final int displayOrder;
    private final String level;
    private final int cycleIndex;
    private final String title;
    private final String message;
    private final String confirmText;
    private final long updatedAt;

    public StartupDialogConfig(
            String id,
            int displayOrder,
            String level,
            int cycleIndex,
            String title,
            String message,
            String confirmText,
            long updatedAt
    ) {
        this.id = normalize(id);
        this.displayOrder = displayOrder;
        this.level = normalize(level);
        this.cycleIndex = cycleIndex;
        this.title = normalize(title);
        this.message = normalize(message);
        this.confirmText = normalize(confirmText);
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public String getLevel() {
        return level;
    }

    public int getCycleIndex() {
        return cycleIndex;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getConfirmText() {
        return confirmText.isEmpty() ? "知道了" : confirmText;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public boolean isPrimary() {
        return "primary".equalsIgnoreCase(level);
    }

    public boolean isSecondary() {
        return "secondary".equalsIgnoreCase(level);
    }

    public String getDisplayText() {
        if (title.isEmpty()) {
            return message;
        }
        if (message.isEmpty()) {
            return title;
        }
        return title + "\n\n" + message;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
