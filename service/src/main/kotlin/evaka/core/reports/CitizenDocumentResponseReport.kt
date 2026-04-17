// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.reports

import evaka.core.Audit
import evaka.core.AuditId
import evaka.core.daycare.getDaycare
import evaka.core.daycare.service.DaycareGroup
import evaka.core.daycare.service.DaycareService
import evaka.core.document.DocumentTemplateContent
import evaka.core.document.childdocument.DocumentContent
import evaka.core.document.childdocument.DocumentStatus
import evaka.core.shared.ChildId
import evaka.core.shared.DaycareId
import evaka.core.shared.DocumentTemplateId
import evaka.core.shared.GroupId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.db.Predicate
import evaka.core.shared.domain.BadRequest
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.security.AccessControl
import evaka.core.shared.security.Action
import evaka.core.shared.security.actionrule.toPredicate
import java.time.LocalDate
import org.jdbi.v3.json.Json
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class CitizenDocumentResponseReport(
    private val accessControl: AccessControl,
    private val daycareService: DaycareService,
) {

    @GetMapping("/employee/reports/citizen-document-response-report")
    fun getCitizenDocumentResponseReport(
        db: Database,
        clock: EvakaClock,
        user: AuthenticatedUser.Employee,
        @RequestParam unitId: DaycareId,
        @RequestParam groupId: GroupId,
        @RequestParam documentTemplateId: DocumentTemplateId,
    ): List<CitizenDocumentResponseReportRow> {
        return db.connect { dbc ->
            dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Group.READ_CITIZEN_DOCUMENT_RESPONSE_REPORT,
                        groupId,
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

                    tx.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)
                    getCitizenDocumentResponseRows(
                        documentTemplateId = documentTemplateId,
                        groupIds = listOf(groupId),
                        examDate = clock.now().toLocalDate(),
                        tx = tx,
                    )
                }
                .also {
                    Audit.CitizenDocumentResponseReportRead.log(
                        targetId = AuditId(documentTemplateId),
                        objectId = AuditId(groupId),
                        meta = mapOf("unitId" to unitId),
                    )
                }
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
                    SELECT dt.id, dt.name, dt.type, dt.content
                    FROM document_template dt
                    WHERE validity @> ${bind(clock.today())}
                      AND published IS TRUE
                      AND dt.type = 'CITIZEN_BASIC'
                      -- only show templates that have checkbox or radio button group type questions
                      AND (dt.content @> '{"sections": [{"questions": [{"type": "CHECKBOX"}]}]}' ::jsonb
                        OR dt.content @> '{"sections": [{"questions": [{"type": "RADIO_BUTTON_GROUP"}]}]}' ::jsonb)
                """
                            )
                        }
                        .toList<CitizenDocumentResponseReportTemplate>()
                }
                .also { Audit.ChildDocumentsReportTemplatesRead.log() }
        }
    }

    @GetMapping("/employee/reports/citizen-document-response-report/group-options")
    fun getCitizenDocumentResponseReportGroupOptions(
        db: Database,
        clock: EvakaClock,
        user: AuthenticatedUser.Employee,
        @RequestParam unitId: DaycareId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate? = null,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate? = null,
        @RequestParam includeClosed: Boolean = true,
    ): List<DaycareGroup> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Unit.READ_GROUPS,
                        unitId,
                    )
                    val groupFilter =
                        accessControl.getAuthorizationFilter(
                            tx,
                            user,
                            clock,
                            Action.Group.READ_CITIZEN_DOCUMENT_RESPONSE_REPORT,
                        )
                    daycareService.getDaycareGroups(
                        tx,
                        unitId,
                        from,
                        to,
                        includeClosed,
                        groupFilter?.toPredicate() ?: Predicate.alwaysTrue(),
                    )
                }
            }
            .also {
                Audit.CitizenDocumentResponseReportGroupOptionsRead.log(
                    targetId = AuditId(unitId),
                    meta = mapOf("size" to it.size),
                )
            }
    }
}

data class CitizenDocumentResponseReportTemplate(
    val id: DocumentTemplateId,
    val name: String,
    @Json val content: DocumentTemplateContent,
)

data class CitizenDocumentResponseReportRow(
    val childId: ChildId,
    val firstName: String,
    val lastName: String,
    val isBackup: Boolean,
    @Json val documentContent: DocumentContent?,
    val answeredAt: HelsinkiDateTime?,
    val documentStatus: DocumentStatus?,
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
SELECT p.id                   AS child_id,
       p.first_name           AS first_name,
       p.last_name            AS last_name,
       latest_doc.answered_at AS answered_at,
       latest_doc.content     AS document_content,
       latest_doc.status      AS document_status,
       rp.is_backup
FROM realized_placement_one(${bind(examDate)}) rp
JOIN person p ON p.id = rp.child_id
JOIN placement pl on rp.placement_id = pl.id
LEFT JOIN LATERAL (
    SELECT cd.id, cd.answered_at, cd.child_id, cd.content, cd.status
    FROM child_document cd
    WHERE cd.child_id = pl.child_id
      AND cd.template_id = ${bind(documentTemplateId)}
      AND (cd.status = 'COMPLETED' OR cd.status = 'CITIZEN_DRAFT')
      AND daterange(pl.start_date, pl.end_date, '[]') @> cd.content_locked_at::date
    ORDER BY cd.answered_at DESC, cd.created_at DESC
    LIMIT 1) latest_doc ON true
WHERE rp.group_id = ANY (${bind(groupIds)})
        """
                    .trimIndent()
            )
        }
        .toList<CitizenDocumentResponseReportRow>()
}
