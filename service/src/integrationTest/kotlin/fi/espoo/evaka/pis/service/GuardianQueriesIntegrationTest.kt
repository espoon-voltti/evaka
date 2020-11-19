// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.service

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAdult_2
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GuardianQueriesIntegrationTest : FullApplicationTest() {
    @BeforeEach
    protected fun beforeEach() {
        db.transaction { it.resetDatabase() }
        db.transaction { insertGeneralTestFixtures(it.handle) }
        db.transaction { it.insertGuardianTestFixtures() }
    }

    @Test
    fun addAndGetGuardianRelationships() {
        db.read { tx ->
            val guardian1Children = tx.getGuardianChildren(testAdult_1.id)
            Assertions.assertEquals(2, guardian1Children.size)
            Assertions.assertTrue(guardian1Children.contains(testChild_1.id))
            Assertions.assertTrue(guardian1Children.contains(testChild_2.id))

            val child2Guardians = tx.getChildGuardians(testChild_2.id)
            Assertions.assertEquals(2, child2Guardians.size)
            Assertions.assertTrue(child2Guardians.contains(testAdult_1.id))
            Assertions.assertTrue(child2Guardians.contains(testAdult_2.id))
        }
    }

    @Test
    fun deleteGuardianChildren() {
        db.transaction { it.deleteGuardianChildRelationShips(testAdult_1.id) }
        db.read { tx ->
            Assertions.assertEquals(0, tx.getGuardianChildren(testAdult_1.id).size)
            Assertions.assertEquals(1, tx.getGuardianChildren(testAdult_2.id).size)
        }
    }

    private fun Database.Transaction.insertGuardianTestFixtures() {
        // 1 is the parent of child 1 and 2
        insertGuardian(testAdult_1.id, testChild_1.id)
        insertGuardian(testAdult_1.id, testChild_2.id)

        insertGuardian(testAdult_2.id, testChild_2.id)
    }
}
