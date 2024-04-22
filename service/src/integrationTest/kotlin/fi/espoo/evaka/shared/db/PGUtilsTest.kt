// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.db

import fi.espoo.evaka.PureJdbiTest
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.postgresql.util.PSQLState

class PGUtilsTest : PureJdbiTest(resetDbBeforeEach = false) {
    @Test
    fun `psqlCause finds the underlying PSQLException from a long chain of wrappers`() {
        val e =
            assertThrows<Exception> {
                db.transaction { tx ->
                    try {
                        try {
                            try {
                                try {
                                    tx.execute {
                                        // language=
                                        sql("Not valid SQL")
                                    }
                                } catch (e: Throwable) {
                                    throw Exception("Wrap", e)
                                }
                            } catch (e: Throwable) {
                                throw RuntimeException("Wrapper", e)
                            }
                        } catch (e: Throwable) {
                            throw Exception("Wrappest", e)
                        }
                    } catch (e: Throwable) {
                        throw Exception("More is more", e)
                    }
                }
            }
        assertEquals("More is more", e.message)
        val psql = e.psqlCause()
        assertEquals(PSQLState.SYNTAX_ERROR.state, psql?.sqlState)
    }
}
