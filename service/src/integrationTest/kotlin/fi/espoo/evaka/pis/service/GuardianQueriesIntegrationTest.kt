// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.service

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.db.handle
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAdult_2
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import org.jdbi.v3.core.Handle
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GuardianQueriesIntegrationTest : FullApplicationTest() {
    @BeforeEach
    private fun beforeEach() {
        jdbi.handle { h ->
            resetDatabase(h)
            insertGeneralTestFixtures(h)
        }
    }

    @Test
    fun addAndGetGuardianRelationships(): Unit = jdbi.handle { h ->
        insertGuardianTestFixtures(h)
        val guardian1Children = getGuardianChildren(h, testAdult_1.id)
        Assertions.assertEquals(2, guardian1Children.size)
        Assertions.assertTrue(guardian1Children.contains(testChild_1.id))
        Assertions.assertTrue(guardian1Children.contains(testChild_2.id))

        val child2Guardians = getChildGuardians(h, testChild_2.id)
        Assertions.assertEquals(2, child2Guardians.size)
        Assertions.assertTrue(child2Guardians.contains(testAdult_1.id))
        Assertions.assertTrue(child2Guardians.contains(testAdult_2.id))
    }

    @Test
    fun deleteGuardianChildren(): Unit = jdbi.handle { h ->
        insertGuardianTestFixtures(h)
        deleteGuardianChildRelationShips(h, testAdult_1.id)
        Assertions.assertEquals(0, getGuardianChildren(h, testAdult_1.id).size)
        Assertions.assertEquals(1, getGuardianChildren(h, testAdult_2.id).size)
    }

    private fun insertGuardianTestFixtures(h: Handle) {
        // 1 is the parent of child 1 and 2
        insertGuardian(h, testAdult_1.id, testChild_1.id)
        insertGuardian(h, testAdult_1.id, testChild_2.id)

        insertGuardian(h, testAdult_2.id, testChild_2.id)
    }
}
