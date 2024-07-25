// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class UnitsReportController(private val accessControl: AccessControl) {
    @GetMapping(
        "/reports/units", // deprecated
        "/employee/reports/units"
    )
    fun getUnitsReport(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock
    ): List<UnitsReportRow> {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Global.READ_UNITS_REPORT
                    )
                    it.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)
                    it.getUnitRows()
                }
            }
            .also { Audit.UnitsReportRead.log() }
    }
}

private fun Database.Read.getUnitRows(): List<UnitsReportRow> {
    return createQuery {
            sql(
                """
            SELECT 
                u.id,
                u.name,
                ca.name AS care_area_name,
                'CENTRE' = ANY(u.type) AS care_type_centre,
                'FAMILY' = ANY(u.type) AS care_type_family,
                'GROUP_FAMILY' = ANY(u.type) AS care_type_group_family,
                'CLUB' = ANY(u.type) AS care_type_club,
                'PRESCHOOL' = ANY(u.type) AS care_type_preschool,
                'PREPARATORY_EDUCATION' = ANY(u.type) AS care_type_preparatory_education,
                u.club_apply_period IS NOT NULL as club_apply,
                u.daycare_apply_period IS NOT NULL as daycare_apply,
                u.preschool_apply_period IS NOT NULL as preschool_apply,
                u.provider_type,
                u.upload_to_varda,
                u.upload_children_to_varda,
                u.upload_to_koski,
                u.oph_unit_oid,
                u.oph_organizer_oid,
                u.invoiced_by_municipality,
                coalesce(u.cost_center, '') AS cost_center,
                (u.street_address || ', ' || u.postal_code || ', ' || u.post_office) as address,
                u.unit_manager_name,
                u.unit_manager_phone
            FROM daycare u
            JOIN care_area ca ON ca.id = u.care_area_id
        """
            )
        }
        .toList<UnitsReportRow>()
}

data class UnitsReportRow(
    val id: DaycareId,
    val name: String,
    val careAreaName: String,
    val careTypeCentre: Boolean,
    val careTypeFamily: Boolean,
    val careTypeGroupFamily: Boolean,
    val careTypeClub: Boolean,
    val careTypePreschool: Boolean,
    val careTypePreparatoryEducation: Boolean,
    val clubApply: Boolean,
    val daycareApply: Boolean,
    val preschoolApply: Boolean,
    val providerType: ProviderType,
    val uploadToVarda: Boolean,
    val uploadChildrenToVarda: Boolean,
    val uploadToKoski: Boolean,
    val ophUnitOid: String?,
    val ophOrganizerOid: String?,
    val invoicedByMunicipality: Boolean,
    val costCenter: String,
    val address: String,
    val unitManagerName: String,
    val unitManagerPhone: String
)
