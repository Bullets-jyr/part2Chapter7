package kr.co.bullets.part2chapter7

import com.google.gson.annotations.SerializedName

enum class Category {
    @SerializedName("POP")
    POP, // 강수확률
    @SerializedName("PTY")
    PTY, // 강수형태
    @SerializedName("SKY")
    SKY, // 하늘상태
    @SerializedName("TMP")
    TMP, // 1시간 기온
}