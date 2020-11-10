package fi.espoo.evaka.shared.db

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.resetDatabase
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.core.statement.UnableToExecuteStatementException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class DatabaseTest : PureJdbiTest() {
    private lateinit var db: Database

    @BeforeEach
    fun beforeEach() {
        db = Database(jdbi)
        jdbi.handle { resetDatabase(it) }
    }

    @Test
    fun `transaction savepoint should be able to recover from a database constraint exception`() {
        db.connect { db ->
            db.transaction { tx ->
                tx.execute("INSERT INTO holiday (date) VALUES ('2020-01-01')")
                assertThrows<UnableToExecuteStatementException> {
                    tx.subTransaction {
                        tx.execute("INSERT INTO holiday (date) VALUES ('2020-01-01')")
                    }
                }
                tx.execute("INSERT INTO holiday (date) VALUES ('2020-01-02')")
            }
            assertEquals(
                listOf(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 2)),
                db.read { it.createQuery("SELECT date FROM holiday ORDER BY date").mapTo<LocalDate>().toList() }
            )
        }
    }

    @Test
    fun `transaction savepoint should be able to recover from an application exception`() {
        db.connect { db ->
            db.transaction { tx ->
                tx.execute("INSERT INTO holiday (date) VALUES ('2020-01-01')")
                assertThrows<RuntimeException> {
                    tx.subTransaction<Unit> {
                        tx.execute("INSERT INTO holiday (date) VALUES ('2020-01-02')")
                        throw RuntimeException("Test")
                    }
                }
            }
            assertEquals(
                listOf(LocalDate.of(2020, 1, 1)),
                db.read { it.createQuery("SELECT date FROM holiday ORDER BY date").mapTo<LocalDate>().toList() }
            )
        }
    }
}
