package com.zcshou.gogogo.route.data;

import com.zcshou.gogogo.route.domain.model.RouteDefinition;
import com.zcshou.gogogo.route.domain.model.RoutePoint;
import com.zcshou.gogogo.route.domain.model.RouteShareInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

final class RouteJsonCodec {
    private static final String KEY_VERSION = "version";
    private static final String KEY_NAME = "name";
    private static final String KEY_CREATED_AT = "createdAt";
    private static final String KEY_UPDATED_AT = "updatedAt";
    private static final String KEY_SHARE = "share";
    private static final String KEY_SHARE_ID = "shareId";
    private static final String KEY_SHARED = "shared";
    private static final String KEY_PRIVACY_MODE = "privacyMode";
    private static final String KEY_DOWNLOADED_FROM_SHARED = "downloadedFromShared";
    private static final String KEY_SHARED_AT = "sharedAt";
    private static final String KEY_POINTS = "points";

    String encode(RouteDefinition routeDefinition) throws JSONException {
        JSONObject root = new JSONObject();
        root.put(KEY_VERSION, 1);
        root.put(KEY_NAME, routeDefinition.getName());
        root.put(KEY_CREATED_AT, routeDefinition.getCreatedAt());
        root.put(KEY_UPDATED_AT, routeDefinition.getUpdatedAt());
        if (routeDefinition.isSharedRoute() || routeDefinition.isPrivacyProtected()) {
            root.put(KEY_SHARE, encodeShareInfo(routeDefinition.getShareInfo()));
        }
        JSONArray pointArray = new JSONArray();
        for (RoutePoint point : routeDefinition.getPoints()) {
            pointArray.put(RoutePointJsonCodec.encode(point));
        }
        root.put(KEY_POINTS, pointArray);
        return root.toString(2);
    }

    RouteDefinition decode(String id, String fallbackName, String content, File file) throws JSONException {
        String trimmed = content.trim();
        if (trimmed.startsWith("[")) {
            JSONArray array = new JSONArray(trimmed);
            return new RouteDefinition(id, fallbackName, System.currentTimeMillis(), System.currentTimeMillis(), parsePoints(array), file);
        }

        JSONObject root = new JSONObject(trimmed);
        String routeName = root.optString(KEY_NAME, fallbackName);
        long createdAt = root.optLong(KEY_CREATED_AT, System.currentTimeMillis());
        long updatedAt = root.optLong(KEY_UPDATED_AT, System.currentTimeMillis());
        RouteShareInfo shareInfo = decodeShareInfo(root.optJSONObject(KEY_SHARE));
        JSONArray pointsArray = root.optJSONArray(KEY_POINTS);
        if (pointsArray == null) {
            throw new JSONException("Route points are missing");
        }
        return new RouteDefinition(id, routeName, createdAt, updatedAt, parsePoints(pointsArray), file, shareInfo);
    }

    private List<RoutePoint> parsePoints(JSONArray pointArray) throws JSONException {
        List<RoutePoint> points = new ArrayList<>();
        for (int index = 0; index < pointArray.length(); index++) {
            points.add(RoutePointJsonCodec.decode(pointArray.getJSONObject(index)));
        }
        return points;
    }

    private JSONObject encodeShareInfo(RouteShareInfo shareInfo) throws JSONException {
        JSONObject shareJson = new JSONObject();
        shareJson.put(KEY_SHARE_ID, shareInfo.getShareId());
        shareJson.put(KEY_SHARED, shareInfo.isShared());
        shareJson.put(KEY_PRIVACY_MODE, shareInfo.isPrivacyMode());
        shareJson.put(KEY_DOWNLOADED_FROM_SHARED, shareInfo.isDownloadedFromShared());
        shareJson.put(KEY_SHARED_AT, shareInfo.getSharedAt());
        return shareJson;
    }

    private RouteShareInfo decodeShareInfo(JSONObject shareJson) {
        if (shareJson == null) {
            return RouteShareInfo.NONE;
        }
        return new RouteShareInfo(
                shareJson.optString(KEY_SHARE_ID, ""),
                shareJson.optBoolean(KEY_SHARED, false),
                shareJson.optBoolean(KEY_PRIVACY_MODE, false),
                shareJson.optBoolean(KEY_DOWNLOADED_FROM_SHARED, false),
                shareJson.optLong(KEY_SHARED_AT, 0L)
        );
    }
}
