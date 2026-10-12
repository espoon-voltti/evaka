// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.pis.service

import evaka.core.FullApplicationTest
import evaka.core.shared.ChildId
import evaka.core.shared.PersonId
import evaka.core.shared.db.Database
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.HelsinkiDateTime
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
    fun `replacing guardian children changes only the added and removed relations`() {
        db.transaction { insertGuardianTestFixtures(it) }
        val createdBefore = db.read { it.getGuardianCreated(adult1.id, child2.id) }

        val changes = db.transaction { it.replaceGuardianChildren(adult1.id, setOf(child2.id)) }

        assertEquals(
            GuardianRelationChanges(added = emptyList(), removed = listOf(child1.id)),
            changes,
        )
        db.read { tx ->
            assertEquals(listOf(child2.id), tx.getGuardianChildIds(adult1.id))
            assertEquals(createdBefore, tx.getGuardianCreated(adult1.id, child2.id))
            assertEquals(listOf(child2.id), tx.getGuardianChildIds(adult2.id))
        }
    }

    @Test
    fun `replacing child guardians changes only the added and removed relations`() {
        db.transaction { insertGuardianTestFixtures(it) }
        val createdBefore = db.read { it.getGuardianCreated(adult1.id, child1.id) }

        val changes = db.transaction {
            it.replaceChildGuardians(child1.id, setOf(adult1.id, adult2.id))
        }

        assertEquals(
            GuardianRelationChanges(added = listOf(adult2.id), removed = emptyList()),
            changes,
        )
        db.read { tx ->
            assertEquals(setOf(adult1.id, adult2.id), tx.getChildGuardians(child1.id).toSet())
            assertEquals(createdBefore, tx.getGuardianCreated(adult1.id, child1.id))
        }
    }

    @Test
    fun `replacing with an empty set removes all relations`() {
        db.transaction { insertGuardianTestFixtures(it) }

        val changes = db.transaction { it.replaceGuardianChildren(adult1.id, emptySet()) }

        assertEquals(setOf(child1.id, child2.id), changes.removed.toSet())
        db.read { tx ->
            assertEquals(0, tx.getGuardianChildIds(adult1.id).size)
            assertEquals(1, tx.getGuardianChildIds(adult2.id).size)
        }
    }

    private fun Database.Read.getGuardianCreated(guardianId: PersonId, childId: ChildId) =
        createQuery {
            sql(
                "SELECT created FROM guardian WHERE guardian_id = ${bind(guardianId)} AND child_id = ${bind(childId)}"
            )
        }
        .exactlyOne<HelsinkiDateTime>()

    private fun insertGuardianTestFixtures(tx: Database.Transaction) {
        // adult1 is the guardian of child1 and child2
        tx.insertGuardian(adult1.id, child1.id)
        tx.insertGuardian(adult1.id, child2.id)

        tx.insertGuardian(adult2.id, child2.id)
    }
}
