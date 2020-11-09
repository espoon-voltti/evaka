// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.dao

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.placement.getDaycarePlacements
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.db.handle
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class PlacementQueriesIntegrationTest : PureJdbiTest() {
    // data from migration scripts
    val childId = UUID.fromString("2b929594-13ab-4042-9b13-74e84921e6f0")
    val daycareId = UUID.fromString("68851e10-eb86-443e-b28d-0f6ee9642a3c")

    val placementId = UUID.randomUUID()
    val placementStart = LocalDate.now().plusDays(300)
    val placementEnd = placementStart.plusDays(200)

    @BeforeEach
    fun setUp() {
        val legacyDataSql = this.javaClass.getResource("/legacy_db_data.sql").readText()
        jdbi.handle {
            resetDatabase(it)
            it.execute(legacyDataSql)
            it.execute(
                // language=sql
                """
                INSERT INTO placement (id, type, child_id, unit_id, start_date, end_date)
                VALUES ('$placementId', '${PlacementType.DAYCARE}'::placement_type, '$childId', '$daycareId', '$placementStart', '$placementEnd')
                """.trimIndent()
            )
        }
    }

    @Test
    fun `get daycare placements returns correct data`() = jdbi.handle { h ->
        val placements = h.getDaycarePlacements(daycareId, null, null, null)
        assertThat(placements).hasSize(1)
        val placement = placements.first()
        assertThat(placement.id).isNotNull()
        assertThat(placement.child.id).isEqualTo(childId)
        assertThat(placement.daycare.id).isEqualTo(daycareId)
        assertThat(placement.startDate).isEqualTo(placementStart)
        assertThat(placement.endDate).isEqualTo(placementEnd)
    }

    /*
    search:       ------------------------------------------------------
    placement:                    xxxxxxxxxxxxxxxxxx
    matches: yes
     */
    @Test
    fun `get daycare placements overlap test #01`() = jdbi.handle { h ->
        val fromDate = null
        val toDate = null
        val placements = h.getDaycarePlacements(daycareId, null, fromDate, toDate)
        assertThat(placements).isNotEmpty
    }

    /*
    search:       -----------e
    placement:                    xxxxxxxxxxxxxxxxxx
    matches: no
     */
    @Test
    fun `get daycare placements overlap test #02`() = jdbi.handle { h ->
        val fromDate = null
        val toDate = placementStart.minusDays(100)
        val placements = h.getDaycarePlacements(daycareId, null, fromDate, toDate)
        assertThat(placements).isEmpty()
    }

    /*
    search:       ---------------------------e
    placement:                    xxxxxxxxxxxxxxxxxx
    matches: yes
     */
    @Test
    fun `get daycare placements overlap test #03`() = jdbi.handle { h ->
        val fromDate = null
        val toDate = placementStart.plusDays(100)
        val placements = h.getDaycarePlacements(daycareId, null, fromDate, toDate)
        assertThat(placements).isNotEmpty
    }

    /*
    search:       --------------------------------------------e
    placement:                    xxxxxxxxxxxxxxxxxx
    matches: yes
     */
    @Test
    fun `get daycare placements overlap test #04`() = jdbi.handle { h ->
        val fromDate = null
        val toDate = placementEnd.plusDays(100)
        val placements = h.getDaycarePlacements(daycareId, null, fromDate, toDate)
        assertThat(placements).isNotEmpty
    }

    /*
    search:                s---------------------------------------------
    placement:                    xxxxxxxxxxxxxxxxxx
    matches: yes
     */
    @Test
    fun `get daycare placements overlap test #05`() = jdbi.handle { h ->
        val fromDate = placementStart.minusDays(100)
        val toDate = null
        val placements = h.getDaycarePlacements(daycareId, null, fromDate, toDate)
        assertThat(placements).isNotEmpty
    }

    /*
    search:                                  s----------------------------
    placement:                    xxxxxxxxxxxxxxxxxx
    matches: yes
     */
    @Test
    fun `get daycare placements overlap test #06`() = jdbi.handle { h ->
        val fromDate = placementStart.plusDays(100)
        val toDate = null
        val placements = h.getDaycarePlacements(daycareId, null, fromDate, toDate)
        assertThat(placements).isNotEmpty
    }

    /*
    search:                                            s-----------------
    placement:                    xxxxxxxxxxxxxxxxxx
    matches: no
     */
    @Test
    fun `get daycare placements overlap test #07`() = jdbi.handle { h ->
        val fromDate = placementEnd.plusDays(100)
        val toDate = null
        val placements = h.getDaycarePlacements(daycareId, null, fromDate, toDate)
        assertThat(placements).isEmpty()
    }

    /*
    search:                s---e
    placement:                    xxxxxxxxxxxxxxxxxx
    matches: no
     */
    @Test
    fun `get daycare placements overlap test #08`() = jdbi.handle { h ->
        val fromDate = placementStart.minusDays(100)
        val toDate = placementStart.minusDays(1)
        val placements = h.getDaycarePlacements(daycareId, null, fromDate, toDate)
        assertThat(placements).isEmpty()
    }

    /*
    search:                s---------------e
    placement:                    xxxxxxxxxxxxxxxxxx
    matches: yes
     */
    @Test
    fun `get daycare placements overlap test #09`() = jdbi.handle { h ->
        val fromDate = placementStart.minusDays(100)
        val toDate = placementStart.plusDays(100)
        val placements = h.getDaycarePlacements(daycareId, null, fromDate, toDate)
        assertThat(placements).isNotEmpty
    }

    /*
    search:                s------e     (boundary value / inclusive test)
    placement:                    xxxxxxxxxxxxxxxxxx
    matches: yes
     */
    @Test
    fun `get daycare placements overlap test #09b`() = jdbi.handle { h ->
        val fromDate = placementStart.minusDays(100)
        val toDate = placementStart
        val placements = h.getDaycarePlacements(daycareId, null, fromDate, toDate)
        assertThat(placements).isNotEmpty
    }

    /*
    search:                                 s-----------e
    placement:                    xxxxxxxxxxxxxxxxxx
    matches: yes
     */
    @Test
    fun `get daycare placements overlap test #10`() = jdbi.handle { h ->
        val fromDate = placementStart.plusDays(100)
        val toDate = placementEnd.plusDays(100)
        val placements = h.getDaycarePlacements(daycareId, null, fromDate, toDate)
        assertThat(placements).isNotEmpty
    }

    /*
    search:                                        s---e  (test boundary case)
    placement:                    xxxxxxxxxxxxxxxxxx
    matches: yes
     */
    @Test
    fun `get daycare placements overlap test #11`() = jdbi.handle { h ->
        val fromDate = placementEnd
        val toDate = placementEnd.plusDays(150)
        val placements = h.getDaycarePlacements(daycareId, null, fromDate, toDate)
        assertThat(placements).isNotEmpty
    }

    /*
    search:                                            s---e
    placement:                    xxxxxxxxxxxxxxxxxx
    matches: no
     */
    @Test
    fun `get daycare placements overlap test #12`() = jdbi.handle { h ->
        val fromDate = placementEnd.plusDays(1)
        val toDate = placementEnd.plusDays(150)
        val placements = h.getDaycarePlacements(daycareId, null, fromDate, toDate)
        assertThat(placements).isEmpty()
    }

    /*
    search:                s--------------------------------e
    placement:                    xxxxxxxxxxxxxxxxxx
    matches: yes
     */
    @Test
    fun `get daycare placements overlap test #13`() = jdbi.handle { h ->
        val fromDate = placementStart.minusDays(100)
        val toDate = placementEnd.plusDays(100)
        val placements = h.getDaycarePlacements(daycareId, null, fromDate, toDate)
        assertThat(placements).isNotEmpty
    }
}
