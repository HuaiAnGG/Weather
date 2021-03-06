package com.example.huaian.weather.view.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.huaian.weather.R;
import com.example.huaian.weather.gson.Forecast;
import com.example.huaian.weather.gson.Weather;
import com.example.huaian.weather.service.AutoUpdateService;
import com.example.huaian.weather.util.ConstantUtil;
import com.example.huaian.weather.util.HttpUtil;
import com.example.huaian.weather.util.LogUtil;
import com.example.huaian.weather.util.Utility;

import java.io.IOException;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import static com.example.huaian.weather.util.ConstantUtil.BING_PIC;

public class WeatherActivity extends AppCompatActivity {

    @BindView(R.id.weather_layout)
    ScrollView weatherLayout;

    @BindView(R.id.title_city)
    TextView titleCity;

    @BindView(R.id.title_update_time)
    TextView titleUpdateTime;

    @BindView(R.id.degree_text)
    TextView degreeText;

    @BindView(R.id.weather_info_text)
    TextView weatherInfoText;

    @BindView(R.id.forecast_layout)
    LinearLayout forecastLayout;

    @BindView(R.id.aqi_text)
    TextView apiText;

    @BindView(R.id.pm25_text)
    TextView pm25Text;

    @BindView(R.id.comfort_text)
    TextView comfortText;

    @BindView(R.id.car_wash_text)
    TextView carWashText;

    @BindView(R.id.spor_text)
    TextView sportText;

    @BindView(R.id.bing_pic_img)
    ImageView bingPicImg;

    @BindView(R.id.swipe_layout)
    public SwipeRefreshLayout swipeRefresh;

    @BindView(R.id.nav_btn)
    Button navBtn;

    @BindView(R.id.drawer_layout)
    public DrawerLayout drawerLayout;

    Unbinder unbinder;

    private static final String TAG = WeatherActivity.class.getSimpleName();

    /**
     * 记录城市的天气Id
     */
    private String mWeatherId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //  系统在5.0以上才会执行沉浸式状态栏
        if (Build.VERSION.SDK_INT >= 21) {
            View decorView = getWindow().getDecorView();
            //  全屏 | 保持整个View稳定
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            //  设置透明色
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        setContentView(R.layout.activity_weather);

        //  绑定butterknife
        unbinder = ButterKnife.bind(this);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String bingPic = prefs.getString("bing_pic", null);
        if (bingPic != null) {
            Glide.with(this).load(bingPic).into(bingPicImg);
        } else {
            loadBingPic();
        }
        String weatherString = prefs.getString("weather", null);
        if (weatherString != null) {
            //  有缓存直接解析天气数据
            Weather weather = Utility.handleWeatherResponse(weatherString);
            mWeatherId = weather.basic.weatherId;
            showWeatherInfo(weather);
        } else {
            //  无缓存的时候去服务器查询天气数据
            mWeatherId = getIntent().getStringExtra("weather_id");
            //  无数据先隐藏，避免突兀
            weatherLayout.setVisibility(View.INVISIBLE);
            requestWeather(mWeatherId);
        }
        swipeRefresh.setColorSchemeResources(R.color.colorPrimary);
        //  下拉刷新
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestWeather(mWeatherId);
            }
        });
    }

    /**
     * 加载必应每日一图
     */
    private void loadBingPic() {
        HttpUtil.HttpOkHttpRequest(BING_PIC, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String bingPic = response.body().string();
                SharedPreferences.Editor editor =
                        PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                editor.putString("bing_pic", bingPic);
                editor.apply();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(WeatherActivity.this).load(bingPic).into(bingPicImg);
                    }
                });
            }
        });
    }

    /**
     * 根据天气id请求城市天气信息
     *
     * @param weatherId
     */
    public void requestWeather(final String weatherId) {

        String weatherUrl = ConstantUtil.WEATHER_URL + weatherId + ConstantUtil.WEATHER_KEY;
        LogUtil.d(TAG, "requestWeather: " + weatherUrl);
        HttpUtil.HttpOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                        //  取消刷新动画
                        swipeRefresh.setRefreshing(false);
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText = response.body().string();
                final Weather weather = Utility.handleWeatherResponse(responseText);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (weather != null && "ok".equals(weather.status)) {
                            SharedPreferences.Editor editor =
                                    PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                            editor.putString("weather", responseText);
                            editor.apply();
                            mWeatherId = weather.basic.weatherId;
                            showWeatherInfo(weather);
                            Toast.makeText(WeatherActivity.this, "天气信息已经更新", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                        }
                        //  取消刷新动画
                        swipeRefresh.setRefreshing(false);
                    }
                });
            }
        });
        //  加载每日一图
        loadBingPic();
    }

    /**
     * 处理并展示 Weather 实体中的数据
     *
     * @param weather
     */
    public void showWeatherInfo(Weather weather) {
        String cityName = weather.basic.cityName;
        String updateTime = weather.basic.update.updateTime.split(" ")[1];
        String degree = weather.now.temperature + "℃";
        String weatherInfo = weather.now.more.info;
        titleCity.setText(cityName);
        titleUpdateTime.setText(updateTime);
        degreeText.setText(degree);
        weatherInfoText.setText(weatherInfo);
        forecastLayout.removeAllViews();
        List listTest = weather.forecastList;
        for (Forecast forecast : weather.forecastList) {
            View view = LayoutInflater.from(this)
                    .inflate(R.layout.forecast_item, forecastLayout, false);
            TextView dateText = view.findViewById(R.id.date_text);
            TextView infoText = view.findViewById(R.id.info_text);
            TextView maxText = view.findViewById(R.id.max_text);
            TextView minText = view.findViewById(R.id.min_text);

            dateText.setText(forecast.date);
            infoText.setText(forecast.more.info);
            maxText.setText(forecast.temperature.max);
            minText.setText(forecast.temperature.min);
            forecastLayout.addView(view);
        }
        if (weather.aqi != null) {
            apiText.setText(weather.aqi.city.aqi);
            pm25Text.setText(weather.aqi.city.pm25);
        }
        String comfort = "舒适度：" + weather.suggestion.comfort.info;
        String carWash = "洗车指数：" + weather.suggestion.carWash.info;
        String sport = "运动建议:" + weather.suggestion.sport.info;
        comfortText.setText(comfort);
        carWashText.setText(carWash);
        sportText.setText(sport);
        weatherLayout.setVisibility(View.VISIBLE);
        Intent intent = new Intent(this, AutoUpdateService.class);
        startService(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbinder.unbind();
    }

    @OnClick(R.id.nav_btn)
    public void onViewClicked() {
        //  系统默认方向弹出导航栏
        drawerLayout.openDrawer(GravityCompat.START);
    }
}
