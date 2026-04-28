// SPDX-FileCopyrightText: 2023-2026 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.kangasala.export

import evaka.core.assistance.PreschoolAssistanceLevel
import evaka.core.placement.PlacementType
import evaka.core.shared.AreaId
import evaka.core.shared.PersonId
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevGuardian
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.DevPlacement
import evaka.core.shared.dev.DevPreschoolAssistance
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.FiniteDateRange
import evaka.instance.kangasala.AbstractKangasalaIntegrationTest
import java.time.LocalDate
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

private val areaId = AreaId(UUID.fromString("c27fccb5-66cc-4d45-b146-e150128cd0e3"))
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

class PreschoolerExportTest : AbstractKangasalaIntegrationTest() {
    @Test
    fun `mimimal data`() {
        val child =
            DevPerson(
                dateOfBirth = LocalDate.of(2019, 1, 1),
                ssn = "010119A9142",
                lastName = "Korhonen",
                firstName = "Esko Ilmari",
                streetAddress = "Koivukuja 3",
                postalCode = "36200",
                postOffice = "KANGASALA",
            )
        val guardian1 =
            DevPerson(
                id = PersonId(UUID.fromString("00000000-0000-0000-0000-000000000001")),
                ssn = "010180-9026",
                lastName = "Korhonen",
                firstName = "Matti Tapani",
                streetAddress = "Rantatie 7",
                postalCode = "36220",
                postOffice = "KANGASALA",
                phone = "0401234567",
                email = "matti.korhonen@example.com",
            )
        val guardian2 =
            DevPerson(
                id = PersonId(UUID.fromString("00000000-0000-0000-0000-000000000002")),
                ssn = "150575-920V",
                lastName = "Virtanen",
                firstName = "Maija Liisa",
                streetAddress = "Tammelankatu 5",
                postalCode = "36240",
                postOffice = "KANGASALA",
                phone = "0409876543",
                email = "maija.virtanen@example.com",
            )
        db.transaction { tx ->
            val unitId = tx.insert(DevDaycare(areaId = areaId))
            val childId = tx.insert(child, DevPersonType.CHILD)
            tx.insert(guardian1, DevPersonType.ADULT)
            tx.insert(guardian2, DevPersonType.ADULT)
            tx.insert(DevGuardian(guardianId = guardian1.id, childId = childId))
            tx.insert(DevGuardian(guardianId = guardian2.id, childId = childId))
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

        val rows = db.read { tx ->
            tx.createQuery {
                    sql("SELECT * FROM kangasala_preschooler_export(2019, ${bind(date)})")
                }
                .toList<PreschoolerExportRow>()
        }

        assertThat(rows).hasSize(1)
        val row = rows.first()
        assertThat(row.`esioppilaan henkilötunnus`).isEqualTo(child.ssn)
        assertThat(row.`esioppilaan sukunimi`).isEqualTo(child.lastName)
        assertThat(row.`esioppilaan etunimet`).isEqualTo(child.firstName)
        assertThat(row.`esioppilaan lähiosoite`).isEqualTo(child.streetAddress)
        assertThat(row.`esioppilaan postinumero`)
            .isEqualTo("${child.postalCode} ${child.postOffice}")
        assertThat(row.`huoltajan 1 henkilötunnus`).isEqualTo(guardian1.ssn)
        assertThat(row.`huoltajan 1 sukunimi`).isEqualTo(guardian1.lastName)
        assertThat(row.`huoltajan 1 etunimet`).isEqualTo(guardian1.firstName)
        assertThat(row.`huoltajan 1 lähiosoite`).isEqualTo(guardian1.streetAddress)
        assertThat(row.`huoltajan 1 postinumero`)
            .isEqualTo("${guardian1.postalCode} ${guardian1.postOffice}")
        assertThat(row.`huoltajan 1 puhelinnumero`).isEqualTo(guardian1.phone)
        assertThat(row.`huoltajan 1 sähköpostiosoite`).isEqualTo(guardian1.email)
        assertThat(row.`huoltajan 2 henkilötunnus`).isEqualTo(guardian2.ssn)
        assertThat(row.`huoltajan 2 sukunimi`).isEqualTo(guardian2.lastName)
        assertThat(row.`huoltajan 2 etunimet`).isEqualTo(guardian2.firstName)
        assertThat(row.`huoltajan 2 lähiosoite`).isEqualTo(guardian2.streetAddress)
        assertThat(row.`huoltajan 2 postinumero`)
            .isEqualTo("${guardian2.postalCode} ${guardian2.postOffice}")
        assertThat(row.`huoltajan 2 puhelinnumero`).isEqualTo(guardian2.phone)
        assertThat(row.`huoltajan 2 sähköpostiosoite`).isEqualTo(guardian2.email)
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

        val rows = db.read { tx ->
            tx.createQuery {
                    sql("SELECT * FROM kangasala_preschooler_export(2019, ${bind(date)})")
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

        val rows = db.read { tx ->
            tx.createQuery {
                    sql("SELECT * FROM kangasala_preschooler_export(2019, ${bind(date)})")
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

        val rows = db.read { tx ->
            tx.createQuery {
                    sql("SELECT * FROM kangasala_preschooler_export(2019, ${bind(date)})")
                }
                .toList<PreschoolerExportRow>()
        }

        val extendedCompulsoryEducation = hasExtendedCompulsoryEducation(level)
        if (extendedCompulsoryEducation) {
            assertThat(rows).isEmpty()
        } else {
            assertThat(rows).hasSize(1)
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

        val rows = db.read { tx ->
            tx.createQuery {
                    sql("SELECT * FROM kangasala_preschooler_export(2019, ${bind(date)})")
                }
                .toList<PreschoolerExportRow>()
        }

        assertThat(rows).hasSize(1)
    }
}

private data class PreschoolerExportRow(
    val `esioppilaan henkilötunnus`: String?,
    val `esioppilaan sukunimi`: String,
    val `esioppilaan etunimet`: String,
    val `esioppilaan lähiosoite`: String,
    val `esioppilaan postinumero`: String,
    val `huoltajan 1 henkilötunnus`: String?,
    val `huoltajan 1 sukunimi`: String?,
    val `huoltajan 1 etunimet`: String?,
    val `huoltajan 1 lähiosoite`: String?,
    val `huoltajan 1 postinumero`: String?,
    val `huoltajan 1 puhelinnumero`: String?,
    val `huoltajan 1 sähköpostiosoite`: String?,
    val `huoltajan 2 henkilötunnus`: String?,
    val `huoltajan 2 sukunimi`: String?,
    val `huoltajan 2 etunimet`: String?,
    val `huoltajan 2 lähiosoite`: String?,
    val `huoltajan 2 postinumero`: String?,
    val `huoltajan 2 puhelinnumero`: String?,
    val `huoltajan 2 sähköpostiosoite`: String?,
)
