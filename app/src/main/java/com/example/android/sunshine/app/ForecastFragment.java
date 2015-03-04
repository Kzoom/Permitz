package com.example.android.sunshine.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;



/**
 * Created by Kelley on 2/11/2015.
 * moved from MainActivity
 */

/**
 * A placeholder fragment containing a simple view.
 */
public class ForecastFragment extends Fragment {

    private ArrayAdapter<String> myForecastAdapter;

    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //add so fragment can handle menu events
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.forecastfragment, menu);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_refresh) {
              //2/19/15 ksw...refactor, move this functionality to updateWeather() method
//            FetchWeatherTask weatherTask = new FetchWeatherTask();
//            //weatherTask.execute("94043");
//            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
//            String theLocation = sharedPref.getString(getString(R.string.pref_location_key), getString(R.string.pref_location_default));
//            weatherTask.execute(theLocation);

            updateWeather();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        ////OpenWeatherMap APPID = b50370d08a32b50b598912ceb9c41c63
        ////http://api.openweathermap.org/data/2.5/forecast/daily?q=90291,USA&mode=json&units=metric&cnt=7
        //List<String> weekForecast = new ArrayList<String>(Arrays.asList(forecastArray));

        myForecastAdapter = new ArrayAdapter<String>(
                getActivity(),  // the current context (this fragment's parent activity
                R.layout.list_item_forecast,  // ID of list item layout
                R.id.list_item_forecast_textview,  // ID of textview to be populated
                //weekForecast  // faked data
                new ArrayList<String>()
        );

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        //View rootview is the root of the fragment_main, inflated above
        ListView mylistView = (ListView) rootView.findViewById(R.id.listview_forecast);
        mylistView.setAdapter(myForecastAdapter);

        //2/17/15 ksw...start of lesson 3
        mylistView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {

                String forecast = myForecastAdapter.getItem(position);

                Intent displayIntent = new Intent(getActivity(), DetailActivity.class).putExtra(Intent.EXTRA_TEXT, forecast);
                startActivity(displayIntent);
            }
        });

        return rootView;
    }

    private void updateWeather(){
        FetchPermitTask weatherTask = new FetchPermitTask();
        //weatherTask.execute("94043");
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String theLocation = sharedPref.getString(getString(R.string.pref_location_key), getString(R.string.pref_location_default));
        weatherTask.execute(theLocation);
    }

    @Override
    public void onStart() {
        super.onStart();
        updateWeather();
    }

    public class FetchPermitTask extends AsyncTask<String, Void, String[]> {

        private final String LOG_TAG = FetchPermitTask.class.getSimpleName();

        //****************************

        /* The date/time conversion code is going to be moved outside the asynctask later,
        * so for convenience we're breaking it out into its own method now.
        */
        private String getReadableDateString(long time){
            // Because the API returns a unix timestamp (measured in seconds),
            // it must be converted to milliseconds in order to be converted to valid date.
            Date date = new Date(time * 1000);
            SimpleDateFormat format = new SimpleDateFormat("E, MMM d");
            return format.format(date).toString();
        }

        /**
         * Prepare the weather high/lows for presentation.
         */
        private String formatHighLows(double high, double low) {
            // For presentation, assume the user doesn't care about tenths of a degree.
            long roundedHigh = Math.round(high);
            long roundedLow = Math.round(low);

            //2/19/15 ksw...convert temperatures per preferred units
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String units = sharedPref.getString(getString(R.string.unit_key), getString(R.string.unit_default));
            Log.v("sharedPref", "units: " + units);

            if (units.equals("F")){
                roundedHigh = Math.round((high * 9/5) + 32);
                roundedLow = Math.round((high * 9/5) + 32);
            }

            String highLowStr = roundedHigh + "/" + roundedLow;
            return highLowStr;
        }

        /**
         * Take the String representing the complete forecast in JSON Format and
         * pull out the data we need to construct the Strings needed for the wireframes.
         *
         * Fortunately parsing is easy: constructor takes the JSON string and converts it
         * into an Object hierarchy for us.
         */
        private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays)
                throws JSONException {

            // These are the names of the JSON objects that need to be extracted.
            final String OWM_LIST = "list";
            final String OWM_WEATHER = "weather";
            final String OWM_TEMPERATURE = "temp";
            final String OWM_MAX = "max";
            final String OWM_MIN = "min";
            final String OWM_DATETIME = "dt";
            final String OWM_DESCRIPTION = "main";

            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

            String[] resultStrs = new String[numDays];
            for(int i = 0; i < weatherArray.length(); i++) {
            // For now, using the format "Day, description, hi/low"
                String day;
                String description;
                String highAndLow;

                // Get the JSON object representing the day
                JSONObject dayForecast = weatherArray.getJSONObject(i);

                // The date/time is returned as a long. We need to convert that
                // into something human-readable, since most people won't read "1400356800" as
                // "this saturday".
                long dateTime = dayForecast.getLong(OWM_DATETIME);
                day = getReadableDateString(dateTime);

                // description is in a child array called "weather", which is 1 element long.
                JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);

                // Temperatures are in a child object called "temp". Try not to name variables
                // "temp" when working with temperature. It confuses everybody.
                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                double high = temperatureObject.getDouble(OWM_MAX);
                double low = temperatureObject.getDouble(OWM_MIN);

                highAndLow = formatHighLows(high, low);
                resultStrs[i] = day + " - " + description + " - " + highAndLow;
            }

            for (String s : resultStrs){
                Log.v(LOG_TAG, "Forecast: " + s);
            }

            return resultStrs;
        }
        //***************************



        @Override
        protected String[] doInBackground(String... params) {

            if (params.length == 0){
                //no zip input, nothing to lookup
                return null;
            }

            //ksw...Code from their network call snippet

            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String forecastJsonStr = null;

            String format = "json";
            String units = "metric";
            int numDays = 7;

            try {
                // Construct the URL for the OpenWeatherMap query
                // Possible parameters are avaiable at OWM's forecast API page, at
                // http://openweathermap.org/API#forecast

                //URL url = new URL("http://api.openweathermap.org/data/2.5/forecast/daily?q=94043&mode=json&units=metric&cnt=7");

                // 2/11/15 ksw...my pass at URL building, which worked.
                // 2/17/15 ksw...But theirs is more robust in the long run, so change to it
               /* Uri.Builder builder = new Uri.Builder();
                builder.scheme("http")
                        .authority("api.openweathermap.org")
                        .appendPath("data")
                        .appendPath("2.5")
                        .appendPath("forecast")
                        .appendPath("daily")
                        .appendQueryParameter("q",params[0])
                        .appendQueryParameter("mode","json")
                        .appendQueryParameter("units","metric")
                        .appendQueryParameter("cnt","7");

                URL url = new URL(builder.build().toString());
                Log.v(LOG_TAG, "Build URL: " + url.toString());
                */

                // 2/11/15 ksw...alt method from stackoverflow & the udacity class. More flexible in long run. Clean up later
                //2/17/15 ksw...changed to use their method
                final String FORECAST_BASE_URL = "http://api.openweathermap.org/data/2.5/forecast/daily?";
                final String QUERY_PARAM = "q";
                final String FORMAT_PARAM = "mode";
                final String UNITS_PARAM = "units";
                final String DAYS_PARAM = "cnt";

                //You can declare all this the above way or even inside the Uri.parse() and appendQueryParameter()

                Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                        .appendQueryParameter(QUERY_PARAM, params[0])
                        .appendQueryParameter(FORMAT_PARAM, format)
                        .appendQueryParameter(UNITS_PARAM, units)
                        .appendQueryParameter(DAYS_PARAM, Integer.toString(numDays))
                        .build();

                //At last

                URL url = new URL(builtUri.toString());
                Log.v(LOG_TAG, "Build URL: " + url.toString());



                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null; // their code was forecastJsonStr = null;
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
                    // Stream was empty. No point in parsing.
                    return null; // their code was forecastJsonStr = null;
                }
                forecastJsonStr = buffer.toString();

                Log.v(LOG_TAG, "Forecast JSON string: " + forecastJsonStr);


            } catch (IOException e) {
                Log.e(LOG_TAG, "Error on Xoom", e);
                // If the code didn't successfully get the weather data, there's no point in attempting
                // to parse it.
                return null; // their code was forecastJsonStr = null;
            } finally{
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream on Xoom", e);
                    }
                }
            }

            try {
                return getWeatherDataFromJson(forecastJsonStr, numDays);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }

            return null;

        }

        @Override
        protected void onPostExecute(String[] result) {
            if (result != null)
                myForecastAdapter.clear();
                for (String dayForecastStr : result){
                    myForecastAdapter.add(dayForecastStr);
                }  //and that's the new data

        }


        //// 3/3/15 ksw...New section for Permitz code

        private String[] getPermitDataFromJson(String permitJsonStr, int numDays)
                throws JSONException {

            // These are the names of the JSON objects that need to be extracted.
            final String OWM_LIST = "list";
            //final String OWM_WEATHER = "weather";
            //final String OWM_TEMPERATURE = "temp";
            //final String OWM_MAX = "max";
            //final String OWM_MIN = "min";

            final String OWM_ADDR_START = "address_start";
            final String OWM_ADDR_END = "address_end";
            final String OWM_STREET = "street_name";
            final String OWM_STREET_SUFFIX = "street_suffix";
            final String OWM_VALUE = "valuation";
            final String ZIP = "zip_code";

            final String OWM_DATETIME = "issue_date";
            final String OWM_DESCRIPTION = "work_description";
            final String OWM_LOCATION = "location_1";

            JSONObject permitJson = new JSONObject(permitJsonStr);
            //JSONArray listArray = permitJson.getJSONArray(OWM_LIST);
            JSONArray permitArray = permitJson.getJSONArray(OWM_LOCATION);

            String[] resultStrs = new String[numDays];
            for(int i = 0; i < permitArray.length(); i++) {
                // For now, using the format "Day, description, hi/low"
                String day;
                String description;
                String highAndLow;

                // Get the JSON object representing the day
                JSONObject dayPermit = permitArray.getJSONObject(i);

                // The date/time is returned as a long. We need to convert that
                // into something human-readable, since most people won't read "1400356800" as
                // "this saturday".
                long dateTime = dayPermit.getLong(OWM_DATETIME);
                day = getReadableDateString(dateTime);

                // description is in a child array called "weather", which is 1 element long.
                JSONObject weatherObject = dayPermit.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);

                // Temperatures are in a child object called "temp". Try not to name variables
                // "temp" when working with temperature. It confuses everybody.
                JSONObject temperatureObject = dayPermit.getJSONObject(OWM_TEMPERATURE);
                double high = temperatureObject.getDouble(OWM_MAX);
                double low = temperatureObject.getDouble(OWM_MIN);

                highAndLow = formatHighLows(high, low);
                resultStrs[i] = day + " - " + description + " - " + highAndLow;
            }

            for (String s : resultStrs){
                Log.v(LOG_TAG, "Permits: " + s);
            }

            return resultStrs;
        }
        //***************************

        @Override
        protected String[] doInBackground2(String... params) {

            if (params.length == 0){
                //no zip input, nothing to lookup
                return null;
            }

            //ksw...Code from their network call snippet

            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String permitJsonStr = null;

            String format = "json";
            String units = "metric";
            String sinceDate = "'2015-02-27T00:00:00'";
            int numDays = 7;

            try {
                // Construct the URL for the OpenWeatherMap query
                // Possible parameters are avaiable at OWM's forecast API page, at
                // http://openweathermap.org/API#forecast

                //URL url = new URL("http://api.openweathermap.org/data/2.5/forecast/daily?q=94043&mode=json&units=metric&cnt=7");

                // 2/11/15 ksw...my pass at URL building, which worked.
                // 2/17/15 ksw...But theirs is more robust in the long run, so change to it
               /* Uri.Builder builder = new Uri.Builder();
                builder.scheme("http")
                        .authority("api.openweathermap.org")
                        .appendPath("data")
                        .appendPath("2.5")
                        .appendPath("forecast")
                        .appendPath("daily")
                        .appendQueryParameter("q",params[0])
                        .appendQueryParameter("mode","json")
                        .appendQueryParameter("units","metric")
                        .appendQueryParameter("cnt","7");

                URL url = new URL(builder.build().toString());
                Log.v(LOG_TAG, "Build URL: " + url.toString());
                */

                // 3/3/15 ksw...build SoQL query for Permits
                final String PERMIT_BASE_URL = "https://data.lacity.org/resource/yv23-pmwf.json?";
                final String SELECT_COLS = "$select=zip_code, issue_date, address_start, address_end, street_name, street_suffix, work_description, valuation"
                final String QUERY_PARAM = " AND zip_code = ";
                final String FORMAT_PARAM = "mode";
                final String UNITS_PARAM = "units";
                final String DAYS_PARAM = "&$where=issue_date >= ";

                //You can declare all this the above way or even inside the Uri.parse() and appendQueryParameter()

                Uri builtUri = Uri.parse(PERMIT_BASE_URL).buildUpon()
                        .appendQueryParameter(SELECT_COLS)
                        //.appendQueryParameter(FORMAT_PARAM, format)  //json
                        //.appendQueryParameter(UNITS_PARAM, units)    //metric
                        .appendQueryParameter(DAYS_PARAM, sinceDate)
                        .appendQueryParameter(QUERY_PARAM, params[0])  //zip_code
                        .build();

                //At last

                URL url = new URL(builtUri.toString());
                Log.v(LOG_TAG, "Build URL: " + url.toString());



                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null; // their code was forecastJsonStr = null;
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
                    // Stream was empty. No point in parsing.
                    return null; // their code was forecastJsonStr = null;
                }
                permitJsonStr = buffer.toString();

                Log.v(LOG_TAG, "Permit JSON string: " + permitJsonStr);


            } catch (IOException e) {
                Log.e(LOG_TAG, "Error on Xoom", e);
                // If the code didn't successfully get the weather data, there's no point in attempting
                // to parse it.
                return null; // their code was forecastJsonStr = null;
            } finally{
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream on Xoom", e);
                    }
                }
            }

            try {
                return getPermitDataFromJson(permitJsonStr, numDays);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }

            return null;

        }

        @Override
        protected void onPostExecute2(String[] result) {
            if (result != null)
                myForecastAdapter.clear();
            for (String dayPermitStr : result){
                myForecastAdapter.add(dayPermitStr);
            }  //and that's the new data

        }
    }



}