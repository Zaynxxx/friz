package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.example.data.PreferencesManager
import com.example.ui.BlockerScreen
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
class GreetingScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun blocker_overlay_screenshot() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val preferencesManager = PreferencesManager(context)

        composeTestRule.setContent {
            MyApplicationTheme {
                BlockerScreen(
                    appLabel = "Distractive Social Media",
                    packageName = "com.mock.social",
                    preferencesManager = preferencesManager,
                    onExitToHome = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
    }
}
