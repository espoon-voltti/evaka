// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.service

import fi.espoo.evaka.daycare.AbstractIntegrationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.shared.db.withSpringHandle
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.insertTestDaycareGroup
import fi.espoo.evaka.testDaycare
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.time.LocalDate
import java.util.UUID

class CaretakerServiceIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    lateinit var jdbc: NamedParameterJdbcTemplate

    @Autowired
    lateinit var service: CaretakerService

    private val daycareId = testDaycare.id
    private val groupId = UUID.randomUUID()
    private val groupStart = LocalDate.of(2000, 1, 1)

    @BeforeEach
    fun setup() {
        withSpringHandle(dataSource) {
            insertGeneralTestFixtures(it)
            it.insertTestDaycareGroup(
                DevDaycareGroup(
                    id = groupId,
                    daycareId = daycareId,
                    startDate = groupStart
                )
            )
        }
        jdbc.update(
            "INSERT INTO daycare_caretaker (group_id, start_date, amount) VALUES (:groupId, :start, :amount)",
            mapOf("groupId" to groupId, "start" to groupStart, "amount" to 3)
        )
    }

    @Test
    fun `group initially has one row`() {
        val caretakers = service.getCaretakers(groupId)
        assertEquals(1, caretakers.size)
        assertEquals(groupId, caretakers[0].groupId)
        assertEquals(groupStart, caretakers[0].startDate)
        assertEquals(null, caretakers[0].endDate)
        assertEquals(3.0, caretakers[0].amount)
    }

    @Test
    fun `inserting caretaker row`() {
        val start2 = LocalDate.of(2000, 6, 1)
        service.insert(
            groupId = groupId,
            startDate = start2,
            endDate = null,
            amount = 5.0
        )
        val caretakers = service.getCaretakers(groupId)
        assertEquals(2, caretakers.size)

        assertEquals(start2, caretakers[0].startDate)
        assertEquals(null, caretakers[0].endDate)
        assertEquals(5.0, caretakers[0].amount)

        assertEquals(groupStart, caretakers[1].startDate)
        assertEquals(start2.minusDays(1), caretakers[1].endDate)
        assertEquals(3.0, caretakers[1].amount)
    }

    @Test
    fun `updating caretaker row`() {
        val id = service.getCaretakers(groupId).first().id
        service.update(
            groupId = groupId,
            id = id,
            startDate = LocalDate.of(2000, 7, 1),
            endDate = LocalDate.of(2000, 8, 1),
            amount = 2.0
        )

        val rows = service.getCaretakers(groupId)
        assertEquals(1, rows.size)
        val updated = rows.first()
        assertEquals(LocalDate.of(2000, 7, 1), updated.startDate)
        assertEquals(LocalDate.of(2000, 8, 1), updated.endDate)
        assertEquals(2.0, updated.amount)
    }
}
