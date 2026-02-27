// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.assistance

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevOtherAssistanceMeasure
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import fi.espoo.evaka.shared.security.actionrule.DefaultActionRuleMapping
import fi.espoo.evaka.shared.security.upsertEmployeeUser
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class OtherAssistanceMeasureAccessControlTest : PureJdbiTest(resetDbBeforeEach = true) {
    private val accessControl = AccessControl(DefaultActionRuleMapping(), noopTracer)

    private val clock = MockEvakaClock(HelsinkiDateTime.of(LocalDateTime.of(2022, 1, 1, 12, 0)))

    @Test
    fun `unit supervisor can read ended other assistance measures, staff cannot`() {
        val (daycareId, measureId) =
            db.transaction { tx ->
                val areaId = tx.insert(DevCareArea())
                val daycareId = tx.insert(DevDaycare(areaId = areaId))
                val childId = tx.insert(DevPerson(), DevPersonType.CHILD)
                tx.insert(
                    DevPlacement(
                        childId = childId,
                        unitId = daycareId,
                        startDate = LocalDate.of(2020, 1, 1),
                        endDate = LocalDate.of(2100, 1, 1),
                    )
                )
                val measureId =
                    tx.insert(
                        DevOtherAssistanceMeasure(
                            childId = childId,
                            validDuring =
                                FiniteDateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 6, 1)),
                            type = OtherAssistanceMeasureType.TRANSPORT_BENEFIT,
                        )
                    )
                daycareId to measureId
            }

        val supervisorEmployeeId = db.transaction { tx -> tx.insert(DevEmployee()) }
        db.transaction { tx ->
            tx.upsertEmployeeUser(supervisorEmployeeId)
            tx.insertDaycareAclRow(daycareId, supervisorEmployeeId, UserRole.UNIT_SUPERVISOR)
        }
        val supervisorUser =
            AuthenticatedUser.Employee(supervisorEmployeeId, setOf(UserRole.UNIT_SUPERVISOR))

        val staffEmployeeId = db.transaction { tx -> tx.insert(DevEmployee()) }
        db.transaction { tx ->
            tx.upsertEmployeeUser(staffEmployeeId)
            tx.insertDaycareAclRow(daycareId, staffEmployeeId, UserRole.STAFF)
        }
        val staffUser = AuthenticatedUser.Employee(staffEmployeeId, setOf(UserRole.STAFF))

        db.read { tx ->
            assertTrue(
                accessControl.hasPermissionFor(
                    tx,
                    supervisorUser,
                    clock,
                    Action.OtherAssistanceMeasure.READ,
                    measureId,
                )
            )
            assertFalse(
                accessControl.hasPermissionFor(
                    tx,
                    staffUser,
                    clock,
                    Action.OtherAssistanceMeasure.READ,
                    measureId,
                )
            )
        }
    }
}
