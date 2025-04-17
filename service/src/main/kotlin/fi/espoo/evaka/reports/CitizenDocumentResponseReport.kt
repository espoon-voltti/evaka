// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.daycare.getDaycare
import fi.espoo.evaka.daycare.getDaycareGroups
import fi.espoo.evaka.document.childdocument.DocumentContent
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.DocumentTemplateId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
class CitizenDocumentResponseReport(private val accessControl: AccessControl) {

    @GetMapping("/employee/reports/citizen-document-response-report")
    fun getCitizenDocumentResponseReport(
        db: Database,
        clock: EvakaClock,
        user: AuthenticatedUser.Employee,
        @RequestParam unitId: DaycareId,
        @RequestParam groupId: GroupId?,
        @RequestParam documentTemplateId: DocumentTemplateId,
    ): List<CitizenDocumentResponseReportRow> {
        return db.connect { dbc ->
            dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Unit.READ_CITIZEN_DOCUMENT_RESPONSE_REPORT,
                        unitId,
                    )
                    val examDate = clock.now().toLocalDate()
                    val daycare = tx.getDaycare(unitId) ?: throw BadRequest("No such unit")
                    val isUnitActive =
                        daycare.openingDate != null &&
                            daycare.openingDate.isBefore(examDate) &&
                            (daycare.closingDate == null || daycare.closingDate.isAfter(examDate))
                    if (!isUnitActive) {
                        throw BadRequest("Unit not active")
                    }
                    val groupIds =
                        if (groupId != null) listOf(groupId)
                        else tx.getDaycareGroups(daycare.id, examDate, examDate).map { it.id }
                    tx.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)
                    getCitizenDocumentResponseRows(
                        documentTemplateId = documentTemplateId,
                        groupIds = groupIds,
                        examDate = clock.now().toLocalDate(),
                        tx = tx,
                    )
                }
                .also { Audit.CitizenDocumentResponseReportRead.log(
                    meta = mapOf("unitId" to unitId, "groupId" to groupId, "templateId" to documentTemplateId)
                ) }
        }
    }

    @GetMapping("/employee/reports/citizen-document-response-report/template-options")
    fun getCitizenDocumentResponseTemplateOptions(
        db: Database,
        clock: EvakaClock,
        user: AuthenticatedUser.Employee,
    ): List<CitizenDocumentResponseReportTemplate> {
        return db.connect { dbc ->
            dbc.read { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Global.READ_DOCUMENT_TEMPLATE,
                )
                tx.createQuery {
                    sql(
                        """
                    SELECT dt.id, dt.name, dt.type
                    FROM document_template dt
                    WHERE validity @> ${bind(clock.today())}
                    AND published IS TRUE
                    AND dt.type = 'CITIZEN_BASIC'
                    AND dt.content -> 'sections' -> 'questions' ? 'CHECKBOX'
                """
                    )
                }
                    .toList<CitizenDocumentResponseReportTemplate>()
            }
                .also { Audit.ChildDocumentsReportTemplatesRead.log() }
        }
    }
}

data class CitizenDocumentResponseReportTemplate(
    val id: DocumentTemplateId,
    val name: String
)

data class CitizenDocumentResponseReportRow(
    val childId: ChildId,
    val firstName: String,
    val lastName: String,
    val documentContent: DocumentContent,
    val responseDate: LocalDate,
)

fun getCitizenDocumentResponseRows(
    tx: Database.Read,
    groupIds: List<GroupId>,
    documentTemplateId: DocumentTemplateId,
    examDate: LocalDate,
): List<CitizenDocumentResponseReportRow> {
    return tx.createQuery {
            sql(
                """
            SELECT 
                p.id AS child_id,
                p.first_name AS first_name,
                p.last_name AS last_name,
                cd.content_modified_at AS content_modified_at,
                content AS document_content
            FROM daycare_group_placement dgp
            JOIN person p ON p.id = cd.child_id
            JOIN child_document cd ON cd.child_id = dgp.child_id
             AND cd.template_id = ${bind(documentTemplateId)}
             AND cd.status = 'COMPLETED'
            WHERE daterange(dgp.start_date, dgp.end_date, '[]') @> ${bind(examDate)}
            AND dgp.daycare_group_id = ANY (${bind(groupIds)})
        """
                    .trimIndent()
            )
        }
        .toList<CitizenDocumentResponseReportRow>()
}
