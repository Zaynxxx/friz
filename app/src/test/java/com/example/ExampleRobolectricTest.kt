package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.PreferencesManager
import com.example.util.PermissionUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34]) // Match stable Robolectric target bounds
class ExampleRobolectricTest {

    @Test
    fun verifyAppNameIsFreezeX() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("FreezeX", appName)
    }

    @Test
    fun verifyPermissionsDefaultState() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // On JVM clean emulated sandboxes, advanced system parameters default to untrusted/false
        val isAccessibilityEnabled = PermissionUtils.isAccessibilityServiceEnabled(context)
        val isOverlayEnabled = PermissionUtils.isOverlayPermissionGranted(context)

        assertFalse(isAccessibilityEnabled)
        assertFalse(isOverlayEnabled)
    }

    @Test
    fun testPreferencesFocusSessionTransitions() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val preferencesManager = PreferencesManager(context)

        // Default constraints check
        var isActive = preferencesManager.focusModeActiveFlow.first()
        assertFalse(isActive)

        // Enable session interval limit
        preferencesManager.startFocusSession(45)
        isActive = preferencesManager.focusModeActiveFlow.first()
        assertTrue(isActive)

        val endTime = preferencesManager.focusModeEndTimeFlow.first()
        assertTrue(endTime > System.currentTimeMillis())

        // Stop focus session limits
        preferencesManager.stopFocusSession()
        isActive = preferencesManager.focusModeActiveFlow.first()
        assertFalse(isActive)
    }

    @Test
    fun testPreferencesStrictModeToggle() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val preferencesManager = PreferencesManager(context)

        var strictMode = preferencesManager.strictModeFlow.first()
        assertFalse(strictMode)

        preferencesManager.setStrictMode(true)
        strictMode = preferencesManager.strictModeFlow.first()
        assertTrue(strictMode)
    }
}
