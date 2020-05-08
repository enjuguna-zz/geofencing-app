package com.example.geofencing_app;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private Marker currentUser;
    private DatabaseReference myLocationRef;
    private GeoFire geoFire;
    private List<LatLng> advertisingArea;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        Dexter.withActivity(this)
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        buildLocationRequest();
                        buildLocationCallback();
                        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MapsActivity.this);


                        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                                .findFragmentById(R.id.map);
                        mapFragment.getMapAsync(MapsActivity.this);

                        settingGeoFire();
                        initArea();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        Toast.makeText(MapsActivity.this, "You must enable location permission", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {

                    }
                }).check();


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.

    }

    private void initArea() {
        advertisingArea = new ArrayList<>();
        advertisingArea.add(new LatLng(-1.2891,  36.8266));
        advertisingArea.add(new LatLng(-0.2850, 36.0693));
        advertisingArea.add(new LatLng(-0.0982, 34.7620));

        FirebaseDatabase.getInstance()
                .getReference("AdvertisingArea")
                .push()
                .setValue(advertisingArea)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Toast.makeText(MapsActivity.this, "Updated", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MapsActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void settingGeoFire() {
    myLocationRef = FirebaseDatabase.getInstance().getReference("MyLocation");
    geoFire = new GeoFire(myLocationRef);

    }

    private void buildLocationCallback() {
    locationCallback = new LocationCallback(){
        @Override
        public void onLocationResult(final LocationResult locationResult) {
           if(mMap != null){


               geoFire.setLocation("You", new GeoLocation(locationResult.getLastLocation().getLatitude(),
                       locationResult.getLastLocation().getLongitude()), new GeoFire.CompletionListener() {
                   @Override
                   public void onComplete(String key, DatabaseError error) {
                       if(currentUser != null) currentUser.remove();
                       currentUser = mMap.addMarker(new MarkerOptions()
                               .position(new LatLng(locationResult.getLastLocation().getLatitude(),
                                       locationResult.getLastLocation().getLongitude()))
                               .title("You"));
                       //After adding the Marker, Move the Camera
                       mMap.animateCamera(CameraUpdateFactory
                               .newLatLngZoom(currentUser.getPosition(), 12.0f));
                   }
               });
           }
        }
    };

    }

    private void buildLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setSmallestDisplacement(10f);

    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

      mMap.getUiSettings().setZoomControlsEnabled(true);

      if (fusedLocationProviderClient != null)
          fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());

      //Add a Circle for the Advertising Area
        for (LatLng latLng: advertisingArea){
            mMap.addCircle(new CircleOptions().center(latLng)
            .radius(500) //500 Meters
            .strokeColor(Color.BLUE)
            .fillColor(0x220000FF)//22 is a transparent Code
            .strokeWidth(5.0f)
            );

       //Create a GeoQuery when the user is in the Advertising Location
            GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(latLng.latitude, latLng.longitude) ,0.5f);//300m
            geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
                @Override
                public void onKeyEntered(String key, GeoLocation location) {
                    sendNotification("Geofence", String.format(" %s You have Entered the Advertising area", key));
                    Toast.makeText(MapsActivity.this, "You have Entered the Advertising Area", Toast.LENGTH_SHORT).show();
                }
                    //When the user exits the Advertised area
                @Override
                public void onKeyExited(String key) {
                    sendNotification("Geofence", String.format(" %s You have Left the Advertising area", key));
                    Toast.makeText(MapsActivity.this, "You have left the Advertising Area", Toast.LENGTH_SHORT).show();
                }
                    //When the user is within the advertised area
                @Override
                public void onKeyMoved(String key, GeoLocation location) {
                  //  sendNotification("Geofence", String.format(" %s You are moving within Advertising area", key));
                }

                @Override
                public void onGeoQueryReady() {

                }

                @Override
                public void onGeoQueryError(DatabaseError error) {
                    Toast.makeText(MapsActivity.this, ""+error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void sendNotification(String title, String content) {
        String NOTIFICATION_CHANNEL_ID = "edmt_multiple_location";
        NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "My Notification",
                    NotificationManager.IMPORTANCE_DEFAULT);

            //config
            notificationChannel.setDescription("Channel Description");
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setVibrationPattern(new long[]{0,1000,500,1000});
            notificationChannel.enableVibration(true);
            notificationManager.createNotificationChannel(notificationChannel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        builder.setContentTitle(title)
                .setContentText(content)
                .setAutoCancel(false)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),R.mipmap.ic_launcher));

        Notification notification = builder.build();
        notificationManager.notify(new Random().nextInt(), notification);

    }

    @Override
    protected void onStop() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        super.onStop();


    }



    }


