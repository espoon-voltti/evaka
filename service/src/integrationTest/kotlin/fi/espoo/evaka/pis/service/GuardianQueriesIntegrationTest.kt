// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.service

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAdult_2
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GuardianQueriesIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            listOf(testAdult_1, testAdult_2).forEach { tx.insert(it, DevPersonType.ADULT) }
            listOf(testChild_1, testChild_2).forEach { tx.insert(it, DevPersonType.CHILD) }
        }
    }

    @Test
    fun addAndGetGuardianRelationships() {
        db.transaction { insertGuardianTestFixtures(it) }
        db.read { tx ->
            val guardian1Children = tx.getGuardianChildIds(testAdult_1.id)
            assertEquals(2, guardian1Children.size)
            assertTrue(guardian1Children.contains(testChild_1.id))
            assertTrue(guardian1Children.contains(testChild_2.id))

            val child2Guardians = tx.getChildGuardians(testChild_2.id)
            assertEquals(2, child2Guardians.size)
            assertTrue(child2Guardians.contains(testAdult_1.id))
            assertTrue(child2Guardians.contains(testAdult_2.id))
        }
    }

    @Test
    fun deleteGuardianChildren() {
        db.transaction { tx ->
            insertGuardianTestFixtures(tx)
            tx.deleteGuardianChildRelationShips(testAdult_1.id)
        }

        db.read { tx ->
            assertEquals(0, tx.getGuardianChildIds(testAdult_1.id).size)
            assertEquals(1, tx.getGuardianChildIds(testAdult_2.id).size)
        }
    }

    private fun insertGuardianTestFixtures(tx: Database.Transaction) {
        // 1 is the parent of child 1 and 2
        tx.insertGuardian(testAdult_1.id, testChild_1.id)
        tx.insertGuardian(testAdult_1.id, testChild_2.id)

        tx.insertGuardian(testAdult_2.id, testChild_2.id)
    }
}
