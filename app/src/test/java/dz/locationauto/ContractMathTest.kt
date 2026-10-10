package dz.locationauto

import dz.locationauto.data.Contract
import org.junit.Assert.*
import org.junit.Test

class ContractMathTest {

    private fun c(start: Long, end: Long, rate: Long, extras: Long = 0) =
        Contract(vehicleId = 1, clientId = 1, startDateEpoch = start,
            endDateEpoch = end, dailyRateDzd = rate, extraChargesDzd = extras)

    @Test fun `single day contract counts 1 day`() {
        val now = 1_700_000_000_000L
        assertEquals(1, c(now, now, 5_000).days())
    }

    @Test fun `three day contract total`() {
        val now = 1_700_000_000_000L
        val ct = c(now, now + 2 * 86_400_000, 5_000)
        assertEquals(3, ct.days())
        assertEquals(15_000, ct.totalDzd())
    }

    @Test fun `extras added to total`() {
        val now = 1_700_000_000_000L
        val ct = c(now, now, 8_000, extras = 3_500)
        assertEquals(11_500, ct.totalDzd())
    }

    @Test fun `end before start still bills one day`() {
        val now = 1_700_000_000_000L
        assertEquals(1, c(now, now - 86_400_000, 4_000).days())
    }
}
