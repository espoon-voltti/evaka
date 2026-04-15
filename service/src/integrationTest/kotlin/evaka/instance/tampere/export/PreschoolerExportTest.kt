// SPDX-FileCopyrightText: 2023-2025 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.tampere.export

import evaka.core.assistance.PreschoolAssistanceLevel
import evaka.core.placement.PlacementType
import evaka.core.shared.AreaId
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevDaycareGroup
import evaka.core.shared.dev.DevDaycareGroupPlacement
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.DevPlacement
import evaka.core.shared.dev.DevPreschoolAssistance
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.FiniteDateRange
import evaka.instance.tampere.AbstractTampereIntegrationTest
import java.time.LocalDate
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

private val areaId = AreaId(UUID.fromString("6529e31e-9777-11eb-ba88-33a923255570"))
private val date = LocalDate.of(2025, 11, 18)

private fun extendedCompulsoryEducationFn(level: PreschoolAssistanceLevel) =
    when (level) {
        PreschoolAssistanceLevel.INTENSIFIED_SUPPORT -> false
        PreschoolAssistanceLevel.SPECIAL_SUPPORT -> false
        PreschoolAssistanceLevel.SPECIAL_SUPPORT_WITH_DECISION_LEVEL_1 -> true
        PreschoolAssistanceLevel.SPECIAL_SUPPORT_WITH_DECISION_LEVEL_2 -> true
        PreschoolAssistanceLevel.CHILD_SUPPORT -> false
        PreschoolAssistanceLevel.CHILD_SUPPORT_AND_EXTENDED_COMPULSORY_EDUCATION -> true
        PreschoolAssistanceLevel.CHILD_SUPPORT_AND_OLD_EXTENDED_COMPULSORY_EDUCATION -> true
        PreschoolAssistanceLevel.CHILD_SUPPORT_2_AND_OLD_EXTENDED_COMPULSORY_EDUCATION -> true
        PreschoolAssistanceLevel.GROUP_SUPPORT -> false
    }

