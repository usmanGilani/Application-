package com.example

import android.app.Application
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.ui.MainDashboardView
import com.example.viewmodel.LoadViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DashboardUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testEverythingAndTabsNavigation() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = LoadViewModel(app)

        composeTestRule.setContent {
            MainDashboardView(viewModel = viewModel)
        }

        // 1. Initial Township screen elements are displayed
        composeTestRule.onNodeWithTag("nav_township").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_blocks").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_substation").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_houses").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_settings").assertIsDisplayed()

        // 2. Click Blocks Tab
        composeTestRule.onNodeWithTag("nav_blocks").performClick()
        composeTestRule.waitForIdle()

        // 3. Click Substation Tab (Grid IQ)
        composeTestRule.onNodeWithTag("nav_substation").performClick()
        composeTestRule.waitForIdle()

        // Click Substation 1 card
        composeTestRule.onNodeWithTag("sub_card_Substation_1").performClick()
        composeTestRule.waitForIdle()

        // Click back button to overview
        composeTestRule.onNodeWithText("Return to Substation Grid Overview").performClick()
        composeTestRule.waitForIdle()

        // 4. Click Houses Tab
        composeTestRule.onNodeWithTag("nav_houses").performClick()
        composeTestRule.waitForIdle()

        // Test filtering toggle
        composeTestRule.onNodeWithTag("advanced_filters_toggle").performClick()
        composeTestRule.waitForIdle()

        // Toggle back
        composeTestRule.onNodeWithTag("advanced_filters_toggle").performClick()
        composeTestRule.waitForIdle()

        // 5. Click Settings Tab
        composeTestRule.onNodeWithTag("nav_settings").performClick()
        composeTestRule.waitForIdle()
    }
}
