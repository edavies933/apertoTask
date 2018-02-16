package com.example.emda.apertotask;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {


    final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 101;
    protected static String INTENT_FILTER_ACTION_SHOW_TRENDS = "android.intent.action.SHOW_TRENDS";
    protected static String INTENT_FILTER_ACTION_ENABLE_GPS = "android.intent.action.ENABLE_GPS";
    private RecyclerView mRecyclerView;
    private SingleListViewAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private ArrayList<String> dataSet = new ArrayList<>();
    BroadcastReceiver trendsReceiver;
    Intent intent;
    static ProgressDialog trendLoadingProgressDialog;
    private static List<BroadcastReceiver> receivers = new ArrayList<BroadcastReceiver>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRecyclerView = findViewById(R.id.recycler_view);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new SingleListViewAdapter(dataSet);
        mRecyclerView.setAdapter(mAdapter);

        intent = new Intent(this, TwitterTrendService.class);

        //registering  trends and gps  broadcastreceivers
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(INTENT_FILTER_ACTION_SHOW_TRENDS);
        intentFilter.addAction(INTENT_FILTER_ACTION_ENABLE_GPS);
        trendsReceiver = new TrendsBroadcastReceiver();
        registerReceiver(trendsReceiver, intentFilter);
    }

    /**
     * BroadcastReceiver to process data from TwitterTrendService.
     */
    class TrendsBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null) {
                if (intent.getAction().equals(INTENT_FILTER_ACTION_SHOW_TRENDS)) {
                    dataSet.clear();
                    dataSet.addAll(intent.getStringArrayListExtra("data_item"));
                    mAdapter.notifyDataSetChanged();
                } else if (intent.getAction().equals(INTENT_FILTER_ACTION_ENABLE_GPS)) {
                    displayPromptForEnablingGPS(context);
                }
            }
        }
    }

    // Method to start the service to fetch available twitter trends

    public void startService() {
        startService(new Intent(getBaseContext(), TwitterTrendService.class));
    }

    // Method to stop the service
    public void stopService() {
        stopService(new Intent(getBaseContext(), TwitterTrendService.class));
    }

    /**
     * Shows dialog box prompting the user to enable GPS.
     *
     * @param context
     */
    public void displayPromptForEnablingGPS(Context context) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        final String message = "this App requires GPS to work, Enable GPS now?";

        builder.setMessage(message)
                .setPositiveButton("Yes",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface d, int id) {
                                LocationManager service = (LocationManager) getSystemService(LOCATION_SERVICE);
                                boolean enabled = service
                                        .isProviderEnabled(LocationManager.GPS_PROVIDER);
                                /**
                                 check if GPS is enabled and if not takes the user to the GSP settings
                                 */

                                if (!enabled) {
                                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                    startActivity(intent);
                                }

                                d.dismiss();
                            }
                        })
                .setNegativeButton("No",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface d, int id) {
                                d.cancel();

                            }
                        });
        builder.create().show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request was cancelled , the result arrays will be empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                        grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    startService();
                }
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        //check for location permission
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
            ActivityCompat.requestPermissions(this, permissions, PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        } else {
            startService();
            trendLoadingProgressDialog = new ProgressDialog(this);
            trendLoadingProgressDialog.setMessage("Please wait, Loading Trends...");
            trendLoadingProgressDialog.setCancelable(false);


        }
    }
    //checks if the receiver is registered
    public boolean isReceiverRegistered(BroadcastReceiver receiver){
        boolean registered = receivers.contains(receiver);
        Log.i(getClass().getSimpleName(), "is Receiver "+receiver+" Registered? "+ registered);
        return registered;
    }

    public void unregisterReceiver(BroadcastReceiver receiver){
        //checks if the receiver is registered first before un registering
        if (isReceiverRegistered(receiver)){
            receivers.remove(receiver);
            this.unregisterReceiver(receiver);
            Log.i(getClass().getSimpleName(), "Unregistered Receiver: "+receiver);
        }
    }
    @Override
    protected void onStop() {
        super.onStop();
        if(isReceiverRegistered(trendsReceiver))
            unregisterReceiver(trendsReceiver);
        stopService();
        if(trendLoadingProgressDialog != null)
            trendLoadingProgressDialog.hide();
    }


}


