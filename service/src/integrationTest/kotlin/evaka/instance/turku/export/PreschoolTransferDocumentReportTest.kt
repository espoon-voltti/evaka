// SPDX-FileCopyrightText: 2026 City of Turku
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.turku.export

import evaka.core.PureJdbiTest
import evaka.core.assistance.PreschoolAssistanceLevel
import evaka.core.document.DocumentTemplateContent
import evaka.core.document.childdocument.DocumentContent
import evaka.core.document.childdocument.DocumentStatus
import evaka.core.placement.PlacementType
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevChildDocument
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevDocumentTemplate
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.DevPlacement
import evaka.core.shared.dev.DevPreschoolAssistance
import evaka.core.shared.dev.insert
import evaka.core.shared.dev.resetDatabase
import evaka.core.shared.dev.runSqlScripts
import evaka.core.shared.domain.DateRange
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.user.EvakaUser
import evaka.core.user.EvakaUserType
import java.time.LocalDate
import java.time.LocalTime
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PreschoolTransferDocumentReportTest : PureJdbiTest(resetDbBeforeEach = true) {
    private val date = LocalDate.of(2025, 11, 18)
    private val timestamp = HelsinkiDateTime.of(date, LocalTime.of(12, 0))

    private val area = DevCareArea()
    private val unit = DevDaycare(areaId = area.id, name = "Ankkalinnake")
    private val employee = DevEmployee()

    private val templateNameFi =
        "Tiedonsiirto esiopetuksesta perusopetukseen koulupaikkapäätöksen suunnittelun tueksi ja opetuksen järjestämiseksi."
    private val templateNameSv =
        "Överföring av uppgifter från förskoleundervisningen till den grundläggande utbildningen som stöd för beslut om skolplacering och för ordnandet av undervisningen."

    @BeforeAll
    fun createSqlFunction() {
        db.transaction { tx ->
            tx.resetDatabase()
            tx.runSqlScripts("turku/db/migration")
        }
    }

    @BeforeEach
    fun initTestData() {
        db.transaction { tx ->
            tx.insert(area)
            tx.insert(unit)
            tx.insert(employee)
        }
    }

    @Test
    fun `child with completed transfer document and preschool placement is included`() {
        db.transaction { tx ->
            val childId =
                tx.insert(
                    DevPerson(
                        ssn = "010119A9555",
                        firstName = "Aku",
                        lastName = "Ankka",
                        dateOfBirth = LocalDate.of(2019, 1, 1),
                    ),
                    DevPersonType.CHILD,
                )
            tx.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL,
                    childId = childId,
                    unitId = unit.id,
                    startDate = date,
                    endDate = date,
                    createdBy = employee.evakaUserId,
                    modifiedBy = employee.evakaUserId,
                )
            )
            val templateId =
                tx.insert(
                    DevDocumentTemplate(
                        name = templateNameFi,
                        validity = DateRange(LocalDate.of(2025, 8, 1), LocalDate.of(2026, 7, 31)),
                        content = DocumentTemplateContent(sections = emptyList()),
                    )
                )
            tx.insert(
                DevChildDocument(
                    status = DocumentStatus.COMPLETED,
                    createdBy = employee.evakaUserId,
                    childId = childId,
                    templateId = templateId,
                    content = DocumentContent(answers = emptyList()),
                    modifiedAt = timestamp,
                    modifiedBy = employee.evakaUserId,
                    contentLockedAt = timestamp,
                    contentLockedBy = null,
                )
            )
        }

        val rows =
            db.read { tx ->
                tx.createQuery {
                        sql("SELECT * FROM preschool_transfer_document_report(${bind(date)})")
                    }
                    .toList<PreschoolTransferDocumentReportRow>()
            }

        assertThat(rows)
            .containsExactly(
                PreschoolTransferDocumentReportRow(
                    henkilötunnus = "010119A9555",
                    nimi = "Ankka Aku",
                    yksikkö = "Ankkalinnake",
                    `tuen taso esiopetuksessa` = null,
                )
            )
    }

    @Test
    fun `child with completed transfer document and support level`() {
        db.transaction { tx ->
            val childId =
                tx.insert(
                    DevPerson(
                        ssn = "010119A9555",
                        firstName = "Aku",
                        lastName = "Ankka",
                        dateOfBirth = LocalDate.of(2019, 1, 1),
                    ),
                    DevPersonType.CHILD,
                )
            tx.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL,
                    childId = childId,
                    unitId = unit.id,
                    startDate = date,
                    endDate = date,
                    createdBy = employee.evakaUserId,
                    modifiedBy = employee.evakaUserId,
                )
            )
            val templateId =
                tx.insert(
                    DevDocumentTemplate(
                        name = templateNameFi,
                        validity = DateRange(LocalDate.of(2025, 8, 1), LocalDate.of(2026, 7, 31)),
                        content = DocumentTemplateContent(sections = emptyList()),
                    )
                )
            tx.insert(
                DevChildDocument(
                    status = DocumentStatus.COMPLETED,
                    createdBy = employee.evakaUserId,
                    childId = childId,
                    templateId = templateId,
                    content = DocumentContent(answers = emptyList()),
                    modifiedAt = timestamp,
                    modifiedBy = employee.evakaUserId,
                    contentLockedAt = timestamp,
                    contentLockedBy = null,
                )
            )
            tx.insert(
                DevPreschoolAssistance(
                    childId = childId,
                    validDuring = FiniteDateRange(date, date),
                    level = PreschoolAssistanceLevel.INTENSIFIED_SUPPORT,
                    modifiedBy =
                        EvakaUser(
                            employee.evakaUserId,
                            employee.firstName + " " + employee.lastName,
                            EvakaUserType.EMPLOYEE,
                        ),
                )
            )
        }

        val rows =
            db.read { tx ->
                tx.createQuery {
                        sql("SELECT * FROM preschool_transfer_document_report(${bind(date)})")
                    }
                    .toList<PreschoolTransferDocumentReportRow>()
            }

        assertThat(rows)
            .containsExactly(
                PreschoolTransferDocumentReportRow(
                    henkilötunnus = "010119A9555",
                    nimi = "Ankka Aku",
                    yksikkö = "Ankkalinnake",
                    `tuen taso esiopetuksessa` = "Tehostettu tuki",
                )
            )
    }

    @Test
    fun `child with multiple support levels`() {
        db.transaction { tx ->
            val childId =
                tx.insert(
                    DevPerson(
                        ssn = "010119A9555",
                        firstName = "Aku",
                        lastName = "Ankka",
                        dateOfBirth = LocalDate.of(2019, 1, 1),
                    ),
                    DevPersonType.CHILD,
                )
            tx.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL,
                    childId = childId,
                    unitId = unit.id,
                    startDate = date,
                    endDate = date,
                    createdBy = employee.evakaUserId,
                    modifiedBy = employee.evakaUserId,
                )
            )
            val templateId =
                tx.insert(
                    DevDocumentTemplate(
                        name = templateNameFi,
                        validity = DateRange(LocalDate.of(2025, 8, 1), LocalDate.of(2026, 7, 31)),
                        content = DocumentTemplateContent(sections = emptyList()),
                    )
                )
            tx.insert(
                DevChildDocument(
                    status = DocumentStatus.COMPLETED,
                    createdBy = employee.evakaUserId,
                    childId = childId,
                    templateId = templateId,
                    content = DocumentContent(answers = emptyList()),
                    modifiedAt = timestamp,
                    modifiedBy = employee.evakaUserId,
                    contentLockedAt = timestamp,
                    contentLockedBy = null,
                )
            )
            val modifiedBy =
                EvakaUser(
                    employee.evakaUserId,
                    employee.firstName + " " + employee.lastName,
                    EvakaUserType.EMPLOYEE,
                )
            tx.insert(
                DevPreschoolAssistance(
                    childId = childId,
                    validDuring = FiniteDateRange(date, date),
                    level = PreschoolAssistanceLevel.INTENSIFIED_SUPPORT,
                    modifiedBy = modifiedBy,
                )
            )
            tx.insert(
                DevPreschoolAssistance(
                    childId = childId,
                    validDuring = FiniteDateRange(date.plusDays(1), date.plusDays(1)),
                    level = PreschoolAssistanceLevel.SPECIAL_SUPPORT,
                    modifiedBy = modifiedBy,
                )
            )
        }

        val rows =
            db.read { tx ->
                tx.createQuery {
                        sql("SELECT * FROM preschool_transfer_document_report(${bind(date)})")
                    }
                    .toList<PreschoolTransferDocumentReportRow>()
            }

        assertThat(rows).hasSize(1)
        assertThat(rows[0].henkilötunnus).isEqualTo("010119A9555")
        assertThat(rows[0].`tuen taso esiopetuksessa`).contains("Tehostettu tuki")
        assertThat(rows[0].`tuen taso esiopetuksessa`).contains("Erityinen tuki")
    }

    @Test
    fun `child without completed document is not included`() {
        db.transaction { tx ->
            val childId =
                tx.insert(DevPerson(dateOfBirth = LocalDate.of(2019, 1, 1)), DevPersonType.CHILD)
            tx.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL,
                    childId = childId,
                    unitId = unit.id,
                    startDate = date,
                    endDate = date,
                    createdBy = employee.evakaUserId,
                    modifiedBy = employee.evakaUserId,
                )
            )
        }

        val rows =
            db.read { tx ->
                tx.createQuery {
                        sql("SELECT * FROM preschool_transfer_document_report(${bind(date)})")
                    }
                    .toList<PreschoolTransferDocumentReportRow>()
            }

        assertThat(rows).isEmpty()
    }

    @Test
    fun `child with non-preschool placement is not included`() {
        db.transaction { tx ->
            val childId =
                tx.insert(DevPerson(dateOfBirth = LocalDate.of(2019, 1, 1)), DevPersonType.CHILD)
            tx.insert(
                DevPlacement(
                    type = PlacementType.DAYCARE,
                    childId = childId,
                    unitId = unit.id,
                    startDate = date,
                    endDate = date,
                    createdBy = employee.evakaUserId,
                    modifiedBy = employee.evakaUserId,
                )
            )
            val templateId =
                tx.insert(
                    DevDocumentTemplate(
                        name = templateNameFi,
                        validity = DateRange(LocalDate.of(2025, 8, 1), LocalDate.of(2026, 7, 31)),
                        content = DocumentTemplateContent(sections = emptyList()),
                    )
                )
            tx.insert(
                DevChildDocument(
                    status = DocumentStatus.COMPLETED,
                    createdBy = employee.evakaUserId,
                    childId = childId,
                    templateId = templateId,
                    content = DocumentContent(answers = emptyList()),
                    modifiedAt = timestamp,
                    modifiedBy = employee.evakaUserId,
                    contentLockedAt = timestamp,
                    contentLockedBy = null,
                )
            )
        }

        val rows =
            db.read { tx ->
                tx.createQuery {
                        sql("SELECT * FROM preschool_transfer_document_report(${bind(date)})")
                    }
                    .toList<PreschoolTransferDocumentReportRow>()
            }

        assertThat(rows).isEmpty()
    }

    @Test
    fun `preparatory placement is included`() {
        db.transaction { tx ->
            val childId =
                tx.insert(
                    DevPerson(
                        ssn = "010119A9555",
                        firstName = "Aku",
                        lastName = "Ankka",
                        dateOfBirth = LocalDate.of(2019, 1, 1),
                    ),
                    DevPersonType.CHILD,
                )
            tx.insert(
                DevPlacement(
                    type = PlacementType.PREPARATORY,
                    childId = childId,
                    unitId = unit.id,
                    startDate = date,
                    endDate = date,
                    createdBy = employee.evakaUserId,
                    modifiedBy = employee.evakaUserId,
                )
            )
            val templateId =
                tx.insert(
                    DevDocumentTemplate(
                        name = templateNameFi,
                        validity = DateRange(LocalDate.of(2025, 8, 1), LocalDate.of(2026, 7, 31)),
                        content = DocumentTemplateContent(sections = emptyList()),
                    )
                )
            tx.insert(
                DevChildDocument(
                    status = DocumentStatus.COMPLETED,
                    createdBy = employee.evakaUserId,
                    childId = childId,
                    templateId = templateId,
                    content = DocumentContent(answers = emptyList()),
                    modifiedAt = timestamp,
                    modifiedBy = employee.evakaUserId,
                    contentLockedAt = timestamp,
                    contentLockedBy = null,
                )
            )
        }

        val rows =
            db.read { tx ->
                tx.createQuery {
                        sql("SELECT * FROM preschool_transfer_document_report(${bind(date)})")
                    }
                    .toList<PreschoolTransferDocumentReportRow>()
            }

        assertThat(rows)
            .containsExactly(
                PreschoolTransferDocumentReportRow(
                    henkilötunnus = "010119A9555",
                    nimi = "Ankka Aku",
                    yksikkö = "Ankkalinnake",
                    `tuen taso esiopetuksessa` = null,
                )
            )
    }

    @Test
    fun `Swedish template name is also matched`() {
        db.transaction { tx ->
            val childId =
                tx.insert(
                    DevPerson(
                        ssn = "010119A9555",
                        firstName = "Aku",
                        lastName = "Ankka",
                        dateOfBirth = LocalDate.of(2019, 1, 1),
                    ),
                    DevPersonType.CHILD,
                )
            tx.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL,
                    childId = childId,
                    unitId = unit.id,
                    startDate = date,
                    endDate = date,
                    createdBy = employee.evakaUserId,
                    modifiedBy = employee.evakaUserId,
                )
            )
            val templateId =
                tx.insert(
                    DevDocumentTemplate(
                        name = templateNameSv,
                        validity = DateRange(LocalDate.of(2025, 8, 1), LocalDate.of(2026, 7, 31)),
                        content = DocumentTemplateContent(sections = emptyList()),
                    )
                )
            tx.insert(
                DevChildDocument(
                    status = DocumentStatus.COMPLETED,
                    createdBy = employee.evakaUserId,
                    childId = childId,
                    templateId = templateId,
                    content = DocumentContent(answers = emptyList()),
                    modifiedAt = timestamp,
                    modifiedBy = employee.evakaUserId,
                    contentLockedAt = timestamp,
                    contentLockedBy = null,
                )
            )
        }

        val rows =
            db.read { tx ->
                tx.createQuery {
                        sql("SELECT * FROM preschool_transfer_document_report(${bind(date)})")
                    }
                    .toList<PreschoolTransferDocumentReportRow>()
            }

        assertThat(rows)
            .containsExactly(
                PreschoolTransferDocumentReportRow(
                    henkilötunnus = "010119A9555",
                    nimi = "Ankka Aku",
                    yksikkö = "Ankkalinnake",
                    `tuen taso esiopetuksessa` = null,
                )
            )
    }

    @Test
    fun `draft document is also included`() {
        db.transaction { tx ->
            val childId =
                tx.insert(
                    DevPerson(
                        ssn = "010119A9555",
                        firstName = "Aku",
                        lastName = "Ankka",
                        dateOfBirth = LocalDate.of(2019, 1, 1),
                    ),
                    DevPersonType.CHILD,
                )
            tx.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL,
                    childId = childId,
                    unitId = unit.id,
                    startDate = date,
                    endDate = date,
                    createdBy = employee.evakaUserId,
                    modifiedBy = employee.evakaUserId,
                )
            )
            val templateId =
                tx.insert(
                    DevDocumentTemplate(
                        name = templateNameFi,
                        validity = DateRange(LocalDate.of(2025, 8, 1), LocalDate.of(2026, 7, 31)),
                        content = DocumentTemplateContent(sections = emptyList()),
                    )
                )
            tx.insert(
                DevChildDocument(
                    status = DocumentStatus.DRAFT,
                    createdBy = employee.evakaUserId,
                    childId = childId,
                    templateId = templateId,
                    content = DocumentContent(answers = emptyList()),
                    modifiedAt = timestamp,
                    modifiedBy = employee.evakaUserId,
                    contentLockedAt = timestamp,
                    contentLockedBy = null,
                )
            )
        }

        val rows =
            db.read { tx ->
                tx.createQuery {
                        sql("SELECT * FROM preschool_transfer_document_report(${bind(date)})")
                    }
                    .toList<PreschoolTransferDocumentReportRow>()
            }

        assertThat(rows)
            .containsExactly(
                PreschoolTransferDocumentReportRow(
                    henkilötunnus = "010119A9555",
                    nimi = "Ankka Aku",
                    yksikkö = "Ankkalinnake",
                    `tuen taso esiopetuksessa` = null,
                )
            )
    }
}

private data class PreschoolTransferDocumentReportRow(
    val henkilötunnus: String?,
    val nimi: String,
    val yksikkö: String,
    val `tuen taso esiopetuksessa`: String?,
)
