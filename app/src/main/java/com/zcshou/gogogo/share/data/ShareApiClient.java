package com.zcshou.gogogo.share.data;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.zcshou.gogogo.BuildConfig;
import com.zcshou.gogogo.nfc.domain.NfcPayload;
import com.zcshou.gogogo.route.data.RoutePointJsonCodec;
import com.zcshou.gogogo.route.domain.model.RoutePoint;
import com.zcshou.gogogo.share.domain.model.SharedNfcEntry;
import com.zcshou.gogogo.share.domain.model.SharedRoutePayload;
import com.zcshou.gogogo.share.domain.model.SharedRouteSummary;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public final class ShareApiClient {
    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient okHttpClient;
    private final HttpUrl baseUrl;

    public ShareApiClient() throws IOException {
        this(BuildConfig.SHARE_API_BASE_URL);
    }

    ShareApiClient(String configuredBaseUrl) throws IOException {
        okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(15L, TimeUnit.SECONDS)
                .readTimeout(20L, TimeUnit.SECONDS)
                .writeTimeout(20L, TimeUnit.SECONDS)
                .build();
        baseUrl = parseBaseUrl(configuredBaseUrl);
    }

    public SharedRoutePayload uploadRoute(String name, boolean privacyMode, List<RoutePoint> points) throws IOException {
        JSONObject bodyJson = new JSONObject();
        try {
            bodyJson.put("name", name);
            bodyJson.put("privacyMode", privacyMode);
            JSONArray pointsArray = new JSONArray();
            for (RoutePoint point : points) {
                pointsArray.put(RoutePointJsonCodec.encode(point));
            }
            bodyJson.put("points", pointsArray);
        } catch (JSONException exception) {
            throw new IOException("Unable to encode shared route request", exception);
        }

        Request request = new Request.Builder()
                .url(buildUrl("api/shared/routes"))
                .post(RequestBody.create(bodyJson.toString(), JSON_MEDIA_TYPE))
                .build();
        return parseSharedRoute(executeObject(request));
    }

    public List<SharedRouteSummary> getSharedRoutes() throws IOException {
        Request request = new Request.Builder()
                .url(buildUrl("api/shared/routes"))
                .get()
                .build();
        JSONArray items = executeArray(request);
        List<SharedRouteSummary> routes = new ArrayList<>();
        for (int index = 0; index < items.length(); index++) {
            routes.add(parseSharedRouteSummary(items.optJSONObject(index)));
        }
        return routes;
    }

    public SharedRoutePayload getSharedRoute(String shareId) throws IOException {
        if (TextUtils.isEmpty(shareId)) {
            throw new IOException("Missing shared route id");
        }
        Request request = new Request.Builder()
                .url(buildUrl("api/shared/routes/" + shareId))
                .get()
                .build();
        return parseSharedRoute(executeObject(request));
    }

    public SharedNfcEntry uploadNfc(String name, NfcPayload payload) throws IOException {
        JSONObject bodyJson = new JSONObject();
        try {
            bodyJson.put("name", name);
            bodyJson.put("url", payload.getUrl());
            bodyJson.put("packageName", payload.getPackageName());
            bodyJson.put("source", payload.getSource());
        } catch (JSONException exception) {
            throw new IOException("Unable to encode shared NFC request", exception);
        }

        Request request = new Request.Builder()
                .url(buildUrl("api/shared/nfc"))
                .post(RequestBody.create(bodyJson.toString(), JSON_MEDIA_TYPE))
                .build();
        return parseSharedNfc(executeObject(request));
    }

    public List<SharedNfcEntry> getSharedNfcEntries() throws IOException {
        Request request = new Request.Builder()
                .url(buildUrl("api/shared/nfc"))
                .get()
                .build();
        JSONArray items = executeArray(request);
        List<SharedNfcEntry> entries = new ArrayList<>();
        for (int index = 0; index < items.length(); index++) {
            entries.add(parseSharedNfc(items.optJSONObject(index)));
        }
        return entries;
    }

    private HttpUrl parseBaseUrl(String configuredBaseUrl) throws IOException {
        if (configuredBaseUrl == null || configuredBaseUrl.trim().isEmpty()) {
            return null;
        }
        String normalized = configuredBaseUrl.trim();
        if (!normalized.endsWith("/")) {
            normalized = normalized + "/";
        }
        try {
            return HttpUrl.get(normalized);
        } catch (IllegalArgumentException exception) {
            throw new IOException("Invalid SHARE_API_BASE_URL", exception);
        }
    }

    private HttpUrl buildUrl(String relativePath) throws IOException {
        if (baseUrl == null) {
            throw new IOException("请先配置 SHARE_API_BASE_URL");
        }
        HttpUrl url = baseUrl.resolve(relativePath);
        if (url == null) {
            throw new IOException("Unable to resolve share API path: " + relativePath);
        }
        return url;
    }

    private JSONArray executeArray(Request request) throws IOException {
        String responseText = executeRequest(request);
        try {
            String trimmed = responseText.trim();
            if (trimmed.startsWith("[")) {
                return new JSONArray(trimmed);
            }
            JSONObject root = new JSONObject(trimmed);
            JSONArray items = root.optJSONArray("items");
            if (items != null) {
                return items;
            }
            items = root.optJSONArray("list");
            if (items != null) {
                return items;
            }
            JSONObject data = root.optJSONObject("data");
            if (data != null) {
                items = data.optJSONArray("items");
                if (items != null) {
                    return items;
                }
                items = data.optJSONArray("list");
                if (items != null) {
                    return items;
                }
            }
            throw new IOException("Shared API response does not contain an items array");
        } catch (JSONException exception) {
            throw new IOException("Unable to parse shared API array response", exception);
        }
    }

    private JSONObject executeObject(Request request) throws IOException {
        String responseText = executeRequest(request);
        try {
            JSONObject root = new JSONObject(responseText);
            JSONObject item = root.optJSONObject("item");
            if (item != null) {
                return item;
            }
            JSONObject data = root.optJSONObject("data");
            if (data != null) {
                JSONObject nestedItem = data.optJSONObject("item");
                if (nestedItem != null) {
                    return nestedItem;
                }
                JSONObject nestedRoute = data.optJSONObject("route");
                if (nestedRoute != null) {
                    return nestedRoute;
                }
                JSONObject nestedNfc = data.optJSONObject("nfc");
                if (nestedNfc != null) {
                    return nestedNfc;
                }
                if (data.has("id") || data.has("name") || data.has("points")) {
                    return data;
                }
            }
            JSONObject route = root.optJSONObject("route");
            if (route != null) {
                return route;
            }
            JSONObject nfc = root.optJSONObject("nfc");
            if (nfc != null) {
                return nfc;
            }
            return root;
        } catch (JSONException exception) {
            throw new IOException("Unable to parse shared API object response", exception);
        }
    }

    private String executeRequest(Request request) throws IOException {
        try (Response response = okHttpClient.newCall(request).execute()) {
            ResponseBody responseBody = response.body();
            String responseText = responseBody == null ? "" : responseBody.string();
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code() + ": " + responseText);
            }
            return responseText;
        }
    }

    private SharedRouteSummary parseSharedRouteSummary(JSONObject jsonObject) throws IOException {
        if (jsonObject == null) {
            throw new IOException("Shared route item is empty");
        }
        String id = optString(jsonObject, "id", "shareId", "routeId");
        String name = optString(jsonObject, "name", "title");
        boolean privacyMode = optBoolean(jsonObject, "privacyMode", "privacy");
        int pointCount = optInt(jsonObject, "pointCount", "pointsCount");
        JSONArray points = jsonObject.optJSONArray("points");
        if (pointCount <= 0 && points != null) {
            pointCount = points.length();
        }
        return new SharedRouteSummary(id, name, privacyMode, pointCount, optLong(jsonObject, "createdAt", "createTime"));
    }

    private SharedRoutePayload parseSharedRoute(JSONObject jsonObject) throws IOException {
        if (jsonObject == null) {
            throw new IOException("Shared route payload is empty");
        }
        JSONArray pointArray = jsonObject.optJSONArray("points");
        if (pointArray == null) {
            throw new IOException("Shared route points are missing");
        }
        List<RoutePoint> points = new ArrayList<>();
        for (int index = 0; index < pointArray.length(); index++) {
            try {
                points.add(RoutePointJsonCodec.decode(pointArray.getJSONObject(index)));
            } catch (JSONException exception) {
                throw new IOException("Unable to decode shared route point", exception);
            }
        }
        return new SharedRoutePayload(
                optString(jsonObject, "id", "shareId", "routeId"),
                optString(jsonObject, "name", "title"),
                optBoolean(jsonObject, "privacyMode", "privacy"),
                optLong(jsonObject, "createdAt", "createTime"),
                points
        );
    }

    private SharedNfcEntry parseSharedNfc(JSONObject jsonObject) throws IOException {
        if (jsonObject == null) {
            throw new IOException("Shared NFC payload is empty");
        }
        return new SharedNfcEntry(
                optString(jsonObject, "id", "shareId", "nfcId"),
                optString(jsonObject, "name", "title"),
                optString(jsonObject, "url", "uri", "link"),
                optString(jsonObject, "packageName", "package", "packageId"),
                optString(jsonObject, "source", "from"),
                optLong(jsonObject, "createdAt", "createTime")
        );
    }

    private String optString(@NonNull JSONObject jsonObject, String... keys) {
        for (String key : keys) {
            if (jsonObject.has(key)) {
                String value = jsonObject.optString(key, "");
                if (!TextUtils.isEmpty(value)) {
                    return value;
                }
            }
        }
        return "";
    }

    private boolean optBoolean(@NonNull JSONObject jsonObject, String... keys) {
        for (String key : keys) {
            if (jsonObject.has(key)) {
                Object value = jsonObject.opt(key);
                if (value instanceof Boolean) {
                    return (Boolean) value;
                }
                if (value instanceof Number) {
                    return ((Number) value).intValue() != 0;
                }
                if (value instanceof String) {
                    return Boolean.parseBoolean((String) value) || "1".equals(value);
                }
            }
        }
        return false;
    }

    private int optInt(@NonNull JSONObject jsonObject, String... keys) {
        for (String key : keys) {
            if (jsonObject.has(key)) {
                return jsonObject.optInt(key, 0);
            }
        }
        return 0;
    }

    private long optLong(@NonNull JSONObject jsonObject, String... keys) {
        for (String key : keys) {
            if (jsonObject.has(key)) {
                Object value = jsonObject.opt(key);
                if (value instanceof Number) {
                    return ((Number) value).longValue();
                }
                if (value instanceof String) {
                    try {
                        return Long.parseLong((String) value);
                    } catch (NumberFormatException ignored) {
                        return 0L;
                    }
                }
            }
        }
        return 0L;
    }
}
