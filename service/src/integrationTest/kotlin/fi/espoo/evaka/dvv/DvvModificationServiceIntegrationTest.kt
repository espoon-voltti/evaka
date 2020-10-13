// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later
package fi.espoo.evaka.dvv

import deleteDvvModificationToken
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.db.handle
import getDvvModificationToken
import getNextDvvModificationToken
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import storeDvvModificationToken

class DvvModificationServiceIntegrationTest : DvvModificationServiceIntegrationTestBase() {

    @BeforeEach
    private fun beforeEach() {
        jdbi.handle { h ->
            resetDatabase(h)
            storeDvvModificationToken(jdbi, "100", "101", 0, 0)
        }
    }

    @BeforeEach
    private fun afterEach() {
        jdbi.handle { h ->
            deleteDvvModificationToken(jdbi, "100")
        }
    }

    @Test
    fun `get modification token for today`() = jdbi.handle { h ->

        assertEquals("101", getNextDvvModificationToken(jdbi))
        val response = dvvModificationsService.getDvvModifications(listOf("nimenmuutos"))
        assertEquals(1, response.size)
        assertEquals("102", getNextDvvModificationToken(jdbi))
        val createdDvvModificationToken = getDvvModificationToken(jdbi, "101")!!
        assertEquals("101", createdDvvModificationToken.token)
        assertEquals("102", createdDvvModificationToken.nextToken)
        assertEquals(1, createdDvvModificationToken.ssnsSent)
        assertEquals(1, createdDvvModificationToken.modificationsReceived)

        deleteDvvModificationToken(jdbi, "101")
    }

    @Test
    fun `person date of death`() {
        dvvModificationsService.updatePersonsFromDvv(listOf("kuollut"))
    }
}
