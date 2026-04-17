package com.zcshou.gogogo.route.data;

import com.zcshou.gogogo.route.domain.model.RoutePoint;
import com.zcshou.utils.MapUtils;

import org.json.JSONException;
import org.json.JSONObject;

public final class RoutePointJsonCodec {
    public static final String KEY_BD_LONGITUDE = "bdLongitude";
    public static final String KEY_BD_LATITUDE = "bdLatitude";
    public static final String KEY_WGS_LONGITUDE = "wgsLongitude";
    public static final String KEY_WGS_LATITUDE = "wgsLatitude";
    public static final String KEY_ALTITUDE = "altitude";

    private RoutePointJsonCodec() {
    }

    public static JSONObject encode(RoutePoint point) throws JSONException {
        JSONObject pointJson = new JSONObject();
        pointJson.put(KEY_BD_LONGITUDE, point.getBdLongitude());
        pointJson.put(KEY_BD_LATITUDE, point.getBdLatitude());
        pointJson.put(KEY_WGS_LONGITUDE, point.getWgsLongitude());
        pointJson.put(KEY_WGS_LATITUDE, point.getWgsLatitude());
        pointJson.put(KEY_ALTITUDE, point.getAltitude());
        return pointJson;
    }

    public static RoutePoint decode(JSONObject pointJson) throws JSONException {
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

        return new RoutePoint(bdLongitude, bdLatitude, wgsLongitude, wgsLatitude, altitude);
    }

    private static Double optDouble(JSONObject object, String... keys) {
        for (String key : keys) {
            if (object.has(key)) {
                return object.optDouble(key);
            }
        }
        return null;
    }
}
