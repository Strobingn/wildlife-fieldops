package com.strobingn.wildlifefieldops.ai

import com.strobingn.wildlifefieldops.data.model.InventoryItem
import com.strobingn.wildlifefieldops.data.model.Job
import com.strobingn.wildlifefieldops.data.model.JobPriority
import com.strobingn.wildlifefieldops.data.model.JobStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AIOperationsEngineTest {

    @Test
    fun urgentOverdueJobRanksHigh() {
        val now = 1_800_000_000_000L
        val job = Job(
            title = "Raccoon attic damage",
            address = "12 Test Road",
            priority = JobPriority.URGENT,
            status = JobStatus.PENDING,
            scheduledDate = now - 2 * 86_400_000L,
            notes = "Roof entry with droppings"
        )

        val result = AIOperationsEngine.analyze(listOf(job), emptyList(), now)

        assertTrue(result.prioritizedJobs.first().score >= 75)
        assertTrue(result.safetySignals.isNotEmpty())
    }

    @Test
    fun repeatAddressCreatesPropertyRisk() {
        val jobs = listOf(
            Job(title = "Bat entry", address = "10 Main St", status = JobStatus.COMPLETED),
            Job(title = "Soffit damage", address = " 10  MAIN ST ", priority = JobPriority.HIGH)
        )

        val result = AIOperationsEngine.analyze(jobs, emptyList())

        assertTrue(result.propertyRisks.first().serviceCount == 2)
        assertTrue(result.propertyRisks.first().score > 0)
    }

    @Test
    fun lowStockUsesRealInventoryLevels() {
        val item = InventoryItem(
            name = "Hardware cloth",
            quantityOnHand = 2.0,
            quantityReserved = 1.0,
            reorderLevel = 5.0,
            reorderQuantity = 10.0
        )

        val result = AIOperationsEngine.analyze(emptyList(), listOf(item))

        assertTrue(result.inventoryForecasts.single().recommendedOrder >= 9.0)
    }

    @Test
    fun liveAiPromptDoesNotContainAddresses() {
        val address = "987 Private Customer Lane"
        val result = AIOperationsEngine.analyze(
            listOf(Job(title = "Squirrel exclusion", address = address)),
            emptyList()
        )

        assertFalse(result.toPrivacySafePrompt().contains(address))
    }
}
