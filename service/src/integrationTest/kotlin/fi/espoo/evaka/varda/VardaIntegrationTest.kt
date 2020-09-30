// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.pis.service.PersonService
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.db.handle
import fi.espoo.evaka.shared.dev.insertTestVardaOrganizer
import org.jdbi.v3.core.kotlin.mapTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

// todo: make this varda unit test
class VardaIntegrationTest : FullApplicationTest() {
    // depends on how many units are created at insertGeneralTestFixtures()
    private val staleUnitAmount = 4

    @Autowired
    lateinit var personService: PersonService

    @BeforeEach
    fun beforeEach() {
        jdbi.handle { h ->
            insertGeneralTestFixtures(h)
            insertTestVardaOrganizer(h)
        }
        assertEquals(0, getChildIds().size)
        assertEquals(0, getVardaUnits().size)
    }

    @AfterEach
    fun afterEach() {
        jdbi.handle { h -> resetDatabase(h) }
    }

    @Test
    fun `querying units`() {
        val units = queryStaleUnits()
        assertEquals(staleUnitAmount, units.size)
    }

    @Test
    fun `upserting unit uploads`() {
        val units = queryStaleUnits()
        jdbi.useHandle<Exception> { h ->
            units.forEach {
                val id = (10000..999999).random().toLong()
                setUnitUploaded(it.copy(vardaUnitId = id, ophUnitOid = id.toString()), h)
            }
        }
        assertEquals(staleUnitAmount, getVardaUnits().size)
    }

    @Test
    fun `getting stale organizer`() {
        jdbi.useHandle<Exception> { h ->
            val organizer = getStaleOrganizer(h)
            assertNotEquals(organizer, null)
        }
    }

    @Test
    fun `upserting organizer upload`() {
        val organizer = getOrganizers().firstOrNull()
        check(organizer != null)
        check(organizer.uploadedAt == null)

        jdbi.handle { h ->
            setOrganizerUploaded(organizer, h)
        }

        assertEquals(1, getUploadedOrganizers().size)
    }

    @Test
    fun `updating all varda objects`() {
        assertEquals(0, getUploadedOrganizers().size)

        assertEquals(0, getVardaUnits().size)

        updateAll()

        assertEquals(staleUnitAmount, getVardaUnits().size)
        assertEquals(1, getUploadedOrganizers().size)
    }

    @Test
    fun `updating stale unit`() {
        updateAll()

        staleOneUnit()

        assertEquals(1, queryStaleUnits().size)
        updateAll()
        assertEquals(0, queryStaleUnits().size)
    }

    private fun updateAll() {
        updateAll(jdbi, vardaClient, true, objectMapper, personService)
    }

    private fun queryStaleUnits(): List<VardaUnit> {
        var units = emptyList<VardaUnit>()
        jdbi.useHandle<Exception> { h ->
            units = getNewOrStaleUnits(h)
        }
        return units
    }

    private fun getVardaUnits(): MutableList<MutableMap<String, Any>> {
        var uploads = mutableListOf<MutableMap<String, Any>>()
        jdbi.useHandle<Exception> { h ->
            uploads = h.createQuery("SELECT * FROM varda_unit").mapToMap().list()
        }
        return uploads
    }

    private fun staleOneUnit() {
        //language=SQL
        val sql =
            """
                UPDATE varda_unit 
                SET uploaded_at = '1900-01-01' 
                WHERE evaka_daycare_id IN (SELECT evaka_daycare_id FROM varda_unit WHERE evaka_daycare_id IS NOT NULL LIMIT 1)
            """.trimIndent()
        jdbi.useHandle<Exception> { h ->
            h.execute(sql)
        }
    }

    private fun getOrganizers(): List<VardaOrganizer> {
        //language=SQL
        val sql =
            """
            SELECT id, varda_organizer_id, varda_organizer_oid, url, email, phone, iban, municipality_code, created_at, updated_at, uploaded_at
            FROM varda_organizer
            """.trimIndent()

        return jdbi.handle { h -> h.createQuery(sql).mapTo<VardaOrganizer>().list() }.filterNotNull()
    }

    private fun getUploadedOrganizers() = getOrganizers().filter { it.uploadedAt != null }

    private fun getChildIds(): MutableList<MutableMap<String, Any>> =
        (
            jdbi.handle { h ->
                h.createQuery("SELECT varda_child_id FROM varda_child").mapToMap().list()
            } ?: emptyList()
            ).toMutableList()
}
