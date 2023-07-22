package com.navigine.mockgps;

import android.os.Build;
import android.os.Bundle;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

public class MainActivity extends AppCompatActivity {
    final private Fragment mSelectOnMap = new SelectOnMapFragment();
    final private FragmentManager mManager = getSupportFragmentManager();

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mManager.beginTransaction().add(R.id.main__frame_layout, mSelectOnMap, "SelectOnMap").commitAllowingStateLoss();

    }
}
