package com.coolweather.android;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.coolweather.android.gson.Forecast;
import com.coolweather.android.gson.Weather;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.Utility;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {

    public DrawerLayout drawerLayout;
    private Button navButton;
    public SwipeRefreshLayout swipeRefresh;
    private String mWeatherId;
    private ScrollView weatherLayout;
    private TextView titleCity;
    private TextView titleUpdateTime;
    private TextView degreeText;
    private TextView weatherInfoText;
    private LinearLayout forecastLayout;
    private TextView aqiText;
    private TextView pm25Text;
    private TextView comfortText;
    private TextView carWashText;
    private TextView sportText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_weather );
        weatherLayout = findViewById( R.id.weather_layout );
        titleCity = findViewById( R.id.title_city );
        titleUpdateTime = findViewById( R.id.title_update_time );
        degreeText = findViewById( R.id.degree_text );
        weatherInfoText = findViewById( R.id.weather_info_text );
        forecastLayout = findViewById( R.id.forecast_layout );
        aqiText = findViewById( R.id.aqi_text );
        pm25Text = findViewById( R.id.pm25_text );
        comfortText = findViewById( R.id.comfort_text );
        carWashText = findViewById( R.id.car_wash_text );
        sportText = findViewById( R.id.sport_text );
        swipeRefresh = findViewById( R.id.swipe_refresh );
        swipeRefresh.setColorSchemeResources( R.color.colorPrimary );
        drawerLayout = findViewById( R.id.drawer_layout );
        navButton = findViewById( R.id.nav_button );
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences( this );
        String weatherString = prefs.getString( "weather",null );
        if(weatherString != null){
            Weather weather = Utility.handleWeatherResponse( weatherString );
            mWeatherId = weather.basic.weatherId;
            showWeatherInfo(weather);
        }else {
            mWeatherId = getIntent().getStringExtra( "weather_id" );
            weatherLayout.setVisibility( View.INVISIBLE );
            requestWeather(mWeatherId);
        }

        navButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer( GravityCompat.START );
            }
        } );

        swipeRefresh.setOnRefreshListener( new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestWeather( mWeatherId );
            }
        } );
    }

    public void requestWeather(final String weatherId) {
        String weatherUrl = "http://guolin.tech/api/weather?cityid="+
                weatherId + "&key=edb440cdc2b54409aa3c28409a26525d";
        HttpUtil.sendOkHttpRequest( weatherUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread( new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this,"天气信息获取失败",Toast.LENGTH_SHORT).show();
                        swipeRefresh.setRefreshing( false );
                    }
                } );

            }

            @Override
            public void onResponse(Call call,Response response)throws IOException {
                final String responseText = response.body().string();
                final Weather weather = Utility.handleWeatherResponse( responseText );
                runOnUiThread( new Runnable() {
                    @Override
                    public void run() {
                        if (weather != null && "ok".equals( weather.status )){
                            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences( WeatherActivity.this ).edit();
                            editor.putString( "weather",responseText );
                            editor.apply();
                            mWeatherId = weather.basic.weatherId;
                            showWeatherInfo( weather );
                        }else {
                            Toast.makeText( WeatherActivity.this,"获取天气信息失败",Toast.LENGTH_SHORT ).show();
                        }
                        swipeRefresh.setRefreshing( false );
                    }
                } );

            }


        } );
    }

    private void showWeatherInfo(Weather weather) {
        String cityName = weather.basic.cityName;
        String updateTime = weather.basic.update.updateTime.split( "" )[1];
        String degree = weather.now.temperature + "oC";
        String weatherInfo = weather.now.more.info;
        titleCity.setText( cityName );
        titleUpdateTime.setText( updateTime );
        degreeText.setText( degree );
        weatherInfoText.setText( weatherInfo );
        forecastLayout.removeAllViews();
        for(Forecast forecast : weather.forecastList){
            View view = LayoutInflater.from( this ).inflate( R.layout.forecast_item,forecastLayout,false );
            TextView dateText = view.findViewById( R.id.date_text );
            TextView infoText = view.findViewById( R.id.info_text );
            TextView maxText = view.findViewById( R.id.max_text );
            TextView minText = view.findViewById( R.id.min_text );
            dateText.setText( forecast.date );
            infoText.setText( forecast.more.info );
            maxText.setText( forecast.temperature.max );
            minText.setText( forecast.temperature.min );
            forecastLayout.addView( view );
        }
        if(weather.aqi != null){
            aqiText.setText( weather.aqi.city.api );
            pm25Text.setText( weather.aqi.city.pm25 );
        }
        String comfort = "舒适度：" + weather.suggestion.comfort.info;
        String carWash = "汽车指数" + weather.suggestion.carWash.info;
        String sport = "运动建议" + weather.suggestion.sport.info;
        comfortText.setText( comfort );
        carWashText.setText( carWash );
        weatherLayout.setVisibility( View.VISIBLE );

    }
}
