package com.example.shiv.omw;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

import static com.example.shiv.omw.R.id.map;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private String duration;
    private String myCoords;
    private String coords;
    private LatLng sw;
    private LatLng ne;
    private LatLng[] lineSteps;
    private final String key = "&key=AIzaSyBvCV0lEysqdzYJLVXZ1Q37_6ZuKh4t0rU";
    private final String website = "https://maps.googleapis.com/maps/api/directions/json?units=imperial&origin=";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        duration = "";
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
//         Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(map);
        mapFragment.getMapAsync(this);
        getDistanceInfo gd = new getDistanceInfo(this);
        gd.execute();
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        Intent intent = getIntent();
        LatLng loc = new LatLng(intent.getDoubleExtra("MYLAT", 0), intent.getDoubleExtra("MYLONG", 0));
        LatLng end = new LatLng(intent.getDoubleExtra("LAT", 0), intent.getDoubleExtra("LONG", 0));
        mMap.addMarker(new MarkerOptions().position(loc).title("Marker at your location"));
        mMap.addMarker(new MarkerOptions().position(end).title("Marker at destination"));
//        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng((loc.latitude + end.latitude) / 2, (loc.longitude + end.longitude) / 2), 10));
        if (sw == null || ne == null) {
            sw = new LatLng(Math.min(loc.latitude, end.latitude), Math.min(loc.longitude, end.longitude));
            ne = new LatLng(Math.max(loc.latitude, end.latitude), Math.max(loc.longitude, end.longitude));
        }
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(new LatLngBounds(sw, ne), 2));
        try {
            mMap.setMyLocationEnabled(true);
        } catch (SecurityException e) {

        }
    }

    private class getDistanceInfo extends AsyncTask<Void, Void, String> {
        private Activity parent;

        public getDistanceInfo(Activity a) {
            a = parent;
        }

        @Override
        protected String doInBackground(Void... params) {
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            String jsonStr = "";
            String text = "";
            try {
                Intent intent = getIntent();
                myCoords = intent.getDoubleExtra("MYLAT", 0) + "," + intent.getDoubleExtra("MYLONG", 0);
                coords = intent.getDoubleExtra("LAT", 0) + "," + intent.getDoubleExtra("LONG", 0);
                String web = website + myCoords + "&destination=" + coords + key;
                Log.e("lol", web);

                URL url = new URL(web);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    return "";
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return "";
                }
                jsonStr = buffer.toString();
            } catch (IOException e) {
                return "";
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                    }
                }
            }
            JSONObject jsonObject = new JSONObject();
            try {

                jsonObject = new JSONObject(jsonStr);
                JSONArray array = jsonObject.getJSONArray("routes");
                JSONObject routes = array.getJSONObject(0);
                JSONArray legs = routes.getJSONArray("legs");
                JSONObject duration = legs.getJSONObject(0);
                JSONObject value = duration.getJSONObject("duration");

                JSONObject bounds = routes.getJSONObject("bounds");
                JSONObject northeast = bounds.getJSONObject("northeast");
                ne = new LatLng(Double.parseDouble(northeast.getString("lat")), Double.parseDouble(northeast.getString("lng")));
                JSONObject southwest = bounds.getJSONObject("southwest");
                sw = new LatLng(Double.parseDouble(southwest.getString("lat")), Double.parseDouble(southwest.getString("lng")));

                JSONArray steps = duration.getJSONArray("steps");
                lineSteps = new LatLng[steps.length() + 1];
                for (int i = 0; i < steps.length(); i++) {
                    JSONObject curr = steps.getJSONObject(i);
                    JSONObject start = curr.getJSONObject("start_location");
                    lineSteps[i] = new LatLng(Double.parseDouble(start.getString("lat")), Double.parseDouble(start.getString("lng")));
                }
                JSONObject curr = steps.getJSONObject(steps.length() - 1);
                JSONObject end = curr.getJSONObject("end_location");
                lineSteps[steps.length()] = new LatLng(Double.parseDouble(end.getString("lat")), Double.parseDouble(end.getString("lng")));
                Log.e("lol", Arrays.toString(lineSteps));
                text = value.getString("text");

            } catch (JSONException e) {
                e.printStackTrace();
            }
            return text;
        }

        @Override
        protected void onPostExecute(String result) {
            duration = result;
            sendMessage();
            addPolylines();
        }
    }

    public void sendMessage() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS},
                    1);
        }
        SmsManager smsManager = SmsManager.getDefault();
        String message = getResources().getString(R.string.message1) + " " + duration + " " + getResources().getString(R.string.message2);
        message += "\n I'm at " + myCoords + " and going to " + coords;
        try {
            smsManager.sendTextMessage(getIntent().getStringExtra("PHONENUM"), null, message, null, null);
            Toast.makeText(getApplicationContext(), "SMS sent.",
                    Toast.LENGTH_LONG).show();
        } catch (SecurityException e) {
            Toast.makeText(getApplicationContext(),
                    "SMS failed, please try again.", Toast.LENGTH_LONG).show();
        }
    }

    public void addPolylines() {
        Log.e("lol", Arrays.toString(lineSteps));
        for (int i = 0; i < lineSteps.length - 1; i++) {
            mMap.addPolyline(new PolylineOptions().clickable(false).add(lineSteps[i], lineSteps[i + 1]));
        }
    }
}
