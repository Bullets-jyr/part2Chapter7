package kr.co.bullets.part2chapter7

import android.Manifest
import android.app.PendingIntent
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.widget.RemoteViews
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices

class UpdateWeatherService : Service() {

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        // notification channel
        // foregroundService

        val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(this)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: 위젯을 권한없음 상태로 표시하고, 클릭했을 때 권한 팝업을 얻을 수 있도록 수정
            return super.onStartCommand(intent, flags, startId)
        }
        LocationServices.getFusedLocationProviderClient(this).lastLocation.addOnSuccessListener {
            WeatherRepository.getVillageForecast(
                longitude = it.longitude,
                latitude = it.latitude,
                successCallback = { forecastList ->
                    val pendingServiceIntent: PendingIntent =
                        Intent(this, UpdateWeatherService::class.java).let { intent ->
                            PendingIntent.getService(this, 1, intent, PendingIntent.FLAG_IMMUTABLE)
                        }

                    val currentForecast = forecastList.first()

                    RemoteViews(
                        packageName,
                        R.layout.widget_weather
                    ).apply {
                        setTextViewText(
                            R.id.temperature_text_view,
                            getString(R.string.temparature_text, currentForecast.temperature)
                        )
                        setTextViewText(
                            R.id.weather_text_view,
                            currentForecast.weather
                        )
                        setOnClickPendingIntent(R.id.temperature_text_view, pendingServiceIntent)
                    }.also { remoteViews ->
                        val appWidgetName =
                            ComponentName(this, WeatherAppWidgetProvider::class.java)
                        appWidgetManager.updateAppWidget(appWidgetName, remoteViews)
                    }

                    stopSelf()
                },
                failureCallback = {
                    // TODO: 위젯을 에러 상태로 표시
                    stopSelf()
                },
            )
        }

        return super.onStartCommand(intent, flags, startId)
    }
}