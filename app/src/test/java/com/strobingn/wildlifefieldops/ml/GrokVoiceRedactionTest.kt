package com.strobingn.wildlifefieldops.ml

import com.strobingn.wildlifefieldops.ml.vision.TaxonomyMapper
import com.strobingn.wildlifefieldops.ml.voice.GrokVoiceJobParser
import com.strobingn.wildlifefieldops.ml.voice.RegexVoiceJobParser
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GrokVoiceRedactionTest {

    @Test
    fun redactsPhoneNumbersBeforeCloud() {
        val parser = GrokVoiceJobParser(
            TaxonomyMapper.default(),
            RegexVoiceJobParser(TaxonomyMapper.default())
        )
        val out = parser.redactSensitive("Call me at 555-123-4567 about the raccoon")
        assertTrue(out.contains("[phone]"))
        assertFalse(out.contains("555-123-4567"))
    }
}
