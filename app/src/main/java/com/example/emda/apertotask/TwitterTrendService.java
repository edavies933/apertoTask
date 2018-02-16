package com.example.emda.apertotask;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by emda on 2/16/2018.
 */

public class TwitterTrendService extends Service implements LocationListener {

    /**
     * indicates how to behave if the service is killed
     */
    int mStartMode = Service.START_NOT_STICKY;
    private LocationManager locationManager;
    private NetworkUtils networkUtils;
    private Context context;
    private DownloadTrendsTask downloadTrendsTask;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        this.context = this;
        networkUtils = NetworkUtils.getInstance(context);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                    5000, 100, this);
        }
        return mStartMode;
    }


    @Override
    public void onLocationChanged(Location location) {
        downloadTrendsTask = new DownloadTrendsTask();
        downloadTrendsTask.execute(location);
    }


    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
        Intent i = new Intent(MainActivity.INTENT_FILTER_ACTION_ENABLE_GPS);
        context.sendBroadcast(i);

    }

    /**
     * AsyncTask to process the HTTP requests and notify the MainActivity when latest trends were retrieved.
     */
    private class DownloadTrendsTask extends AsyncTask<Location, Void, List<String>> {

        @Override
        protected void onPreExecute() {
            if (MainActivity.trendLoadingProgressDialog != null)
            MainActivity.trendLoadingProgressDialog.show();
            super.onPreExecute();

        }

        @Override
        protected List<String> doInBackground(Location... locations) {
            List<String> trends = new ArrayList<>();
            if (!networkUtils.isAuthenticated()) {
                networkUtils.authenticateToTwitter();
            }
            for (Location location : locations) {
                trends = networkUtils.getTwitterTrends(location);
                if (isCancelled())
                    break;
            }
            return trends;
        }

        protected void onPostExecute(List<String> result) {
       /*     Intent i = new Intent(MainActivity.INTENT_FILTER_ACTION_SHOW_TRENDS)
                    .putStringArrayListExtra("data", (ArrayList<String>) result);*/

            Intent intent = new Intent();
            intent.setAction(MainActivity.INTENT_FILTER_ACTION_SHOW_TRENDS);
            intent.putStringArrayListExtra("data_item", (ArrayList<String>) result);
            //close static dialog box
            if(MainActivity.trendLoadingProgressDialog != null)
                MainActivity.trendLoadingProgressDialog.hide();
            context.sendBroadcast(intent);


        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (downloadTrendsTask != null)
            downloadTrendsTask.cancel(true);
    }
}
