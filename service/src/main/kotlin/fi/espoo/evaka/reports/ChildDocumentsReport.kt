// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.document.ChildDocumentType
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.DocumentTemplateId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.time.LocalDate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class ChildDocumentsReport(private val accessControl: AccessControl) {
    data class UnitRow(
        val unitId: DaycareId,
        val unitName: String,
        val drafts: Int,
        val prepared: Int,
        val completed: Int,
        val none: Int,
        val total: Int,
        val groups: List<GroupRow>,
    )

    data class GroupRow(
        val groupId: GroupId,
        val groupName: String,
        val drafts: Int,
        val prepared: Int,
        val completed: Int,
        val none: Int,
        val total: Int,
    )

    @GetMapping("/employee/reports/child-documents")
    fun getChildDocumentsReport(
        db: Database,
        clock: EvakaClock,
        user: AuthenticatedUser.Employee,
        @RequestParam templateIds: Set<DocumentTemplateId> = emptySet(),
        @RequestParam unitIds: Set<DaycareId> = emptySet(),
    ): List<UnitRow> {
        if (templateIds.isEmpty() || unitIds.isEmpty())
            throw BadRequest("Both templateIds and unitIds must be provided")

        return db.connect { dbc ->
            dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Unit.READ_CHILD_DOCUMENTS_REPORT,
                        unitIds,
                    )
                    tx.getUnitRows(
                        templateIds = templateIds,
                        unitIds = unitIds,
                        today = clock.today(),
                    )
                }
                .also {
                    Audit.ChildDocumentsReportRead.log(
                        meta = mapOf("templateIds" to templateIds, "unitIds" to unitIds)
                    )
                }
        }
    }

    data class ChildDocumentsReportTemplate(
        val id: DocumentTemplateId,
        val name: String,
        val type: ChildDocumentType,
    )

    @GetMapping("/employee/reports/child-documents/template-options")
    fun getChildDocumentsReportTemplateOptions(
        db: Database,
        clock: EvakaClock,
        user: AuthenticatedUser.Employee,
    ): List<ChildDocumentsReportTemplate> {
        return db.connect { dbc ->
            dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Global.READ_DOCUMENT_TEMPLATE,
                    )
                    val templateTypes =
                        setOf(
                            ChildDocumentType.PEDAGOGICAL_REPORT,
                            ChildDocumentType.PEDAGOGICAL_ASSESSMENT,
                            ChildDocumentType.HOJKS,
                            ChildDocumentType.VASU,
                            ChildDocumentType.LEOPS,
                            ChildDocumentType.OTHER,
                        )
                    tx.createQuery {
                            sql(
                                """
                    SELECT id, name, type
                    FROM document_template
                    WHERE validity @> ${bind(clock.today())} AND published AND type = ANY(${bind(templateTypes)})
                """
                            )
                        }
                        .toList<ChildDocumentsReportTemplate>()
                }
                .also { Audit.ChildDocumentsReportTemplatesRead.log() }
        }
    }

    private data class DataRow(
        val unitId: DaycareId,
        val unitName: String,
        val groupId: GroupId,
        val groupName: String,
        val drafts: Int,
        val prepared: Int,
        val completed: Int,
        val none: Int,
        val total: Int,
    )

    private fun Database.Read.getUnitRows(
        templateIds: Set<DocumentTemplateId>,
        unitIds: Set<DaycareId>,
        today: LocalDate,
    ): List<UnitRow> {
        return createQuery {
                sql(
                    """
WITH valid_children AS (
    SELECT dt.id AS template_id, pl.child_id, pl.unit_id, dgp.daycare_group_id AS group_id
    FROM document_template dt
    JOIN placement pl ON pl.type = ANY(dt.placement_types)
    JOIN daycare d ON pl.unit_id = d.id
    JOIN daycare_group_placement dgp ON pl.id = dgp.daycare_placement_id
      AND daterange(dgp.start_date, dgp.end_date, '[]') @> ${bind(today)}
    JOIN daycare_group dg ON dgp.daycare_group_id = dg.id
    WHERE dt.id = ANY(${bind(templateIds)})
      AND pl.unit_id = ANY(${bind(unitIds)})
      AND daterange(pl.start_date, pl.end_date, '[]') @> ${bind(today)}
      AND lower(dt.language::text) = lower(d.language::text)
      AND daterange(dg.start_date, dg.end_date, '[]') @> ${bind(today)}
)
SELECT
    d.id AS unit_id,
    d.name AS unit_name,
    dg.id AS group_id,
    dg.name AS group_name,
    COUNT(DISTINCT vc.child_id) FILTER (WHERE cd.status = 'DRAFT') AS drafts,
    COUNT(DISTINCT vc.child_id) FILTER (WHERE cd.status = 'PREPARED') AS prepared,
    COUNT(DISTINCT vc.child_id) FILTER (WHERE cd.status = 'COMPLETED') AS completed,
    COUNT(DISTINCT vc.child_id) FILTER (WHERE cd.id IS NULL) AS none,
    COUNT(DISTINCT vc.child_id) AS total
FROM daycare d
JOIN daycare_group dg ON d.id = dg.daycare_id AND daterange(dg.start_date, dg.end_date, '[]') @> ${bind(today)}
LEFT JOIN valid_children vc ON d.id = vc.unit_id AND dg.id = vc.group_id
LEFT JOIN child_document cd ON vc.child_id = cd.child_id AND cd.template_id = ANY(${bind(templateIds)})
WHERE d.id = ANY(${bind(unitIds)})
GROUP BY d.id, d.name, dg.id, dg.name
        """
                )
            }
            .toList<DataRow>()
            .groupBy { it.unitId to it.unitName }
            .map { (unit, rows) ->
                UnitRow(
                    unitId = unit.first,
                    unitName = unit.second,
                    drafts = rows.sumOf { it.drafts },
                    prepared = rows.sumOf { it.prepared },
                    completed = rows.sumOf { it.completed },
                    none = rows.sumOf { it.none },
                    total = rows.sumOf { it.total },
                    groups =
                        rows.map {
                            GroupRow(
                                groupId = it.groupId,
                                groupName = it.groupName,
                                drafts = it.drafts,
                                prepared = it.prepared,
                                completed = it.completed,
                                none = it.none,
                                total = it.total,
                            )
                        },
                )
            }
    }
}
