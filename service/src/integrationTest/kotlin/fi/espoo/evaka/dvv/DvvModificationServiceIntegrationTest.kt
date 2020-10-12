// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later
package fi.espoo.evaka.dvv

import deleteDvvModificationToken
import fi.espoo.evaka.FullApplicationTest
import getDvvModificationToken
import getNextDvvModificationToken
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import storeDvvModificationToken

class DvvModificationServiceIntegrationTest : FullApplicationTest() {

    @Test
    fun `get modification token for today`() {
        storeDvvModificationToken(jdbi, "100", "101", 0, 0)
        assertEquals("101", getNextDvvModificationToken(jdbi))
        val response = dvvModificationsService.getDvvModifications(listOf("nimenmuutos"))
        assertEquals(1, response.size)
        assertEquals("102", getNextDvvModificationToken(jdbi))
        val createdDvvModificationToken = getDvvModificationToken(jdbi, "101")!!
        assertEquals("101", createdDvvModificationToken.token)
        assertEquals("102", createdDvvModificationToken.nextToken)
        assertEquals(1, createdDvvModificationToken.ssnsSent)
        assertEquals(1, createdDvvModificationToken.modificationsReceived)
        deleteDvvModificationToken(jdbi, "100")
        deleteDvvModificationToken(jdbi, "101")
    }
}
