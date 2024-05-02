// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda

import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class VardaUnitIntegrationTest : VardaIntegrationTest(resetDbBeforeEach = true) {
    lateinit var clock: MockEvakaClock

    val area = DevCareArea()
    val unitId1 = DaycareId(UUID.randomUUID())
    val unitId2 = DaycareId(UUID.randomUUID())

    @BeforeEach
    fun beforeEach() {
        clock = MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2020, 1, 1), LocalTime.of(8, 0, 0)))

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(DevDaycare(areaId = area.id, id = unitId1, name = "Päiväkoti 1"))
            tx.insert(DevDaycare(areaId = area.id, id = unitId2, name = "Päiväkoti 2"))
        }
    }

    @Test
    fun `uploading municipal units works`() {
        val client = TestClient()

        updateUnits(client)
        assertEquals(2, getVardaUnits(db).size)
        assertEquals(2, client.units.size)

        val uploadedUnit = client.findUnitByName("Päiväkoti 1")
        assertEquals(vardaClient.sourceSystem, uploadedUnit.lahdejarjestelma)
        assertEquals(listOf("FI"), uploadedUnit.toimintakieli_koodi)
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
                        WHERE id = ${bind(unitId1)};
                        ALTER TABLE daycare ENABLE TRIGGER set_timestamp;
                        """
                    )
                }
                .execute()
        }
        clock.tick()

        val client = TestClient()

        updateUnits(client)
        assertEquals(closingDate.toString(), client.findUnitByName("Päiväkoti 1").paattymis_pvm)

        clock.tick()
        db.transaction {
            it.createUpdate {
                    sql(
                        """
                        ALTER TABLE daycare DISABLE TRIGGER set_timestamp;
                        UPDATE daycare SET closing_date = NULL, updated = ${bind(clock.now())}
                        WHERE id = ${bind(unitId1)};
                        ALTER TABLE daycare ENABLE TRIGGER set_timestamp;
                        """
                    )
                }
                .execute()
        }
        val unit = db.read { getNewOrStaleUnits(it) }.find { it.evakaDaycareId == unitId1 }

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

        updateUnits(client)
        assertEquals(null, client.findUnitByName("Päiväkoti 1").paattymis_pvm)
    }

    @Test
    fun `not uploading purchased units works`() {
        val client = TestClient()

        updateUnits(client)
        assertEquals(2, getVardaUnits(db).size)

        db.transaction {
            it.insert(
                DevDaycare(
                    areaId = area.id,
                    name = "Ostettu päiväkoti",
                    providerType = ProviderType.PURCHASED
                )
            )
        }

        updateUnits(client)

        assertEquals(2, getVardaUnits(db).size)
    }

    @Test
    fun `updating stale unit works`() {
        val client = TestClient()

        updateUnits(client)
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
                        UPDATE daycare SET street_address = 'new address', updated =
    ${bind(clock.now())}
                        WHERE id = ${bind(daycareId)};
                        ALTER TABLE daycare ENABLE TRIGGER set_timestamp;
                        """
                    )
                }
                .execute()
        }

        updateUnits(client)

        val updatedUnit = getVardaUnits(db).toList().find { it.evakaDaycareId == daycareId }!!
        assertEquals(vardaId, updatedUnit.vardaUnitId)
        assertEquals(createdAt, updatedUnit.createdAt)
        assertNotEquals(lastSuccessAt, updatedUnit.lastSuccessAt)
    }

    private fun updateUnits(client: VardaUnitClient) {
        val ophMunicipalOrganizerIdUrl = "${vardaEnv.url}/v1/vakajarjestajat/${ophEnv.organizerId}/"
        updateUnits(
            db,
            clock,
            client,
            vardaClient.sourceSystem,
            ophEnv.municipalityCode,
            ophMunicipalOrganizerIdUrl
        )
    }
}

class TestClient : VardaUnitClient {
    val units = mutableMapOf<Long, VardaUnitRequest>()
    private var nextId = 1L

    override fun createUnit(unit: VardaUnitRequest): VardaUnitResponse {
        val id = nextId++
        units[id] = unit
        return VardaUnitResponse(id, "foo.$id")
    }

    override fun updateUnit(id: Long, unit: VardaUnitRequest): VardaUnitResponse {
        if (!units.containsKey(id)) throw IllegalStateException("Unit not found")
        units[id] = unit
        return VardaUnitResponse(id, "foo.$id")
    }

    fun findUnitByName(name: String) = units.values.find { it.nimi == name }!!
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
