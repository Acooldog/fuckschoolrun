package com.acooldog.toolbox.share.domain.model;

public final class FsrBindingState {
    private final String status;
    private final String token;
    private final String command;
    private final String jobId;
    private final int queuePosition;
    private final String message;
    private final String qqNumber;
    private final String qqDisplayName;
    private final String groupId;

    public FsrBindingState(
            String status,
            String token,
            String command,
            String jobId,
            int queuePosition,
            String message,
            String qqNumber,
            String qqDisplayName,
            String groupId
    ) {
        this.status = normalize(status);
        this.token = normalize(token);
        this.command = normalize(command);
        this.jobId = normalize(jobId);
        this.queuePosition = Math.max(0, queuePosition);
        this.message = normalize(message);
        this.qqNumber = normalize(qqNumber);
        this.qqDisplayName = normalize(qqDisplayName);
        this.groupId = normalize(groupId);
    }

    public String getStatus() {
        return status;
    }

    public String getToken() {
        return token;
    }

    public String getCommand() {
        return command;
    }

    public String getJobId() {
        return jobId;
    }

    public int getQueuePosition() {
        return queuePosition;
    }

    public String getMessage() {
        return message;
    }

    public String getQqNumber() {
        return qqNumber;
    }

    public String getQqDisplayName() {
        return qqDisplayName;
    }

    public String getGroupId() {
        return groupId;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
