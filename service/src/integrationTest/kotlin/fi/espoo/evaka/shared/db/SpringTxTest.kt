// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.db

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.resetDatabase
import org.jdbi.v3.core.JdbiException
import org.jdbi.v3.core.kotlin.mapTo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionSynchronizationManager
import javax.sql.DataSource

class SpringTxTest : FullApplicationTest() {
    @Autowired
    private lateinit var txManager: PlatformTransactionManager

    @Autowired
    private lateinit var dataSource: DataSource

    @BeforeEach
    private fun beforeEach() {
        assertFalse(TransactionSynchronizationManager.isSynchronizationActive(), "unexpected transaction")
        jdbi.handle { resetDatabase(it) }
    }

    @Test
    fun testCommit() {
        withSpringTx(txManager) {
            assertNormalTx()
            insertDummyData()
        }
        assertNoTx()
        assertEquals(1, countDummyData())
    }

    @Test
    fun testRollbackOnDbConstraintViolation() {
        assertThrows<JdbiException> {
            withSpringTx(txManager) {
                assertNormalTx()
                insertDummyData()
                insertDummyData() // unique constraint violation
            }
        }
        assertNoTx()
        assertEquals(0, countDummyData())
    }

    @Test
    fun testRollbackOnRuntimeException() {
        assertThrows<RuntimeException> {
            withSpringTx(txManager) {
                assertNormalTx()
                insertDummyData()

                throw RuntimeException()
            }
        }
        assertNoTx()
        assertEquals(0, countDummyData())
    }

    @Test
    fun testRollbackOnException() {
        assertThrows<Exception> {
            withSpringTx(txManager) {
                assertNormalTx()
                insertDummyData()

                throw Exception()
            }
        }
        assertNoTx()
        assertEquals(0, countDummyData())
    }

    @Test
    fun testRollbackOnThrowable() {
        assertThrows<Throwable> {
            withSpringTx(txManager) {
                assertNormalTx()
                insertDummyData()

                throw Throwable()
            }
        }
        assertNoTx()
        assertEquals(0, countDummyData())
    }

    @Test
    fun testRollbackOnReadOnly() {
        assertThrows<JdbiException> {
            withSpringTx(txManager, readOnly = true) {
                assertReadOnlyTx()
                insertDummyData() // read only -> should fail
            }
        }
        assertNoTx()
        assertEquals(0, countDummyData())
    }

    private fun insertDummyData() = withSpringHandle(dataSource) {
        it.execute("INSERT INTO holiday (date) VALUES ('2019-01-01')")
    }

    private fun countDummyData() = withSpringHandle(dataSource) {
        it.createQuery("SELECT count(*) FROM holiday WHERE date = '2019-01-01'").mapTo<Int>().single()
    }
}

private fun assertNormalTx() {
    assertTrue(TransactionSynchronizationManager.isSynchronizationActive())
    assertTrue(TransactionSynchronizationManager.isActualTransactionActive())
    assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly())
}

private fun assertReadOnlyTx() {
    assertTrue(TransactionSynchronizationManager.isSynchronizationActive())
    assertTrue(TransactionSynchronizationManager.isActualTransactionActive())
    assertTrue(TransactionSynchronizationManager.isCurrentTransactionReadOnly())
}

private fun assertNoTx() {
    assertFalse(TransactionSynchronizationManager.isSynchronizationActive())
    assertFalse(TransactionSynchronizationManager.isActualTransactionActive())
}
