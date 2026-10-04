package com.example

import com.example.data.remote.*
import org.junit.Assert.*
import org.junit.Test

class RetrofitMoshiClientTest {

    @Test
    fun testRetrofitClientConfiguration() {
        val retrofit = RetrofitClient.getRetrofit()
        assertNotNull("Retrofit instance should not be null", retrofit)
        assertNotNull("ApiService should not be null", RetrofitClient.apiService)
        assertEquals(
            "https://ais-dev-ijbtofik5sed2qvdau7kll-911496704946.asia-southeast1.run.app/",
            RetrofitClient.getBaseUrl()
        )

        // Test updating base URL dynamically
        RetrofitClient.setBaseUrl("http://10.0.2.2:8000")
        assertEquals("http://10.0.2.2:8000/", RetrofitClient.getBaseUrl())

        // Reset back to dev URL
        RetrofitClient.setBaseUrl("https://ais-dev-ijbtofik5sed2qvdau7kll-911496704946.asia-southeast1.run.app/")
    }

    @Test
    fun testMoshiJsonSerializationMultiDayRequest() {
        val moshi = RetrofitClient.moshi
        val adapter = moshi.adapter(MultiDayForecastRequest::class.java)

        val request = MultiDayForecastRequest(
            plantId = "plant_college_200",
            startDate = "2026-09-22",
            days = 7,
            cloudCoverDelta = 10.0,
            temperatureDelta = -2.5
        )

        val json = adapter.toJson(request)
        assertTrue("JSON should contain plant_id", json.contains("plant_college_200"))
        assertTrue("JSON should contain days", json.contains("\"days\":7"))
        assertTrue("JSON should contain cloud_cover_delta", json.contains("\"cloud_cover_delta\":10.0"))

        val parsed = adapter.fromJson(json)
        assertNotNull(parsed)
        assertEquals("plant_college_200", parsed?.plantId)
        assertEquals(7, parsed?.days)
        assertEquals(10.0, parsed?.cloudCoverDelta ?: 0.0, 0.01)
    }

    @Test
    fun testMoshiJsonDeserializationDayAheadResponse() {
        val sampleJson = """
        {
          "plant_id": "plant_college_200",
          "date": "2026-09-22",
          "model_used": "CNN-LSTM + ENN",
          "r2_score": 0.9991,
          "mae_kw": 1.579,
          "rmse_kw": 1.850,
          "peak_power_kw": 178.5,
          "total_energy_kwh": 1184.0,
          "hourly_forecasts": [
            {
              "hour": 12,
              "time_label": "12:00",
              "predicted_kw": 178.5,
              "lower_bound_kw": 172.0,
              "upper_bound_kw": 185.0,
              "actual_kw": 176.2,
              "ghi": 850.0,
              "temperature": 32.5,
              "cloud_cover": 12.0,
              "weather_condition": "CLEAR"
            }
          ]
        }
        """.trimIndent()

        val moshi = RetrofitClient.moshi
        val adapter = moshi.adapter(DayAheadForecastResponse::class.java)
        val response = adapter.fromJson(sampleJson)

        assertNotNull("Response should be parsed", response)
        assertEquals("plant_college_200", response?.plantId)
        assertEquals("CNN-LSTM + ENN", response?.modelUsed)
        assertEquals(0.9991, response?.r2Score ?: 0.0, 0.0001)
        assertEquals(1.579, response?.maeKw ?: 0.0, 0.001)
        assertEquals(1, response?.hourlyForecasts?.size)

        val firstHour = response?.hourlyForecasts?.first()
        assertEquals(12, firstHour?.hour)
        assertEquals(178.5, firstHour?.predictedKw ?: 0.0, 0.1)
        assertEquals(176.2, firstHour?.actualKw ?: 0.0, 0.1)
    }

