package com.example.data.remote

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // Default development server URL with fallback to local FastAPI instance
    private const val DEFAULT_BASE_URL = "https://ais-dev-ijbtofik5sed2qvdau7kll-911496704946.asia-southeast1.run.app/"
    private const val EMULATOR_LOCAL_URL = "http://10.0.2.2:8000/"

    @Volatile
    private var currentBaseUrl: String = DEFAULT_BASE_URL

    val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private val headerInterceptor = Interceptor { chain ->
        val original = chain.request()
        val requestBuilder = original.newBuilder()
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .header("User-Agent", "SolarCast-Android/1.0 (CNN-LSTM-ENN)")
            .method(original.method, original.body)
        chain.proceed(requestBuilder.build())
    }

    private val loggingInterceptor: HttpLoggingInterceptor by lazy {
        HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.BASIC
            }
        }
    }

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(headerInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    @Volatile
    private var retrofitInstance: Retrofit? = null

    @Volatile
    private var apiServiceInstance: SolarApiService? = null

    fun getBaseUrl(): String = currentBaseUrl

    @Synchronized
    fun setBaseUrl(newBaseUrl: String) {
        val formattedUrl = if (newBaseUrl.endsWith("/")) newBaseUrl else "$newBaseUrl/"
        if (formattedUrl != currentBaseUrl) {
            currentBaseUrl = formattedUrl
            retrofitInstance = null
            apiServiceInstance = null
        }
    }

    @Synchronized
    fun getRetrofit(): Retrofit {
        return retrofitInstance ?: Retrofit.Builder()
            .baseUrl(currentBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build().also { retrofitInstance = it }
    }

    val apiService: SolarApiService
        get() {
            return apiServiceInstance ?: synchronized(this) {
                apiServiceInstance ?: getRetrofit().create(SolarApiService::class.java).also {
                    apiServiceInstance = it
                }
            }
        }

    private const val OPEN_METEO_BASE_URL = "https://api.open-meteo.com/"

    @Volatile
    private var openMeteoServiceInstance: OpenMeteoApiService? = null

    val openMeteoApiService: OpenMeteoApiService
        get() {
            return openMeteoServiceInstance ?: synchronized(this) {
                openMeteoServiceInstance ?: Retrofit.Builder()
                    .baseUrl(OPEN_METEO_BASE_URL)
                    .client(okHttpClient)
                    .addConverterFactory(MoshiConverterFactory.create(moshi))
                    .build()
                    .create(OpenMeteoApiService::class.java)
                    .also { openMeteoServiceInstance = it }
            }
        }

    private const val OPEN_WEATHER_BASE_URL = "https://api.openweathermap.org/"

    @Volatile
    private var openWeatherServiceInstance: OpenWeatherApiService? = null

    val openWeatherApiService: OpenWeatherApiService
        get() {
            return openWeatherServiceInstance ?: synchronized(this) {
                openWeatherServiceInstance ?: Retrofit.Builder()
                    .baseUrl(OPEN_WEATHER_BASE_URL)
                    .client(okHttpClient)
                    .addConverterFactory(MoshiConverterFactory.create(moshi))
                    .build()
                    .create(OpenWeatherApiService::class.java)
                    .also { openWeatherServiceInstance = it }
            }
        }
}
