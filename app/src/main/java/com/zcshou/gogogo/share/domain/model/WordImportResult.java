package com.acooldog.toolbox.share.domain.model;

public final class WordImportResult {
    private final String htmlContent;
    private final String plainText;

    public WordImportResult(String htmlContent, String plainText) {
        this.htmlContent = htmlContent == null ? "" : htmlContent.trim();
        this.plainText = plainText == null ? "" : plainText.trim();
    }

    public String getHtmlContent() {
        return htmlContent;
    }

    public String getPlainText() {
        return plainText;
    }
}
