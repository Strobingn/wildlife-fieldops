package com.strobingn.wildlifefieldops.ml

import com.strobingn.wildlifefieldops.data.model.FindingSeverity
import com.strobingn.wildlifefieldops.data.model.JobPriority
import com.strobingn.wildlifefieldops.ml.commit.CaptureSeverityMapper
import org.junit.Assert.assertEquals
import org.junit.Test

class CaptureSeverityMapperTest {

    @Test
    fun severityScoresMap() {
        assertEquals(FindingSeverity.NONE, CaptureSeverityMapper.toFindingSeverity(0))
        assertEquals(FindingSeverity.LOW, CaptureSeverityMapper.toFindingSeverity(1))
        assertEquals(FindingSeverity.MODERATE, CaptureSeverityMapper.toFindingSeverity(2))
        assertEquals(FindingSeverity.HIGH, CaptureSeverityMapper.toFindingSeverity(3))
        assertEquals(FindingSeverity.CRITICAL, CaptureSeverityMapper.toFindingSeverity(4))
        assertEquals(FindingSeverity.CRITICAL, CaptureSeverityMapper.toFindingSeverity(99))
    }

    @Test
    fun priorityStringsMap() {
        assertEquals(JobPriority.LOW, CaptureSeverityMapper.toJobPriority("low"))
        assertEquals(JobPriority.MEDIUM, CaptureSeverityMapper.toJobPriority("MEDIUM"))
        assertEquals(JobPriority.HIGH, CaptureSeverityMapper.toJobPriority("HIGH"))
        assertEquals(JobPriority.URGENT, CaptureSeverityMapper.toJobPriority("URGENT"))
    }

    @Test
    fun blankPriority_usesSeverity() {
        assertEquals(JobPriority.URGENT, CaptureSeverityMapper.toJobPriority("", 4))
        assertEquals(JobPriority.HIGH, CaptureSeverityMapper.toJobPriority("", 3))
        assertEquals(JobPriority.MEDIUM, CaptureSeverityMapper.toJobPriority("", 1))
    }
}
