package com.example.emda.apertotask;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.net.ssl.HttpsURLConnection;

/**
 * Created by emda on 2/16/2018.
 */

/**
 * Thia class encodes this application consumer key and secret into a specially encoded set of credentials
 *  makes a request to the POST oauth2 / token endpoint to exchange these credentials for a bearer token from the teitter api
 *  uses the bearer token for  authentication when  accessing the REST API for trending tweets
 */
public class NetworkUtils {

    public static final String twitterAuthPreference = "com.example.emda.apertotask.BEARER_TOKEN";
    private static NetworkUtils nInstance;
    private Context context;

    private NetworkUtils() {
    }

    public static NetworkUtils getInstance(Context context) {
        if (nInstance == null) {
            nInstance = new NetworkUtils();
            nInstance.context = context;
        }
        return nInstance;
    }


    /**
     * Writes a request to a connection
     *
     * @param connection the httpURLConnection
     * @param textBody   the client credentials
     * @return return true on a successful write
     */
    private static boolean writeRequest(HttpsURLConnection connection, String textBody) {
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
            bufferedWriter.write(textBody);
            bufferedWriter.flush();
            bufferedWriter.close();

            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Bearer token is saved in sharedPreferences.
     *
     * @param bearerToken thr bearer token receive from the twitter api
     */
    private void saveBearerTokenToSharedPreference(String bearerToken) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(twitterAuthPreference, bearerToken);
        editor.apply();
    }


    /**
     * Gets saved bearer token from SharedPreferences.
     *
     * @return returns saved bearer token from shared preference
     */
    private String getSavedBearerToken() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(twitterAuthPreference, "");
    }

    /**
     * method which parses the web responses from the InputStream to a String.
     *
     * @param inputStream
     * @return the web page response in string format
     * @throws Exception
     */

    private String fetchWebPageResponse(InputStream inputStream) throws Exception {

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
        String webPageResponse = "", data = "";

        while ((data = reader.readLine()) != null) {
            webPageResponse += data + "\n";
        }

        return webPageResponse;
    }

    /**
     * Checks if bearer token was retrieved is so then app authentication was done.
     *
     * @return returns true if authentication was already done and the bearer token was saved
     */
    public boolean isAuthenticated() {
        return !getSavedBearerToken().equals("");
    }

    /**
     * Helper method to get locality from a  Location object latitude and longitude.
     *
     * @param location location object from the GPS
     * @return the name of a particular city
     */
    private String getLocality(Location location) {
        String city = "";
        Geocoder coder = new Geocoder(context, Locale.ENGLISH);
        try {
            List<Address> results = coder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (results.size() > 0) {
                Address place = results.get(0);
                city = place.getLocality();
                city = city.replace(" ", "%20");
            }
        } catch (IOException e) {
            e.getLocalizedMessage();
        }
        return city;
    }

    /**
     * Return the yahoo api woeid for a given locality
     *
     * @param location the location to get woeid
     * @return returns the yahoo API woeid for a given location
     */

    public String getWOEID(Location location) {
        String woeid = "";
        try {
            String city = getLocality(location);
            URL url = new URL("http://query.yahooapis.com/v1/public/yql?q=select%20*%20from%20geo.places%20where%20text%3D%22" +
                    city + "%22%20limit%201&diagnostics=false&format=json");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            //timeout in milliseconds
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(10000);
            conn.setRequestProperty("Host", "query.yahooapis.com");
            conn.setRequestProperty("User-Agent", "twitter_trends");
            conn.setRequestMethod("GET");

            if (conn.getResponseCode() == HttpsURLConnection.HTTP_OK) {
                String response = fetchWebPageResponse(conn.getInputStream());
                try {
                    JSONObject data = new JSONObject(response);
                    JSONObject query = data.getJSONObject("query");
                    JSONObject results = query.getJSONObject("results");
                    JSONObject place = results.getJSONObject("place");
                    woeid = place.getString("woeid");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            Log.e("Network Issue ", e.getLocalizedMessage());
        }
        return woeid;
    }

    /**
     * Method to process the Twitter OAuth in other to retrieve the bearer token needed to make API calls.
     */
    public void authenticateToTwitter() {
        try {
            String encodedConsumerKey = URLEncoder.encode(context.getResources().getString(R.string.consumer_key), "UTF-8");
            String encodeConsumerSecret = URLEncoder.encode(context.getResources().getString(R.string.consumer_secret), "UTF-8");
            String fullKey = encodedConsumerKey + ":" + encodeConsumerSecret;
            fullKey = Base64.encodeToString(fullKey.getBytes(), Base64.DEFAULT);
            fullKey = fullKey.replace("\n", "");

            URL url = new URL("https://api.twitter.com/oauth2/token");
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            //timeout in milliseconds
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(10000);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Host", "api.twitter.com");
            conn.setRequestProperty("User-Agent", "twitter_trends");
            conn.setRequestProperty("Authorization", "Basic " + fullKey);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
            conn.setRequestProperty("Content-Length", "29");
            conn.setUseCaches(false);
            writeRequest(conn, "grant_type=client_credentials");
            if (conn.getResponseCode() == HttpsURLConnection.HTTP_OK) {
                String response = fetchWebPageResponse(conn.getInputStream());
                JSONObject jsonObject = new JSONObject(response);
                saveBearerTokenToSharedPreference(jsonObject.getString("access_token"));
            }

            conn.disconnect();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Method to process a GET request to the Twitter trends API.
     *
     * @param location a given location object
     * @return returns an array list of the trends from the twitter API
     */
    public ArrayList<String> getTwitterTrends(Location location) {
        ArrayList<String> trends = new ArrayList<>();
        if (isAuthenticated()) {
            String woeid = getWOEID(location);
            try {
                URL url = new URL("https://api.twitter.com/1.1/trends/place.json?id=" + woeid);
                HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                //timeout in milliseconds
                conn.setReadTimeout(10000);
                conn.setConnectTimeout(10000);
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Host", "api.twitter.com");
                conn.setRequestProperty("User-Agent", "twitter_trends");
                conn.setRequestProperty("Authorization", "Bearer " + getSavedBearerToken());
                conn.setDoInput(true);
                conn.setDoOutput(false);
                if (conn.getResponseCode() == HttpsURLConnection.HTTP_OK) {
                    InputStream is = conn.getInputStream();
                    String webPageResponse = fetchWebPageResponse(is);
                    try {
                        JSONArray dataArray = new JSONArray(webPageResponse);
                        JSONObject trendsObject = dataArray.getJSONObject(0);
                        JSONArray trendsArray = trendsObject.getJSONArray("trends");
                        JSONObject trend;
                        for (int i = 0; i < trendsArray.length(); i++) {
                            trend = trendsArray.getJSONObject(i);
                            trends.add(trend.getString("name"));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return trends;
    }


}
