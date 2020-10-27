// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.insertTestVardaOrganizer
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.db.handle
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.insertTestCareArea
import fi.espoo.evaka.shared.dev.insertTestDaycare
import fi.espoo.evaka.testAreaCode
import fi.espoo.evaka.testAreaId
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
import fi.espoo.evaka.testPurchasedDaycare
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.kotlin.mapTo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class VardaUnitIntegrationTest : FullApplicationTest() {
    @BeforeEach
    fun beforeEach() {
        jdbi.handle { h ->
            resetDatabase(h)
            h.insertTestCareArea(DevCareArea(id = testAreaId, name = testDaycare.areaName, areaCode = testAreaCode))
            h.insertTestDaycare(DevDaycare(areaId = testAreaId, id = testDaycare.id, name = testDaycare.name))
            h.insertTestDaycare(DevDaycare(areaId = testAreaId, id = testDaycare2.id, name = testDaycare2.name))
            insertTestVardaOrganizer(h)
        }
    }

    @Test
    fun `uploading municipal units works`() {
        jdbi.handle { h ->
            updateUnits(h)
            assertEquals(2, getVardaUnits(h).size)
        }
    }

    @Test
    fun `not uploading purchased units works`() {
        jdbi.handle { h ->
            updateUnits(h)
            assertEquals(2, getVardaUnits(h).size)

            h.insertTestDaycare(DevDaycare(areaId = testAreaId, id = testPurchasedDaycare.id, name = testPurchasedDaycare.name, providerType = ProviderType.PURCHASED))
            updateUnits(h)

            assertEquals(2, getVardaUnits(h).size)
        }
    }

    @Test
    fun `updating stale unit works`() {
        jdbi.handle { h ->
            updateUnits(h)
            val unitToStale = getVardaUnits(h)[0]
            val daycareId = unitToStale.evakaDaycareId
            val vardaId = unitToStale.vardaUnitId
            val uploadedAt = unitToStale.uploadedAt
            val createdAt = unitToStale.createdAt

            h.createUpdate("UPDATE daycare SET street_address = 'new address' WHERE id = :daycareId")
                .bind("daycareId", daycareId)
                .execute()

            updateUnits(h)

            val updatedUnit = getVardaUnits(h).toList().find { it.evakaDaycareId == daycareId }!!
            assertEquals(vardaId, updatedUnit.vardaUnitId)
            assertEquals(createdAt, updatedUnit.createdAt)
            assertNotEquals(uploadedAt, updatedUnit.uploadedAt)
        }
    }

    private fun updateUnits(h: Handle) {
        updateUnits(h, vardaClient, vardaOrganizerName)
    }
}

fun getVardaUnits(h: Handle): List<VardaUnitRow> {
    return h.createQuery("SELECT * FROM varda_unit")
        .mapTo<VardaUnitRow>()
        .toList()
}

data class VardaUnitRow(
    val evakaDaycareId: UUID,
    val vardaUnitId: Long,
    val uploadedAt: Instant,
    val createdAt: Instant
)
