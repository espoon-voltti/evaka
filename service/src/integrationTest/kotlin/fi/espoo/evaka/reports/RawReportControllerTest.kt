// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.DevServiceNeed
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertServiceNeedOption
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.snDaycareFullDay35
import fi.espoo.evaka.snDefaultDaycare
import java.time.LocalDate
import java.time.LocalTime
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class RawReportControllerTest : FullApplicationTest(resetDbBeforeEach = true) {

    @Autowired private lateinit var rawReportController: RawReportController

    @Test
    fun `service need works`() {
        val user =
            db.transaction { tx ->
                val admin = DevEmployee(roles = setOf(UserRole.ADMIN))
                tx.insert(admin)
                admin.user
            }
        val clock =
            MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2024, 10, 23), LocalTime.of(8, 47)))
        db.transaction { tx ->
            val areaId = tx.insert(DevCareArea())
            val unitId = tx.insert(DevDaycare(areaId = areaId))
            tx.insertServiceNeedOption(snDefaultDaycare)
            tx.insertServiceNeedOption(snDaycareFullDay35)

            tx.insert(DevPerson(firstName = "Eka"), DevPersonType.CHILD).also { childId ->
                val placementId =
                    tx.insert(
                        DevPlacement(
                            childId = childId,
                            unitId = unitId,
                            startDate = clock.today(),
                            endDate = clock.today(),
                        )
                    )
                tx.insert(
                    DevServiceNeed(
                        placementId = placementId,
                        optionId = snDaycareFullDay35.id,
                        startDate = clock.today(),
                        endDate = clock.today(),
                        confirmedBy = user.evakaUserId,
                    )
                )
            }

            tx.insert(DevPerson(firstName = "Toka"), DevPersonType.CHILD).also { childId ->
                tx.insert(
                    DevPlacement(
                        childId = childId,
                        unitId = unitId,
                        startDate = clock.today(),
                        endDate = clock.today(),
                    )
                )
            }
        }

        val rows =
            rawReportController.getRawReport(
                dbInstance(),
                user,
                clock,
                clock.today(),
                clock.today(),
            )

        assertThat(rows)
            .extracting({ it.firstName }, { it.hasServiceNeed }, { it.serviceNeed })
            .containsExactlyInAnyOrder(
                Tuple("Eka", true, snDaycareFullDay35.nameFi),
                Tuple("Toka", false, null),
            )
    }

    @Test
    fun `municipality of residence is shown correctly`() {
        val clock =
            MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2024, 10, 23), LocalTime.of(8, 47)))

        val user =
            db.transaction { tx ->
                val admin = DevEmployee(roles = setOf(UserRole.ADMIN))
                tx.insert(admin)
                admin.user
            }

        db.transaction { tx ->
            tx.insertServiceNeedOption(snDefaultDaycare)
            val areaId = tx.insert(DevCareArea())
            val unitId = tx.insert(DevDaycare(areaId = areaId))

            tx.insert(
                    DevPerson(
                        firstName = "Kotikunnallinen",
                        municipalityOfResidence = "Säkkijärvi",
                    ),
                    DevPersonType.CHILD,
                )
                .also { childId ->
                    tx.insert(
                        DevPlacement(
                            childId = childId,
                            unitId = unitId,
                            startDate = clock.today(),
                            endDate = clock.today(),
                        )
                    )
                }

            tx.insert(
                    DevPerson(firstName = "Kotikunnaton", municipalityOfResidence = ""),
                    DevPersonType.CHILD,
                )
                .also { childId ->
                    tx.insert(
                        DevPlacement(
                            childId = childId,
                            unitId = unitId,
                            startDate = clock.today(),
                            endDate = clock.today(),
                        )
                    )
                }
        }

        val rows =
            rawReportController.getRawReport(
                dbInstance(),
                user,
                clock,
                clock.today(),
                clock.today(),
            )

        assertThat(rows)
            .extracting({ it.firstName }, { it.municipalityOfResidence })
            .containsExactlyInAnyOrder(
                Tuple("Kotikunnallinen", "Säkkijärvi"),
                Tuple("Kotikunnaton", ""),
            )
    }

    @Test
    fun `aromi customer id is shown correctly`() {
        val clock =
            MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2024, 10, 23), LocalTime.of(8, 47)))

        val user =
            db.transaction { tx ->
                val admin = DevEmployee(roles = setOf(UserRole.ADMIN))
                tx.insert(admin)
                admin.user
            }
        val testArea = DevCareArea()
        val testUnit = DevDaycare(areaId = testArea.id)
        val aromiGroup =
            DevDaycareGroup(
                daycareId = testUnit.id,
                name = "Aromi group",
                aromiCustomerId = "DAYCARE_PK",
            )
        db.transaction { tx ->
            tx.insertServiceNeedOption(snDefaultDaycare)
            tx.insert(testArea)
            tx.insert(testUnit)

            val nonAromiGroupId =
                tx.insert(DevDaycareGroup(daycareId = testUnit.id, name = "Non-Aromi group"))

            tx.insert(aromiGroup)

            tx.insert(DevPerson(firstName = "Anselmi"), DevPersonType.CHILD).also { childId ->
                val aromiPlacementId =
                    tx.insert(
                        DevPlacement(
                            childId = childId,
                            unitId = testUnit.id,
                            startDate = clock.today().minusYears(1),
                            endDate = clock.today().plusYears(1),
                        )
                    )
                tx.insert(
                    DevDaycareGroupPlacement(
                        daycarePlacementId = aromiPlacementId,
                        daycareGroupId = aromiGroup.id,
                        startDate = clock.today().minusYears(1),
                        endDate = clock.today().plusYears(1),
                    )
                )
            }

            tx.insert(DevPerson(firstName = "Benselmi"), DevPersonType.CHILD).also { childId ->
                val nonAromiPlacementId =
                    tx.insert(
                        DevPlacement(
                            childId = childId,
                            unitId = testUnit.id,
                            startDate = clock.today().minusYears(1),
                            endDate = clock.today().plusYears(1),
                        )
                    )

                tx.insert(
                    DevDaycareGroupPlacement(
                        daycarePlacementId = nonAromiPlacementId,
                        daycareGroupId = nonAromiGroupId,
                        startDate = clock.today().minusYears(1),
                        endDate = clock.today().plusYears(1),
                    )
                )
            }
        }

        val rows =
            rawReportController.getRawReport(
                dbInstance(),
                user,
                clock,
                clock.today(),
                clock.today(),
            )

        assertThat(rows)
            .extracting({ it.firstName }, { it.aromiCustomerId })
            .containsExactlyInAnyOrder(
                Tuple("Anselmi", aromiGroup.aromiCustomerId),
                Tuple("Benselmi", null),
            )
    }
}
