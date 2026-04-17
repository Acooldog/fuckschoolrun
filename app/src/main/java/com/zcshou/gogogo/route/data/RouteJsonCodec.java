package com.zcshou.gogogo.route.data;

import com.zcshou.gogogo.route.domain.model.RouteDefinition;
import com.zcshou.gogogo.route.domain.model.RoutePoint;
import com.zcshou.utils.MapUtils;

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
    private static final String KEY_POINTS = "points";
    private static final String KEY_BD_LONGITUDE = "bdLongitude";
    private static final String KEY_BD_LATITUDE = "bdLatitude";
    private static final String KEY_WGS_LONGITUDE = "wgsLongitude";
    private static final String KEY_WGS_LATITUDE = "wgsLatitude";
    private static final String KEY_ALTITUDE = "altitude";

    String encode(RouteDefinition routeDefinition) throws JSONException {
        JSONObject root = new JSONObject();
        root.put(KEY_VERSION, 1);
        root.put(KEY_NAME, routeDefinition.getName());
        root.put(KEY_CREATED_AT, routeDefinition.getCreatedAt());
        root.put(KEY_UPDATED_AT, routeDefinition.getUpdatedAt());
        JSONArray pointArray = new JSONArray();
        for (RoutePoint point : routeDefinition.getPoints()) {
            JSONObject pointJson = new JSONObject();
            pointJson.put(KEY_BD_LONGITUDE, point.getBdLongitude());
            pointJson.put(KEY_BD_LATITUDE, point.getBdLatitude());
            pointJson.put(KEY_WGS_LONGITUDE, point.getWgsLongitude());
            pointJson.put(KEY_WGS_LATITUDE, point.getWgsLatitude());
            pointJson.put(KEY_ALTITUDE, point.getAltitude());
            pointArray.put(pointJson);
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
        JSONArray pointsArray = root.optJSONArray(KEY_POINTS);
        if (pointsArray == null) {
            throw new JSONException("Route points are missing");
        }
        return new RouteDefinition(id, routeName, createdAt, updatedAt, parsePoints(pointsArray), file);
    }

    private List<RoutePoint> parsePoints(JSONArray pointArray) throws JSONException {
        List<RoutePoint> points = new ArrayList<>();
        for (int index = 0; index < pointArray.length(); index++) {
            JSONObject pointJson = pointArray.getJSONObject(index);
            Double bdLongitude = optDouble(pointJson, KEY_BD_LONGITUDE, "bdLng", "longitude", "lng");
            Double bdLatitude = optDouble(pointJson, KEY_BD_LATITUDE, "bdLat", "latitude", "lat");
            Double wgsLongitude = optDouble(pointJson, KEY_WGS_LONGITUDE, "gpsLongitude", "wgsLng");
            Double wgsLatitude = optDouble(pointJson, KEY_WGS_LATITUDE, "gpsLatitude", "wgsLat");
            double altitude = pointJson.optDouble(KEY_ALTITUDE, 55d);

            if (bdLongitude == null || bdLatitude == null) {
                if (wgsLongitude == null || wgsLatitude == null) {
                    throw new JSONException("Point coordinates are missing");
                }
                double[] bdCoordinates = MapUtils.wgs2bd09(wgsLongitude, wgsLatitude);
                bdLongitude = bdCoordinates[0];
                bdLatitude = bdCoordinates[1];
            }

            if (wgsLongitude == null || wgsLatitude == null) {
                double[] wgsCoordinates = MapUtils.bd2wgs(bdLongitude, bdLatitude);
                wgsLongitude = wgsCoordinates[0];
                wgsLatitude = wgsCoordinates[1];
            }

            points.add(new RoutePoint(bdLongitude, bdLatitude, wgsLongitude, wgsLatitude, altitude));
        }
        return points;
    }

    private Double optDouble(JSONObject object, String... keys) {
        for (String key : keys) {
            if (object.has(key)) {
                return object.optDouble(key);
            }
        }
        return null;
    }
}
