package com.monk.geotraveler;

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

    LocationManager mLocationManager;
    private Location mHomeLocation;
    private boolean mIsHomeLocationSet = false;
    private MapView mMapView;
    private GoogleMap mGoogleMap;

    //View Components
    private TextView mDistanceMaxTodayTxt;
    private TextView mDistanceTotalTxt;
    private TextView mDistanceMaxTxt;

    //View Components Variables
    private float mDistanceMaxTodayValue = 0;
    private float mDistanceTotalValue = 0;
    private float mDistanceMaxValue = 0;

    public static GoogleMapFragment newInstance() {
        GoogleMapFragment fragment = new GoogleMapFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLocationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
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
        final MarkerOptions marker = setupMarker();
        setupMapOptions();
        startLocationUpdates();
        //getLastKnownLocation(marker);
    }

    public void setupMapOptions(){
        mGoogleMap.setMyLocationEnabled(true);
        mGoogleMap.setBuildingsEnabled(false);
        mGoogleMap.getUiSettings().setAllGesturesEnabled(true);
        mGoogleMap.getUiSettings().setMapToolbarEnabled(false);
        mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mGoogleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng point) {
                mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(point));
                PopupMenu popupMenu = new PopupMenu(/*getActivity().getApplicationContext()*/getContext(), getView(), Gravity.CENTER);
                popupMenu.getMenuInflater().inflate(R.menu.popup_menu, popupMenu.getMenu());
                popupMenu.show();
            }
        });
        setupMyHomeLocation();
    }

    public void setupMyHomeLocation(){
        mGoogleMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                if(!mIsHomeLocationSet) {
                    MarkerOptions tempMarker = new MarkerOptions();
                    tempMarker.title("My Home");
                    tempMarker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
                    tempMarker.position(latLng);
                    mHomeLocation = new Location(getBestProvider());
                    mHomeLocation.setLatitude(latLng.latitude);
                    mHomeLocation.setLongitude(latLng.longitude);
                    mGoogleMap.clear();
                    mGoogleMap.addMarker(tempMarker);
                    mIsHomeLocationSet = true;
                }
            }
        });
    }

    public MarkerOptions setupMarker(){
        MarkerOptions mMarker = new MarkerOptions();
        mMarker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
        return mMarker;
    }

    public String getBestProvider(){
        Criteria criteria = new Criteria();
        String bestProvider = mLocationManager.getBestProvider(criteria,true);
        return bestProvider;
    }

    private void startLocationUpdates() {
        LocationListener locationListener = new MyLocationListener();
        mLocationManager.requestLocationUpdates(getBestProvider(), 10, 1, locationListener);
    }

    public void getLastKnownLocation(MarkerOptions mMarker){
        LocationManager locationManager = (LocationManager) getActivity().getSystemService(LOCATION_SERVICE);
        Location location = locationManager.getLastKnownLocation(getBestProvider());
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        mGoogleMap.addMarker(mMarker.position(latLng));
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 21));
    }

    //Interface for communicating with other Activities
    public interface OnFragmentInteractionListener {
        void onMapFragmentInteraction(Uri uri);
    }

    class MyLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {
            Date currentTime = Calendar.getInstance().getTime();
            Log.d("onLocationChanged", "Location Changed! Time: " + currentTime.getHours());
            if(mIsHomeLocationSet) {
                float distance = location.distanceTo(mHomeLocation);
                if(mDistanceMaxTodayValue < distance){
                    mDistanceMaxTodayValue = distance;
                    mDistanceMaxTodayTxt.setText(Float.toString(mDistanceMaxTodayValue));
                }
                if(mDistanceMaxValue < distance ){
                    mDistanceMaxValue = distance;
                    mDistanceTotalTxt.setText(Float.toString(mDistanceTotalValue));
                }
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