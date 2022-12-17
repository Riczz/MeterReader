package com.riczz.meterreader;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;

import org.opencv.android.OpenCVLoader;

public final class MainActivity extends AppCompatActivity {

    private static final String LOG_TAG = MainActivity.class.getName();
    private static final String APP_VERSION = "1.0";

    private DrawerLayout drawerLayout;
    private MaterialToolbar topAppbar;
    private NavigationView navigationView;

    static {
        System.loadLibrary("opencv_java4");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ((TextView)findViewById(R.id.appTitle)).setText(String.format(
                getString(R.string.app_version_text), MainActivity.APP_VERSION
        ));

        if (OpenCVLoader.initDebug()) {
            Log.d(LOG_TAG, "OpenCV initialized.");
            Log.d(LOG_TAG, "Version: " + OpenCVLoader.OPENCV_VERSION);

            ((TextView)findViewById(R.id.appSubTitle)).setText(String.format(
                    getString(R.string.opencv_version_text), OpenCVLoader.OPENCV_VERSION
            ));
        }

        drawerLayout = findViewById(R.id.drawer);
        topAppbar = findViewById(R.id.topAppBar);
        navigationView = findViewById(R.id.navigationView);

        setSupportActionBar(topAppbar);

        navigationView.getMenu().clear();
        navigationView.inflateMenu(R.menu.menu_sidebar);

        topAppbar.setNavigationOnClickListener(view -> drawerLayout.open());
        navigationView.setNavigationItemSelectedListener(item -> {
            item.setChecked(true);
            drawerLayout.close();

            Intent intent = new Intent(this, ReportActivity.class);

            if (item.getItemId() == R.id.gas_meter_report) {
                intent.putExtra("IS_GAS_METER", true);
                startActivity(intent);
            } else if (item.getItemId() == R.id.electric_meter_report) {
                intent.putExtra("IS_GAS_METER", false);
                startActivity(intent);
            } else if (item.getItemId() == R.id.settings_menu) {
                intent = new Intent(this, PreviewActivity.class);
                intent.putExtra("SETTINGS_ONLY", true);
                startActivity(intent);
            }

            return true;
        });
    }
}