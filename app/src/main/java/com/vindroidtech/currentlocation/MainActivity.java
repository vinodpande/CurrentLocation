package com.vindroidtech.currentlocation;

import android.Manifest;
import android.content.Context;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements LocationListener, View.OnClickListener {

    private static final int REQUEST_CHECK_SETTINGS = 1;
    LocationManager locationManager;
    Button btnGetLocation, btnSaveLocation;
    EditText txtLatitude, txtLongitude, txtAddress, txtSpot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (hasGPSOnDevice(this)) {
            if (isGPSEnable(this)) {
                Toast.makeText(this, "GPS Enable", Toast.LENGTH_LONG).show();
            } else {
                enableGPSSetting(this);
            }
        } else Toast.makeText(this, "No GPS Service to Your Device", Toast.LENGTH_LONG).show();


        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        btnGetLocation = findViewById(R.id.btnGetLocation);
        btnGetLocation.setOnClickListener(this);
        btnSaveLocation = findViewById(R.id.btnSaveLocation);
        btnSaveLocation.setOnClickListener(this);
        txtLatitude = findViewById(R.id.txtlatitude);
        txtLongitude = findViewById(R.id.txtLongitude);
        txtAddress = findViewById(R.id.txtAddress);
        txtSpot = findViewById(R.id.txtSpot);


    }

    @Override
    public void onLocationChanged(Location location) {

        if (location != null) {
            txtLatitude.setText(String.valueOf(location.getLatitude()));
            txtLongitude.setText(String.valueOf(location.getLongitude()));
            txtAddress.setText(getAddress(location));
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

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btnGetLocation) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1, 1, this);
            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            onLocationChanged(location);
        }

        if (view.getId() == R.id.btnSaveLocation) {

            sendDataTOServer();

        }

    }

    String getAddress(Location location) {
        StringBuilder stringBuilder = new StringBuilder();
        Geocoder gcd = new Geocoder(getBaseContext(), Locale.getDefault());
        List<Address> addresses;
        try {
            addresses = gcd.getFromLocation(location.getLatitude(),
                    location.getLongitude(), 1);
            if (addresses.size() > 0) {
                stringBuilder.append(addresses.get(0).getLocality());
                stringBuilder.append("\n");
                stringBuilder.append(addresses.get(0).getAddressLine(0));
                stringBuilder.append("\n");
                stringBuilder.append(addresses.get(0).getFeatureName());
                stringBuilder.append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stringBuilder.toString();
    }

    public void sendDataTOServer() {


        if (TextUtils.isEmpty(txtLatitude.getText().toString()) && TextUtils.isEmpty(txtLongitude.getText().toString()) && TextUtils.isEmpty(txtAddress.getText().toString())) {
            Toast.makeText(MainActivity.this, "Empty", Toast.LENGTH_LONG).show();
        } else {
            LocationModel locationModel = new LocationModel();
            locationModel.setLatitude(txtLatitude.getText().toString());
            locationModel.setLongitude(txtLongitude.getText().toString());
            locationModel.setAddress(txtAddress.getText().toString());
            locationModel.setSpot(txtSpot.getText().toString());
            new SendToServer(locationModel).execute();
        }

    }


    class SendToServer extends AsyncTask<Void, Void, Void> {
        LocationModel locationModel;
        String response;

        public SendToServer(LocationModel locationModel) {
            this.locationModel = locationModel;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                GetText(locationModel);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Toast.makeText(getApplicationContext(), response, Toast.LENGTH_LONG).show();
        }

        public void GetText(LocationModel locationModel) throws UnsupportedEncodingException {


            // Create data variable for sent values to server

            String data = URLEncoder.encode("latitude", "UTF-8")
                    + "=" + URLEncoder.encode(locationModel.getLatitude(), "UTF-8");

            data += "&" + URLEncoder.encode("longitude", "UTF-8") + "="
                    + URLEncoder.encode(locationModel.getLongitude(), "UTF-8");

            data += "&" + URLEncoder.encode("address", "UTF-8")
                    + "=" + URLEncoder.encode(locationModel.getAddress(), "UTF-8");

            data += "&" + URLEncoder.encode("spot", "UTF-8")
                    + "=" + URLEncoder.encode(locationModel.getSpot(), "UTF-8");

            BufferedReader reader = null;

            // Send data
            try {
                // Defined URL  where to send data
                URL url = new URL("http://abadgraminpolice.vindroidtech.com/accidentspotsave.php");
                // Send POST data request

                URLConnection conn = url.openConnection();
                conn.setDoOutput(true);
                OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
                wr.write(data);
                wr.flush();

                // Get the server response

                reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line = null;
                // Read Server Response
                while ((line = reader.readLine()) != null) {
                    // Append server response in string
                    sb.append(line + "\n");
                }
                response = sb.toString();
                Log.d("responsee", "hi" + response);


            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                try {
                    reader.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

        }
    }


    public boolean hasGPSOnDevice(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) return false;
        List<String> provider = locationManager.getAllProviders();
        if (provider == null) return false;
        return provider.contains(locationManager.GPS_PROVIDER);
    }

    public boolean isGPSEnable(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(locationManager.GPS_PROVIDER);
    }

    private void enableGPSSetting(Context context) {
        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(context)
                .addApi(LocationServices.API).build();
        googleApiClient.connect();

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(10000 / 2);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
        builder.setAlwaysShow(true);

        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        Log.i("TAGONE", "All location settings are satisfied.");
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        Log.i("TAGONE", "Location settings are not satisfied. Show the user a dialog to upgrade location settings ");

                        try {
                            // Show the dialog by calling startResolutionForResult(), and check the result
                            // in onActivityResult().
                            status.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException e) {
                            Log.i("TAGONE", "PendingIntent unable to execute request.");
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        Log.i("TAGONE", "Location settings are inadequate, and cannot be fixed here. Dialog not created.");
                        break;
                }
            }
        });
    }

}
