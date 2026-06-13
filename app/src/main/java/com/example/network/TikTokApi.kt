package com.example.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class TikWmResponse(
    @Json(name = "code") val code: Int,
    @Json(name = "msg") val msg: String,
    @Json(name = "data") val data: TikWmVideoData?
)

@JsonClass(generateAdapter = true)
data class TikWmVideoData(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String?,
    @Json(name = "cover") val cover: String?,
    @Json(name = "origin_cover") val originCover: String?,
    @Json(name = "duration") val duration: Int?,
    @Json(name = "play") val play: String?,    // No-watermark video URL
    @Json(name = "wmplay") val wmplay: String?,  // Watermarked video URL
    @Json(name = "music") val music: String?,
    @Json(name = "author") val author: TikWmAuthor?
)

@JsonClass(generateAdapter = true)
data class TikWmAuthor(
    @Json(name = "id") val id: String?,
    @Json(name = "unique_id") val uniqueId: String?,
    @Json(name = "nickname") val nickname: String?,
    @Json(name = "avatar") val avatar: String?
)

interface TikTokApi {
    @GET("api/")
    suspend fun getVideoDetails(
        @Query("url") url: String
    ): TikWmResponse

    @FormUrlEncoded
    @POST("api/")
    suspend fun getVideoDetailsForm(
        @Field("url") url: String
    ): TikWmResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://www.tikwm.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val apiService: TikTokApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(TikTokApi::class.java)
    }
}
