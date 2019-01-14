package com.monk.geotraveler;

import android.app.Activity;
import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.Calendar;
import java.util.Date;

import static android.content.Context.LOCATION_SERVICE;

public class GoogleMapFragment extends Fragment
        implements OnMapReadyCallback {

    private final String TAG = "GoogleMap ";

    OnFragmentInteractionListener mCallback;

    LocationManager mLocationManager;
    private MapView mMapView;
    private GoogleMap mGoogleMap;

    //View Components
    private TextView mDistanceMaxTodayTxt;
    private TextView mDistanceTotalTxt;
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
        mUser = new User();
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
        mDistanceTotalTxt = v.findViewById(R.id.distanceTotalTxt);
        mDistanceMaxTxt = v.findViewById(R.id.maxRangeTxt);
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
            public void onMapClick(LatLng point) {
                mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(point));
                PopupMenu popupMenu = new PopupMenu(getContext(), getView(), Gravity.CENTER);
                popupMenu.getMenuInflater().inflate(R.menu.popup_menu, popupMenu.getMenu());
                popupMenu.show();
            }
        });
        setupMyHomeLocation();
    }

    private void setupMyHomeLocation(){
        mGoogleMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                if(!mUser.isHomeLocationSet()) {
                    MarkerOptions marker = getMarkerOptions(latLng);
                    mGoogleMap.clear();
                    mGoogleMap.addMarker(marker);
                    mUser.setHomeLocation(latLng.latitude, latLng.longitude);
                    updateUserHomeInfoDB(mUser);
                }
            }
        });
    }

    private MarkerOptions getMarkerOptions(LatLng latLng){
        MarkerOptions marker = new MarkerOptions();
        marker.title("My Home");
        marker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
        marker.position(latLng);

        return  marker;
    }

    public String getBestProvider(){
        Criteria criteria = new Criteria();
        String bestProvider = mLocationManager.getBestProvider(criteria,false);
        return bestProvider;
    }

    private void startLocationUpdates() {
        LocationListener locationListener = new MyLocationListener();
        mLocationManager.requestLocationUpdates(getBestProvider(), 100, 1, locationListener);
    }

    public void updateUserInfo(User user){
        mUser = user;
        mDistanceTotalTxt.setText(Float.toString(user.getDistanceTotalValue()));
        mDistanceMaxTodayTxt.setText(Float.toString(user.getDistanceMaxTodayValue()));
        mDistanceMaxTxt.setText(Float.toString(user.getDistanceMaxValue()));
    }

    public void updateUserHomeView(User user){
        /*Log.d(TAG, "updateUserHomeView: Is home Location Set?:" + user.isHomeLocationSet()
                + "| " + user.getHomeLatitude() + "| " + user.getHomeLongitude());*/
        if(user.isHomeLocationSet()){
            LatLng latLng = new LatLng(user.getHomeLatitude(), user.getHomeLongitude());
            MarkerOptions marker = getMarkerOptions(latLng);
            mGoogleMap.addMarker(marker);
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        }
        else mGoogleMap.clear();
    }

    public void updateUserInfoDB(User user){
        Bundle data = new Bundle();
        data.putFloat("maxDistanceToday", user.getDistanceMaxTodayValue());
        data.putFloat("maxDistance", user.getDistanceMaxValue());
        data.putFloat("totalDistance", user.getDistanceTotalValue());
        mCallback.onUserValueChange(getResources().getInteger(R.integer.UPDATE_USER_INFO_DB), data);
    }

    public void updateUserHomeInfoDB(User user){
        Bundle data = new Bundle();
        data.putDouble("homeLatitude", user.getHomeLatitude());
        data.putDouble("homeLongitude", user.getHomeLongitude());
        data.putBoolean("homeFlag", user.isHomeLocationSet());
        mCallback.onUserValueChange(getResources().getInteger(R.integer.UPDATE_USER_HOME_DB), data);
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
                updateUserInfo(mUser);
                updateUserInfoDB(mUser);
            }
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {
        }

        @Override
        public void onProviderEnabled(String s) {
        }

        @Override
        public void onProviderDisabled(String s) {
        }
    }
}