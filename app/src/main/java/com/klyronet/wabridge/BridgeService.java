package com.klyronet.wabridge;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;
import java.net.URI;
public class BridgeService extends Service {
    private static final String TAG = "BridgeService";
    private static final String SERVER_URL = "wss://emu.klyronet.com/phone-bridge";
    private static final String CHANNEL_ID = "wa_bridge";
    private static BridgeService instance;
    private static WebSocketClient wsClient;
    private Handler handler = new Handler(Looper.getMainLooper());
    private String deviceId;
    public static BridgeService getInstance() { return instance; }
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        deviceId = getDeviceId();
        createNotificationChannel();
        startForeground(1, buildNotification("Connecting..."));
        connect();
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) { return START_STICKY; }
    @Override
    public IBinder onBind(Intent intent) { return null; }
    @Override
    public void onDestroy() { super.onDestroy(); if (wsClient != null) wsClient.close(); instance = null; }
    private void connect() {
        try {
            URI uri = new URI(SERVER_URL + "?device=" + deviceId);
            wsClient = new WebSocketClient(uri) {
                @Override
                public void onOpen(ServerHandshake h) {
                    Log.d(TAG, "Connected!");
                    updateNotification("Connected to CRM ✅");
                    try {
                        JSONObject reg = new JSONObject();
                        reg.put("type", "register");
                        reg.put("device", deviceId);
                        reg.put("model", Build.MODEL);
                        reg.put("android", Build.VERSION.RELEASE);
                        send(reg.toString());
                    } catch (Exception e) { Log.e(TAG, e.getMessage()); }
                }
                @Override
                public void onMessage(String message) {
                    try {
                        JSONObject cmd = new JSONObject(message);
                        handler.post(() -> {
                            WAAccessibilityService svc = WAAccessibilityService.getInstance();
                            if (svc != null) svc.executeCommand(cmd);
                        });
                    } catch (Exception e) { Log.e(TAG, e.getMessage()); }
                }
                @Override
                public void onClose(int code, String reason, boolean remote) {
                    updateNotification("Reconnecting...");
                    handler.postDelayed(() -> connect(), 3000);
                }
                @Override
                public void onError(Exception e) {
                    handler.postDelayed(() -> connect(), 5000);
                }
            };
            wsClient.connect();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            handler.postDelayed(this::connect, 5000);
        }
    }
    public static void sendToServer(String message) {
        if (wsClient != null && wsClient.isOpen()) wsClient.send(message);
    }
    private String getDeviceId() {
        android.content.SharedPreferences prefs = getSharedPreferences("wa_bridge", MODE_PRIVATE);
        String id = prefs.getString("device_id", null);
        if (id == null) { id = java.util.UUID.randomUUID().toString().substring(0, 8); prefs.edit().putString("device_id", id).apply(); }
        return id;
    }
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "WA Bridge", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }
    private Notification buildNotification(String text) {
        return new Notification.Builder(this, CHANNEL_ID).setContentTitle("WA Bridge").setContentText(text).setSmallIcon(android.R.drawable.ic_dialog_info).build();
    }
    private void updateNotification(String text) {
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(1, buildNotification(text));
    }
}
