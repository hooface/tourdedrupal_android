package com.yanniboi.tourdedrupal;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import android.net.ConnectivityManager;
import android.app.Activity;
import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;
import android.widget.Button;
//import android.util.Log;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.widget.TextView;

public class MainActivity extends Activity {
 
    private static final long MINIMUM_DISTANCE_CHANGE_FOR_UPDATES = 50; // in Meters
    private static final long MINIMUM_TIME_BETWEEN_UPDATES = 1000 * 60; // in Milliseconds
    private static final int RESULT_SETTINGS = 1;

   
    public LocationManager locationManager;
    public LocationListener mlocListener; 
    protected Location activeLocation;
    protected ConnectivityManager connectivityManager;
    protected Button settingsButton;
    protected Button retrieveLocationButton;
    protected Button sendLocationButton;

	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.activity_main);

    	retrieveLocationButton = (Button) findViewById(R.id.retrieve_location_button);
    	sendLocationButton = (Button) findViewById(R.id.send_location_button);
  
    	locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        SharedPreferences sharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(this);

        String time = sharedPrefs.getString("prefUpdateFrequency", "60000");

        mlocListener = new MyLocationListener();
        if (sharedPrefs.getBoolean("prefRunBackground", false)) {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 
                Long.valueOf(time), 
                MINIMUM_DISTANCE_CHANGE_FOR_UPDATES,
                mlocListener
            );
        }
  
    	connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		if ((connectivityManager.getActiveNetworkInfo() != null) && connectivityManager.getActiveNetworkInfo().isAvailable() && connectivityManager.getActiveNetworkInfo().isConnected()) {
			Toast.makeText(MainActivity.this, getString(R.string.update_online), Toast.LENGTH_LONG).show();
		}
		else {
			Toast.makeText(MainActivity.this, getString(R.string.update_offline), Toast.LENGTH_LONG).show();
		}
  
		retrieveLocationButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showCurrentLocation();
			}
		});   

		sendLocationButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				new updateTask().execute();
			}
		});

        showUserSettings();
    }
 
    @Override
    protected void onDestroy() {
        stopListening();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        SharedPreferences sharedPrefs = PreferenceManager
            .getDefaultSharedPreferences(this);
        if (sharedPrefs.getBoolean("prefRunBackground", false) == false) {
            Toast.makeText(MainActivity.this, String.valueOf(sharedPrefs.getBoolean("prefRunBackground", false)), Toast.LENGTH_LONG).show();
            stopListening();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        SharedPreferences sharedPrefs = PreferenceManager
            .getDefaultSharedPreferences(this);

        String time = sharedPrefs.getString("prefUpdateFrequency", "60000");
        if (sharedPrefs.getBoolean("prefRunBackground", false)) {
            startListening(time);
        }
        super.onResume();
    }
    
    private void startListening(String time) {
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER, 
            Long.valueOf(time), 
            MINIMUM_DISTANCE_CHANGE_FOR_UPDATES,
            mlocListener
        );
    }

    private void stopListening() {
        locationManager.removeUpdates(mlocListener);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	getMenuInflater().inflate(R.menu.settings, menu);
    	return true;
    }
 
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
 
        case R.id.menu_settings:
            Intent i = new Intent(this, UserSettingActivity.class);
            startActivityForResult(i, RESULT_SETTINGS);
            break;
 
        }
 
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
 
        switch (requestCode) {
        case RESULT_SETTINGS:
            showUserSettings();
            break;
 
        }
 
    }
 
    private void showUserSettings() {
        SharedPreferences sharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(this);
 
        StringBuilder builder = new StringBuilder();
 
        builder.append("\n Username: "
                + sharedPrefs.getString("prefUsername", "NULL"));
 
        builder.append("\n Run in background:"
                + sharedPrefs.getBoolean("prefRunBackground", false));
 
        builder.append("\n Update Frequency: "
                + sharedPrefs.getString("prefUpdateFrequency", "NULL"));
 
        TextView settingsTextView = (TextView) findViewById(R.id.textUserSettings);
 
        settingsTextView.setText(builder.toString());
    }

    public NotificationManager getNotificationManager() {
    	return (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }
 
    public void showCurrentLocation() {
    	Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

    	if (location != null) {
    		String message = String.format(
				"Current Location \n Longitude: %1$s \n Latitude: %2$s",
                 location.getLongitude(), location.getLatitude()
    		);
    		Toast.makeText(MainActivity.this, message,
    		Toast.LENGTH_LONG).show();
    	}
    }


 
    /**
     * Update task.
     */
    class updateTask extends AsyncTask<Context, Integer, String> {

    	protected String doInBackground(Context... params) {

    		int siteStatus = -1;

    		try {
    			siteStatus = sendCurrentLocation();
    		}
    		catch (IOException ignored) {}
             
    		if (siteStatus == 200) {
    			return "online";
    		}
    		else {
    			return "offline";
    		}
    	}
     
     
    	@Override
    	protected void onPostExecute(String sResponse) {
    		if (sResponse.equals("offline")) {
    			String message = "site response is not 200....";
    			Toast.makeText(MainActivity.this, message,
					Toast.LENGTH_LONG).show();
    		}
    		else if (sResponse.equals("online")) {
    			String message = "site response is 200! :)";
    			Toast.makeText(MainActivity.this, message,
    				Toast.LENGTH_LONG).show();
    		}
    		else {
    			String message = "unknown error";
    			Toast.makeText(MainActivity.this, message,
    				Toast.LENGTH_LONG).show();
    		}     
    	}
    }
     
    /**
     * Download the program from the internet and save it locally.
     */
    public int sendCurrentLocation() throws IOException {
    	int siteStatus = -1;

    	if (activeLocation == null) {
        	activeLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
    	}
    	//Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

    	if (activeLocation != null) {
    		String lat = Double.toString(activeLocation.getLatitude());
    		String lng = Double.toString(activeLocation.getLongitude());
     
    		try {
    			String link = "http://six-gs.com/autoping/checkin?id=1234&lat=" + lat + "&lng=" + lng;

    			URL url = new URL(link);

    			HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
    			siteStatus = urlConnection.getResponseCode();
	     
    			urlConnection.disconnect();
    		}
    		catch (IOException ignored) {}
    	}
    	return siteStatus;
    }


    private class MyLocationListener implements LocationListener {

    	public void onLocationChanged(Location location) {
    		String message = String.format(
    			"New Location \n Longitude: %1$s \n Latitude: %2$s",
    			location.getLongitude(), location.getLatitude()
    		);
    		//Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
    		
    		// Make sure a location is set.
        	if (activeLocation == null) {
            	activeLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        	}
        	
        	// Check if new location is better.
        	if (isBetterLocation(location, activeLocation)) {
        		new updateTask().execute();
        		String distance = Float.toString(location.distanceTo(activeLocation));
            	activeLocation = location;
            	
        		Toast.makeText(MainActivity.this, message + " Distance: " + distance, Toast.LENGTH_LONG).show();
        	}

    	}

    	public void onStatusChanged(String s, int i, Bundle b) {
    		Toast.makeText(MainActivity.this, "Provider status changed",
                 Toast.LENGTH_LONG).show();
    	}

    	public void onProviderDisabled(String s) {
    		Toast.makeText(MainActivity.this,
                 "Provider disabled by the user. GPS turned off",
                 Toast.LENGTH_LONG).show();
    	}

    	public void onProviderEnabled(String s) {
    		Toast.makeText(MainActivity.this,
                 "Provider enabled by the user. GPS turned on",
                 Toast.LENGTH_LONG).show();
    	}
    }

    /**
     * Determines whether one location reading is better than the current location.
     * 
     * @param location
     *            The new Location that you want to evaluate
     * @param currentBestLocation
     *            The current Location fix, to which you want to compare the new one
     *            
     * @return indicates if you should use the new location
     */
    public static boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > MINIMUM_TIME_BETWEEN_UPDATES;
        boolean isSignificantlyOlder = timeDelta < - MINIMUM_TIME_BETWEEN_UPDATES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same locationProvider
        boolean isFromSameProvider = isSameProvider(location.getProvider(), currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /**
     * Validates if the provider are equal.
     * 
     * @param provider1 - provider
     * @param provider2 - provider
     * 
     * @return <code>TRUE</code> if the provider are the same
     */
    public static boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }
}