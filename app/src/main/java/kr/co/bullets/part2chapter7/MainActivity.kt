package kr.co.bullets.part2chapter7

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import kr.co.bullets.part2chapter7.databinding.ActivityMainBinding
import kr.co.bullets.part2chapter7.databinding.ItemForecastBinding
import java.util.Locale

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

            Thread {
                try {
                    val addressList = Geocoder(this, Locale.KOREA).getFromLocation(
                        it.latitude,
                        it.longitude,
                        1
                    )
                    runOnUiThread {
                        binding.locationTextView.text = addressList?.get(0)?.thoroughfare.orEmpty()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.start()

            WeatherRepository.getVillageForecast(
                longitude = it.longitude,
                latitude = it.latitude,
                successCallback = { list ->
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
                            itemView.temperatureTextView.text =
                                getString(R.string.temparature_text, forecast.temperature)
                            addView(itemView.root)
                        }
                    }
//                    Log.e("MainActivity", forecastDateTimeMap.toString())
                    Log.e("MainActivity", list.toString())
                },
                failureCallback = {
                    it.printStackTrace()
                },
            )
        }
    }
}