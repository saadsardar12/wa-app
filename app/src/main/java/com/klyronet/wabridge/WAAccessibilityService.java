package com.klyronet.wabridge;
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import org.json.JSONObject;
import java.util.List;
public class WAAccessibilityService extends AccessibilityService {
    private static final String TAG = "WAService";
    private static WAAccessibilityService instance;
    private Handler handler = new Handler(Looper.getMainLooper());
    public static WAAccessibilityService getInstance() { return instance; }
    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        Log.d(TAG, "Service Connected");
        BridgeService.sendToServer("{\"type\":\"accessibility_ready\"}");
    }
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {}
    @Override
    public void onInterrupt() {}
    @Override
    public void onDestroy() { super.onDestroy(); instance = null; }
    public void tap(float x, float y) {
        Path path = new Path();
        path.moveTo(x, y);
        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(new GestureDescription.StrokeDescription(path, 0, 100));
        dispatchGesture(builder.build(), null, null);
    }
    public boolean setTextById(String resourceId, String text) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return false;
        List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByViewId(resourceId);
        if (nodes != null && !nodes.isEmpty()) {
            AccessibilityNodeInfo node = nodes.get(0);
            node.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
            Bundle args = new Bundle();
            args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
            return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
        }
        return false;
    }
    public boolean tapById(String resourceId) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return false;
        List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByViewId(resourceId);
        if (nodes != null && !nodes.isEmpty()) {
            AccessibilityNodeInfo node = nodes.get(0);
            Rect bounds = new Rect();
            node.getBoundsInScreen(bounds);
            tap((bounds.left + bounds.right) / 2f, (bounds.top + bounds.bottom) / 2f);
            return true;
        }
        return false;
    }
    public boolean tapByText(String text) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return false;
        List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByText(text);
        if (nodes != null && !nodes.isEmpty()) {
            AccessibilityNodeInfo node = nodes.get(0);
            Rect bounds = new Rect();
            node.getBoundsInScreen(bounds);
            tap((bounds.left + bounds.right) / 2f, (bounds.top + bounds.bottom) / 2f);
            return true;
        }
        return false;
    }
    public void executeCommand(JSONObject cmd) {
        try {
            String type = cmd.getString("type");
            switch (type) {
                case "tap": tap((float)cmd.getDouble("x"), (float)cmd.getDouble("y")); break;
                case "tap_id": tapById(cmd.getString("id")); break;
                case "tap_text": tapByText(cmd.getString("text")); break;
                case "set_text": setTextById(cmd.getString("id"), cmd.getString("text")); break;
                case "register_whatsapp": registerWhatsApp(cmd.getString("country_code"), cmd.getString("phone")); break;
                case "enter_otp": enterOTP(cmd.getString("otp")); break;
            }
        } catch (Exception e) { Log.e(TAG, "Command error: " + e.getMessage()); }
    }
    private void registerWhatsApp(String cc, String phone) {
        handler.post(() -> {
            try {
                android.content.Intent intent = getPackageManager().getLaunchIntentForPackage("com.whatsapp.w4b");
                if (intent == null) intent = getPackageManager().getLaunchIntentForPackage("com.whatsapp");
                if (intent != null) { intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(intent); }
                handler.postDelayed(() -> { tapByText("AGREE AND CONTINUE"); tapByText("Agree and continue");
                    handler.postDelayed(() -> {
                        setTextById("com.whatsapp.w4b:id/registration_cc", cc);
                        setTextById("com.whatsapp:id/registration_cc", cc);
                        handler.postDelayed(() -> {
                            setTextById("com.whatsapp.w4b:id/registration_phone", phone);
                            setTextById("com.whatsapp:id/registration_phone", phone);
                            handler.postDelayed(() -> {
                                tapById("com.whatsapp.w4b:id/registration_submit");
                                tapById("com.whatsapp:id/registration_submit");
                                BridgeService.sendToServer("{\"type\":\"registration_submitted\"}");
                            }, 2000);
                        }, 2000);
                    }, 5000);
                }, 8000);
            } catch (Exception e) { Log.e(TAG, "Error: " + e.getMessage()); }
        });
    }
    private void enterOTP(String otp) {
        handler.postDelayed(() -> {
            setTextById("com.whatsapp.w4b:id/pin_entry", otp);
            setTextById("com.whatsapp:id/pin_entry", otp);
            BridgeService.sendToServer("{\"type\":\"otp_entered\"}");
        }, 1000);
    }
}