    @Test
    fun testMoshiJsonDeserializationModelsMetricsResponse() {
        val sampleJson = """
        {
          "models": [
            {
              "name": "CNN-LSTM + ENN",
              "type": "Hybrid Deep Neural Network",
              "r2": 0.9991,
              "mae_kw": 1.579,
              "rmse_kw": 1.850,
              "mape_percent": 2.14,
              "latency_ms": 182,
              "is_primary": true,
              "description": "Spatial CNN + Temporal LSTM"
            }
          ],
          "neural_layers": [
            {
              "layer_number": 1,
              "type": "Conv1D",
              "configuration": "64 filters, kernel=3",
              "output_shape": "(None, 24, 64)",
              "purpose": "Local spatial feature extraction"
            }
          ],
          "feature_importances": [
            {
              "name": "GHI",
              "score": 0.38,
              "category": "Solar Irradiance"
            }
          ]
        }
        """.trimIndent()

        val moshi = RetrofitClient.moshi
        val adapter = moshi.adapter(ModelsMetricsResponse::class.java)
        val metrics = adapter.fromJson(sampleJson)

        assertNotNull(metrics)
        assertEquals(1, metrics?.models?.size)
        assertEquals("CNN-LSTM + ENN", metrics?.models?.first()?.name)
        assertTrue(metrics?.models?.first()?.isPrimary == true)
        assertEquals(1, metrics?.neuralLayers?.size)
        assertEquals("Conv1D", metrics?.neuralLayers?.first()?.type)
        assertEquals(1, metrics?.featureImportances?.size)
        assertEquals("GHI", metrics?.featureImportances?.first()?.name)
    }

    @Test
    fun testOpenMeteoResponseDeserialization() {
        val sampleOpenMeteoJson = """
        {
          "latitude": 28.625,
          "longitude": 77.25,
          "timezone": "Asia/Kolkata",
          "elevation": 216.0,
          "hourly": {
            "time": ["2026-09-22T00:00", "2026-09-22T01:00", "2026-09-22T12:00"],
            "temperature_2m": [24.5, 23.8, 34.2],
            "relative_humidity_2m": [60.0, 65.0, 38.0],
            "cloud_cover": [10.0, 15.0, 20.0],
            "direct_normal_irradiance": [0.0, 0.0, 780.0],
            "diffuse_radiation": [0.0, 0.0, 140.0],
            "shortwave_radiation_instant": [0.0, 0.0, 850.0],
            "wind_speed_10m": [2.5, 2.1, 3.8],
            "weather_code": [0, 1, 0]
          }
        }
        """.trimIndent()

        val moshi = RetrofitClient.moshi
        val adapter = moshi.adapter(OpenMeteoResponse::class.java)
        val response = adapter.fromJson(sampleOpenMeteoJson)

        assertNotNull("OpenMeteoResponse should be parsed", response)
        assertEquals(28.625, response?.latitude ?: 0.0, 0.001)
        assertEquals(77.25, response?.longitude ?: 0.0, 0.001)
        assertEquals("Asia/Kolkata", response?.timezone)
        assertEquals(216.0, response?.elevation ?: 0.0, 0.1)

        val hourly = response?.hourly
        assertNotNull("Hourly data should be present", hourly)
        assertEquals(3, hourly?.time?.size)
        assertEquals("2026-09-22T12:00", hourly?.time?.get(2))
        assertEquals(850.0, hourly?.shortwaveRadiationInstant?.get(2) ?: 0.0, 0.1)
        assertEquals(780.0, hourly?.directNormalIrradiance?.get(2) ?: 0.0, 0.1)
        assertEquals(140.0, hourly?.diffuseRadiation?.get(2) ?: 0.0, 0.1)
        assertEquals(34.2, hourly?.temperature2m?.get(2) ?: 0.0, 0.1)
        assertEquals(20.0, hourly?.cloudCover?.get(2) ?: 0.0, 0.1)
    }

    @Test
    fun testOpenMeteoApiServiceInstance() {
        val service = RetrofitClient.openMeteoApiService
        assertNotNull("OpenMeteoApiService should not be null", service)
    }

    @Test
    fun testLocationHelperPresets() {
        val presets = com.example.util.LocationHelper.PRESET_SOLAR_LOCATIONS
        assertTrue("Presets should have multiple locations", presets.size >= 5)
        val campus = presets.first()
        assertEquals("Campus 200kW Plant (Delhi)", campus.name)
        assertEquals(28.6139, campus.latitude, 0.001)
        assertEquals(77.2090, campus.longitude, 0.001)
    }
}
