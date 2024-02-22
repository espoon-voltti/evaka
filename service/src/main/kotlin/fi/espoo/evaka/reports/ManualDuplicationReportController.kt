// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.decision.DecisionType
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.time.LocalDate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class ManualDuplicationReportController(private val accessControl: AccessControl) {
    @GetMapping("/reports/manual-duplication")
    fun getManualDuplicationReport(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @RequestParam("viewMode") viewMode: ManualDuplicationReportViewMode?
    ): List<ManualDuplicationReportRow> {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Global.READ_MANUAL_DUPLICATION_REPORT
                    )
                    it.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)
                    it.getManualDuplicationReportRows(
                        viewMode ?: ManualDuplicationReportViewMode.NONDUPLICATED
                    )
                }
            }
            .also { Audit.ManualDuplicationReportRead.log() }
    }

    private fun Database.Read.getManualDuplicationReportRows(
        viewMode: ManualDuplicationReportViewMode
    ): List<ManualDuplicationReportRow> {
        val showDuplicatedWhereClause =
            when (viewMode) {
                ManualDuplicationReportViewMode.DUPLICATED ->
                    "AND EXISTS(select from person per where per.duplicate_of = p.id)"
                ManualDuplicationReportViewMode.NONDUPLICATED ->
                    """
                        AND NOT EXISTS(select
                             from application transfer_app
                                      join decision transfer_decision on transfer_app.id = transfer_decision.application_id
                             where transfer_app.child_id = conn_app.child_id
                               and transfer_app.id <> conn_app.id
                               and transfer_decision.status = 'ACCEPTED'
                               and daterange(transfer_decision.start_date, transfer_decision.end_date, '[]') &&
                                   daterange(connected_decision.start_date, connected_decision.end_date, '[]')
                               and transfer_decision.sent_date > connected_decision.sent_date)
                        AND NOT EXISTS(select from person per where per.duplicate_of = p.id)
                    """
                        .trimIndent()
            }

        val sql =
            """
select conn_app.id                                           as application_id,
       p.id                                                  as child_id,
       p.first_name                                          as child_first_name,
       p.last_name                                           as child_last_name,
       p.date_of_birth,
       connected_decision.type                               as connected_decision_type,
       d.id                                                  as connected_daycare_id,
       d.name                                                as connected_daycare_name,
       connected_decision.start_date                         as connected_start_date,
       connected_decision.end_date                           as connected_end_date,
       conn_app.document -> 'serviceNeedOption' ->> 'nameFi' as connected_sno_name,
       preschool_decision.preschool_daycare_id,
       preschool_decision.preschool_daycare_name,
       preschool_decision.preschool_decision_type,
       preschool_decision.preschool_start_date,
       preschool_decision.preschool_end_date
from decision connected_decision
         join application conn_app on connected_decision.application_id = conn_app.id
         join daycare d on connected_decision.unit_id = d.id
         join person p on conn_app.child_id = p.id
         join lateral (
    select pre_d.type       as preschool_decision_type,
           d.id             as preschool_daycare_id,
           d.name           as preschool_daycare_name,
           pre_d.start_date as preschool_start_date,
           pre_d.end_date   as preschool_end_date
    from decision pre_d
    join daycare d on pre_d.unit_id = d.id
    where pre_d.application_id = conn_app.id
      and pre_d.type = 'PRESCHOOL'
      and pre_d.status = 'ACCEPTED'
      and daterange(pre_d.start_date, pre_d.end_date, '[]') &&
          daterange(connected_decision.start_date, connected_decision.end_date, '[]')
      and connected_decision.unit_id <> pre_d.unit_id ) preschool_decision on true
where connected_decision.type = 'PRESCHOOL_DAYCARE'
  and connected_decision.status = 'ACCEPTED'
  $showDuplicatedWhereClause;
            """
                .trimIndent()

        @Suppress("DEPRECATION") return createQuery(sql).toList<ManualDuplicationReportRow>()
    }

    data class ManualDuplicationReportRow(
        val applicationId: ApplicationId,
        val connectedDaycareId: DaycareId,
        val connectedDaycareName: String,
        val childId: ChildId,
        val childFirstName: String,
        val childLastName: String,
        val dateOfBirth: LocalDate,
        val connectedDecisionType: DecisionType,
        val connectedStartDate: LocalDate,
        val connectedEndDate: LocalDate,
        val connectedSnoName: String?,
        val preschoolDaycareId: DaycareId,
        val preschoolDaycareName: String,
        val preschoolDecisionType: DecisionType,
        val preschoolStartDate: LocalDate,
        val preschoolEndDate: LocalDate
    )
}

enum class ManualDuplicationReportViewMode {
    DUPLICATED,
    NONDUPLICATED
}
