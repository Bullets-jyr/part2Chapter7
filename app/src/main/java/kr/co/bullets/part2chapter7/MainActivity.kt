package kr.co.bullets.part2chapter7

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import kr.co.bullets.part2chapter7.databinding.ActivityMainBinding
import kr.co.bullets.part2chapter7.databinding.ItemForecastBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(android.Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                // Only approximate location access granted.
                updateLocation()
            }

            else -> {
                // No location access granted.
                Toast.makeText(this, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        locationPermissionRequest.launch(
            arrayOf(
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )


    }

    private fun transformSky(forecast: ForecastEntity): String {
        return when (forecast.forecastValue.toInt()) {
            1 -> "맑음"
            3 -> "구름많음"
            else -> "흐림"
        }
    }

    private fun transformRainType(forecast: ForecastEntity): String {
        return when (forecast.forecastValue.toInt()) {
            0 -> "없음"
            1 -> "비"
            2 -> "비/눈"
            3 -> "눈"
            else -> "소나기"
        }
    }

    private fun updateLocation() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionRequest.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener {
//            Log.e("MainActivity", it.toString())

            val retrofit = Retrofit.Builder()
                .baseUrl("http://apis.data.go.kr/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val service = retrofit.create(WeatherService::class.java)

            val baseDateTime = BaseDateTime.getBaseDateTime()
            val converter = GeoPointConverter()
            val point = converter.convert(lat = it.latitude, lon = it.longitude)
            service.getVillageForecast(
                serviceKey = "",
//            baseDate = "20230702",
//            baseTime = "0500",
                baseDate = baseDateTime.baseDate,
                baseTime = baseDateTime.baseTime,
                nx = point.nx,
                ny = point.ny,
            ).enqueue(object : Callback<WeatherEntity> {
                override fun onResponse(
                    call: Call<WeatherEntity>,
                    response: Response<WeatherEntity>
                ) {
                    val forecastDateTimeMap = mutableMapOf<String, Forecast>()

                    val forecastList =
                        response.body()?.response?.body?.items?.forecastEntities.orEmpty()
                    for (forecast in forecastList) {
//                    Log.e("MainActivity", forecast.toString())

                        if (forecastDateTimeMap["${forecast.forecastDate}/${forecast.forecastTime}"] == null) {
                            forecastDateTimeMap["${forecast.forecastDate}/${forecast.forecastTime}"] =
                                Forecast(
                                    forecastDate = forecast.forecastDate,
                                    forecastTime = forecast.forecastTime
                                )
                        }

                        forecastDateTimeMap["${forecast.forecastDate}/${forecast.forecastTime}"]?.apply {
                            when (forecast.category) {
                                Category.POP -> precipitation = forecast.forecastValue.toInt()
                                Category.PTY -> precipitationType = transformRainType(forecast)
                                Category.SKY -> sky = transformSky(forecast)
                                Category.TMP -> temperature = forecast.forecastValue.toDouble()
                                else -> {}
                            }
                        }
                    }

                    val list = forecastDateTimeMap.values.toMutableList()

                    list.sortWith { forecast1, forecast2 ->
                        val forecast1DateTime = "${forecast1.forecastDate}${forecast1.forecastTime}"
                        val forecast2DateTime = "${forecast2.forecastDate}${forecast2.forecastTime}"

                        return@sortWith forecast1DateTime.compareTo(forecast2DateTime)
                    }

                    Log.e("MainActivity", list.toString())

                    val currentForecast = list.first()

                    binding.temperatureTextView.text =
                        getString(R.string.temparature_text, currentForecast.temperature)
                    binding.skyTextView.text = currentForecast.weather
                    binding.precipitationTextView.text =
                        getString(R.string.precipitation_text, currentForecast.precipitation)

                    binding.childForecastLayout.apply {
                        list.forEachIndexed { index, forecast ->
                            if (index == 0) {
                                return@forEachIndexed
                            }
                            val itemView = ItemForecastBinding.inflate(layoutInflater)
                            itemView.timeTextView.text = forecast.forecastTime
                            itemView.weatherTextView.text = forecast.weather
                            itemView.temperatureTextView.text = getString(R.string.temparature_text, forecast.temperature)
                            addView(itemView.root)
                        }
                    }

//                    Log.e("MainActivity", forecastDateTimeMap.toString())
                }

                override fun onFailure(call: Call<WeatherEntity>, t: Throwable) {
                    t.printStackTrace()
                }
            })
        }
    }
}