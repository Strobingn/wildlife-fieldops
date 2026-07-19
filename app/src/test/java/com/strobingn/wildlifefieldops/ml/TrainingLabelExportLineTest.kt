package com.strobingn.wildlifefieldops.ml

import com.strobingn.wildlifefieldops.ml.export.TrainingLabelExportLine
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainingLabelExportLineTest {

    @Test
    fun serializesJsonLine() {
        val line = TrainingLabelExportLine(
            id = "lab-1",
            photoId = "photo-1",
            target = "SPECIES",
            labelId = "raccoon",
            source = "MODEL_ACCEPTED",
            modelConfidence = 0.91f,
            createdAt = 1L
        )
        val raw = Json.encodeToString(line)
        assertTrue(raw.contains("raccoon"))
        assertTrue(raw.contains("SPECIES"))
        assertTrue(raw.contains("photo-1"))
    }
}
