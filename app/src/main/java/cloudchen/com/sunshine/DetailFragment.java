package cloudchen.com.sunshine;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import cloudchen.com.sunshine.data.WeatherContract;

/**
 * A placeholder fragment containing a simple view.
 */
public class DetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = DetailFragment.class.getSimpleName();

    private static final int DETAIL_LOADER = 0;

    private static final String[] FORECAST_COLUMNS = {
            WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_DATETEXT,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.WeatherEntry.COLUMN_HUMIDITY,
            WeatherContract.WeatherEntry.COLUMN_WIND_SPEED,
            WeatherContract.WeatherEntry.COLUMN_PRESSURE,
            WeatherContract.WeatherEntry.COLUMN_DEGREES,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING
    };
    public static final String DATE_KEY = "forecast_date";

    private static final String SHARE_HASHTAG = " #SunshineApp";
    private String mForecastText;
    private ShareActionProvider mShareActionProvider;

    private String mLocation;
    private String mForecast;

    public DetailFragment() {
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mForecastText = getActivity().getIntent().getStringExtra(DATE_KEY);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(DETAIL_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);
        TextView textView = (TextView) rootView.findViewById(R.id.detail_date_textview);
        textView.setText(mForecastText);
        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.detail, menu);
        MenuItem item = menu.findItem(R.id.menu_item_share);
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);
        if (mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(getShareIntent());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_settings:
                Intent intent = new Intent(getActivity(), SettingsActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_view_location:
                showMap();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mLocation != null &&
                !mLocation.equals(Utility.getPreferredLocation(getActivity()))) {
            getLoaderManager().restartLoader(DETAIL_LOADER, null, this);
        }
    }


    private void showMap() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String location = sharedPreferences.getString(getString(R.string.pref_location_key), getString(R.string.pref_location_default));
        Uri geoLocation = Uri.parse("geo:0,0?q="+location);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(geoLocation);
        if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    private Intent getShareIntent() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType("text/plain");
        String msg = mForecastText + SHARE_HASHTAG;
        shareIntent.putExtra(Intent.EXTRA_TEXT, msg);
        return shareIntent;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.v(TAG, "In onCreateLoader");
        Intent intent = getActivity().getIntent();
        if (intent == null || !intent.hasExtra(DATE_KEY)) {
            return null;
        }
        String forecastDate = intent.getStringExtra(DATE_KEY);

        // Sort order:  Ascending, by date.
        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATETEXT + " ASC";

        Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(
                Utility.getPreferredLocation(getActivity()), forecastDate);
        Log.v(TAG, weatherForLocationUri.toString());

        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return new CursorLoader(
                getActivity(),
                weatherForLocationUri,
                FORECAST_COLUMNS,
                null,
                null,
                sortOrder
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Log.v(TAG, "In onLoadFinished");
        if (!data.moveToFirst()) { return; }

        String dateString = data.getString(data.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_DATETEXT));
        String dateName = Utility.getFormattedMonthDay(getActivity(),dateString);
        ((TextView) getView().findViewById(R.id.detail_date_textview)).setText(dateName);
        String dayName = Utility.getDayName(getActivity(), dateString);
        ((TextView) getView().findViewById(R.id.detail_day_textview)).setText(dayName);

        String weatherDescription =
                data.getString(data.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_SHORT_DESC));
        ((TextView) getView().findViewById(R.id.detail_forecast_textview)).setText(weatherDescription);

        boolean isMetric = Utility.isMetric(getActivity());

        String high = Utility.formatTemperature(
                getActivity(), data.getDouble(data.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP)), isMetric);
        ((TextView) getView().findViewById(R.id.detail_high_textview)).setText(high);

        String low = Utility.formatTemperature(
                getActivity(), data.getDouble(data.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP)), isMetric);
        ((TextView) getView().findViewById(R.id.detail_low_textview)).setText(low);

        double humidity = data.getDouble(data.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_HUMIDITY));
        ((TextView) getView().findViewById(R.id.detail_humidity_textview)).setText(
                String.format(getString(R.string.format_humidity), humidity)
        );

        float windSpeed = data.getFloat(data.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_HUMIDITY));
        float degrees = data.getFloat(data.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_DEGREES));
        ((TextView) getView().findViewById(R.id.detail_wind_textview)).setText(
                Utility.getFormattedWind(getActivity(), windSpeed, degrees)
        );

        float pressure = data.getFloat(data.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_PRESSURE));
        ((TextView) getView().findViewById(R.id.detail_pressure_textview)).setText(
                String.format(getString(R.string.format_pressure), pressure)
        );

        // We still need this for the share intent
        mForecast = String.format("%s - %s - %s/%s", dateString, weatherDescription, high, low);
        Log.v(TAG, "Forecast String: " + mForecast);

        // If onCreateOptionsMenu has already happened, we need to update the share intent now.
        if (mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(createShareForecastIntent(mForecast));
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        getLoaderManager().restartLoader(DETAIL_LOADER, null, this);
    }

    private Intent createShareForecastIntent(String forecast) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, forecast + SHARE_HASHTAG);
        return shareIntent;
    }
}
