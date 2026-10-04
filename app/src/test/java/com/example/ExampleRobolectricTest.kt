package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SolarForecastDatabase
import com.example.data.repository.SolarRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("SolarCast", appName)
  }

  @Test
  fun `verify solar repository seed and retrieval`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = SolarForecastDatabase.getInstance(context)
    val repo = SolarRepository(db.solarForecastDao())

    repo.initializeIfNeeded()
    val plants = repo.getAllPlants().first()
    assertTrue("Plants should be seeded", plants.isNotEmpty())

    val firstPlant = plants.first()
    assertEquals("plant_college_200", firstPlant.id)
    assertEquals(200.0, firstPlant.capacityKw, 0.01)

    val daySummaries = repo.getDaySummaries(firstPlant.id)
    assertEquals("Should generate 7 days of forecast summaries", 7, daySummaries.size)

    val benchmarks = repo.getModelBenchmarks()
    val primary = benchmarks.find { it.isPrimary }
    assertNotNull("Primary benchmark should exist", primary)
    assertTrue("R2 should exceed 0.998 threshold", (primary?.r2 ?: 0.0) >= 0.998)
  }
}

