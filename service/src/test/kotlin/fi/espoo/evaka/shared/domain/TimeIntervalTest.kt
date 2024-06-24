// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.domain

import java.time.LocalTime
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class TimeIntervalTest {
    @Test
    fun `overlaps returns false if there is no overlap`() {
        //   01 02 03 04 05
        // A ------_
        // B       ------_
        val a = testRange(1, 3)
        val b = testRange(3, 5)
        assertFalse(a.overlaps(b))
        assertFalse(b.overlaps(a))

        //   01 02 03 04 05 06
        // C ------_
        // D          ------_
        val c = testRange(1, 3)
        val d = testRange(4, 6)
        assertFalse(c.overlaps(d))
        assertFalse(d.overlaps(c))

        //   00 01 02     23 00
        // E ------_
        // F              ---_
        val e = testRange(0, 2)
        val f = testRange(23, 0)
        assertFalse(e.overlaps(f))
        assertFalse(f.overlaps(e))

        //   00 01 02 03
        // G ------_
        // H       -----..
        val g = testRange(0, 2)
        val h = testRange(2, null)
        assertFalse(g.overlaps(h))
        assertFalse(h.overlaps(g))
    }

    @Test
    fun `overlaps works when there is overlap`() {
        //   01 02 03 04 05
        // A ---------_
        // B    ---------_
        val a = testRange(1, 4)
        val b = testRange(2, 5)
        assertTrue(a.overlaps(b))
        assertTrue(b.overlaps(a))

        //   01 02 03 04 05 06
        // C ---------_
        // D       ---------_
        val c = testRange(1, 4)
        val d = testRange(3, 6)
        assertTrue(c.overlaps(d))
        assertTrue(d.overlaps(c))

        //   01 02 03 04 05 06
        // E ---------------_
        // F    ---------_
        val e = testRange(1, 6)
        val f = testRange(2, 5)
        assertTrue(e.overlaps(f))
        assertTrue(f.overlaps(e))

        //   01 02 03 04 05 06
        // G ------_
        // H    ------..
        val g = testRange(1, 3)
        val h = testRange(2, null)
        assertTrue(g.overlaps(h))
        assertTrue(h.overlaps(g))

        //   01 02 03 04 05 06
        // I ---..
        // J          ------_
        val i = testRange(1, null)
        val j = testRange(4, 6)
        assertTrue(i.overlaps(j))
        assertTrue(j.overlaps(i))
    }

    @Test
    fun `a range intersects fully with itself`() {
        //   01 02 03 04 05 06
        // X ---------------_
        // X ---------------_
        val x = testRange(1, 6)
        assertTrue(x.overlaps(x))

        //   01 02 03 04 05 06
        // Y    ---..
        // Y    ---..
        val y = testRange(2, null)
        assertTrue(y.overlaps(y))
    }

    @Test
    fun `a range includes all its start, middle, but not its end`() {
        val range = testRange(1, 10)
        assertTrue(range.includes(testTime(1)))
        assertTrue(range.includes(testTime(5)))
        assertFalse(range.includes(testTime(10)))
    }

    @Test
    fun `a range doesn't include a localtime outside its bounds`() {
        //   01 02 03 04 05
        // A    ---------_
        // B --
        // C             --
        val a = testRange(2, 5)
        val b = testTime(1)
        val c = testTime(5)
        assertFalse(a.includes(b))
        assertFalse(a.includes(c))

        //   00 01 ... 22 23 00
        // D           ------_
        // E --
        val d = testRange(22, 0)
        val e = testTime(0)
        assertFalse(d.includes(e))
    }

    @Test
    fun `startsAfter works`() {
        //   01 02 03 04 05
        // A    ---_
        // B --
        // C    --
        // D       --
        val a = testRange(2, 3)
        val b = testTime(1)
        val c = testTime(2)
        val d = testTime(3)
        assertTrue(a.startsAfter(b))
        assertFalse(a.startsAfter(c))
        assertFalse(a.startsAfter(d))
    }

    @Test
    fun `a range includes a localtime inside its bounds`() {
        //   00 01 02 03
        // A ---------_
        // B    ---......
        // C       --
        val a = testRange(0, 3)
        val b = testRange(1, null)
        val c = testTime(2)
        assertTrue(a.includes(c))
        assertTrue(b.includes(c))
    }

    private fun testTime(hour: Int) = LocalTime.of(hour, 0)

    private fun testRange(
        from: Int,
        to: Int?
    ) = TimeInterval(testTime(from), to?.let { testTime(it) })
}
