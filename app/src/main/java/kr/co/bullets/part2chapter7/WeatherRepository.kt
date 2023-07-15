package kr.co.bullets.part2chapter7

import android.util.Log
import kr.co.bullets.part2chapter7.databinding.ItemForecastBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object WeatherRepository {
    private val retrofit = Retrofit.Builder()
        .baseUrl("http://apis.data.go.kr/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val service = retrofit.create(WeatherService::class.java)

    fun getVillageForecast(
        longitude: Double,
        latitude: Double,
        successCallback: (List<Forecast>) -> Unit,
        failureCallback: (Throwable) -> Unit,
    ) {
        val baseDateTime = BaseDateTime.getBaseDateTime()
        val converter = GeoPointConverter()
//        val point = converter.convert(lat = it.latitude, lon = it.longitude)
        val point = converter.convert(lat = latitude, lon = longitude)
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

                if (list.isEmpty()) {
                    failureCallback(NullPointerException())
                } else {
                    successCallback(list)
                }
//                val currentForecast = list.first()
            }

            override fun onFailure(call: Call<WeatherEntity>, t: Throwable) {
                failureCallback(t)
//                t.printStackTrace()
            }
        })
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
}