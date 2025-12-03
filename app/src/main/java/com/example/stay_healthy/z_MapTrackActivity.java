package com.example.stay_healthy;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

public class z_MapTrackActivity extends AppCompatActivity {
    private GoogleMap mMap;
    private FusedLocationProviderClient fused;
    private LocationCallback callback;
    private List<LatLng> path = new ArrayList<>();
    private Button startBtn, stopBtn;
    private TextView statusText;
    private boolean tracking = false;
    private static final int REQ = 1234;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_track);

        startBtn = findViewById(R.id.startBtn);
        stopBtn = findViewById(R.id.stopBtn);
        statusText = findViewById(R.id.statusText);

        fused = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(googleMap -> mMap = googleMap);

        callback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if(locationResult == null) return;
                for(Location loc : locationResult.getLocations()) {
                    LatLng p = new LatLng(loc.getLatitude(), loc.getLongitude());
                    path.add(p);
                    mMap.addPolyline(new PolylineOptions().addAll(path));
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(p, 16f));
                }
            }
        };

        startBtn.setOnClickListener(v -> {
            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ);
                return;
            }
            startTracking();
        });

        stopBtn.setOnClickListener(v -> stopTracking());
    }

    private void startTracking() {
        tracking = true;
        statusText.setText("Tracking...");
        LocationRequest req = LocationRequest.create();
        req.setInterval(2000);
        req.setFastestInterval(1000);
        req.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        fused.requestLocationUpdates(req, callback, null);
    }

    private void stopTracking() {
        tracking = false;
        statusText.setText("Stopped");
        fused.removeLocationUpdates(callback);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQ) {
            if(grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) startTracking();
            else Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
        }
    }
}
