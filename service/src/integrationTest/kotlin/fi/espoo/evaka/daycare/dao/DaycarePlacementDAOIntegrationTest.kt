// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.dao

import fi.espoo.evaka.daycare.AbstractIntegrationTest
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.db.handle
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.UUID

class DaycarePlacementDAOIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    lateinit var placementDAO: DaycarePlacementDAO

    // data from migration scripts
    final val childId = UUID.fromString("2b929594-13ab-4042-9b13-74e84921e6f0")
    final val daycareId = UUID.fromString("68851e10-eb86-443e-b28d-0f6ee9642a3c")

    final val placementId = UUID.randomUUID()
    final val placementStart = LocalDate.now().plusDays(300)
    final val placementEnd = placementStart.plusDays(200)

    @BeforeAll
    fun beforeAll() {
        jdbi.handle {
            it.execute("DELETE FROM daycare_group_placement WHERE EXISTS (SELECT 1 FROM placement WHERE child_id = ?)", childId)
            it.execute("DELETE FROM placement WHERE child_id = ?", childId)
        }
    }

    @BeforeEach
    fun setUp() {
        jdbi.handle {
            it.execute(
                // language=sql
                """
                INSERT INTO placement (id, type, child_id, unit_id, start_date, end_date)
                VALUES ('$placementId', '${PlacementType.DAYCARE}'::placement_type, '$childId', '$daycareId', '$placementStart', '$placementEnd')
                """.trimIndent()
            )
        }
    }

    @AfterEach
    fun afterEach() {
        jdbi.handle { it.execute("DELETE FROM placement WHERE id = '$placementId'") }
    }

    @Test
    fun `get daycare placements returns correct data`() {
        val placements = placementDAO.getDaycarePlacements(daycareId, null, null, null)
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
    fun `get daycare placements overlap test #01`() {
        val fromDate = null
        val toDate = null
        val placements = placementDAO.getDaycarePlacements(daycareId, null, fromDate, toDate)
        assertThat(placements).isNotEmpty
    }

    /*
    search:       -----------e
    placement:                    xxxxxxxxxxxxxxxxxx
    matches: no
     */
    @Test
    fun `get daycare placements overlap test #02`() {
        val fromDate = null
        val toDate = placementStart.minusDays(100)
        val placements = placementDAO.getDaycarePlacements(daycareId, null, fromDate, toDate)
        assertThat(placements).isEmpty()
    }

    /*
    search:       ---------------------------e
    placement:                    xxxxxxxxxxxxxxxxxx
    matches: yes
     */
    @Test
    fun `get daycare placements overlap test #03`() {
        val fromDate = null
        val toDate = placementStart.plusDays(100)
        val placements = placementDAO.getDaycarePlacements(daycareId, null, fromDate, toDate)
        assertThat(placements).isNotEmpty
    }

    /*
    search:       --------------------------------------------e
    placement:                    xxxxxxxxxxxxxxxxxx
    matches: yes
     */
    @Test
    fun `get daycare placements overlap test #04`() {
        val fromDate = null
        val toDate = placementEnd.plusDays(100)
        val placements = placementDAO.getDaycarePlacements(daycareId, null, fromDate, toDate)
        assertThat(placements).isNotEmpty
    }

    /*
    search:                s---------------------------------------------
    placement:                    xxxxxxxxxxxxxxxxxx
    matches: yes
     */
    @Test
    fun `get daycare placements overlap test #05`() {
        val fromDate = placementStart.minusDays(100)
        val toDate = null
        val placements = placementDAO.getDaycarePlacements(daycareId, null, fromDate, toDate)
        assertThat(placements).isNotEmpty
    }

    /*
    search:                                  s----------------------------
    placement:                    xxxxxxxxxxxxxxxxxx
    matches: yes
     */
    @Test
    fun `get daycare placements overlap test #06`() {
        val fromDate = placementStart.plusDays(100)
        val toDate = null
        val placements = placementDAO.getDaycarePlacements(daycareId, null, fromDate, toDate)
        assertThat(placements).isNotEmpty
    }

    /*
    search:                                            s-----------------
    placement:                    xxxxxxxxxxxxxxxxxx
    matches: no
     */
    @Test
    fun `get daycare placements overlap test #07`() {
        val fromDate = placementEnd.plusDays(100)
        val toDate = null
        val placements = placementDAO.getDaycarePlacements(daycareId, null, fromDate, toDate)
        assertThat(placements).isEmpty()
    }

    /*
    search:                s---e
    placement:                    xxxxxxxxxxxxxxxxxx
    matches: no
     */
    @Test
    fun `get daycare placements overlap test #08`() {
        val fromDate = placementStart.minusDays(100)
        val toDate = placementStart.minusDays(1)
        val placements = placementDAO.getDaycarePlacements(daycareId, null, fromDate, toDate)
        assertThat(placements).isEmpty()
    }

    /*
    search:                s---------------e
    placement:                    xxxxxxxxxxxxxxxxxx
    matches: yes
     */
    @Test
    fun `get daycare placements overlap test #09`() {
        val fromDate = placementStart.minusDays(100)
        val toDate = placementStart.plusDays(100)
        val placements = placementDAO.getDaycarePlacements(daycareId, null, fromDate, toDate)
        assertThat(placements).isNotEmpty
    }

    /*
    search:                s------e     (boundary value / inclusive test)
    placement:                    xxxxxxxxxxxxxxxxxx
    matches: yes
     */
    @Test
    fun `get daycare placements overlap test #09b`() {
        val fromDate = placementStart.minusDays(100)
        val toDate = placementStart
        val placements = placementDAO.getDaycarePlacements(daycareId, null, fromDate, toDate)
        assertThat(placements).isNotEmpty
    }

    /*
    search:                                 s-----------e
    placement:                    xxxxxxxxxxxxxxxxxx
    matches: yes
     */
    @Test
    fun `get daycare placements overlap test #10`() {
        val fromDate = placementStart.plusDays(100)
        val toDate = placementEnd.plusDays(100)
        val placements = placementDAO.getDaycarePlacements(daycareId, null, fromDate, toDate)
        assertThat(placements).isNotEmpty
    }

    /*
    search:                                        s---e  (test boundary case)
    placement:                    xxxxxxxxxxxxxxxxxx
    matches: yes
     */
    @Test
    fun `get daycare placements overlap test #11`() {
        val fromDate = placementEnd
        val toDate = placementEnd.plusDays(150)
        val placements = placementDAO.getDaycarePlacements(daycareId, null, fromDate, toDate)
        assertThat(placements).isNotEmpty
    }

    /*
    search:                                            s---e
    placement:                    xxxxxxxxxxxxxxxxxx
    matches: no
     */
    @Test
    fun `get daycare placements overlap test #12`() {
        val fromDate = placementEnd.plusDays(1)
        val toDate = placementEnd.plusDays(150)
        val placements = placementDAO.getDaycarePlacements(daycareId, null, fromDate, toDate)
        assertThat(placements).isEmpty()
    }

    /*
    search:                s--------------------------------e
    placement:                    xxxxxxxxxxxxxxxxxx
    matches: yes
     */
    @Test
    fun `get daycare placements overlap test #13`() {
        val fromDate = placementStart.minusDays(100)
        val toDate = placementEnd.plusDays(100)
        val placements = placementDAO.getDaycarePlacements(daycareId, null, fromDate, toDate)
        assertThat(placements).isNotEmpty
    }
}
