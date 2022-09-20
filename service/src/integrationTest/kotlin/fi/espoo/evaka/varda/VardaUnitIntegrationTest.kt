// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda

import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.insertTestCareArea
import fi.espoo.evaka.shared.dev.insertTestDaycare
import fi.espoo.evaka.shared.domain.RealEvakaClock
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
import fi.espoo.evaka.testPurchasedDaycare
import fi.espoo.evaka.varda.integration.MockVardaIntegrationEndpoint
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class VardaUnitIntegrationTest : VardaIntegrationTest(resetDbBeforeEach = true) {
    @Autowired lateinit var mockEndpoint: MockVardaIntegrationEndpoint

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insertTestCareArea(testArea)
            tx.insertTestDaycare(
                DevDaycare(areaId = testArea.id, id = testDaycare.id, name = testDaycare.name)
            )
            tx.insertTestDaycare(
                DevDaycare(areaId = testArea.id, id = testDaycare2.id, name = testDaycare2.name)
            )
        }

        mockEndpoint.cleanUp()
    }

    @Test
    fun `uploading municipal units works`() {
        updateUnits()
        assertEquals(2, getVardaUnits(db).size)
        assertEquals(2, mockEndpoint.units.size)
        assertEquals(
            vardaClient.sourceSystem,
            mockEndpoint.units.values.elementAt(0).lahdejarjestelma
        )
        assertEquals("[FI]", mockEndpoint.units.values.elementAt(0).asiointikieli_koodi.toString())
    }
    @Test
    fun `opening a closed unit`() {
        val ophMunicipalOrganizerIdUrl = "${vardaEnv.url}/v1/vakajarjestajat/${ophEnv.organizerId}/"
        val closingDate = LocalDate.now()
        db.transaction {
            it.createUpdate(
                    "UPDATE DAYCARE set closing_date = current_date WHERE id = :daycareId".trimIndent(
                    )
                )
                .bind("daycareId", testDaycare.id)
                .bind("closingDate", closingDate)
                .execute()
        }
        updateUnits()
        assertEquals(
            closingDate.toString(),
            mockEndpoint.units.values.first { it.nimi == testDaycare.name }.paattymis_pvm
        )

        db.transaction {
            it.createUpdate(
                    "UPDATE DAYCARE set closing_date = NULL WHERE id = :daycareId".trimIndent()
                )
                .bind("daycareId", testDaycare.id)
                .execute()
        }
        val unit =
            db.read {
                    getNewOrStaleUnits(
                        it,
                        ophEnv.municipalityCode,
                        ophMunicipalOrganizerIdUrl,
                        vardaClient.sourceSystem
                    )
                }
                .find { it.name == testDaycare.name }

        // Because of too tight serialization annotation the unit closing date removal (setting as
        // null)
        // was dropped out
        assert(
            jsonMapper
                .writeValueAsString(unit!!.toVardaUnitRequest())
                .contains(""""paattymis_pvm":null""")
        )

        updateUnits()
        assertEquals(
            null,
            mockEndpoint.units.values.first { it.nimi == testDaycare.name }.paattymis_pvm
        )
    }

    @Test
    fun `not uploading purchased units works`() {
        updateUnits()
        assertEquals(2, getVardaUnits(db).size)

        db.transaction {
            it.insertTestDaycare(
                DevDaycare(
                    areaId = testArea.id,
                    id = testPurchasedDaycare.id,
                    name = testPurchasedDaycare.name,
                    providerType = ProviderType.PURCHASED
                )
            )
        }
        updateUnits()

        assertEquals(2, getVardaUnits(db).size)
    }

    @Test
    fun `updating stale unit works`() {
        updateUnits()
        val unitToStale = getVardaUnits(db)[0]
        val daycareId = unitToStale.evakaDaycareId
        val vardaId = unitToStale.vardaUnitId
        val uploadedAt = unitToStale.uploadedAt
        val createdAt = unitToStale.createdAt

        db.transaction {
            it.createUpdate(
                    "UPDATE daycare SET street_address = 'new address' WHERE id = :daycareId"
                )
                .bind("daycareId", daycareId)
                .execute()
        }

        updateUnits()

        val updatedUnit = getVardaUnits(db).toList().find { it.evakaDaycareId == daycareId }!!
        assertEquals(vardaId, updatedUnit.vardaUnitId)
        assertEquals(createdAt, updatedUnit.createdAt)
        assertNotEquals(uploadedAt, updatedUnit.uploadedAt)
    }

    private fun updateUnits() {
        val ophMunicipalOrganizerIdUrl = "${vardaEnv.url}/v1/vakajarjestajat/${ophEnv.organizerId}/"
        updateUnits(
            db,
            RealEvakaClock(),
            vardaClient,
            ophEnv.municipalityCode,
            ophMunicipalOrganizerIdUrl
        )
    }
}

fun getVardaUnits(db: Database.Connection): List<VardaUnitRow> =
    db.read { it.createQuery("SELECT * FROM varda_unit").mapTo<VardaUnitRow>().toList() }

data class VardaUnitRow(
    val evakaDaycareId: DaycareId,
    val vardaUnitId: Long,
    val uploadedAt: Instant,
    val createdAt: Instant
)
