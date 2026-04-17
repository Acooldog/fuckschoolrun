package com.zcshou.gogogo.route.data;

import android.content.Context;

import com.zcshou.gogogo.route.domain.model.RouteDefinition;
import com.zcshou.gogogo.route.domain.model.RoutePoint;
import com.zcshou.gogogo.route.domain.model.RouteShareInfo;
import com.zcshou.gogogo.route.domain.repository.RouteRepository;

import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public final class FileRouteRepository implements RouteRepository {
    private final File routeDirectory;
    private final RouteJsonCodec routeJsonCodec;

    public FileRouteRepository(Context context) {
        File externalDirectory = context.getExternalFilesDir("routes");
        if (externalDirectory == null) {
            externalDirectory = new File(context.getFilesDir(), "routes");
        }
        routeDirectory = externalDirectory;
        routeJsonCodec = new RouteJsonCodec();
    }

    @Override
    public List<RouteDefinition> getRoutes() throws IOException {
        ensureDirectory();
        File[] files = routeDirectory.listFiles((dir, name) -> name.endsWith(".route.json") || name.endsWith(".json"));
        if (files == null) {
            return new ArrayList<>();
        }
        Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
        List<RouteDefinition> routes = new ArrayList<>();
        for (File file : files) {
            routes.add(readRoute(file));
        }
        return routes;
    }

    @Override
    public RouteDefinition getRoute(String routeId) throws IOException {
        File routeFile = new File(routeDirectory, routeId);
        if (!routeFile.exists()) {
            return null;
        }
        return readRoute(routeFile);
    }

    @Override
    public RouteDefinition saveRoute(String routeName, List<RoutePoint> points) throws IOException {
        return saveRoute(routeName, points, RouteShareInfo.NONE);
    }

    @Override
    public RouteDefinition saveRoute(String routeName, List<RoutePoint> points, RouteShareInfo shareInfo) throws IOException {
        ensureDirectory();
        long now = System.currentTimeMillis();
        String safeName = sanitizeFileName(routeName);
        File routeFile = new File(routeDirectory, safeName + ".route.json");
        RouteDefinition routeDefinition = new RouteDefinition(routeFile.getName(), routeName, now, now, points, routeFile, shareInfo);
        writeText(routeFile, encodeRoute(routeDefinition));
        return routeDefinition;
    }

    @Override
    public RouteDefinition importRoute(String displayName, InputStream inputStream) throws IOException {
        ensureDirectory();
        String content = readText(inputStream);
        String safeName = sanitizeFileName(displayName == null || displayName.trim().isEmpty() ? "imported-route" : displayName);
        File routeFile = new File(routeDirectory, safeName + ".route.json");
        RouteDefinition importedRoute;
        try {
            importedRoute = routeJsonCodec.decode(routeFile.getName(), displayName, content, routeFile);
        } catch (JSONException exception) {
            throw new IOException("Unable to parse route file", exception);
        }
        writeText(routeFile, encodeRoute(importedRoute));
        return readRoute(routeFile);
    }

    @Override
    public void deleteRoute(String routeId) throws IOException {
        File routeFile = new File(routeDirectory, routeId);
        if (routeFile.exists() && !routeFile.delete()) {
            throw new IOException("Unable to delete route file");
        }
    }

    private RouteDefinition readRoute(File routeFile) throws IOException {
        try (FileInputStream inputStream = new FileInputStream(routeFile)) {
            return routeJsonCodec.decode(routeFile.getName(), routeFile.getName(), readText(inputStream), routeFile);
        } catch (JSONException exception) {
            throw new IOException("Unable to decode route file: " + routeFile.getName(), exception);
        }
    }

    private String encodeRoute(RouteDefinition routeDefinition) throws IOException {
        try {
            return routeJsonCodec.encode(routeDefinition);
        } catch (JSONException exception) {
            throw new IOException("Unable to encode route", exception);
        }
    }

    private String readText(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, length);
        }
        return outputStream.toString(StandardCharsets.UTF_8.name());
    }

    private void writeText(File routeFile, String text) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(routeFile, false)) {
            outputStream.write(text.getBytes(StandardCharsets.UTF_8));
        }
    }

    private void ensureDirectory() throws IOException {
        if (!routeDirectory.exists() && !routeDirectory.mkdirs()) {
            throw new IOException("Unable to create route directory");
        }
    }

    private String sanitizeFileName(String routeName) {
        String normalized = Normalizer.normalize(routeName, Normalizer.Form.NFKC)
                .replaceAll("[\\\\/:*?\"<>|]", "_")
                .replaceAll("\\s+", "_");
        if (normalized.isEmpty()) {
            return "route_" + System.currentTimeMillis();
        }
        return normalized;
    }
}
