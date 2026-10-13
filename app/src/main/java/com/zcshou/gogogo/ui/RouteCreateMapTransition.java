package com.acooldog.toolbox.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BaiduMapOptions;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.TextureMapView;

/** A temporary live map layer; the existing map stays visible until incoming tiles are ready. */
public final class RouteCreateMapTransition {
    public interface Listener {
        void prepareMap(BaiduMap map);
        void onMapChanged(TextureMapView view);
        void onLoadFailed();
        void onMapInteraction();
    }

    private final FrameLayout host;
    private final Listener listener;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Session session;

    public RouteCreateMapTransition(FrameLayout host, Listener listener) {
        this.host = host;
        this.listener = listener;
    }

    public void start(TextureMapView currentView, boolean satellite) {
        cancel();
        Session next = new Session(currentView, satellite);
        session = next;
        listener.prepareMap(next.map);
        next.map.setOnMapTileLoadedCallback(() -> handler.post(() -> {
            if (session != next || next.animator != null) {
                return;
            }
            next.tilesReady = true;
            // Let the newly loaded tile frame reach the TextureView before exposing it.
            next.layer.postOnAnimation(() -> next.layer.postOnAnimation(() -> reveal(next)));
        }));
        // Preload below the current map without a zero-area clip: TextureView needs to draw
        // to initialize its surface, even though this layer is not yet visible to the user.
        host.addView(next.layer, 0, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        next.view.onResume();
        handler.postDelayed(next.timeout, 8000L);
    }

    @Nullable
    public BaiduMap getIncomingMap() {
        return session == null ? null : session.map;
    }

    public void syncMapStatus(MapStatus status) {
        if (session != null) {
            session.map.setMapStatus(MapStatusUpdateFactory.newMapStatus(status));
        }
    }

    public void onMapTouch(MotionEvent event) {
        Session pending = session;
        if (pending == null || pending.animator != null) {
            return;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            pending.gestureInProgress = true;
        } else if (event.getActionMasked() == MotionEvent.ACTION_UP
                || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            pending.gestureInProgress = false;
            if (pending.tilesReady) {
                pending.layer.postOnAnimation(() -> reveal(pending));
            }
        }
    }

    public void cancel() {
        Session previous = session;
        session = null;
        if (previous == null) {
            return;
        }
        clearCallbacks(previous);
        if (previous.animator != null) {
            previous.animator.removeAllListeners();
            previous.animator.removeAllUpdateListeners();
            previous.animator.cancel();
        }
        previous.map.setMyLocationEnabled(false);
        previous.view.onPause();
        previous.view.onDestroy();
        host.removeView(previous.layer);
    }

    private void reveal(Session next) {
        if (session != next || next.animator != null || next.gestureInProgress
                || host.getWidth() == 0) {
            return;
        }
        handler.removeCallbacks(next.timeout);
        next.map.setOnMapTileLoadedCallback(null);
        next.updateClip(0f);
        next.layer.bringToFront();
        next.animator = ValueAnimator.ofFloat(0f, 1f);
        next.animator.setDuration(320L);
        next.animator.setInterpolator(new PathInterpolator(0.4f, 0f, 0.2f, 1f));
        next.animator.addUpdateListener(animation ->
                next.updateClip((float) animation.getAnimatedValue()));
        next.animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (session != next) {
                    return;
                }
                session = null;
                clearCallbacks(next);
                next.layer.setClipBounds(null);
                int viewId = next.currentView.getId();
                next.currentView.setId(android.view.View.NO_ID);
                next.view.setId(viewId);
                // Keep the fully revealed layer in place; never detach its TextureView at commit.
                listener.onMapChanged(next.view);
                next.currentView.getMap().setOnMapClickListener(null);
                next.currentView.getMap().setOnMarkerClickListener(null);
                next.currentView.getMap().setOnMapTouchListener(null);
                next.currentView.getMap().setOnMapStatusChangeListener(null);
                next.currentView.getMap().setMyLocationEnabled(false);
                next.currentView.onPause();
                next.currentView.onDestroy();
                ViewGroup oldParent = (ViewGroup) next.currentView.getParent();
                oldParent.removeView(next.currentView);
                if (oldParent != host) {
                    host.removeView(oldParent);
                }
                next.currentView = null;
            }
        });
        next.animator.start();
    }

    private void clearCallbacks(Session target) {
        handler.removeCallbacks(target.timeout);
        target.map.setOnMapTileLoadedCallback(null);
    }

    private final class Session {
        TextureMapView currentView;
        final TextureMapView view;
        final BaiduMap map;
        final FrameLayout layer;
        final boolean satellite;
        final Runnable timeout;
        ValueAnimator animator;
        boolean tilesReady;
        boolean gestureInProgress;

        Session(TextureMapView currentView, boolean satellite) {
            this.currentView = currentView;
            this.satellite = satellite;
            view = new TextureMapView(host.getContext(), new BaiduMapOptions()
                    .mapStatus(currentView.getMap().getMapStatus())
                    .mapType(satellite ? BaiduMap.MAP_TYPE_SATELLITE : BaiduMap.MAP_TYPE_NORMAL)
                    .zoomControlsEnabled(false)
                    .logoPosition(currentView.getLogoPosition()));
            map = view.getMap();
            layer = new FrameLayout(host.getContext()) {
                @Override
                public boolean dispatchTouchEvent(MotionEvent event) {
                    // During reveal, only map gestures are paused; drawing controls stay usable.
                    if (session != Session.this) {
                        return super.dispatchTouchEvent(event);
                    }
                    if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                        listener.onMapInteraction();
                    }
                    return animator == null ? Session.this.currentView.dispatchTouchEvent(event) : true;
                }
            };
            layer.addView(view, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            timeout = () -> {
                if (session == this) {
                    cancel();
                    listener.onLoadFailed();
                }
            };
        }

        void updateClip(float progress) {
            layer.setClipBounds(new Rect(
                    RouteCreateMapReveal.left(host.getWidth(), progress, satellite),
                    0,
                    RouteCreateMapReveal.right(host.getWidth(), progress, satellite),
                    host.getHeight()));
        }
    }
}
