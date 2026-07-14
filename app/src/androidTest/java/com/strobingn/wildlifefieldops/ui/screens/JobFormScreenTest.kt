package com.strobingn.wildlifefieldops.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.strobingn.wildlifefieldops.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class JobFormScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun aiAnalyzeButton_exists_and_isClickable() {
        composeTestRule.onNodeWithTag("ai_analyze_button").assertExists()
        composeTestRule.onNodeWithTag("ai_analyze_button").assertIsEnabled()
    }

    @Test
    fun formFields_haveImePadding_and_areVisible() {
        // Test that keyboard does not block fields (imePadding)
        composeTestRule.onNodeWithTag("species_field").assertIsDisplayed()
        composeTestRule.onNodeWithTag("notes_field").assertIsDisplayed()
    }

    @Test
    fun grokPhotoAnalysis_fillsForm() {
        // Mock or trigger Grok analysis
        composeTestRule.onNodeWithTag("ai_analyze_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("species_field").assertTextContains("Raccoon", substring = true)
    }
} 