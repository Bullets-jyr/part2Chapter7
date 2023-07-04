package kr.co.bullets.part2chapter7

import android.service.controls.templates.TemperatureControlTemplate

data class Forecast(
    val forecastDate: String,
    val forecastTime: String,
    // 강수확률
    var precipitation: Int = 0,
    // 강수형태
    // 없음(0), 비(1), 비/눈(2), 눈(3), 소나기(4)
    var precipitationType: String = "",
    // 하늘상태
    // 맑음(1), 구름많음(3), 흐림(4)
    var sky: String = "",
    // 1시간 기온
    var temperature: Double = 0.0,
) {
    val weather: String
        get() {
            return if (precipitationType == "" || precipitationType == "없음") {
                sky
            } else {
                precipitationType
            }
        }
}
