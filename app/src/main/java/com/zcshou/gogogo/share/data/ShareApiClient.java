package com.acooldog.toolbox.share.data;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.acooldog.toolbox.BuildConfig;
import com.acooldog.toolbox.config.InternalAuthStore;
import com.acooldog.toolbox.nfc.domain.NfcPayload;
import com.acooldog.toolbox.route.domain.model.RouteSimulationConfig;
import com.acooldog.toolbox.route.data.RoutePointJsonCodec;
import com.acooldog.toolbox.route.domain.model.RoutePoint;
import com.acooldog.toolbox.share.domain.model.AppClientConfig;
import com.acooldog.toolbox.share.domain.model.EmailVerificationState;
import com.acooldog.toolbox.share.domain.model.FsrBindingState;
import com.acooldog.toolbox.share.domain.model.InternalAccountProfile;
import com.acooldog.toolbox.share.domain.model.InternalLoginResult;
import com.acooldog.toolbox.share.domain.model.MapSearchResult;
import com.acooldog.toolbox.share.domain.model.SharedNfcEntry;
import com.acooldog.toolbox.share.domain.model.SharedRoutePayload;
import com.acooldog.toolbox.share.domain.model.SharedRouteSummary;
import com.acooldog.toolbox.share.domain.model.SharedSimulationConfigEntry;
import com.acooldog.toolbox.share.domain.model.UsageTipDetail;
import com.acooldog.toolbox.share.domain.model.UsageTipSummary;
import com.acooldog.toolbox.share.domain.model.WordImportResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public final class ShareApiClient {
    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");
    private static final String CLIENT_VARIANT = "fuckschoolrun";
    private static final String PUBLIC_API_HOST = "www.daoges.cn";
    private static final String PUBLIC_API_PATH = "/schoolrun-api";

    private final OkHttpClient okHttpClient;
    private final HttpUrl baseUrl;
    private final Context applicationContext;

    public ShareApiClient() throws IOException {
        this(null, BuildConfig.SHARE_API_BASE_URL);
    }

    public ShareApiClient(Context context) throws IOException {
        this(context, BuildConfig.SHARE_API_BASE_URL);
    }

    ShareApiClient(String configuredBaseUrl) throws IOException {
        this(null, configuredBaseUrl);
    }

    ShareApiClient(Context context, String configuredBaseUrl) throws IOException {
        applicationContext = context == null ? null : context.getApplicationContext();
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

        Request request = clientRequestBuilder(buildUrl("api/shared/routes"))
                .post(RequestBody.create(bodyJson.toString(), JSON_MEDIA_TYPE))
                .build();
        return parseSharedRoute(executeObject(request));
    }

    public List<SharedRouteSummary> getSharedRoutes() throws IOException {
        Request request = clientRequestBuilder(buildUrl("api/shared/routes"))
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
        Request request = clientRequestBuilder(buildUrl("api/shared/routes/" + shareId))
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

        Request request = clientRequestBuilder(buildUrl("api/shared/nfc"))
                .post(RequestBody.create(bodyJson.toString(), JSON_MEDIA_TYPE))
                .build();
        return parseSharedNfc(executeObject(request));
    }

    public List<SharedNfcEntry> getSharedNfcEntries() throws IOException {
        Request request = clientRequestBuilder(buildUrl("api/shared/nfc"))
                .get()
                .build();
        JSONArray items = executeArray(request);
        List<SharedNfcEntry> entries = new ArrayList<>();
        for (int index = 0; index < items.length(); index++) {
            entries.add(parseSharedNfc(items.optJSONObject(index)));
        }
        return entries;
    }

    public AppClientConfig getAppClientConfig() throws IOException {
        Request request = clientRequestBuilder(buildUrl("api/client-config"))
                .get()
                .build();
        return parseAppClientConfig(executeObject(request));
    }

    public InternalLoginResult loginInternalAccount(
            String username,
            String password,
            String deviceId,
            String deviceName
    ) throws IOException {
        JSONObject bodyJson = new JSONObject();
        try {
            bodyJson.put("username", username);
            bodyJson.put("password", password);
            bodyJson.put("deviceId", deviceId);
            bodyJson.put("deviceName", deviceName);
            bodyJson.put("appVariant", CLIENT_VARIANT);
        } catch (JSONException exception) {
            throw new IOException("Unable to encode auth login request", exception);
        }

        Request request = clientRequestBuilder(buildUrl("api/auth/login"))
                .post(RequestBody.create(bodyJson.toString(), JSON_MEDIA_TYPE))
                .build();
        String responseText = executeRequest(request);
        try {
            JSONObject root = new JSONObject(responseText);
            return new InternalLoginResult(
                    root.optString("token", ""),
                    parseInternalAccount(root.optJSONObject("account")),
                    parseFsrBinding(root.optJSONObject("fsrBinding"))
            );
        } catch (JSONException exception) {
            throw new IOException("Unable to parse auth login response", exception);
        }
    }

    public EmailVerificationState getEmailRegistrationStatus(String email) throws IOException {
        HttpUrl url = buildUrl("api/auth/email/register/status").newBuilder()
                .addQueryParameter("email", email == null ? "" : email.trim())
                .build();
        JSONObject root = executeObject(clientRequestBuilder(url).get().build());
        return new EmailVerificationState(
                false,
                root.optBoolean("canSend", true),
                root.optInt("cooldownSeconds", 0),
                root.optLong("expiresAt", 0L)
        );
    }

    public EmailVerificationState sendEmailRegistrationCode(String email) throws IOException {
        JSONObject bodyJson = new JSONObject();
        try {
            bodyJson.put("email", email == null ? "" : email.trim());
            bodyJson.put("appVariant", CLIENT_VARIANT);
        } catch (JSONException exception) {
            throw new IOException("Unable to encode email verification request", exception);
        }
        Request request = clientRequestBuilder(buildUrl("api/auth/email/register/send-code"))
                .post(RequestBody.create(bodyJson.toString(), JSON_MEDIA_TYPE))
                .build();
        JSONObject root = executeObject(request);
        return new EmailVerificationState(
                root.optBoolean("accepted", false),
                root.optBoolean("accepted", false),
                root.optInt("cooldownSeconds", 0),
                root.optLong("expiresAt", 0L)
        );
    }

    public InternalLoginResult registerByEmail(
            String email,
            String code,
            String password,
            String deviceId,
            String deviceName
    ) throws IOException {
        JSONObject bodyJson = new JSONObject();
        try {
            bodyJson.put("email", email == null ? "" : email.trim());
            bodyJson.put("code", code == null ? "" : code.trim());
            bodyJson.put("password", password == null ? "" : password);
            bodyJson.put("deviceId", deviceId == null ? "" : deviceId);
            bodyJson.put("deviceName", deviceName == null ? "" : deviceName);
            bodyJson.put("appVariant", CLIENT_VARIANT);
        } catch (JSONException exception) {
            throw new IOException("Unable to encode email registration request", exception);
        }
        Request request = clientRequestBuilder(buildUrl("api/auth/email/register"))
                .post(RequestBody.create(bodyJson.toString(), JSON_MEDIA_TYPE))
                .build();
        JSONObject root = executeObject(request);
        return new InternalLoginResult(
                root.optString("token", ""),
                parseInternalAccount(root.optJSONObject("account")),
                parseFsrBinding(root.optJSONObject("fsrBinding"))
        );
    }

    public FsrBindingState getFsrBindingStatus(String token, boolean reveal) throws IOException {
        if (TextUtils.isEmpty(token)) {
            throw new IOException("Missing auth token");
        }
        HttpUrl url = buildUrl("api/fsr/binding/status").newBuilder()
                .addQueryParameter("reveal", reveal ? "true" : "false")
                .build();
        JSONObject root = executeObject(newRequestBuilder(url, token).get().build());
        return parseFsrBinding(root);
    }

    public List<MapSearchResult> searchMap(String keyword, String city, String token) throws IOException {
        HttpUrl url = buildUrl("api/map/search").newBuilder()
                .addQueryParameter("provider", "baidu")
                .addQueryParameter("keyword", keyword == null ? "" : keyword.trim())
                .addQueryParameter("city", city == null ? "" : city.trim())
                .addQueryParameter("limit", "20")
                .build();
        JSONArray items = executeArray(newRequestBuilder(url, token).get().build());
        List<MapSearchResult> results = new ArrayList<>();
        for (int index = 0; index < items.length(); index++) {
            JSONObject item = items.optJSONObject(index);
            if (item == null) {
                continue;
            }
            results.add(new MapSearchResult(
                    optString(item, "name"),
                    optString(item, "address"),
                    item.optDouble("latitude", 0d),
                    item.optDouble("longitude", 0d)
            ));
        }
        return results;
    }

    public InternalAccountProfile getInternalAccountProfile(String token) throws IOException {
        if (TextUtils.isEmpty(token)) {
            throw new IOException("Missing auth token");
        }
        Request request = newRequestBuilder(buildUrl("api/auth/me"), token)
                .get()
                .build();
        JSONObject root = executeObject(request);
        JSONObject account = root.optJSONObject("account");
        if (account != null) {
            return parseInternalAccount(account);
        }
        return parseInternalAccount(root);
    }

    public List<UsageTipSummary> getUsageTips(String query, int page, int pageSize, String token) throws IOException {
        HttpUrl url = buildUrl("api/tips").newBuilder()
                .addQueryParameter("q", query == null ? "" : query)
                .addQueryParameter("page", String.valueOf(Math.max(1, page)))
                .addQueryParameter("pageSize", String.valueOf(Math.max(1, pageSize)))
                .build();
        Request request = newRequestBuilder(url, token).get().build();
        JSONArray items = executeArray(request);
        List<UsageTipSummary> results = new ArrayList<>();
        for (int index = 0; index < items.length(); index++) {
            results.add(parseUsageTipSummary(items.optJSONObject(index)));
        }
        return results;
    }

    public UsageTipDetail getUsageTip(String tipId, String token) throws IOException {
        if (TextUtils.isEmpty(tipId)) {
            throw new IOException("Missing usage tip id");
        }
        Request request = newRequestBuilder(buildUrl("api/tips/" + tipId), token).get().build();
        return parseUsageTipDetail(executeObject(request));
    }

    public UsageTipDetail saveUsageTip(
            String tipId,
            String token,
            String title,
            String htmlContent,
            String contributorQq,
            boolean published
    ) throws IOException {
        JSONObject bodyJson = new JSONObject();
        try {
            bodyJson.put("title", title);
            bodyJson.put("htmlContent", htmlContent);
            bodyJson.put("contributorQq", contributorQq);
            bodyJson.put("isPublished", published);
        } catch (JSONException exception) {
            throw new IOException("Unable to encode usage tip request", exception);
        }
        Request.Builder builder = newRequestBuilder(
                buildUrl(TextUtils.isEmpty(tipId) ? "api/tips" : "api/tips/" + tipId),
                token
        );
        builder.method(TextUtils.isEmpty(tipId) ? "POST" : "PUT", RequestBody.create(bodyJson.toString(), JSON_MEDIA_TYPE));
        return parseUsageTipDetail(executeObject(builder.build()));
    }

    public void deleteUsageTip(String tipId, String token) throws IOException {
        if (TextUtils.isEmpty(tipId)) {
            throw new IOException("Missing usage tip id");
        }
        Request request = newRequestBuilder(buildUrl("api/tips/" + tipId), token).delete().build();
        executeRequest(request);
    }

    public WordImportResult importWord(String token, String filename, byte[] bytes) throws IOException {
        MultipartBody multipartBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                        "file",
                        TextUtils.isEmpty(filename) ? "tip.docx" : filename,
                        RequestBody.create(bytes, MediaType.get("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                )
                .build();
        Request request = newRequestBuilder(buildUrl("api/tips/import-word"), token)
                .post(multipartBody)
                .build();
        JSONObject jsonObject = executeObject(request);
        return new WordImportResult(
                optString(jsonObject, "htmlContent"),
                optString(jsonObject, "plainText")
        );
    }

    public List<SharedSimulationConfigEntry> getSharedSimulationConfigs(String query) throws IOException {
        HttpUrl url = buildUrl("api/shared/simulation-configs").newBuilder()
                .addQueryParameter("q", query == null ? "" : query)
                .build();
        Request request = clientRequestBuilder(url).get().build();
        JSONArray items = executeArray(request);
        List<SharedSimulationConfigEntry> results = new ArrayList<>();
        for (int index = 0; index < items.length(); index++) {
            results.add(parseSharedSimulationConfig(items.optJSONObject(index)));
        }
        return results;
    }

    public SharedSimulationConfigEntry getSharedSimulationConfig(String configId) throws IOException {
        if (TextUtils.isEmpty(configId)) {
            throw new IOException("Missing simulation config id");
        }
        Request request = clientRequestBuilder(buildUrl("api/shared/simulation-configs/" + configId))
                .get()
                .build();
        return parseSharedSimulationConfig(executeObject(request));
    }

    public SharedSimulationConfigEntry uploadSharedSimulationConfig(
            String name,
            RouteSimulationConfig config,
            String authorToken
    ) throws IOException {
        JSONObject bodyJson = new JSONObject();
        try {
            bodyJson.put("name", name);
            bodyJson.put("mode", config.getMode() == RouteSimulationConfig.Mode.CADENCE ? "cadence" : "speed");
            bodyJson.put("speed", config.getSpeedMetersPerSecond());
            bodyJson.put("cadence", config.getCadenceStepsPerMinute());
            bodyJson.put("loopCount", config.getLoopCount());
            bodyJson.put("dynamicIntensityEnabled", config.isDynamicIntensityEnabled());
            bodyJson.put("intensityVariationRange", config.getIntensityVariationRangeMetersPerSecond());
            bodyJson.put("intensityVariationFrequency", config.getIntensityVariationFrequency());
            bodyJson.put("naturalPathVariationEnabled", config.isNaturalPathVariationEnabled());
            bodyJson.put("pathVariationAmplitude", config.getPathVariationAmplitudeMeters());
            bodyJson.put("naturalAltitudeVariationEnabled", config.isNaturalAltitudeVariationEnabled());
            bodyJson.put("altitudeBaseMeters", config.getAltitudeBaseMeters());
            bodyJson.put("altitudeVariationRange", config.getAltitudeVariationRangeMeters());
            bodyJson.put("altitudeVariationHeightCentimeters", config.getAltitudeVariationHeightCentimeters());
            bodyJson.put("altitudeVariationProbability", config.getAltitudeVariationProbability());
            bodyJson.put("linkRatioNumerator", config.getLinkRatioNumerator());
            bodyJson.put("stepsPerMeter", config.getStepsPerMeter());
        } catch (JSONException exception) {
            throw new IOException("Unable to encode simulation config request", exception);
        }
        Request request = newRequestBuilder(buildUrl("api/shared/simulation-configs"), authorToken)
                .post(RequestBody.create(bodyJson.toString(), JSON_MEDIA_TYPE))
                .build();
        return parseSharedSimulationConfig(executeObject(request));
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
            HttpUrl parsed = HttpUrl.get(normalized);
            if (!BuildConfig.DEBUG
                    && (!"https".equalsIgnoreCase(parsed.scheme())
                    || !PUBLIC_API_HOST.equalsIgnoreCase(parsed.host())
                    || !(PUBLIC_API_PATH.equals(parsed.encodedPath())
                    || (PUBLIC_API_PATH + "/").equals(parsed.encodedPath())))) {
                throw new IOException("Release builds must use https://www.daoges.cn/schoolrun-api/");
            }
            return parsed;
        } catch (IllegalArgumentException exception) {
            throw new IOException("Invalid SHARE_API_BASE_URL", exception);
        }
    }

    public JSONObject getTutorialConfig(String token) throws IOException {
        return executeObject(newRequestBuilder(buildUrl("api/tutorial/config"), token).get().build());
    }

    public JSONObject getTutorialProgress(String token) throws IOException {
        return executeObject(newRequestBuilder(buildUrl("api/tutorial/progress"), token).get().build());
    }

    public void saveTutorialProgress(String token, JSONObject progress) throws IOException {
        executeObject(newRequestBuilder(buildUrl("api/tutorial/progress"), token)
                .put(RequestBody.create(progress.toString(), JSON_MEDIA_TYPE)).build());
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

    private Request.Builder newRequestBuilder(HttpUrl url, String token) {
        Request.Builder builder = clientRequestBuilder(url);
        if (!TextUtils.isEmpty(token)) {
            builder.header("Authorization", "Bearer " + token.trim());
        }
        return builder;
    }

    private Request.Builder clientRequestBuilder(HttpUrl url) {
        return new Request.Builder()
                .url(url)
                .header("X-Client-Variant", CLIENT_VARIANT)
                .header("X-App-Version", BuildConfig.VERSION_NAME);
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
                return data;
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
                if ((response.code() == 401 || response.code() == 403)
                        && request.header("Authorization") != null
                        && applicationContext != null) {
                    InternalAuthStore store = new InternalAuthStore(applicationContext);
                    if (("Bearer " + store.getToken()).equals(request.header("Authorization"))) {
                        store.clear();
                    }
                }
                throw new IOException(extractErrorMessage(response.code(), responseText));
            }
            return responseText;
        }
    }

    private String extractErrorMessage(int httpCode, String responseText) {
        String normalized = responseText == null ? "" : responseText.trim();
        if (!normalized.isEmpty()) {
            try {
                JSONObject jsonObject = new JSONObject(normalized);
                String detail = jsonObject.optString("detail", "").trim();
                if (!TextUtils.isEmpty(detail)) {
                    return detail;
                }
                JSONObject data = jsonObject.optJSONObject("data");
                if (data != null) {
                    detail = data.optString("detail", "").trim();
                    if (!TextUtils.isEmpty(detail)) {
                        return detail;
                    }
                }
            } catch (JSONException ignored) {
                // Fall back to the raw response text below.
            }
        }
        if (TextUtils.isEmpty(normalized)) {
            return "HTTP " + httpCode;
        }
        return "HTTP " + httpCode + ": " + normalized;
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

    private AppClientConfig parseAppClientConfig(JSONObject jsonObject) throws IOException {
        if (jsonObject == null) {
            throw new IOException("Client config payload is empty");
        }
        return new AppClientConfig(
                optString(jsonObject, "noticeTitle", "title"),
                optString(jsonObject, "noticeMessage", "message"),
                optString(jsonObject, "qqGroupNumber", "groupNumber", "qqGroup"),
                optString(jsonObject, "bilibiliText", "bilibiliLabel"),
                optString(jsonObject, "bilibiliUrl", "bilibiliLink"),
                optBoolean(jsonObject, "forceShutdown"),
                optString(jsonObject, "forceShutdownMessage", "shutdownMessage")
        );
    }

    private InternalAccountProfile parseInternalAccount(JSONObject jsonObject) throws IOException {
        if (jsonObject == null) {
            throw new IOException("Internal account payload is empty");
        }
        return new InternalAccountProfile(
                optString(jsonObject, "id"),
                optString(jsonObject, "username"),
                optString(jsonObject, "remark"),
                optString(jsonObject, "testerType"),
                optString(jsonObject, "testerTypeLabel"),
                optString(jsonObject, "status")
        );
    }

    private FsrBindingState parseFsrBinding(JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }
        return new FsrBindingState(
                optString(jsonObject, "status"),
                optString(jsonObject, "token"),
                optString(jsonObject, "command"),
                optString(jsonObject, "jobId"),
                optInt(jsonObject, "queuePosition"),
                optString(jsonObject, "message"),
                optString(jsonObject, "qqNumber"),
                optString(jsonObject, "qqDisplayName"),
                optString(jsonObject, "groupId")
        );
    }

    private UsageTipSummary parseUsageTipSummary(JSONObject jsonObject) throws IOException {
        if (jsonObject == null) {
            throw new IOException("Usage tip item is empty");
        }
        return new UsageTipSummary(
                optString(jsonObject, "id"),
                optString(jsonObject, "title"),
                optString(jsonObject, "excerpt"),
                optString(jsonObject, "contributorQq", "contributorQQ"),
                optString(jsonObject, "authorUsername"),
                optBoolean(jsonObject, "isPublished", "published"),
                optBoolean(jsonObject, "editable"),
                optLong(jsonObject, "createdAt", "createTime"),
                optLong(jsonObject, "updatedAt", "updateTime")
        );
    }

    private UsageTipDetail parseUsageTipDetail(JSONObject jsonObject) throws IOException {
        if (jsonObject == null) {
            throw new IOException("Usage tip payload is empty");
        }
        return new UsageTipDetail(
                optString(jsonObject, "id"),
                optString(jsonObject, "title"),
                optString(jsonObject, "htmlContent"),
                optString(jsonObject, "plainText"),
                optString(jsonObject, "contributorQq", "contributorQQ"),
                optString(jsonObject, "authorAccountId"),
                optString(jsonObject, "authorUsername"),
                optBoolean(jsonObject, "isPublished", "published"),
                optBoolean(jsonObject, "editable"),
                optLong(jsonObject, "createdAt", "createTime"),
                optLong(jsonObject, "updatedAt", "updateTime")
        );
    }

    private SharedSimulationConfigEntry parseSharedSimulationConfig(JSONObject jsonObject) throws IOException {
        if (jsonObject == null) {
            throw new IOException("Simulation config payload is empty");
        }
        return new SharedSimulationConfigEntry(
                optString(jsonObject, "id"),
                optString(jsonObject, "name"),
                optString(jsonObject, "mode"),
                optDouble(jsonObject, "speed"),
                optDouble(jsonObject, "cadence"),
                optInt(jsonObject, "loopCount", "loop_count"),
                optBoolean(jsonObject, "dynamicIntensityEnabled"),
                optDouble(jsonObject, "intensityVariationRange"),
                optDouble(jsonObject, "intensityVariationFrequency"),
                optBoolean(jsonObject, "naturalPathVariationEnabled"),
                optDouble(jsonObject, "pathVariationAmplitude"),
                optBoolean(jsonObject, "naturalAltitudeVariationEnabled"),
                optDoubleOrDefault(jsonObject, RouteSimulationConfig.DEFAULT_ALTITUDE_BASE_METERS, "altitudeBaseMeters"),
                optDouble(jsonObject, "altitudeVariationRange"),
                optDouble(jsonObject, "altitudeVariationHeightCentimeters"),
                optDouble(jsonObject, "altitudeVariationProbability"),
                optDouble(jsonObject, "linkRatioNumerator"),
                optDouble(jsonObject, "stepsPerMeter"),
                optString(jsonObject, "authorName"),
                optLong(jsonObject, "createdAt", "createTime"),
                optLong(jsonObject, "updatedAt", "updateTime")
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

    private double optDouble(@NonNull JSONObject jsonObject, String... keys) {
        for (String key : keys) {
            if (jsonObject.has(key)) {
                return jsonObject.optDouble(key, 0d);
            }
        }
        return 0d;
    }

    private double optDoubleOrDefault(@NonNull JSONObject jsonObject, double fallback, String... keys) {
        for (String key : keys) {
            if (jsonObject.has(key)) {
                return jsonObject.optDouble(key, fallback);
            }
        }
        return fallback;
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
