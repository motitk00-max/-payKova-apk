package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.audio.KovaAcousticWakeWordDetector
import com.example.domain.tools.ToolExecutionEngine
import com.example.domain.tools.ToolExecutionResult
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
    assertEquals("Kova", appName)
  }

  @Test
  fun `wake word detector initializes and processes silence without false alarm`() {
    val detector = KovaAcousticWakeWordDetector()
    val silenceBuffer = ShortArray(1024) { 0 }
    val detected = detector.processSample(silenceBuffer, 1024)
    assertEquals(false, detected)
  }

  @Test
  fun `tool engine executes battery and time query`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val engine = ToolExecutionEngine(context)
    val batteryResult = engine.executeTool("getBatteryStatus", emptyMap())
    assertTrue(batteryResult is ToolExecutionResult.Success)

    val timeResult = engine.executeTool("getCurrentTime", emptyMap())
    assertTrue(timeResult is ToolExecutionResult.Success)
  }
}

