// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security

import fi.espoo.evaka.daycare.addUnitFeatures
import fi.espoo.evaka.shared.AreaId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.security.actionrule.AccessControlFilter
import fi.espoo.evaka.shared.security.actionrule.HasGlobalRole
import fi.espoo.evaka.shared.security.actionrule.HasUnitRole
import fi.espoo.evaka.shared.security.actionrule.IsMobile
import fi.espoo.evaka.shared.security.actionrule.forTable
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UnitAccessControlTest : AccessControlTest() {
    private lateinit var areaId: AreaId
    private lateinit var daycareId: DaycareId
    private lateinit var featureDaycareId: DaycareId

    private val unitFeature = PilotFeature.MESSAGING
    private val clock =
        MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2022, 1, 1), LocalTime.of(12, 0)))

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            areaId = tx.insert(DevCareArea())
            daycareId = tx.insert(DevDaycare(areaId = areaId))
            featureDaycareId = tx.insert(DevDaycare(areaId = areaId))
            tx.addUnitFeatures(listOf(featureDaycareId), listOf(unitFeature))
        }
    }

    @Test
    fun `HasUnitRole inAnyUnit`() {
        val action = Action.Global.READ_UNITS
        rules.add(
            action,
            HasUnitRole(UserRole.UNIT_SUPERVISOR).withUnitFeatures(unitFeature).inAnyUnit()
        )
        val deniedSupervisor =
            createTestEmployee(
                globalRoles = emptySet(),
                unitRoles =
                    mapOf(daycareId to UserRole.UNIT_SUPERVISOR, featureDaycareId to UserRole.STAFF)
            )
        val permittedSupervisor =
            createTestEmployee(
                globalRoles = emptySet(),
                unitRoles =
                    mapOf(daycareId to UserRole.STAFF, featureDaycareId to UserRole.UNIT_SUPERVISOR)
            )
        db.read { tx ->
            assertFalse(accessControl.hasPermissionFor(tx, deniedSupervisor, clock, action))
            assertTrue(accessControl.hasPermissionFor(tx, permittedSupervisor, clock, action))
        }
    }

    @Test
    fun `HasUnitRole inUnit`() {
        val action = Action.Unit.READ
        rules.add(action, HasUnitRole(UserRole.UNIT_SUPERVISOR).inUnit())
        val unitSupervisor =
            createTestEmployee(
                globalRoles = emptySet(),
                unitRoles = mapOf(daycareId to UserRole.UNIT_SUPERVISOR)
            )
        val otherEmployee =
            createTestEmployee(
                globalRoles = emptySet(),
                unitRoles = mapOf(daycareId to UserRole.STAFF)
            )
        db.read { tx ->
            assertTrue(accessControl.hasPermissionFor(tx, unitSupervisor, clock, action, daycareId))
            assertFalse(accessControl.hasPermissionFor(tx, otherEmployee, clock, action, daycareId))
        }
    }

    @Test
    fun `HasUnitRole inUnit with unit features`() {
        val action = Action.Unit.READ
        rules.add(
            action,
            HasUnitRole(UserRole.UNIT_SUPERVISOR).withUnitFeatures(unitFeature).inUnit()
        )
        val unitSupervisor =
            createTestEmployee(
                globalRoles = emptySet(),
                unitRoles =
                    mapOf(
                        daycareId to UserRole.UNIT_SUPERVISOR,
                        featureDaycareId to UserRole.UNIT_SUPERVISOR
                    )
            )
        val otherEmployee =
            createTestEmployee(
                globalRoles = emptySet(),
                unitRoles = mapOf(daycareId to UserRole.STAFF, featureDaycareId to UserRole.STAFF)
            )
        db.read { tx ->
            assertFalse(
                accessControl.hasPermissionFor(tx, unitSupervisor, clock, action, daycareId)
            )
            assertTrue(
                accessControl.hasPermissionFor(tx, unitSupervisor, clock, action, featureDaycareId)
            )
            assertFalse(accessControl.hasPermissionFor(tx, otherEmployee, clock, action, daycareId))
            assertFalse(
                accessControl.hasPermissionFor(tx, otherEmployee, clock, action, featureDaycareId)
            )
        }
    }

    @Test
    fun `IsMobile inUnit`() {
        val action = Action.Unit.READ
        rules.add(action, IsMobile(requirePinLogin = false).inUnit())
        val unitMobile = createTestMobile(daycareId)
        val otherDaycareId = db.transaction { tx -> tx.insert(DevDaycare(areaId = areaId)) }
        val otherMobile = createTestMobile(otherDaycareId)
        db.read { tx ->
            assertTrue(accessControl.hasPermissionFor(tx, unitMobile, clock, action, daycareId))
            assertFalse(accessControl.hasPermissionFor(tx, otherMobile, clock, action, daycareId))
        }
    }

    @Test
    fun `unit-level action and getAuthorizationFilter`() {
        val action = Action.Unit.READ

        fun getFilter(user: AuthenticatedUser) = db.read { accessControl.getAuthorizationFilter(it, user, clock, action) }

        fun execute(filter: AccessControlFilter.Some<DaycareId>) =
            db.read {
                it
                    .createQuery {
                        sql(
                            """
                            SELECT id FROM daycare
                            WHERE ${predicate(filter.forTable("daycare"))}
                            """.trimIndent()
                        )
                    }.toSet<DaycareId>()
            }

        rules.add(action, HasGlobalRole(UserRole.SERVICE_WORKER))
        rules.add(action, HasUnitRole(UserRole.UNIT_SUPERVISOR).inUnit())
        val unitSupervisor =
            createTestEmployee(
                globalRoles = emptySet(),
                unitRoles =
                    mapOf(
                        daycareId to UserRole.UNIT_SUPERVISOR,
                        featureDaycareId to UserRole.UNIT_SUPERVISOR
                    )
            )
        val otherEmployee =
            createTestEmployee(
                globalRoles = emptySet(),
                unitRoles = mapOf(daycareId to UserRole.STAFF, featureDaycareId to UserRole.STAFF)
            )
        val serviceWorker = createTestEmployee(globalRoles = setOf(UserRole.SERVICE_WORKER))

        getFilter(unitSupervisor)

        assertEquals(
            setOf(daycareId, featureDaycareId),
            execute(getFilter(unitSupervisor) as AccessControlFilter.Some)
        )
        assertEquals(emptySet(), execute(getFilter(otherEmployee) as AccessControlFilter.Some))
        assertEquals(AccessControlFilter.PermitAll, getFilter(serviceWorker))
        assertNull(
            getFilter(
                AuthenticatedUser.Citizen(PersonId(UUID.randomUUID()), CitizenAuthLevel.STRONG)
            )
        )
    }
}
