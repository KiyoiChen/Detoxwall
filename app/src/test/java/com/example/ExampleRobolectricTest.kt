package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.SecurityManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    assertEquals("DetoxWall", appName)
  }

  @Test
  fun `security manager hashes and verifies parent pin`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val securityManager = SecurityManager(context)
    
    assertTrue(securityManager.setParentPin("1234"))
    assertTrue(securityManager.hasPinSet())
    assertTrue(securityManager.verifyPin("1234"))
    assertFalse(securityManager.verifyPin("9999"))
  }

  @Test
  fun `security manager configures screen blank limit within 5m to 5h`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val securityManager = SecurityManager(context)

    // Test default
    assertTrue(securityManager.getScreenBlankLimitMinutes() >= 5)

    // Set 30 mins
    securityManager.setScreenBlankLimitMinutes(30)
    assertEquals(30, securityManager.getScreenBlankLimitMinutes())

    // Clamping below 5 mins
    securityManager.setScreenBlankLimitMinutes(2)
    assertEquals(5, securityManager.getScreenBlankLimitMinutes())

    // Clamping above 5 hours (300 mins)
    securityManager.setScreenBlankLimitMinutes(500)
    assertEquals(300, securityManager.getScreenBlankLimitMinutes())

    // Toggle screen blank state
    securityManager.setScreenBlankEnabled(true)
    assertTrue(securityManager.isScreenBlankEnabled())
    securityManager.setScreenBlankEnabled(false)
    assertFalse(securityManager.isScreenBlankEnabled())
  }
}
