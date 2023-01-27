// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.decision.DecisionType
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.time.LocalDate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class ManualDuplicationReportController(private val accessControl: AccessControl) {
    @GetMapping("/reports/manual-duplication")
    fun getPlacementCountReport(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock
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
                    it.getManualDuplicationReportRows()
                }
            }
            .also { Audit.ManualDuplicationReportRead.log() }
    }

    private fun Database.Read.getManualDuplicationReportRows(): List<ManualDuplicationReportRow> {
        val sql =
            """
select supp_a.id                                       as supplementary_application_id,
       p.id                                            as child_id,
       p.first_name                                    as child_first_name,
       p.last_name                                     as child_last_name,
       p.date_of_birth,
       supplementary_decision.type                     as supplementary_decision_type,
       d.id                                            as supplementary_daycare_id,
       d.name                                          as supplementary_daycare_name,
       supplementary_decision.start_date               as supplementary_start_date,
       supplementary_decision.end_date                 as supplementary_end_date,
       af.document -> 'serviceNeedOption' ->> 'nameFi' as supplementary_sno_name,
       preschool_decision.preschool_daycare_id,
       preschool_decision.preschool_daycare_name,
       preschool_decision.preschool_decision_type,
       preschool_decision.preschool_start_date,
       preschool_decision.preschool_end_date
from decision supplementary_decision
         join application supp_a on supplementary_decision.application_id = supp_a.id
         join application_form af on supp_a.id = af.application_id and af.latest IS TRUE
         join daycare d on supplementary_decision.unit_id = d.id
         join person p on supp_a.child_id = p.id
         join lateral (
    select pre_d.type                        as preschool_decision_type,
           d.id                              as preschool_daycare_id,
           d.name                            as preschool_daycare_name,
           pre_d.start_date as preschool_start_date,
           pre_d.end_date   as preschool_end_date
    from decision pre_d
             join application pre_a on pre_d.application_id = pre_a.id
             join daycare d on pre_d.unit_id = d.id
             join person p on pre_a.child_id = p.id
    where pre_a.child_id = supp_a.child_id
      and pre_d.type = 'PRESCHOOL'
      and pre_d.status = 'ACCEPTED'
      and daterange(pre_d.start_date, pre_d.end_date, '[]') &&
          daterange(supplementary_decision.start_date, supplementary_decision.end_date, '[]')
      and supplementary_decision.unit_id <> pre_d.unit_id ) preschool_decision on true
where supplementary_decision.type = 'PRESCHOOL_DAYCARE'
  and supplementary_decision.status = 'ACCEPTED';
            """
                .trimIndent()

        return createQuery(sql).mapTo<ManualDuplicationReportRow>().toList()
    }

    data class ManualDuplicationReportRow(
        val supplementaryDaycareId: DaycareId,
        val supplementaryDaycareName: String,
        val childId: ChildId,
        val childFirstName: String,
        val childLastName: String,
        val dateOfBirth: LocalDate,
        val supplementaryDecisionType: DecisionType,
        val supplementaryStartDate: LocalDate,
        val supplementaryEndDate: LocalDate,
        val supplementarySnoName: String,
        val preschoolDaycareId: DaycareId,
        val preschoolDaycareName: String,
        val preschoolDecisionType: DecisionType,
        val preschoolStartDate: LocalDate,
        val preschoolEndDate: LocalDate
    )
}
