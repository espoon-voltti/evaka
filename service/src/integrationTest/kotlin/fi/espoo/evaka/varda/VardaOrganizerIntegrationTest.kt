// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.pis.service.PersonService
import fi.espoo.evaka.resetDatabase
import org.jdbi.v3.core.kotlin.mapTo
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class VardaOrganizerIntegrationTest : FullApplicationTest() {
    @Autowired
    lateinit var personService: PersonService

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.resetDatabase()
            insertGeneralTestFixtures(tx.handle)
        }
    }

    @Test
    fun `uploading organizer first time works`() {
        val organizer = getVardaOrganizers().first()
        assertNull(organizer.uploadedAt)

        updateOrganizer()

        val updatedOrganizer = getVardaOrganizers().first()
        assertNotNull(updatedOrganizer.uploadedAt)
    }

    @Test
    fun `updating modified organizer works`() {
        updateOrganizer()

        val originalUploadTimestamp = getVardaOrganizers().first().uploadedAt
        assertNotNull(originalUploadTimestamp)

        db.transaction { it.execute("UPDATE varda_organizer SET email = 'test@test.test', updated_at = now()") }
        updateOrganizer()

        assertNotEquals(originalUploadTimestamp, getVardaOrganizers().first().uploadedAt)
    }

    private fun updateOrganizer() {
        updateOrganizer(db, vardaClient, vardaOrganizerName)
    }

    private fun getVardaOrganizers(): List<VardaOrganizer> = db.read {
        //language=SQL
        val sql =
            """
            SELECT id, varda_organizer_id, varda_organizer_oid, url, email, phone, iban, municipality_code, created_at, updated_at, uploaded_at
            FROM varda_organizer
            """.trimIndent()

        it.createQuery(sql).mapTo<VardaOrganizer>().list()
    }
}
