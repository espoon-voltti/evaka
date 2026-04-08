// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.daycare.dao

import evaka.core.PureJdbiTest
import evaka.core.placement.PlacementType
import evaka.core.placement.getDaycarePlacements
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.DevPlacement
import evaka.core.shared.dev.insert
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PlacementQueriesIntegrationTest : PureJdbiTest(resetDbBeforeEach = true) {
    private val placementStart = LocalDate.now().plusDays(300)
    private val placementEnd = placementStart.plusDays(200)

    private val area = DevCareArea()
    private val daycare = DevDaycare(areaId = area.id)
    private val child = DevPerson()
    private val placement =
        DevPlacement(
            childId = child.id,
            unitId = daycare.id,
            type = PlacementType.DAYCARE,
            startDate = placementStart,
            endDate = placementEnd,
        )

    @BeforeEach
    fun setUp() {
        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(placement)
        }
    }

    @Test
    fun `get daycare placements returns correct data`() {
        db.read { h ->
            val placements = h.getDaycarePlacements(daycare.id, null, null, null)
            assertThat(placements).hasSize(1)
            val placement = placements.first()
            assertThat(placement.id).isNotNull
            assertThat(placement.child.id).isEqualTo(child.id)
            assertThat(placement.daycare.id).isEqualTo(daycare.id)
            assertThat(placement.startDate).isEqualTo(placementStart)
            assertThat(placement.endDate).isEqualTo(placementEnd)
        }
    }

    /*
    search:       ------------------------------------------------------
    placement:                    xxxxxxxxxxxxxxxxxx
    matches: yes
     */
    @Test
    fun `get daycare placements overlap test #01`() {
        db.read { h ->
            val fromDate = null
            val toDate = null
            val placements = h.getDaycarePlacements(daycare.id, null, fromDate, toDate)
            assertThat(placements).isNotEmpty
        }
    }

    /*
    search:       -----------e
    placement:                    xxxxxxxxxxxxxxxxxx
    matches: no
     */
    @Test
    fun `get daycare placements overlap test #02`() {
        db.read { h ->
            val fromDate = null
            val toDate = placementStart.minusDays(100)
            val placements = h.getDaycarePlacements(daycare.id, null, fromDate, toDate)
            assertThat(placements).isEmpty()
        }
    }

    /*
    search:       ---------------------------e
    placement:                    xxxxxxxxxxxxxxxxxx
    matches: yes
     */
    @Test
    fun `get daycare placements overlap test #03`() {
        db.read { h ->
            val fromDate = null
            val toDate = placementStart.plusDays(100)
            val placements = h.getDaycarePlacements(daycare.id, null, fromDate, toDate)
            assertThat(placements).isNotEmpty
        }
    }

    /*
    search:       --------------------------------------------e
    placement:                    xxxxxxxxxxxxxxxxxx
    matches: yes
     */
    @Test
    fun `get daycare placements overlap test #04`() {
        db.read { h ->
            val fromDate = null
            val toDate = placementEnd.plusDays(100)
            val placements = h.getDaycarePlacements(daycare.id, null, fromDate, toDate)
            assertThat(placements).isNotEmpty
        }
    }

    /*
    search:                s---------------------------------------------
    placement:                    xxxxxxxxxxxxxxxxxx
    matches: yes
     */
    @Test
    fun `get daycare placements overlap test #05`() {
        db.read { h ->
            val fromDate = placementStart.minusDays(100)
            val toDate = null
            val placements = h.getDaycarePlacements(daycare.id, null, fromDate, toDate)
            assertThat(placements).isNotEmpty
        }
    }

    /*
    search:                                  s----------------------------
    placement:                    xxxxxxxxxxxxxxxxxx
    matches: yes
     */
    @Test
    fun `get daycare placements overlap test #06`() {
        db.read { h ->
            val fromDate = placementStart.plusDays(100)
            val toDate = null
            val placements = h.getDaycarePlacements(daycare.id, null, fromDate, toDate)
            assertThat(placements).isNotEmpty
        }
    }

    /*
    search:                                            s-----------------
    placement:                    xxxxxxxxxxxxxxxxxx
    matches: no
     */
    @Test
    fun `get daycare placements overlap test #07`() {
        db.read { h ->
            val fromDate = placementEnd.plusDays(100)
            val toDate = null
            val placements = h.getDaycarePlacements(daycare.id, null, fromDate, toDate)
            assertThat(placements).isEmpty()
        }
    }

    /*
    search:                s---e
    placement:                    xxxxxxxxxxxxxxxxxx
    matches: no
     */
    @Test
    fun `get daycare placements overlap test #08`() {
        db.read { h ->
            val fromDate = placementStart.minusDays(100)
            val toDate = placementStart.minusDays(1)
            val placements = h.getDaycarePlacements(daycare.id, null, fromDate, toDate)
            assertThat(placements).isEmpty()
        }
    }

    /*
    search:                s---------------e
    placement:                    xxxxxxxxxxxxxxxxxx
    matches: yes
     */
    @Test
    fun `get daycare placements overlap test #09`() {
        db.read { h ->
            val fromDate = placementStart.minusDays(100)
            val toDate = placementStart.plusDays(100)
            val placements = h.getDaycarePlacements(daycare.id, null, fromDate, toDate)
            assertThat(placements).isNotEmpty
        }
    }

    /*
    search:                s------e     (boundary value / inclusive test)
    placement:                    xxxxxxxxxxxxxxxxxx
    matches: yes
     */
    @Test
    fun `get daycare placements overlap test #09b`() {
        db.read { h ->
            val fromDate = placementStart.minusDays(100)
            val toDate = placementStart
            val placements = h.getDaycarePlacements(daycare.id, null, fromDate, toDate)
            assertThat(placements).isNotEmpty
        }
    }

    /*
    search:                                 s-----------e
    placement:                    xxxxxxxxxxxxxxxxxx
    matches: yes
     */
    @Test
    fun `get daycare placements overlap test #10`() {
        db.read { h ->
            val fromDate = placementStart.plusDays(100)
            val toDate = placementEnd.plusDays(100)
            val placements = h.getDaycarePlacements(daycare.id, null, fromDate, toDate)
            assertThat(placements).isNotEmpty
        }
    }

    /*
    search:                                        s---e  (test boundary case)
    placement:                    xxxxxxxxxxxxxxxxxx
    matches: yes
     */
    @Test
    fun `get daycare placements overlap test #11`() {
        db.read { h ->
            val fromDate = placementEnd
            val toDate = placementEnd.plusDays(150)
            val placements = h.getDaycarePlacements(daycare.id, null, fromDate, toDate)
            assertThat(placements).isNotEmpty
        }
    }

    /*
    search:                                            s---e
    placement:                    xxxxxxxxxxxxxxxxxx
    matches: no
     */
    @Test
    fun `get daycare placements overlap test #12`() {
        db.read { h ->
            val fromDate = placementEnd.plusDays(1)
            val toDate = placementEnd.plusDays(150)
            val placements = h.getDaycarePlacements(daycare.id, null, fromDate, toDate)
            assertThat(placements).isEmpty()
        }
    }

    /*
    search:                s--------------------------------e
    placement:                    xxxxxxxxxxxxxxxxxx
    matches: yes
     */
    @Test
    fun `get daycare placements overlap test #13`() {
        db.read { h ->
            val fromDate = placementStart.minusDays(100)
            val toDate = placementEnd.plusDays(100)
            val placements = h.getDaycarePlacements(daycare.id, null, fromDate, toDate)
            assertThat(placements).isNotEmpty
        }
    }
}
