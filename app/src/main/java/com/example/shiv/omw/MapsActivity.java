package com.example.shiv.omw;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private String duration;
    private final String key = "&key=AIzaSyDP59UlozopevJwm3jocD7jK6Yz7dXb4qs";
    private final String website = "https://maps.googleapis.com/maps/api/distancematrix/json?units=imperial&origins=";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        duration = "";
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        getDistanceInfo gd = new getDistanceInfo();
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
        mMap.addMarker(new MarkerOptions().position(loc).title("Marker at your location"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(loc));
    }

    private class getDistanceInfo extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            String jsonStr = "";
            String text = "";
            try {
                Intent intent = getIntent();
                String web = website + intent.getDoubleExtra("MYLAT", 0) + "," + intent.getDoubleExtra("MYLONG", 0) + "&destinations=" + intent.getDoubleExtra("LAT", 0) + "," + intent.getDoubleExtra("LONG", 0) + key;

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
                JSONArray array = jsonObject.getJSONArray("rows");

                JSONObject rows = array.getJSONObject(0);

                JSONArray elements = rows.getJSONArray("elements");

                JSONObject duration = elements.getJSONObject(0);

                JSONObject value = duration.getJSONObject("duration");

                text = value.getString("text");

            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return text;
        }

        @Override
        protected void onPostExecute(String result) {
            duration = result;
            Log.e("lol", duration);
        }
    }
}
