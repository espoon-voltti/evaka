package fi.espoo.evaka.shared.db

import fi.espoo.evaka.PureJdbiTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.postgresql.util.PSQLState

class PGUtilsTest : PureJdbiTest() {
    @Test
    fun `psqlCause finds the underlying PSQLException from a long chain of wrappers`() {
        val e = assertThrows<Exception> {
            db.transaction { tx ->
                try {
                    try {
                        try {
                            try {
                                tx.execute(
                                    // language=
                                    "Not valid SQL"
                                )
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