class PreschoolerExportTest : AbstractTampereIntegrationTest() {
    @Test
    fun `mimimal data`() {
        db.transaction { tx ->
            val unitId = tx.insert(DevDaycare(areaId = areaId))
            val childId =
                tx.insert(DevPerson(dateOfBirth = LocalDate.of(2019, 1, 1)), DevPersonType.CHILD)
            tx.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL,
                    childId = childId,
                    unitId = unitId,
                    startDate = date,
                    endDate = date,
                )
            )
        }

        val rows =
            db.read { tx ->
                tx.createQuery {
                        sql("SELECT * FROM tampere_preschooler_export(2019, ${bind(date)})")
                    }
                    .toList<PreschoolerExportRow>()
            }

        assertThat(rows).hasSize(1)
    }

    @Test
    fun `full data`() {
        val dateOfBirth = LocalDate.of(2019, 1, 1)
        val expected =
            PreschoolerExportRow(
                "010119A9555",
                "Ankka",
                "Aku",
                "Ankkalinnake",
                "Sudenpennut",
                "Tehostettu tuki",
                "1.2.3.4",
            )
        db.transaction { tx ->
            val unitId =
                tx.insert(
                    DevDaycare(areaId = areaId, name = expected.`esioppilaan esiopetuspaikka`)
                )
            val groupId =
                tx.insert(
                    DevDaycareGroup(
                        daycareId = unitId,
                        name = expected.`esioppilaan esiopetusryhmä`!!,
                    )
                )
            val childId =
                tx.insert(
                    DevPerson(
                        firstName = expected.`esioppilaan etunimet`,
                        lastName = expected.`esioppilaan sukunimi`,
                        dateOfBirth = dateOfBirth,
                        ssn = expected.`esioppilaan henkilötunnus`,
                        ophPersonOid = expected.`esioppilaan oppijanumero`,
                    ),
                    DevPersonType.CHILD,
                )
            val placementId =
                tx.insert(
                    DevPlacement(
                        type = PlacementType.PRESCHOOL,
                        childId = childId,
                        unitId = unitId,
                        startDate = date,
                        endDate = date,
                    )
                )
            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placementId,
                    daycareGroupId = groupId,
                    startDate = date,
                    endDate = date,
                )
            )
            tx.insert(
                DevPreschoolAssistance(
                    childId = childId,
                    validDuring = FiniteDateRange(date, date),
                    level = PreschoolAssistanceLevel.INTENSIFIED_SUPPORT,
                )
            )
        }

        val rows =
            db.read { tx ->
                tx.createQuery {
                        sql("SELECT * FROM tampere_preschooler_export(2019, ${bind(date)})")
                    }
                    .toList<PreschoolerExportRow>()
            }

        assertThat(rows).containsExactly(expected)
    }

    @Test
    fun `wrong year of birth`() {
        db.transaction { tx ->
            val unitId = tx.insert(DevDaycare(areaId = areaId))
            val child1 =
                tx.insert(DevPerson(dateOfBirth = LocalDate.of(2017, 12, 31)), DevPersonType.CHILD)
            tx.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL,
                    childId = child1,
                    unitId = unitId,
                    startDate = date,
                    endDate = date,
                )
            )
            val child2 =
                tx.insert(DevPerson(dateOfBirth = LocalDate.of(2020, 1, 1)), DevPersonType.CHILD)
            tx.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL,
                    childId = child2,
                    unitId = unitId,
                    startDate = date,
                    endDate = date,
                )
            )
        }

        val rows =
            db.read { tx ->
                tx.createQuery {
                        sql("SELECT * FROM tampere_preschooler_export(2019, ${bind(date)})")
                    }
                    .toList<PreschoolerExportRow>()
            }

        assertThat(rows).isEmpty()
    }

    @Test
    fun `wrong placement type`() {
        db.transaction { tx ->
            val unitId = tx.insert(DevDaycare(areaId = areaId))
            val childId =
                tx.insert(DevPerson(dateOfBirth = LocalDate.of(2019, 1, 1)), DevPersonType.CHILD)
            tx.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL_DAYCARE_ONLY,
                    childId = childId,
                    unitId = unitId,
                    startDate = date,
                    endDate = date,
                )
            )
        }

        val rows =
            db.read { tx ->
                tx.createQuery {
                        sql("SELECT * FROM tampere_preschooler_export(2019, ${bind(date)})")
                    }
                    .toList<PreschoolerExportRow>()
            }

        assertThat(rows).isEmpty()
    }

    @ParameterizedTest
    @EnumSource(PreschoolAssistanceLevel::class)
    fun `6-year-old with extended compulsory education is not included`(
        level: PreschoolAssistanceLevel
    ) {
        db.transaction { tx ->
            val unitId = tx.insert(DevDaycare(areaId = areaId))
            val childId =
                tx.insert(DevPerson(dateOfBirth = LocalDate.of(2019, 1, 1)), DevPersonType.CHILD)
            tx.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL,
                    childId = childId,
                    unitId = unitId,
                    startDate = date,
                    endDate = date,
                )
            )
            tx.insert(
                DevPreschoolAssistance(
                    childId = childId,
                    validDuring = FiniteDateRange(date, date),
                    level = level,
                )
            )
        }

        val rows =
            db.read { tx ->
                tx.createQuery {
                        sql("SELECT * FROM tampere_preschooler_export(2019, ${bind(date)})")
                    }
                    .toList<PreschoolerExportRow>()
            }

        val extendedCompulsoryEducation = extendedCompulsoryEducationFn(level)
        if (extendedCompulsoryEducation) {
            assertThat(rows).isEmpty()
        } else {
            assertThat(rows)
                .extracting<String?> { it.`esioppilaan voimassa oleva tuen taso` }
                .hasSize(1)
                .first()
                .isNotNull
                .isNotEqualTo("")
                .isNotEqualTo(level.name)
        }
    }

    @ParameterizedTest
    @EnumSource(PreschoolAssistanceLevel::class)
    fun `7-year-old is always included`(level: PreschoolAssistanceLevel) {
        db.transaction { tx ->
            val unitId = tx.insert(DevDaycare(areaId = areaId))
            val childId =
                tx.insert(DevPerson(dateOfBirth = LocalDate.of(2018, 12, 31)), DevPersonType.CHILD)
            tx.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL,
                    childId = childId,
                    unitId = unitId,
                    startDate = date,
                    endDate = date,
                )
            )
            tx.insert(
                DevPreschoolAssistance(
                    childId = childId,
                    validDuring = FiniteDateRange(date, date),
                    level = level,
                )
            )
        }

        val rows =
            db.read { tx ->
                tx.createQuery {
                        sql("SELECT * FROM tampere_preschooler_export(2019, ${bind(date)})")
                    }
                    .toList<PreschoolerExportRow>()
            }

        assertThat(rows)
            .extracting<String?> { it.`esioppilaan voimassa oleva tuen taso` }
            .hasSize(1)
            .first()
            .isNotNull
            .isNotEqualTo("")
            .isNotEqualTo(level.name)
    }
}

private data class PreschoolerExportRow(
    val `esioppilaan henkilötunnus`: String?,
    val `esioppilaan sukunimi`: String,
    val `esioppilaan etunimet`: String,
    val `esioppilaan esiopetuspaikka`: String,
    val `esioppilaan esiopetusryhmä`: String?,
    val `esioppilaan voimassa oleva tuen taso`: String?,
    val `esioppilaan oppijanumero`: String?,
)
