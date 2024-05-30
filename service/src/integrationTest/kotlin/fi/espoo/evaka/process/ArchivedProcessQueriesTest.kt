// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.process

import fi.espoo.evaka.PureJdbiTest
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class ArchivedProcessQueriesTest : PureJdbiTest(resetDbBeforeEach = true) {
    @Test
    fun `inserting process`() {
        val definition1 = "123.456.789"
        val definition2 = "987.654.321"
        val year1 = 2022
        val year2 = 2023
        assertEquals(1, db.transaction { it.insertProcess(definition1, year1) }.number)
        assertEquals(2, db.transaction { it.insertProcess(definition1, year1) }.number)
        assertEquals(1, db.transaction { it.insertProcess(definition2, year1) }.number)
        assertEquals(1, db.transaction { it.insertProcess(definition1, year2) }.number)
        assertEquals(1, db.transaction { it.insertProcess(definition2, year2) }.number)
        assertEquals(3, db.transaction { it.insertProcess(definition1, year1) }.number)
    }
}
