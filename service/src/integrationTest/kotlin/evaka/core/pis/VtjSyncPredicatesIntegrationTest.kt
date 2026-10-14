// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.pis

import evaka.core.PureJdbiTest
import evaka.core.application.ApplicationStatus
import evaka.core.application.ApplicationType
import evaka.core.application.persistence.daycare.Adult
import evaka.core.application.persistence.daycare.Apply
import evaka.core.application.persistence.daycare.Child
import evaka.core.application.persistence.daycare.DaycareFormV0
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevFridgePartnership
import evaka.core.shared.dev.DevParentship
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.DevPlacement
import evaka.core.shared.dev.insert
import evaka.core.shared.dev.insertTestApplication
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.user.updateLastStrongLogin
import java.time.LocalDate
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class VtjSyncPredicatesIntegrationTest : PureJdbiTest(resetDbBeforeEach = true) {
    private val today = LocalDate.of(2026, 3, 2)

    private val area = DevCareArea()
    private val daycare = DevDaycare(areaId = area.id)
    private val adult = DevPerson(ssn = "010180-999A")
    private val other = DevPerson(ssn = "020180-999Y")
    private val child = DevPerson(ssn = "010120A999T", dateOfBirth = LocalDate.of(2020, 1, 1))

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
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
    fun `the head of a conflict fridge child row is not a dependant-sync person`() {
        insertParentship(today.minusYears(1), today.plusYears(1))
        db.transaction { it.execute { sql("UPDATE fridge_child SET conflict = true") } }

        assertFalse(isDependantSyncPerson())
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
    fun `a partner in a conflict partnership is not a dependant-sync person`() {
        insertPartnership(today.minusYears(1), null, conflict = true)

        assertFalse(isDependantSyncPerson())
    }

    @Test
    fun `a minor with a placement that ends today is a guardian-sync child`() {
        insertPlacement(endDate = today)

        assertTrue(isGuardianSyncChild())
    }

    @Test
    fun `a minor with a placement that ended yesterday is not a guardian-sync child`() {
        insertPlacement(endDate = today.minusDays(1))

        assertFalse(isGuardianSyncChild())
    }

    @ParameterizedTest
    @EnumSource(
        value = ApplicationStatus::class,
        names = ["ACTIVE", "REJECTED", "CANCELLED"],
        mode = EnumSource.Mode.EXCLUDE,
    )
    fun `a minor with an application that is not in a final state is a guardian-sync child`(
        status: ApplicationStatus
    ) {
        insertApplication(status)

        assertTrue(isGuardianSyncChild())
    }

    @ParameterizedTest
    @EnumSource(
        value = ApplicationStatus::class,
        names = ["ACTIVE", "REJECTED", "CANCELLED"],
    )
    fun `a minor with an application in a final state is not a guardian-sync child`(
        status: ApplicationStatus
    ) {
        insertApplication(status)

        assertFalse(isGuardianSyncChild())
    }

    @Test
    fun `a person who turned 18 is not a guardian-sync child`() {
        insertPlacement(endDate = LocalDate.of(2040, 1, 1))

        assertTrue(db.read { it.isGuardianSyncChild(child.id, LocalDate.of(2037, 12, 31)) })
        assertFalse(db.read { it.isGuardianSyncChild(child.id, LocalDate.of(2038, 1, 1)) })
    }

    @Test
    fun `a minor with no SSN is not a guardian-sync child`() {
        val noSsn = DevPerson(ssn = null, dateOfBirth = LocalDate.of(2020, 1, 1))
        db.transaction { tx ->
            tx.insert(noSsn, DevPersonType.CHILD)
            tx.insert(
                DevPlacement(
                    childId = noSsn.id,
                    unitId = daycare.id,
                    startDate = today,
                    endDate = today,
                )
            )
        }

        assertFalse(db.read { it.isGuardianSyncChild(noSsn.id, today) })
    }

    private fun isDependantSyncPerson() = db.read { it.isDependantSyncPerson(adult.id, today) }

    private fun isGuardianSyncChild() = db.read { it.isGuardianSyncChild(child.id, today) }

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

    private fun insertPlacement(endDate: LocalDate) {
        db.transaction {
            it.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycare.id,
                    startDate = today.minusYears(1),
                    endDate = endDate,
                )
            )
        }
    }

    private fun insertApplication(status: ApplicationStatus) {
        db.transaction {
            it.insertTestApplication(
                type = ApplicationType.DAYCARE,
                status = status,
                guardianId = adult.id,
                childId = child.id,
                confidential = true,
                document =
                    DaycareFormV0(
                        type = ApplicationType.DAYCARE,
                        child = Child(dateOfBirth = null),
                        guardian = Adult(),
                        apply = Apply(preferredUnits = listOf(daycare.id)),
                    ),
            )
        }
    }
}
