package com.acooldog.toolbox.config;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.acooldog.toolbox.share.domain.model.UsageTipDetail;
import com.acooldog.toolbox.share.domain.model.UsageTipSummary;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class TipsCacheStore {
    private static final String PREFS_NAME = "tips_cache_store";
    private static final String KEY_LIST = "tips_list";
    private static final String KEY_DETAIL_PREFIX = "tips_detail_";

    private final SharedPreferences preferences;

    public TipsCacheStore(Context context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveList(List<UsageTipSummary> items) {
        JSONArray array = new JSONArray();
        if (items != null) {
            for (UsageTipSummary item : items) {
                if (item == null) {
                    continue;
                }
                JSONObject object = new JSONObject();
                try {
                    object.put("id", item.getId());
                    object.put("title", item.getTitle());
                    object.put("excerpt", item.getExcerpt());
                    object.put("contributorQq", item.getContributorQq());
                    object.put("authorUsername", item.getAuthorUsername());
                    object.put("published", item.isPublished());
                    object.put("editable", item.isEditable());
                    object.put("createdAt", item.getCreatedAt());
                    object.put("updatedAt", item.getUpdatedAt());
                    array.put(object);
                } catch (Exception ignored) {
                }
            }
        }
        preferences.edit().putString(KEY_LIST, array.toString()).apply();
    }

    public List<UsageTipSummary> getList() {
        List<UsageTipSummary> results = new ArrayList<>();
        String raw = preferences.getString(KEY_LIST, "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.optJSONObject(i);
                if (object == null) {
                    continue;
                }
                results.add(new UsageTipSummary(
                        object.optString("id", ""),
                        object.optString("title", ""),
                        object.optString("excerpt", ""),
                        object.optString("contributorQq", ""),
                        object.optString("authorUsername", ""),
                        object.optBoolean("published", true),
                        object.optBoolean("editable", false),
                        object.optLong("createdAt", 0L),
                        object.optLong("updatedAt", 0L)
                ));
            }
        } catch (Exception ignored) {
        }
        return results;
    }

    public void saveDetail(UsageTipDetail detail) {
        if (detail == null || TextUtils.isEmpty(detail.getId())) {
            return;
        }
        JSONObject object = new JSONObject();
        try {
            object.put("id", detail.getId());
            object.put("title", detail.getTitle());
            object.put("htmlContent", detail.getHtmlContent());
            object.put("plainText", detail.getPlainText());
            object.put("contributorQq", detail.getContributorQq());
            object.put("authorAccountId", detail.getAuthorAccountId());
            object.put("authorUsername", detail.getAuthorUsername());
            object.put("published", detail.isPublished());
            object.put("editable", detail.isEditable());
            object.put("createdAt", detail.getCreatedAt());
            object.put("updatedAt", detail.getUpdatedAt());
            preferences.edit().putString(KEY_DETAIL_PREFIX + detail.getId(), object.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    public void upsertSummary(UsageTipDetail detail) {
        if (detail == null || TextUtils.isEmpty(detail.getId())) {
            return;
        }
        List<UsageTipSummary> items = getList();
        UsageTipSummary summary = new UsageTipSummary(
                detail.getId(),
                detail.getTitle(),
                buildExcerpt(detail.getPlainText()),
                detail.getContributorQq(),
                detail.getAuthorUsername(),
                detail.isPublished(),
                detail.isEditable(),
                detail.getCreatedAt(),
                detail.getUpdatedAt()
        );
        boolean replaced = false;
        for (int index = 0; index < items.size(); index++) {
            UsageTipSummary item = items.get(index);
            if (item != null && detail.getId().equals(item.getId())) {
                items.set(index, summary);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            items.add(0, summary);
        }
        saveList(items);
    }

    public void removeTip(String tipId) {
        if (TextUtils.isEmpty(tipId)) {
            return;
        }
        List<UsageTipSummary> items = getList();
        for (int index = items.size() - 1; index >= 0; index--) {
            UsageTipSummary item = items.get(index);
            if (item != null && tipId.equals(item.getId())) {
                items.remove(index);
            }
        }
        preferences.edit().remove(KEY_DETAIL_PREFIX + tipId).apply();
        saveList(items);
    }

    public UsageTipDetail getDetail(String tipId) {
        if (TextUtils.isEmpty(tipId)) {
            return null;
        }
        String raw = preferences.getString(KEY_DETAIL_PREFIX + tipId, "");
        if (TextUtils.isEmpty(raw)) {
            return null;
        }
        try {
            JSONObject object = new JSONObject(raw);
            return new UsageTipDetail(
                    object.optString("id", ""),
                    object.optString("title", ""),
                    object.optString("htmlContent", ""),
                    object.optString("plainText", ""),
                    object.optString("contributorQq", ""),
                    object.optString("authorAccountId", ""),
                    object.optString("authorUsername", ""),
                    object.optBoolean("published", true),
                    object.optBoolean("editable", false),
                    object.optLong("createdAt", 0L),
                    object.optLong("updatedAt", 0L)
            );
        } catch (Exception ignored) {
            return null;
        }
    }

    private String buildExcerpt(String plainText) {
        String normalized = plainText == null ? "" : plainText.trim();
        if (normalized.length() <= 120) {
            return normalized;
        }
        return normalized.substring(0, 120) + "...";
    }
}
