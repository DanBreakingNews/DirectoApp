package com.custom.plugin;

import android.os.Build;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.provider.Settings.Global;
import android.provider.Settings;
import android.app.Activity;

import android.location.LocationManager;

import android.location.Criteria;
import android.location.Location;
import android.Manifest;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.Context;

import android.support.v4.app.ActivityCompat;

import android.os.IBinder;
import android.app.Service;
import android.content.Intent;
import android.location.LocationListener;
import android.os.Bundle;

/**
 * This class echoes a string called from JavaScript.
 */
public class GPSDanBreakingNews extends CordovaPlugin {

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("getGPSLocation")) {
            this.getGPSLocation(callbackContext);
            return true;
        }
        return false;
    }
    
    //@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void getGPSLocation(CallbackContext callbackContext) 
	{    
        String mensaje = "";

        if (!this.isDateAndTimeFromNetwork()) {
            mensaje += "Error, debe tomar la fecha y hora de la red.";
        }

        if (isMockLocationEnabled()) {
            if( mensaje.length() > 0 )
            {
                mensaje += "\n";
            }
            mensaje += "Error, su ubicación geográfica es falsa";
        }
        
        this.removeTestProviders();

        if( mensaje.length() > 0 )
        {
            callbackContext.error(mensaje);
        }
        else
        {
            Context context = this.cordova.getActivity().getApplicationContext();
            GPSTracker gps = new GPSTracker(context);

            if(gps.canGetLocation())
            {
                int contador = 10;
                do
                {
                    contador--;

                    if( gps.location == null )
                    {
                        gps.stopUsingGPS();
                        gps = new GPSTracker(context);
                        try {
                            Thread.sleep(300);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }while( contador > 0 );

                if( gps.location != null )
                {
                    try
                    {
                        JSONObject item = new JSONObject();
                        item.put("latitud", gps.getLatitude());
                        item.put("longitud",gps.getLongitude());
                        
                        callbackContext.success(item.toString());
                    }
                    catch( JSONException ex )
                    {
                        callbackContext.error("Error al obtener la ubicación geográfica");
                    }
                }
                else
                {
                    callbackContext.error("Error al obtener la ubicación geográfica");
                }
                gps.stopUsingGPS();
            }
            else
            {
                callbackContext.error("Debe habilitar el GPS o la Red para poder obtener la ubicación geográfica");
            }
        }
    }

    public boolean isMockLocationEnabled() {
        boolean isMockLocation = false;
        try {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {

                LocationManager locationManager = (LocationManager) this.cordova.getActivity().getSystemService(Context.LOCATION_SERVICE);
                Criteria criteria = new Criteria();
                String provider = locationManager.getBestProvider(criteria, true);

                if (ActivityCompat.checkSelfPermission(this.cordova.getActivity().getApplicationContext(), 
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this.cordova.getActivity().getApplicationContext(), 
                        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                {
                    Location location = locationManager.getLastKnownLocation(provider);
                    isMockLocation = location.isFromMockProvider();
                }
            }
            else
            {
                if(Settings.Secure.getInt(this.cordova.getActivity().getContentResolver(), Settings.Secure.ALLOW_MOCK_LOCATION) == 1);
                    isMockLocation = true;
            }
        }
        catch (Exception e)
        {
            return isMockLocation;
        }

        return isMockLocation;
    }

    private void removeTestProviders()
    {
        LocationManager lm = (LocationManager) this.cordova.getActivity().getSystemService(Context.LOCATION_SERVICE);
        try {
            lm.removeTestProvider(LocationManager.GPS_PROVIDER);
            lm.removeTestProvider(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ex) {
        }
    }

    private boolean isDateAndTimeFromNetwork() {
        boolean isDateAndTimeFromNetwork = false;
        try
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                if( Global.getInt(this.cordova.getActivity().getContentResolver(), Global.AUTO_TIME) == 1 &&
                    Global.getInt(this.cordova.getActivity().getContentResolver(), Global.AUTO_TIME_ZONE) == 1
                        )
                {
                    isDateAndTimeFromNetwork = true;
                }
                else
                {
                    isDateAndTimeFromNetwork = false;
                }
            }else
            {
                if(Settings.System.getInt(this.cordova.getActivity().getContentResolver(), Settings.System.AUTO_TIME) == 1 &&
                   Settings.System.getInt(this.cordova.getActivity().getContentResolver(), Settings.System.AUTO_TIME_ZONE) == 1 )
                {
                    isDateAndTimeFromNetwork = true;
                }
                else
                {
                    isDateAndTimeFromNetwork = false;
                }
            }
        }
        catch(Exception ex)
        {

        }
        return  isDateAndTimeFromNetwork;
    }
}


class GPSTracker extends Service implements LocationListener {

    private String mensaje;
    private final Context mContext;

    // flag for GPS status
    boolean isGPSEnabled = false;

    // flag for network status
    boolean isNetworkEnabled = false;

    // flag for GPS status
    boolean canGetLocation = false;

    Location location; // location
    double latitude; // latitude
    double longitude; // longitude

    // The minimum distance to change Updates in meters
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 1; // 10 meters

    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 100; // 100 milisegundos

    // Declaring a Location Manager
    protected LocationManager locationManager;

    public GPSTracker(Context context) {
        this.mContext = context;
        getLocation();
    }

    public Location getLocation() {
        try {
            locationManager = (LocationManager) mContext
                    .getSystemService(LOCATION_SERVICE);

            // getting GPS status
            isGPSEnabled = locationManager
                    .isProviderEnabled(LocationManager.GPS_PROVIDER);

            // getting network status
            isNetworkEnabled = locationManager
                    .isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if ( !isGPSEnabled && !isNetworkEnabled) {
                // no network provider is enabled
            } else {
                this.canGetLocation = true;
                // if GPS Enabled get lat/long using GPS Services

                if (isGPSEnabled) {

                    locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                    
                    if (locationManager != null) {
                        location = locationManager
                                .getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        if (location != null) {
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();

                           mensaje = "Obtenido del GPS";
                        }
                    }
                }

                if (isNetworkEnabled)
                // First get location from Network Provider
                {
                    if (location == null) {
                        locationManager.requestLocationUpdates(
                                LocationManager.NETWORK_PROVIDER,
                                MIN_TIME_BW_UPDATES,
                                MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                        
                        if (locationManager != null) {
                            location = locationManager
                                    .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                            if (location != null) {
                                latitude = location.getLatitude();
                                longitude = location.getLongitude();

                                mensaje = "Obtenido de la Red";
                            }
                        }
                    }
                }
            }

        } catch (SecurityException e)
        {
            e.printStackTrace();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return location;
    }

    /**
     * Stop using GPS listener
     * Calling this function will stop using GPS in your app
     * */
    public void stopUsingGPS() {
        try
        {
            if (locationManager != null) {
                locationManager.removeUpdates(GPSTracker.this);
            }
        }
        catch (SecurityException e)
        {
            e.printStackTrace();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

    }

    /**
     * Function to get latitude
     * */
    public double getLatitude(){
        if(location != null){
            latitude = location.getLatitude();
        }

        // return latitude
        return latitude;
    }

    /**
     * Function to get longitude
     * */
    public double getLongitude(){
        if(location != null){
            longitude = location.getLongitude();
        }

        // return longitude
        return longitude;
    }

    /**
     * Function to check GPS/wifi enabled
     * @return boolean
     * */
    public boolean canGetLocation() {
        return this.canGetLocation;
    }

    /**
     * Function to show settings alert dialog
     * On pressing Settings button will lauch Settings Options
     * */

    @Override
    public void onLocationChanged(Location location) {
        this.location = location;

        latitude = location.getLatitude();
        longitude = location.getLongitude();
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    public String getMensaje()
    {
        return  mensaje;
    }
}
