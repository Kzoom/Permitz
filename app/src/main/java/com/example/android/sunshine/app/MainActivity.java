package com.example.android.sunshine.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;


public class MainActivity extends ActionBarActivity {

    private final String LOG_TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PermitFragment())
                    .commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        if (id == R.id.action_map) {
            openPreferredLocationOnMap();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void openPreferredLocationOnMap(){
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String location = sharedPref.getString(
               getString(R.string.pref_location_key),
               getString(R.string.pref_location_default));
        Log.v(LOG_TAG, "Location: " + location);


        Uri mapLocationUri = Uri.parse("geo:0,0").buildUpon().appendQueryParameter("q", location).build();
        Intent intent = new Intent(Intent.ACTION_VIEW); //, mapLocationUri); ksw...2/26/15 chg'd to match theirs
        intent.setData(mapLocationUri);  // 2/26/15 ksw...uncommented to match their code

        if (intent.resolveActivity(getPackageManager()) != null ) {
            startActivity(intent);

        } else {
            Log.d(LOG_TAG, "Couldn't call " + location + ", no receiving apps installed!");
        }


    }

}
