package com.monk.geotraveler;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;


public class MainScreen extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        GoogleMapFragment.OnFragmentInteractionListener{

    private final String TAG = "GeoWandererMain";
    private FirebaseUser mFirebaseUser;
    private GoogleMapFragment mGoogleMapFragment;
    private DatabaseReference mDatabase;
    private User mUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_screen);

        //Auto Generated Code
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //Receiving Intent
        Intent intent = getIntent();
        mFirebaseUser = (FirebaseUser) intent.getExtras().get("ACC_INFO");

        //Changing welcome message
        TextView contentScreenWelcomeMessage = findViewById(R.id.contentScreenWelcomeMessage);
        contentScreenWelcomeMessage.setText("Welcome " + mFirebaseUser.getDisplayName());

        //Changing user info at navigation view
        View header = navigationView.getHeaderView(0);
        TextView barGmailNameText = header.findViewById(R.id.barGmailNameText);
        TextView barGmailText = header.findViewById(R.id.barGmailText);
        barGmailNameText.setText(mFirebaseUser.getDisplayName());
        barGmailText.setText(mFirebaseUser.getEmail());

        //Firebase database
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mDatabase.child("users").child(mFirebaseUser.getUid()).addListenerForSingleValueEvent(getUserListener());

        //Google Maps Fragment
        mGoogleMapFragment = (GoogleMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            //super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_screen, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            mUser.setHomeLocation(-1, -1);
            mGoogleMapFragment.updateUserHomeInfoDB(mUser);
            mGoogleMapFragment.updateUserHomeView(mUser);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.sign_out) {
            setResult(getResources().getInteger(R.integer.SIGN_OUT));
            finish();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onUserValueChange(int code, Bundle data) {
        if(code == getResources().getInteger(R.integer.UPDATE_USER_INFO_DB)){
            UserDataUpdaterAsync dataUpdater = new UserDataUpdaterAsync(data);
            dataUpdater.execute();
        } else if(code == getResources().getInteger(R.integer.UPDATE_USER_HOME_DB)){
            UserHomeUpdaterAsync homeUpdater = new UserHomeUpdaterAsync(data);
            homeUpdater.execute();
        }
    }

    private ValueEventListener getUserListener(){
        ValueEventListener userListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG, "onDataChange: Data updated");
                if(dataSnapshot.exists()) {     //If user exists in database
                    Log.d(TAG, "onDataChange: User Exists");
                    mUser = dataSnapshot.getValue(User.class);
                }
                else{
                    Log.d(TAG, "onDataChange: User does not exist");
                    mUser = new User(0, 0, 0);
                    writeNewUser(mFirebaseUser.getUid(), mUser);
                }
                mGoogleMapFragment.updateUserInfo(mUser);
                mGoogleMapFragment.updateUserHomeView(mUser);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
            }
        };
        return userListener;
    }

    private void writeNewUser(String userID, User user){
        mDatabase.child("users").child(userID).setValue(user);
    }

    private class UserDataUpdaterAsync extends AsyncTask<Void, Void, Void>{

        Bundle data;

        public UserDataUpdaterAsync(Bundle data){
            this.data = data;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            float maxDistanceToday = (float) data.get("maxDistanceToday");
            float maxDistance = (float) data.get("maxDistance");
            float totalDistance = (float) data.get("totalDistance");
            mUser.setUserInfo(maxDistanceToday, maxDistance, totalDistance);
            writeNewUser(mFirebaseUser.getUid(), mUser);

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Log.d(TAG, "User values updated Async");
        }
    }

    private class UserHomeUpdaterAsync extends AsyncTask<Void, Void, Void>{

        Bundle data;

        public UserHomeUpdaterAsync(Bundle data){
            this.data = data;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            double latitude = data.getDouble("homeLatitude");
            double longitude = data.getDouble("homeLongitude");
            mUser.setHomeLocation(latitude, longitude);
            writeNewUser(mFirebaseUser.getUid(), mUser);

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Log.d(TAG, "User home location updated");
        }
    }

}
