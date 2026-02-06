// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.service

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GuardianQueriesIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    private val adult1 = DevPerson()
    private val adult2 = DevPerson()
    private val child1 = DevPerson()
    private val child2 = DevPerson()

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            listOf(adult1, adult2).forEach { tx.insert(it, DevPersonType.ADULT) }
            listOf(child1, child2).forEach { tx.insert(it, DevPersonType.CHILD) }
        }
    }

    @Test
    fun addAndGetGuardianRelationships() {
        db.transaction { insertGuardianTestFixtures(it) }
        db.read { tx ->
            val guardian1Children = tx.getGuardianChildIds(adult1.id)
            assertEquals(2, guardian1Children.size)
            assertTrue(guardian1Children.contains(child1.id))
            assertTrue(guardian1Children.contains(child2.id))

            val child2Guardians = tx.getChildGuardians(child2.id)
            assertEquals(2, child2Guardians.size)
            assertTrue(child2Guardians.contains(adult1.id))
            assertTrue(child2Guardians.contains(adult2.id))
        }
    }

    @Test
    fun deleteGuardianChildren() {
        db.transaction { tx ->
            insertGuardianTestFixtures(tx)
            tx.deleteGuardianChildRelationShips(adult1.id)
        }

        db.read { tx ->
            assertEquals(0, tx.getGuardianChildIds(adult1.id).size)
            assertEquals(1, tx.getGuardianChildIds(adult2.id).size)
        }
    }

    private fun insertGuardianTestFixtures(tx: Database.Transaction) {
        // adult1 is the guardian of child1 and child2
        tx.insertGuardian(adult1.id, child1.id)
        tx.insertGuardian(adult1.id, child2.id)

        tx.insertGuardian(adult2.id, child2.id)
    }
}
