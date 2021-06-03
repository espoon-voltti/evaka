// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.defaultMunicipalOrganizerOid
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.varda.integration.MockVardaIntegrationEndpoint
import org.jdbi.v3.core.kotlin.mapTo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class VardaOrganizerChildrenIntegrationTest : FullApplicationTest() {
    @Autowired
    lateinit var mockEndpoint: MockVardaIntegrationEndpoint

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.resetDatabase()
            tx.insertGeneralTestFixtures()
        }
        mockEndpoint.cleanUp()
    }

    @Test
    fun `getOrCreateVardaChildByOrganizer returns without api calls if child exists`() {
        val vardaChildId = 31415L
        val vardaPersonOid = "1.2.3.4.5"
        db.transaction {
            insertVardaOrganizerChild(it, testChild_1.id, vardaChildId, vardaPersonOid, defaultMunicipalOrganizerOid)
        }

        val result = getOrCreateVardaChildByOrganizer(db, vardaClient, testChild_1.id, defaultMunicipalOrganizerOid, "")
        assertEquals(vardaChildId, result)

        assertEquals(0, mockEndpoint.people.values.size)
        assertEquals(0, mockEndpoint.children.values.size)
    }

    @Test
    fun `getOrCreateVardaChildByOrganizer creates child when person exists`() {
        db.transaction {
            insertVardaOrganizerChild(it, testChild_1.id, 31415L, "3.1.4.1.5", defaultMunicipalOrganizerOid)
        }

        val result = getOrCreateVardaChildByOrganizer(db, vardaClient, testChild_1.id, "otherOrganizerOid", "")
        assertEquals(1L, result)

        assertEquals(0, mockEndpoint.people.values.size)
        assertEquals(1, mockEndpoint.children.values.size)
    }

    @Test
    fun `getOrCreateVardaChildByOrganizer creates child & person when neither exists`() {
        val result = getOrCreateVardaChildByOrganizer(db, vardaClient, testChild_1.id, "otherOrganizerOid", "")
        assertEquals(1L, result)

        db.transaction {
            val rows = it.createQuery("SELECT * FROM varda_organizer_child")
                .mapTo<VardaChildOrganizerRow>()
                .toList()

            assertEquals(1, rows.size)
            assertEquals(testChild_1.id, rows.first().evakaPersonId)
        }

        assertEquals(1, mockEndpoint.people.values.size)
        assertEquals(1, mockEndpoint.children.values.size)
    }
}
