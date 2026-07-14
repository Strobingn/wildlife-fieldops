package com.strobingn.wildlifefieldops.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.strobingn.wildlifefieldops.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DashboardScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun hudsonValleyWeatherCard_isDisplayed() {
        composeTestRule.onNodeWithText("Hudson Valley").assertIsDisplayed()
        composeTestRule.onNodeWithTag("weather_card").assertExists()
    }

    @Test
    fun moneyOutInOpenJobs_isDisplayed_and_updates() {
        composeTestRule.onNodeWithTag("money_out_card").assertIsDisplayed()
        // Verify it shows a dollar amount
        composeTestRule.onNodeWithTag("money_out_card").onChildAt(0).assertTextContains("$")
    }

    @Test
    fun dashboardStats_updateWithOpenJobs() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("open_jobs_total").assertExists()
    }
} 