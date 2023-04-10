package githiomi.dhosiolux.globalweather;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import githiomi.dhosiolux.globalweather.adapters.ForecastAdapter;
import githiomi.dhosiolux.globalweather.databinding.ActivityMainBinding;
import githiomi.dhosiolux.globalweather.models.Constants;
import githiomi.dhosiolux.globalweather.models.ForecastItem;

public class MainActivity extends AppCompatActivity {

    // Views
    TextView date, town, weatherType, mainTemp, wind, humidity, temp;
    ImageView weatherImage;
    RecyclerView forecastRecycler;

    // Forecast Adapter
    ForecastAdapter forecastAdapter;

    // User location variables
    LocationManager locationManager;

    // Local variables
    String cityName;
    List<ForecastItem> forecastItemList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Binding set up
        ActivityMainBinding mainActivityBinding = ActivityMainBinding.inflate(getLayoutInflater());
        // Update set content view
        setContentView(mainActivityBinding.getRoot());

        date = mainActivityBinding.TVDate;
        town = mainActivityBinding.TVTownName;
        weatherImage = mainActivityBinding.IVWeatherImage;
        weatherType = mainActivityBinding.TVWeatherType;
        mainTemp = mainActivityBinding.TVMainTemperature;
        wind = mainActivityBinding.TVWindSpeed;
        humidity = mainActivityBinding.TVHumidity;
        temp = mainActivityBinding.TVTemperature;
        forecastRecycler = mainActivityBinding.RVForecastView;

        // Init the location manager
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Check for location permission
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // Request permission
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, Constants.PERMISSION_CODE);
        }

        // But if location is available
        Location currentLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        // Get current city name
        cityName = getCityName(currentLocation.getLongitude(), currentLocation.getLatitude());

        // Use the city name to get weather information
        getWeatherData(cityName);

    }

    public String getCityName(double longitude, double latitude) {

        String city = ""; // Default city name

        Geocoder geocoder = new Geocoder(getBaseContext(), Locale.getDefault());

        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 5);

            for (Address address : addresses) {
                if (address != null) {
                    String cityName = address.getLocality();
                    if (cityName != null) {
                        city = cityName;
                    } else {
                        Toast.makeText(this, "User City NOT found! Try Again.", Toast.LENGTH_SHORT).show();
                    }
                }
            }

        } catch (Exception e) {

            System.out.println("Could not find city. Defaulting to Port Louis");
            city = "Port Louis";
            e.printStackTrace();

        }

        return city;

    }

    /**
     * To get the weather data from the API
     *
     * @param townName the town to get the weather data from
     */
    public void getWeatherData(String townName) {

        // Set city name TV to the town name
        town.setText(townName);

        String APIUrl = "http://api.weatherapi.com/v1/forecast.json?key=" + R.string.weather_api_key + "q=" + townName.trim() + "&days=6&aqi=no&alerts=no";

        // Request Queue
        RequestQueue queue = Volley.newRequestQueue(MainActivity.this);

        @SuppressLint("NotifyDataSetChanged") JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.GET, APIUrl, null, response -> {
            // Clear arraylist of previous data
            forecastItemList.clear();

            // Extract Data
            try {

                // Current Weather Data
                String currentTime = response.getJSONObject("location").getString("localtime");
                String mainTemperature = response.getJSONObject("current").getString("temp_c");
                String condition = response.getJSONObject("current").getJSONObject("condition").getString("text");
                String iconUrl = response.getJSONObject("current").getJSONObject("condition").getString("icon");
                int windSpeed = response.getJSONObject("current").getInt("wind_kph");
                int humidityLevel = response.getJSONObject("current").getInt("humidity");
                int feelsLike = response.getJSONObject("current").getInt("feelslike_c");

                // Add data to layout
                date.setText(getDate(currentTime));
                weatherType.setText(condition);

                String fullTemp = mainTemperature + "°C";
                mainTemp.setText(fullTemp);

                wind.setText(windSpeed);
                humidity.setText(humidityLevel);
                temp.setText(feelsLike);

                // Add icon to layout
                String fullIconUrl = "https:" + iconUrl;
                Picasso.get().load(fullIconUrl).into(weatherImage);

                // Get Forecast Data
                for (int i = 0; i < 6; i++) {

                    String day = "Day " + i;

                    JSONArray forecastDay = response.getJSONObject("forecast").getJSONArray("forecastday");
                    String forecastIconUrl = forecastDay.getJSONObject(i).getJSONObject("day").getJSONObject("condition").getString("icon");
                    String forecastCondition = forecastDay.getJSONObject(i).getJSONObject("day").getJSONObject("condition").getString("text");

                    // Create new forecast items
                    forecastItemList.add(new ForecastItem(day, forecastIconUrl, forecastCondition));
                }

                // Set up the adapter
                forecastAdapter.notifyDataSetChanged();
                setUpAdapter(forecastAdapter, forecastRecycler, forecastItemList, MainActivity.this);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }, error -> Toast.makeText(MainActivity.this, "Please enter a valid city name :)", Toast.LENGTH_SHORT).show());

        // Add request to queue
        queue.add(jsonRequest);

    }

    /**
     * Method to convert API date to needed format
     *
     * @param date the date to convert from the API
     * @return newly formatted date -> e MMMM dd, yyyy
     */
    private static String getDate(String date) {
        String newDate = "";

        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm");
        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat outputFormat = new SimpleDateFormat("E MMMM dd, yyyy");

        try {
            Date inputDate = inputFormat.parse(date);
            assert inputDate != null;
            newDate = outputFormat.format(inputDate);
        } catch (ParseException pe) {
            System.out.println("Could not parse date");
            pe.printStackTrace();
        }

        return newDate;

    }

    /**
     * To set up the adapter to the recycler view
     *
     * @param adapter  the adapter to be used in the recycler view
     * @param recyclerView  the recycler view to be used
     * @param forecastItems the list of items to be passed to the adapter
     * @param context       the context to init the adapter
     */
    private static void setUpAdapter(ForecastAdapter adapter, RecyclerView recyclerView, List<ForecastItem> forecastItems, Context context) {

        // Init the adapter
        adapter = new ForecastAdapter(forecastItems, context);

        // Recycler view
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new GridLayoutManager(context, 5, GridLayoutManager.HORIZONTAL, false));
        recyclerView.setHasFixedSize(true);

    }
}