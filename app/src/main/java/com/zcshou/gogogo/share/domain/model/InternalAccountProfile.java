package com.acooldog.toolbox.share.domain.model;

public final class InternalAccountProfile {
    public static final String TYPE_NONE = "";
    public static final String TYPE_ORDINARY = "ordinary";
    public static final String TYPE_ADVANCED = "advanced";
    public static final String TYPE_DONOR = "donor";
    public static final String TYPE_PIONEER = "pioneer";

    private final String id;
    private final String username;
    private final String remark;
    private final String testerType;
    private final String testerTypeLabel;
    private final String status;

    public InternalAccountProfile(String id, String username, String remark, String status) {
        this(id, username, remark, "", "", status);
    }

    public InternalAccountProfile(
            String id,
            String username,
            String remark,
            String testerType,
            String testerTypeLabel,
            String status
    ) {
        this.id = id == null ? "" : id.trim();
        this.username = username == null ? "" : username.trim();
        this.remark = remark == null ? "" : remark.trim();
        this.testerType = normalizeTesterType(testerType);
        this.testerTypeLabel = resolveTesterTypeLabel(this.testerType, testerTypeLabel);
        this.status = status == null ? "" : status.trim();
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getRemark() {
        return remark;
    }

    public String getTesterType() {
        return testerType;
    }

    public String getTesterTypeLabel() {
        return testerTypeLabel;
    }

    public String getStatus() {
        return status;
    }

    public boolean isActive() {
        return "active".equalsIgnoreCase(status);
    }

    public boolean hasTesterType() {
        return !testerType.isEmpty();
    }

    private static String normalizeTesterType(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase();
        switch (normalized) {
            case TYPE_ADVANCED:
            case TYPE_DONOR:
            case TYPE_PIONEER:
            case TYPE_ORDINARY:
                return normalized;
            default:
                return TYPE_NONE;
        }
    }

    private static String resolveTesterTypeLabel(String type, String backendLabel) {
        if (!isBlank(backendLabel)) {
            return backendLabel.trim();
        }
        switch (type) {
            case TYPE_ADVANCED:
                return "高级测试账号";
            case TYPE_DONOR:
                return "贡献者账号";
            case TYPE_PIONEER:
                return "先锋测试账号";
            case TYPE_ORDINARY:
                return "普通测试账号";
            default:
                return "未分类账号";
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
