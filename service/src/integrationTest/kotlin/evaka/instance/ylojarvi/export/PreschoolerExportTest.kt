// SPDX-FileCopyrightText: 2023-2025 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.ylojarvi.export

import evaka.core.assistance.PreschoolAssistanceLevel
import evaka.core.placement.PlacementType
import evaka.core.shared.AreaId
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.DevPlacement
import evaka.core.shared.dev.DevPreschoolAssistance
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.FiniteDateRange
import evaka.instance.ylojarvi.AbstractYlojarviIntegrationTest
import java.time.LocalDate
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

private val areaId = AreaId(UUID.fromString("37ddb551-8913-44cd-94f4-17f9ee0fa8b9"))
private val date = LocalDate.of(2025, 11, 18)

private fun hasExtendedCompulsoryEducation(level: PreschoolAssistanceLevel) =
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

class PreschoolerExportTest : AbstractYlojarviIntegrationTest() {
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
                tx.createQuery { sql("SELECT * FROM preschooler_export(2019, ${bind(date)})") }
                    .toList<PreschoolerExportRow>()
            }

        assertThat(rows).hasSize(1)
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
                tx.createQuery { sql("SELECT * FROM preschooler_export(2019, ${bind(date)})") }
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
                tx.createQuery { sql("SELECT * FROM preschooler_export(2019, ${bind(date)})") }
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
                tx.createQuery { sql("SELECT * FROM preschooler_export(2019, ${bind(date)})") }
                    .toList<PreschoolerExportRow>()
            }

        val extendedCompulsoryEducation = hasExtendedCompulsoryEducation(level)
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
                tx.createQuery { sql("SELECT * FROM preschooler_export(2019, ${bind(date)})") }
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
    val `esioppilaan lähiosoite`: String,
    val `esioppilaan postinumero`: String,
    val `esioppilaan esiopetuspaikka`: String,
    val `esioppilaan esiopetusryhmä`: String?,
    val `esioppilaan voimassa oleva tuen taso`: String?,
    val `huoltajan 1 henkilötunnus`: String?,
    val `huoltajan 1 sukunimi`: String?,
    val `huoltajan 1 etunimet`: String?,
    val `huoltajan 1 lähiosoite`: String?,
    val `huoltajan 1 postinumero`: String?,
    val `huoltajan 1 sähköpostiosoite`: String?,
    val `huoltajan 2 henkilötunnus`: String?,
    val `huoltajan 2 sukunimi`: String?,
    val `huoltajan 2 etunimet`: String?,
    val `huoltajan 2 lähiosoite`: String?,
    val `huoltajan 2 postinumero`: String?,
    val `huoltajan 2 sähköpostiosoite`: String?,
)
