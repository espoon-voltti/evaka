// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.AreaId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.MockEvakaClock
import java.time.LocalDate
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class ServiceNeedReportTest : FullApplicationTest(resetDbBeforeEach = true) {

    @Autowired private lateinit var controller: ServiceNeedReport

    private val clock = MockEvakaClock(2025, 4, 22, 16, 55, 23)
    private val admin = DevEmployee(roles = setOf(UserRole.ADMIN))
    private val area1 = DevCareArea(name = "area 1", shortName = "1")
    private val unit11 =
        DevDaycare(
            areaId = area1.id,
            name = "area 1 / unit 11",
            providerType = ProviderType.MUNICIPAL,
        )
    private val area2 = DevCareArea(name = "area 2", shortName = "2")
    private val unit21 =
        DevDaycare(
            areaId = area2.id,
            name = "area 2 / unit 21",
            providerType = ProviderType.PRIVATE,
        )

    @BeforeEach
    fun setup() {
        db.transaction { tx ->
            tx.insert(admin)
            tx.insert(area1)
            tx.insert(unit11)
            tx.insert(area2)
            tx.insert(unit21)
        }
    }

    @Test
    fun `row filters`() {
        val today = clock.today()
        val unit11Rows =
            (0..8).map { age ->
                ServiceNeedReportRow(
                    careAreaName = area1.name,
                    unitName = unit11.name,
                    unitType = UnitType.DAYCARE,
                    unitProviderType = ProviderType.MUNICIPAL,
                    age = age,
                    fullDay = 0,
                    partDay = 0,
                    fullWeek = 0,
                    partWeek = 0,
                    shiftCare = 0,
                    missingServiceNeed = 0,
                    total = 0,
                )
            }
        val unit21Rows =
            (0..8).map { age ->
                ServiceNeedReportRow(
                    careAreaName = area2.name,
                    unitName = unit21.name,
                    unitType = UnitType.DAYCARE,
                    unitProviderType = ProviderType.PRIVATE,
                    age = age,
                    fullDay = 0,
                    partDay = 0,
                    fullWeek = 0,
                    partWeek = 0,
                    shiftCare = 0,
                    missingServiceNeed = 0,
                    total = 0,
                )
            }
        assertThat(getReport(today)).containsExactlyElementsOf(unit11Rows + unit21Rows)
        assertThat(getReport(today, areaId = area1.id)).containsExactlyElementsOf(unit11Rows)
        assertThat(getReport(today, areaId = area2.id)).containsExactlyElementsOf(unit21Rows)
        assertThat(getReport(today, areaId = AreaId(UUID.randomUUID()))).isEmpty()
        assertThat(getReport(today, providerType = ProviderType.MUNICIPAL))
            .containsExactlyElementsOf(unit11Rows)
        assertThat(getReport(today, providerType = ProviderType.PRIVATE))
            .containsExactlyElementsOf(unit21Rows)
        assertThat(getReport(today, providerType = ProviderType.PURCHASED)).isEmpty()
    }

    @Test
    fun `value filters`() {
        val today = clock.today()
        val unitSupervisor =
            db.transaction { tx ->
                DevEmployee()
                    .also {
                        tx.insert(it, unitRoles = mapOf(unit11.id to UserRole.UNIT_SUPERVISOR))
                    }
                    .user
            }
        val rowsByAge =
            (0..8).associateWith { age ->
                ServiceNeedReportRow(
                    careAreaName = area1.name,
                    unitName = unit11.name,
                    unitType = UnitType.DAYCARE,
                    unitProviderType = ProviderType.MUNICIPAL,
                    age = age,
                    fullDay = 0,
                    partDay = 0,
                    fullWeek = 0,
                    partWeek = 0,
                    shiftCare = 0,
                    missingServiceNeed = 0,
                    total = 0,
                )
            }
        db.transaction { tx ->
            tx.insert(
                    DevPerson(ssn = null, dateOfBirth = today.minusMonths(1)),
                    DevPersonType.CHILD,
                )
                .also { childId ->
                    tx.insert(
                        DevPlacement(
                            unitId = unit11.id,
                            childId = childId,
                            type = PlacementType.DAYCARE,
                            startDate = today,
                            endDate = today,
                        )
                    )
                }
            tx.insert(DevPerson(ssn = null, dateOfBirth = today.minusYears(1)), DevPersonType.CHILD)
                .also { childId ->
                    tx.insert(
                        DevPlacement(
                            unitId = unit11.id,
                            childId = childId,
                            type = PlacementType.PRESCHOOL,
                            startDate = today,
                            endDate = today,
                        )
                    )
                }
        }

        assertThat(getReport(today, user = unitSupervisor))
            .containsExactlyElementsOf(
                replaceMapValue(
                        rowsByAge,
                        0 to { it.copy(missingServiceNeed = 1, total = 1) },
                        1 to { it.copy(missingServiceNeed = 1, total = 1) },
                    )
                    .values
            )
        assertThat(getReport(today, user = unitSupervisor, placementType = PlacementType.DAYCARE))
            .containsExactlyElementsOf(
                replaceMapValue(rowsByAge, 0 to { it.copy(missingServiceNeed = 1, total = 1) })
                    .values
            )
        assertThat(getReport(today, user = unitSupervisor, placementType = PlacementType.PRESCHOOL))
            .containsExactlyElementsOf(
                replaceMapValue(rowsByAge, 1 to { it.copy(missingServiceNeed = 1, total = 1) })
                    .values
            )
    }

    private fun getReport(
        date: LocalDate,
        user: AuthenticatedUser.Employee = admin.user,
        areaId: AreaId? = null,
        providerType: ProviderType? = null,
        placementType: PlacementType? = null,
    ) =
        controller.getServiceNeedReport(
            dbInstance(),
            user,
            clock,
            date,
            areaId,
            providerType,
            placementType,
        )
}

private fun <K, V> replaceMapValue(
    map: Map<K, V>,
    vararg replacements: Pair<K, (oldValue: V) -> V>,
) = map + (replacements.map { (key, transform) -> key to transform(map.getValue(key)) })
