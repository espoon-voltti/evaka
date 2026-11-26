// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.family

import evaka.core.FullApplicationTest
import evaka.core.shared.FosterParentId
import evaka.core.shared.auth.UserRole
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevFosterParent
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.DateRange
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.shared.domain.NotFound
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class FosterParentControllerIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired lateinit var controller: FosterParentController

    private val clock =
        MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2024, 6, 1), LocalTime.of(12, 0)))
    private val admin = DevEmployee(roles = setOf(UserRole.ADMIN))
    private val parent = DevPerson()
    private val child = DevPerson(dateOfBirth = LocalDate.of(2020, 1, 1))
    private val relationship =
        DevFosterParent(
            childId = child.id,
            parentId = parent.id,
            validDuring = DateRange(LocalDate.of(2024, 1, 1), null),
            modifiedAt = clock.now(),
            modifiedBy = admin.evakaUserId,
        )

    @BeforeEach
    fun init() {
        db.transaction { tx ->
            tx.insert(admin)
            tx.insert(parent, DevPersonType.ADULT)
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(relationship)
        }
    }

    @Test
    fun `relationship validity can be updated`() {
        val newValidDuring = DateRange(LocalDate.of(2024, 2, 1), LocalDate.of(2024, 12, 31))

        controller.updateFosterParentRelationshipValidity(
            dbInstance(),
            admin.user,
            clock,
            relationship.id,
            newValidDuring,
        )

        assertEquals(
            listOf(newValidDuring),
            controller.getFosterChildren(dbInstance(), admin.user, clock, parent.id).map {
                it.validDuring
            },
        )
    }

    @Test
    fun `relationship can be deleted`() {
        controller.deleteFosterParentRelationship(dbInstance(), admin.user, clock, relationship.id)

        assertEquals(
            emptyList(),
            controller.getFosterChildren(dbInstance(), admin.user, clock, parent.id),
        )
    }

    @Test
    fun `updating or deleting an unknown relationship throws NotFound`() {
        val unknownId = FosterParentId(UUID.randomUUID())

        assertThrows<NotFound> {
            controller.updateFosterParentRelationshipValidity(
                dbInstance(),
                admin.user,
                clock,
                unknownId,
                DateRange(LocalDate.of(2024, 1, 1), null),
            )
        }
        assertThrows<NotFound> {
            controller.deleteFosterParentRelationship(dbInstance(), admin.user, clock, unknownId)
        }
    }
}
