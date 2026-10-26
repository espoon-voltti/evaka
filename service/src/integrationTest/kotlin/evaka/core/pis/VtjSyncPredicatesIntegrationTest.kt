// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.pis

import evaka.core.PureJdbiTest
import evaka.core.shared.dev.DevFridgePartnership
import evaka.core.shared.dev.DevParentship
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.user.updateLastStrongLogin
import java.time.LocalDate
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class VtjSyncPredicatesIntegrationTest : PureJdbiTest(resetDbBeforeEach = true) {
    private val today = LocalDate.of(2026, 3, 2)

    private val adult = DevPerson(ssn = "010180-999A")
    private val other = DevPerson(ssn = "020180-999Y")
    private val child = DevPerson(ssn = "010120A999T", dateOfBirth = LocalDate.of(2020, 1, 1))

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(adult, DevPersonType.RAW_ROW)
            tx.insert(other, DevPersonType.RAW_ROW)
            tx.insert(child, DevPersonType.CHILD)
        }
    }

    @Test
    fun `a person with no citizen user and no fridge rows is not a dependant-sync person`() {
        assertFalse(isDependantSyncPerson())
    }

    @Test
    fun `a citizen user is a dependant-sync person`() {
        db.transaction { it.updateLastStrongLogin(HelsinkiDateTime.atStartOfDay(today), adult.id) }

        assertTrue(isDependantSyncPerson())
    }

    @Test
    fun `the head of a current fridge child row is a dependant-sync person`() {
        insertParentship(today, today)

        assertTrue(isDependantSyncPerson())
    }

    @Test
    fun `the head of an ended fridge child row is not a dependant-sync person`() {
        insertParentship(today.minusYears(1), today.minusDays(1))

        assertFalse(isDependantSyncPerson())
    }

    @Test
    fun `the head of a fridge child row that starts in the future is not a dependant-sync person`() {
        insertParentship(today.plusDays(1), today.plusYears(1))

        assertFalse(isDependantSyncPerson())
    }

    @Test
    fun `the head of a conflict fridge child row is a dependant-sync person`() {
        insertParentship(today.minusYears(1), today.plusYears(1))
        db.transaction { it.execute { sql("UPDATE fridge_child SET conflict = true") } }

        assertTrue(isDependantSyncPerson())
    }

    @Test
    fun `a partner in a current partnership with no end date is a dependant-sync person`() {
        insertPartnership(today, null)

        assertTrue(isDependantSyncPerson())
    }

    @Test
    fun `a partner in an ended partnership is not a dependant-sync person`() {
        insertPartnership(today.minusYears(1), today.minusDays(1))

        assertFalse(isDependantSyncPerson())
    }

    @Test
    fun `a partner in a partnership that starts in the future is not a dependant-sync person`() {
        insertPartnership(today.plusDays(1), null)

        assertFalse(isDependantSyncPerson())
    }

    @Test
    fun `a partner in a conflict partnership is a dependant-sync person`() {
        insertPartnership(today.minusYears(1), null, conflict = true)

        assertTrue(isDependantSyncPerson())
    }

    private fun isDependantSyncPerson() = db.read { it.isDependantSyncPerson(adult.id, today) }

    private fun insertParentship(startDate: LocalDate, endDate: LocalDate) {
        db.transaction {
            it.insert(
                DevParentship(
                    childId = child.id,
                    headOfChildId = adult.id,
                    startDate = startDate,
                    endDate = endDate,
                )
            )
        }
    }

    private fun insertPartnership(
        startDate: LocalDate,
        endDate: LocalDate?,
        conflict: Boolean = false,
    ) {
        db.transaction {
            it.insert(
                DevFridgePartnership(
                    first = adult.id,
                    second = other.id,
                    startDate = startDate,
                    endDate = endDate,
                    conflict = conflict,
                )
            )
        }
    }
}
