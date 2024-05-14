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
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull
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
        assertEquals(2, getVardaUnitStates(db).size)
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
            it.execute {
                sql(
                    "UPDATE daycare SET closing_date = ${bind(closingDate)} WHERE id = ${bind(unitId1)}"
                )
            }
        }

        val client = TestClient()

        updateUnits(client)
        assertEquals(closingDate, client.findUnitByName("Päiväkoti 1").paattymis_pvm)

        db.transaction {
            it.execute {
                sql(
                    "UPDATE daycare SET closing_date = NULL, updated = ${bind(clock.now())} WHERE id = ${bind(unitId1)}"
                )
            }
        }
        val unit = db.read { it.getVardaUnits() }.find { it.evakaDaycareId == unitId1 }

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
        assertNull(client.findUnitByName("Päiväkoti 1").paattymis_pvm)
    }

    @Test
    fun `not uploading purchased units works`() {
        val client = TestClient()

        updateUnits(client)
        assertEquals(2, getVardaUnitStates(db).size)

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

        assertEquals(2, getVardaUnitStates(db).size)
    }

    @Test
    fun `updating stale unit works`() {
        val client = TestClient()

        updateUnits(client)
        val unitToStale = getVardaUnitStates(db)[0]
        val daycareId = unitToStale.evakaDaycareId
        val vardaId = unitToStale.vardaUnitId
        val createdAt = unitToStale.createdAt

        db.transaction {
            it.execute {
                sql(
                    "UPDATE daycare SET street_address = 'new address' WHERE id = ${bind(daycareId)}"
                )
            }
        }

        clock.tick()
        updateUnits(client)

        val updatedUnit = getVardaUnitStates(db).toList().find { it.evakaDaycareId == daycareId }!!
        assertEquals(vardaId, updatedUnit.vardaUnitId)
        assertEquals(createdAt, updatedUnit.createdAt)
        assertEquals(clock.now(), updatedUnit.lastSuccessAt)
    }

    @Test
    fun `unchanged unit is not updated`() {
        val client = TestClient()
        updateUnits(client)
        assertEquals(2, client.numCalls)
        updateUnits(client)
        assertEquals(2, client.numCalls)
    }

    @Test
    fun `unit with incompatible state is updated`() {
        val client = TestClient()
        updateUnits(client)
        assertEquals(2, client.numCalls)

        db.transaction {
            it.execute {
                sql(
                    """UPDATE varda_unit SET state = '{"foo": "bar"}'::jsonb WHERE evaka_daycare_id = ${bind(unitId1)}"""
                )
            }
        }

        updateUnits(client)
        assertEquals(3, client.numCalls)
    }

    @Test
    fun `unit update error is saved`() {
        val client =
            object : VardaUnitClient {
                override fun createUnit(unit: VardaUnitRequest) = error("unit creation failed")

                override fun updateUnit(id: Long, unit: VardaUnitRequest) =
                    error("unit update failed")
            }

        updateUnits(client)

        getVardaUnitStates(db).also { units ->
            assertEquals(2, units.size)
            units.forEach { unit ->
                assertEquals(clock.now(), unit.erroredAt)
                assertEquals("unit creation failed", unit.error)
            }
        }
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
    var numCalls = 0
    private var nextId = 1L

    override fun createUnit(unit: VardaUnitRequest): VardaUnitResponse {
        numCalls++
        val id = nextId++
        units[id] = unit
        return VardaUnitResponse(id, "foo.$id")
    }

    override fun updateUnit(id: Long, unit: VardaUnitRequest): VardaUnitResponse {
        numCalls++
        if (!units.containsKey(id)) throw IllegalStateException("Unit not found")
        units[id] = unit
        return VardaUnitResponse(id, "foo.$id")
    }

    fun findUnitByName(name: String) = units.values.find { it.nimi == name }!!
}

fun getVardaUnitStates(db: Database.Connection): List<VardaUnitRow> =
    db.read { it.createQuery { sql("SELECT * FROM varda_unit") }.toList<VardaUnitRow>() }

data class VardaUnitRow(
    val evakaDaycareId: DaycareId,
    val vardaUnitId: Long?,
    val lastSuccessAt: HelsinkiDateTime?,
    val createdAt: HelsinkiDateTime,
    val erroredAt: HelsinkiDateTime?,
    val error: String?
)
