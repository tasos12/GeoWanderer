package com.monk.geotraveler;

import android.app.ActionBar;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;


public class GoogleMapFragment extends Fragment
        implements OnMapReadyCallback {

    private final String TAG = "GoogleMap ";

    private OnFragmentInteractionListener mCallback;

    private LocationManager mLocationManager;
    private MapView mMapView;
    private GoogleMap mGoogleMap;

    private GeofencingClient mGeofencingClient;
    private List<MemoryFence> mMemoryFenceList;

    private View mMainScreenView;

    //View Components
    private TextView mDistanceMaxTodayTxt;
    private TextView mDistanceMaxTxt;

    //View Components Variables
    private User mUser;

    public static GoogleMapFragment newInstance() {
        GoogleMapFragment fragment = new GoogleMapFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLocationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        mGeofencingClient = LocationServices.getGeofencingClient(getActivity());
        mUser = new User();
        mMemoryFenceList = new ArrayList<MemoryFence>();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onStart() {
        super.onStart();
        mCallback = (OnFragmentInteractionListener) getActivity();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_map, container, false);
        mDistanceMaxTodayTxt = v.findViewById(R.id.distanceTodayTxt);
        mDistanceMaxTxt = v.findViewById(R.id.maxRangeTxt);
        mMainScreenView = v.getRootView().getRootView();
        return v;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mMapView = (MapView) view.findViewById(R.id.map);
        mMapView.onCreate(savedInstanceState);
        mMapView.onResume();
        mMapView.getMapAsync(this);//when you already implement OnMapReadyCallback in your fragment
    }

    @Override
    public void onMapReady(final GoogleMap mMap) {
        mGoogleMap = mMap;
        setupMapOptions();
        startLocationUpdates();
    }

    private void setupMapOptions(){
        mGoogleMap.setMyLocationEnabled(true);
        mGoogleMap.setBuildingsEnabled(false);
        mGoogleMap.getUiSettings().setAllGesturesEnabled(true);
        mGoogleMap.getUiSettings().setMapToolbarEnabled(false);
        mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mGoogleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(final LatLng point) {
                mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(point));
                setupPopupDisplay(point);
            }
        });
        setupMyHomeLocation();
    }

    private void setupPopupDisplay(final LatLng geofenceLatlng) {

        final View popupView = getLayoutInflater().inflate(R.layout.memory_popup_window, null);
        final PopupWindow popupWindow = new PopupWindow(popupView, ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT);

        Button createBtn =  popupView.findViewById(R.id.createMemoryBtn);
        final EditText memoryID = popupView.findViewById(R.id.memoryNameTxt);
        final EditText memoryDesc = popupView.findViewById(R.id.memoryDescriptionTxt);

        //Window settings
        popupWindow.setFocusable(true);
        popupWindow.setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
        popupWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        popupWindow.setContentView(popupView);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setAnimationStyle(R.style.popup_window_animation_phone);

        createBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateUserMemoriesView(memoryID.getText().toString(), memoryDesc.getText().toString(), geofenceLatlng);
                updateUserMemoriesDB(new MemoryFence(memoryID.getText().toString(), memoryDesc.getText().toString(), geofenceLatlng));
                popupWindow.dismiss();
            }
        });

        int location[] = new int[2];
        mMainScreenView.getLocationOnScreen(location);

        popupWindow.showAtLocation(mMainScreenView, Gravity.TOP, location[0], location[1]);
    }

    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_DWELL);

        ArrayList tempGeofenceList = new ArrayList<Geofence>();
        for(int i = 0;i<mMemoryFenceList.size(); i++){
            tempGeofenceList.add(mMemoryFenceList.get(i).getGeofence());
        }

        builder.addGeofences(tempGeofenceList);
        return builder.build();
    }

    private PendingIntent getGeofencePendingIntent(String description) {
        Intent intent = new Intent(getActivity(), GeofenceTransitionIntentService.class);
        intent.putExtra("description", description);
        PendingIntent mGeofencePendingIntent = PendingIntent.getService(getActivity(), 0, intent, PendingIntent.
                FLAG_UPDATE_CURRENT);
        return mGeofencePendingIntent;
    }

    private void addGeofences(String description){
        mGeofencingClient.addGeofences(getGeofencingRequest(), getGeofencePendingIntent(description)).
                addOnSuccessListener(getActivity(), new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Succesfully added Geofences");
                    }
                })
                .addOnFailureListener(getActivity(), new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "Failed to add Geofences");
                    }
                });
    }

    private MarkerOptions getMarkerOptions(LatLng latLng, String title ,float markerColor){
        MarkerOptions marker = new MarkerOptions();
        marker.title(title);
        marker.icon(BitmapDescriptorFactory.defaultMarker(markerColor));
        marker.position(latLng);

        return  marker;
    }

    private String getBestProvider(){
        Criteria criteria = new Criteria();
        String bestProvider = mLocationManager.getBestProvider(criteria,false);
        return bestProvider;
    }

    private void setupMyHomeLocation(){
        mGoogleMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                if(!mUser.isHomeLocationSet()) {
                    MarkerOptions marker = getMarkerOptions(latLng, "My Home", BitmapDescriptorFactory.HUE_AZURE);
                    mGoogleMap.clear();
                    mGoogleMap.addMarker(marker);
                    mUser.setHomeLocation(latLng.latitude, latLng.longitude);
                    updateUserHomeInfoDB(mUser);
                }
            }
        });
    }


    private void startLocationUpdates() {
        LocationListener locationListener = new MyLocationListener();
        mLocationManager.requestLocationUpdates(getBestProvider(), 100, 1, locationListener);
    }

    public void updateUserInfo(User user){
        mUser = user;
        mDistanceMaxTodayTxt.setText(Float.toString(user.getDistanceMaxTodayValue()));
        mDistanceMaxTxt.setText(Float.toString(user.getDistanceMaxValue()));
    }

    public void updateUserHomeView(User user){
        if(user.isHomeLocationSet()){
            LatLng latLng = new LatLng(user.getHomeLatitude(), user.getHomeLongitude());
            MarkerOptions marker = getMarkerOptions(latLng, "My Home", BitmapDescriptorFactory.HUE_AZURE);
            mGoogleMap.addMarker(marker);
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        }
        else mGoogleMap.clear();
    }

    public void updateUserMemoriesView(String id, String description, LatLng point){
        MemoryFence tempFence = new MemoryFence(id, description, point);
        mMemoryFenceList.add(tempFence);
        mGoogleMap.addCircle(new CircleOptions().center(point).radius(50).strokeColor(Color.BLUE));
        mGoogleMap.addMarker(getMarkerOptions(point, id, BitmapDescriptorFactory.HUE_RED));
        addGeofences(tempFence.getDescription());
    }

    public void updateUserInfoDB(User user){
        Bundle data = new Bundle();
        data.putFloat("maxDistanceToday", user.getDistanceMaxTodayValue());
        data.putFloat("maxDistance", user.getDistanceMaxValue());
        mCallback.onUserValueChange(getResources().getInteger(R.integer.UPDATE_USER_INFO_DB), data);
    }

    public void updateUserHomeInfoDB(User user){
        Bundle data = new Bundle();
        data.putDouble("homeLatitude", user.getHomeLatitude());
        data.putDouble("homeLongitude", user.getHomeLongitude());
        data.putBoolean("homeFlag", user.isHomeLocationSet());
        mCallback.onUserValueChange(getResources().getInteger(R.integer.UPDATE_USER_HOME_DB), data);
    }

    public void updateUserMemoriesDB(MemoryFence memoryFence){
        Bundle data = new Bundle();
        data.putDouble("latitude", memoryFence.getLatitude());
        data.putDouble("longitude", memoryFence.getLongitude());
        data.putString("description", memoryFence.getDescription());
        data.putString("id", memoryFence.getId());
        mCallback.onUserValueChange(getResources().getInteger(R.integer.UPDATE_MEMORIES_DB), data);
    }

    //Interface for communicating with other Activities
    public interface OnFragmentInteractionListener {
        void onUserValueChange(int code, Bundle data);
    }

    class MyLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {
            Date currentTime = Calendar.getInstance().getTime();
            Log.d("onLocationChanged", "Location Changed! Time: " + currentTime.getHours());
            if(mUser.isHomeLocationSet()) {
                Location home = new Location(getBestProvider());
                home.setLatitude(mUser.getHomeLatitude());
                home.setLongitude(mUser.getHomeLongitude());
                float distance = location.distanceTo(home);
                if(mUser.getDistanceMaxTodayValue() < distance){
                    mUser.setDistanceMaxTodayValue(distance);
                }
                if(mUser.getDistanceMaxValue() < distance){
                    mUser.setDistanceMaxValue(distance);
                }
                if(isAdded()) { //checks if fragment is attached  to activity(temp solution)
                    updateUserInfo(mUser);
                    updateUserInfoDB(mUser);
                }
            }
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {}

        @Override
        public void onProviderEnabled(String s) {}

        @Override
        public void onProviderDisabled(String s) {}
    }
}