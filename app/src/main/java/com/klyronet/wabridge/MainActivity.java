package com.klyronet.wabridge;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView statusText;
    private Button accessibilityBtn;
    private Button startBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.statusText);
        accessibilityBtn = findViewById(R.id.accessibilityBtn);
        startBtn = findViewById(R.id.startBtn);

        accessibilityBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
        });

        startBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, BridgeService.class);
            startForegroundService(intent);
            statusText.setText("✅ Connected! Open your CRM to register WhatsApp.");
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
    }

    private void updateStatus() {
        if (isAccessibilityEnabled()) {
            accessibilityBtn.setText("✅ Accessibility Enabled");
            accessibilityBtn.setEnabled(false);
            startBtn.setEnabled(true);
            statusText.setText("Accessibility enabled! Tap Connect to start.");
        } else {
            accessibilityBtn.setText("⚙️ Enable Accessibility");
            accessibilityBtn.setEnabled(true);
            startBtn.setEnabled(false);
            statusText.setText("Please enable Accessibility Service first.");
        }
    }

    private boolean isAccessibilityEnabled() {
        AccessibilityManager am = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
        List services = am.getEnabledAccessibilityServiceList(-1);
        for (Object service : services) {
            if (service.toString().contains("wabridge")) return true;
        }
        return false;
    }
}
