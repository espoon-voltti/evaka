// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.pis.service.PersonService
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.db.handle
import org.jdbi.v3.core.Handle
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
        jdbi.handle { h ->
            resetDatabase(h)
            insertGeneralTestFixtures(h)
        }
    }

    @Test
    fun `uploading organizer first time works`() {
        jdbi.handle { h ->
            val organizer = getVardaOrganizers(h).first()
            assertNull(organizer.uploadedAt)

            updateOrganizer(h)

            val updatedOrganizer = getVardaOrganizers(h).first()
            assertNotNull(updatedOrganizer.uploadedAt)
        }
    }

    @Test
    fun `updating modified organizer works`() {
        jdbi.handle { h ->
            updateOrganizer(h)

            val originalUploadTimestamp = getVardaOrganizers(h).first().uploadedAt
            assertNotNull(originalUploadTimestamp)

            h.execute("UPDATE varda_organizer SET email = 'test@test.test', updated_at = now()")
            updateOrganizer(h)

            assertNotEquals(originalUploadTimestamp, getVardaOrganizers(h).first().uploadedAt)
        }
    }

    private fun updateOrganizer(h: Handle) {
        updateOrganizer(h, vardaClient, vardaOrganizerName)
    }
}

private fun getVardaOrganizers(h: Handle): List<VardaOrganizer> {
    //language=SQL
    val sql =
        """
            SELECT id, varda_organizer_id, varda_organizer_oid, url, email, phone, iban, municipality_code, created_at, updated_at, uploaded_at
            FROM varda_organizer
        """.trimIndent()

    return h.createQuery(sql).mapTo<VardaOrganizer>().list()
}
