package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {
    @Test fun `app name resource`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("遊戲時間小管家（測試版）", context.getString(R.string.app_name))
    }
}
