package com.navigine.mockgps;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AppOpsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.util.Objects;
import java.util.Timer;

import com.google.android.material.snackbar.Snackbar;

public class SelectOnMapFragment extends Fragment {

    private static final Intent[] POWERMANAGER_INTENTS = {
            new Intent().setComponent(new ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")),
            new Intent().setComponent(new ComponentName("com.letv.android.letvsafe", "com.letv.android.letvsafe.AutobootManageActivity")),
            new Intent().setComponent(new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")),
            new Intent().setComponent(new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")),
            new Intent().setComponent(new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity")),
            new Intent().setComponent(new ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")),
            new Intent().setComponent(new ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity")),
            new Intent().setComponent(new ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")),
            new Intent().setComponent(new ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")),
            new Intent().setComponent(new ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager")),
            new Intent().setComponent(new ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")),
            new Intent().setComponent(new ComponentName("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity")),
            new Intent().setComponent(new ComponentName("com.htc.pitroad", "com.htc.pitroad.landingpage.activity.LandingPageActivity")),
            new Intent().setComponent(new ComponentName("com.asus.mobilemanager", "com.asus.mobilemanager.MainActivity"))
    };


    @SuppressLint("StaticFieldLeak")
    static EditText longitude;
    @SuppressLint("StaticFieldLeak")
    static EditText latitude;

    static float longitudeText = 139.707341F;
    static float latitudeText = 35.646691F;

    @SuppressLint("StaticFieldLeak")
    private static Button startButton;

    private static MockLocationImpl mockLocation;

    private Context mContext;
    private Intent mNotificationIntent;
    private SharedPreferences mPreferences;
    private SharedPreferences.Editor mEditor;
    private static boolean isRunning = false;
    private boolean permissionLocationGranted = false;

    public SelectOnMapFragment() {
    }

    @SuppressLint({"SetJavaScriptEnabled", "CommitPrefEdits"})
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_select_on_map, container, false);

        mContext = view.getContext().getApplicationContext();
        mPreferences = mContext.getSharedPreferences("NAVIGINE_FAKE_GPS", Context.MODE_PRIVATE);
        mEditor = mPreferences.edit();

        ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION}, 101);


        boolean mNeedPref = mPreferences.getBoolean("NEED_PREF", true);

        for (Intent intent : POWERMANAGER_INTENTS)
            if (mContext.getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null && mNeedPref) {
                startActivity(intent);
                mEditor.putBoolean("NEED_PREF", false);
                mEditor.apply();
                break;
            }

        mockLocation = new MockLocationImpl(view.getContext());
        mNotificationIntent = new Intent(view.getContext().getApplicationContext(), NotificationService.class);

        longitude = view.findViewById(R.id.longitude);
        latitude = view.findViewById(R.id.latitude);
        startButton = view.findViewById(R.id.start_button);

        init();

        return view;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionLocationGranted = ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (requestCode != 101 || !permissionLocationGranted) {
            Snackbar.make(requireView(), "Location permission has not been granted", Snackbar.LENGTH_LONG);
        }
    }

    static void setLatLng(String mLat, String mLng) {
        latitudeText = Float.parseFloat(mLat);
        longitudeText = Float.parseFloat(mLng);

        latitude.setText(mLat);
        longitude.setText(mLng);
    }

    static String getLat() {
        return latitude.getText().toString();
    }

    static String getLng() {
        return longitude.getText().toString();
    }


    private void init() {
        LocationManager locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        permissionLocationGranted =
                ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                        ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (!permissionLocationGranted)
            return;

        Location networkLoc = Objects.requireNonNull(locationManager).getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        if (networkLoc != null && !isRunning) {
            longitudeText = (float)networkLoc.getLongitude();
            latitudeText = (float)networkLoc.getLatitude();
        } else if (!isRunning) {
            longitudeText = mPreferences.getFloat("LONGITUDE",  longitudeText);
            latitudeText = mPreferences.getFloat("LATITUDE", latitudeText);
        }

        longitude.setText(String.valueOf(longitudeText));
        latitude.setText(String.valueOf(latitudeText));

        longitude.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() != 0) {
                    longitudeText = Float.parseFloat(s.toString());
                    if (longitudeText <= 180.0 && longitudeText >= -180.0) {
                        mEditor.putFloat("LONGITUDE", (float) longitudeText);
                        mEditor.apply();
                        mContext.stopService(mNotificationIntent);
                        tryToStop();
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        latitude.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() != 0) {
                    latitudeText = Float.parseFloat(s.toString());
                    if (latitudeText <= 90.0 && latitudeText >= -90.0) {
                        mEditor.putFloat("LATITUDE", (float) latitudeText);
                        mEditor.apply();
                        mContext.stopService(mNotificationIntent);
                        tryToStop();
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        startButton.setText(isRunning ? "Stop" : "Start");

        startButton.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View v) {
                if (!permissionLocationGranted) {
                    Toast.makeText(v.getContext(), "Location permissions have not been granted. Launch the app again or grant permissions on settings.", Toast.LENGTH_LONG).show();
                    return;
                }
                if (!isMockLocationEnabled()) {
                    Toast.makeText(v.getContext(), "Please turn on Mock Location permission on Developer Settings", Toast.LENGTH_LONG).show();
                    startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
                    return;
                }

                if (isRunning) {
                    mContext.stopService(mNotificationIntent);
                    mockLocation.stopMockLocationUpdates();
                    startButton.setText("Start");
                } else {
                    mockLocation.startMockLocationUpdates(latitudeText, longitudeText);
                    startButton.setText("Stop");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        mContext.startForegroundService(mNotificationIntent);
                    } else {
                        mContext.startService(mNotificationIntent);
                    }
                }
                isRunning = !isRunning;
            }
        });
    }

    private boolean isMockLocationEnabled() {
        boolean isMockLocation;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                AppOpsManager opsManager = (AppOpsManager) mContext.getSystemService(Context.APP_OPS_SERVICE);
                isMockLocation = (Objects.requireNonNull(opsManager).checkOp(AppOpsManager.OPSTR_MOCK_LOCATION, android.os.Process.myUid(), BuildConfig.APPLICATION_ID) == AppOpsManager.MODE_ALLOWED);
            } else {
                isMockLocation = !android.provider.Settings.Secure.getString(mContext.getContentResolver(), "mock_location").equals("0");
            }
        } catch (Exception e) {
            return false;
        }
        return isMockLocation;
    }

    @SuppressLint("SetTextI18n")
    static void tryToStop() {
        if (isRunning) {
            mockLocation.stopMockLocationUpdates();
            startButton.setText("Start");
            isRunning = false;
        }
    }
}
