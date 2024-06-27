// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.db

import fi.espoo.evaka.PureJdbiTest
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.jdbi.v3.core.statement.UnableToExecuteStatementException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class DatabaseTest : PureJdbiTest(resetDbBeforeEach = true) {
    private class TestException : RuntimeException()

    @Test
    fun `transaction savepoint should be able to recover from a database constraint exception`() {
        db.transaction { tx ->
            tx.execute { sql("INSERT INTO holiday (date) VALUES ('2020-01-01')") }
            assertThrows<UnableToExecuteStatementException> {
                tx.subTransaction {
                    tx.execute { sql("INSERT INTO holiday (date) VALUES ('2020-01-01')") }
                }
            }
            tx.execute { sql("INSERT INTO holiday (date) VALUES ('2020-01-02')") }
        }
        assertEquals(
            listOf(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 2)),
            db.read {
                it.createQuery { sql("SELECT date FROM holiday ORDER BY date") }.toList<LocalDate>()
            }
        )
    }

    @Test
    fun `transaction savepoint should be able to recover from an application exception`() {
        db.transaction { tx ->
            tx.execute { sql("INSERT INTO holiday (date) VALUES ('2020-01-01')") }
            assertThrows<TestException> {
                tx.subTransaction<Unit> {
                    tx.execute { sql("INSERT INTO holiday (date) VALUES ('2020-01-02')") }
                    throw TestException()
                }
            }
        }
        assertEquals(
            listOf(LocalDate.of(2020, 1, 1)),
            db.read {
                it.createQuery { sql("SELECT date FROM holiday ORDER BY date") }.toList<LocalDate>()
            }
        )
    }

    @Test
    fun `afterCommit hook is called after a successful commit`() {
        var hookExecuted = false
        db.transaction { tx ->
            tx.afterCommit { hookExecuted = true }
            assertFalse(hookExecuted)
        }
        assertTrue(hookExecuted)
    }

    @Test
    fun `afterCommit hook is not called if the transaction is rolled back`() {
        var hookExecuted = false
        assertThrows<TestException> {
            db.transaction { tx ->
                tx.afterCommit { hookExecuted = true }
                throw TestException()
            }
        }
        assertFalse(hookExecuted)
    }

    @Test
    fun `exceptions thrown by afterCommit hooks are propagated`() {
        assertThrows<TestException> {
            db.transaction { tx -> tx.afterCommit { throw TestException() } }
        }
    }
}
