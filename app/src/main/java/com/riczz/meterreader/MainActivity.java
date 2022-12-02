package com.riczz.meterreader;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;

import org.opencv.android.OpenCVLoader;

public final class MainActivity extends AppCompatActivity {

    private static final String LOG_TAG = MainActivity.class.getName();

    private DrawerLayout drawerLayout;
    private MaterialToolbar topAppbar;
    private NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (OpenCVLoader.initDebug()) {
            Log.d(LOG_TAG, "OpenCV initialized.");
            Log.d(LOG_TAG, "Version: " + OpenCVLoader.OPENCV_VERSION);
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

            if (item.getItemId() == R.id.meter_report) {
                startActivity(new Intent(this, ReportActivity.class));
            }

            return true;
        });
    }
}