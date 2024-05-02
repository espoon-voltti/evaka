// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda

import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
import fi.espoo.evaka.testPurchasedDaycare
import fi.espoo.evaka.varda.integration.MockVardaIntegrationEndpoint
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class VardaUnitIntegrationTest : VardaIntegrationTest(resetDbBeforeEach = true) {
    @Autowired lateinit var mockEndpoint: MockVardaIntegrationEndpoint
    lateinit var clock: MockEvakaClock

    @BeforeEach
    fun beforeEach() {
        clock = MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2020, 1, 1), LocalTime.of(8, 0, 0)))

        db.transaction { tx ->
            tx.insert(testArea)
            tx.insert(
                DevDaycare(areaId = testArea.id, id = testDaycare.id, name = testDaycare.name)
            )
            tx.insert(
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
        assertEquals("[FI]", mockEndpoint.units.values.elementAt(0).toimintakieli_koodi.toString())
    }

    @Test
    fun `opening a closed unit`() {
        val ophMunicipalOrganizerIdUrl = "${vardaEnv.url}/v1/vakajarjestajat/${ophEnv.organizerId}/"
        val closingDate = LocalDate.now()
        db.transaction {
            it.createUpdate {
                    sql(
                        """
                        ALTER TABLE daycare DISABLE TRIGGER set_timestamp;
                        UPDATE daycare SET closing_date = ${bind(closingDate)}, updated = ${bind(clock.now())}
                        WHERE id = ${bind(testDaycare.id)};
                        ALTER TABLE daycare ENABLE TRIGGER set_timestamp;
                        """
                    )
                }
                .execute()
        }
        clock.tick()

        updateUnits()
        assertEquals(
            closingDate.toString(),
            mockEndpoint.units.values.first { it.nimi == testDaycare.name }.paattymis_pvm
        )

        clock.tick()
        db.transaction {
            it.createUpdate {
                    sql(
                        """
                        ALTER TABLE daycare DISABLE TRIGGER set_timestamp;
                        UPDATE daycare SET closing_date = NULL, updated = ${bind(clock.now())}
                        WHERE id = ${bind(testDaycare.id)};
                        ALTER TABLE daycare ENABLE TRIGGER set_timestamp;
                        """
                    )
                }
                .execute()
        }
        val unit = db.read { getNewOrStaleUnits(it) }.find { it.name == testDaycare.name }

        // Because of too tight serialization annotation the unit closing date removal (setting as
        // null) was dropped out
        assert(
            jsonMapper
                .writeValueAsString(
                    unit!!.toVardaUnitRequest(
                        vakajarjestaja = ophMunicipalOrganizerIdUrl,
                        kuntakoodi = ophEnv.municipalityCode,
                        lahdejarjestelma = vardaClient.sourceSystem
                    )
                )
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
            it.insert(
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
        val lastSuccessAt = unitToStale.lastSuccessAt
        val createdAt = unitToStale.createdAt

        clock.tick()
        db.transaction {
            it.createUpdate {
                    sql(
                        """
                        ALTER TABLE daycare DISABLE TRIGGER set_timestamp;
                        UPDATE daycare SET street_address = 'new address', updated = ${bind(clock.now())}
                        WHERE id = ${bind(daycareId)};
                        ALTER TABLE daycare ENABLE TRIGGER set_timestamp;
                        """
                    )
                }
                .execute()
        }

        updateUnits()

        val updatedUnit = getVardaUnits(db).toList().find { it.evakaDaycareId == daycareId }!!
        assertEquals(vardaId, updatedUnit.vardaUnitId)
        assertEquals(createdAt, updatedUnit.createdAt)
        assertNotEquals(lastSuccessAt, updatedUnit.lastSuccessAt)
    }

    private fun updateUnits() {
        val ophMunicipalOrganizerIdUrl = "${vardaEnv.url}/v1/vakajarjestajat/${ophEnv.organizerId}/"
        updateUnits(
            db,
            clock,
            vardaClient,
            vardaClient.sourceSystem,
            ophEnv.municipalityCode,
            ophMunicipalOrganizerIdUrl
        )
    }
}

fun getVardaUnits(db: Database.Connection): List<VardaUnitRow> =
    db.read {
        @Suppress("DEPRECATION") it.createQuery("SELECT * FROM varda_unit").toList<VardaUnitRow>()
    }

data class VardaUnitRow(
    val evakaDaycareId: DaycareId,
    val vardaUnitId: Long,
    val lastSuccessAt: Instant,
    val createdAt: Instant
)
