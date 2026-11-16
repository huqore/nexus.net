package net.nexus.app.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmergencyUpdateTest {
    @Test fun onlyAMajorBumpIsAnEmergency() {
        assertTrue(UpdateChecker.isEmergency("5.0.0", "4.3.0"))
        assertTrue(UpdateChecker.isEmergency("v5.0.1", "4.9.9"))
        assertFalse(UpdateChecker.isEmergency("4.4.0", "4.3.0"))
        assertFalse(UpdateChecker.isEmergency("4.3.0", "4.3.0"))
        assertFalse(UpdateChecker.isEmergency("garbage", "4.3.0"))
    }
}
