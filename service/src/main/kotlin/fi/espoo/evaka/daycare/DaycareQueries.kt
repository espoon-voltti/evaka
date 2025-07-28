// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare

import fi.espoo.evaka.application.ApplicationType
import fi.espoo.evaka.daycare.controllers.ApplicationUnitType
import fi.espoo.evaka.daycare.controllers.PublicUnit
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.shared.AreaId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.Predicate
import fi.espoo.evaka.shared.db.QuerySql
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Coordinate
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.TimeRange
import fi.espoo.evaka.shared.security.PilotFeature
import fi.espoo.evaka.shared.security.actionrule.AccessControlFilter
import fi.espoo.evaka.shared.security.actionrule.toPredicate
import java.time.LocalDate

data class DaycareFields(
    val name: String,
    val openingDate: LocalDate?,
    val closingDate: LocalDate?,
    val areaId: AreaId,
    val type: Set<CareType>,
    val dailyPreschoolTime: TimeRange?,
    val dailyPreparatoryTime: TimeRange?,
    val daycareApplyPeriod: DateRange?,
    val preschoolApplyPeriod: DateRange?,
    val clubApplyPeriod: DateRange?,
    val providerType: ProviderType,
    val capacity: Int,
    val language: Language,
    val withSchool: Boolean,
    val ghostUnit: Boolean,
    val uploadToVarda: Boolean,
    val uploadChildrenToVarda: Boolean,
    val uploadToKoski: Boolean,
    val invoicedByMunicipality: Boolean,
    val costCenter: String?,
    val dwCostCenter: String?,
    val financeDecisionHandlerId: EmployeeId?,
    val additionalInfo: String?,
    val phone: String?,
    val email: String?,
    val url: String?,
    val visitingAddress: VisitingAddress,
    val location: Coordinate?,
    val mailingAddress: MailingAddress,
    val unitManager: UnitManager,
    val preschoolManager: UnitManager,
    val decisionCustomization: DaycareDecisionCustomization,
    val ophUnitOid: String?,
    val ophOrganizerOid: String?,
    val operationTimes: List<TimeRange?>,
    val shiftCareOperationTimes: List<TimeRange?>?,
    val shiftCareOpenOnHolidays: Boolean,
    val businessId: String,
    val iban: String,
    val providerId: String,
    val partnerCode: String,
    val mealtimes: DaycareMealtimes,
) {
    fun validate() {
        if (name.isBlank()) {
            throw BadRequest("Name cannot be blank")
        }
        if (shiftCareOperationTimes != null) {
            operationTimes.zip(shiftCareOperationTimes).forEach { (normal, shiftCare) ->
                if (normal != null && shiftCare == null) {
                    throw BadRequest(
                        "Shift care operation days must include all normal operation days"
                    )
                }
                if (normal != null && shiftCare != null && !shiftCare.contains(normal)) {
                    throw BadRequest("Shift care operation time must contain normal operation time")
                }
            }
        }
        if (openingDate != null && closingDate != null && closingDate.isBefore(openingDate)) {
            throw BadRequest("Closing date cannot be before opening date")
        }
    }

    companion object {}
}

fun validateUnitClosingDate(tx: Database.Read, unitId: DaycareId, closingDate: LocalDate) {
    val openingDate = tx.getDaycare(unitId)?.openingDate
    if (openingDate != null && closingDate.isBefore(openingDate)) {
        throw BadRequest("Closing date cannot be before opening date")
    }
    val lastPlacementDate = tx.getLastPlacementDate(unitId)
    if (lastPlacementDate != null && lastPlacementDate > closingDate) {
        throw BadRequest("Closing date cannot be before the last placement date")
    }
}

data class DaycareGroupSummary(val id: GroupId, val name: String, val endDate: LocalDate?)

fun daycaresQuery(predicate: Predicate) = QuerySql {
    sql(
        """
SELECT
  daycare.id,
  daycare.name,
  daycare.opening_date,
  daycare.closing_date,
  daycare.type,
  daycare.daily_preschool_time,
  daycare.daily_preparatory_time,
  daycare.daycare_apply_period,
  daycare.preschool_apply_period,
  daycare.club_apply_period,
  daycare.provider_type,
  daycare.capacity,
  daycare.language,
  daycare.with_school,
  coalesce(daycare.ghost_unit, false) as ghost_unit,
  daycare.upload_to_varda,
  daycare.upload_children_to_varda,
  daycare.upload_to_koski,
  daycare.invoiced_by_municipality,
  daycare.cost_center,
  daycare.dw_cost_center,
  daycare.additional_info,
  daycare.phone,
  daycare.email,
  daycare.url,
  daycare.street_address,
  daycare.postal_code,
  daycare.post_office,
  daycare.location,
  daycare.mailing_street_address,
  daycare.mailing_po_box,
  daycare.mailing_postal_code,
  daycare.mailing_post_office,
  daycare.decision_daycare_name,
  daycare.decision_preschool_name,
  daycare.decision_handler,
  daycare.decision_handler_address,
  daycare.oph_unit_oid,
  daycare.oph_organizer_oid,
  daycare.operation_days,
  daycare.shift_care_operation_days,
  daycare.shift_care_open_on_holidays,
  daycare.enabled_pilot_features,
  daycare.business_id,
  daycare.iban,
  daycare.provider_id,
  daycare.partner_code,
  daycare.care_area_id, 
  finance_decision_handler.id AS finance_decision_handler_id,
  coalesce(finance_decision_handler.preferred_first_name, finance_decision_handler.first_name) AS finance_decision_handler_first_name,
  finance_decision_handler.last_name AS finance_decision_handler_last_name,
  finance_decision_handler.created AS finance_decision_handler_created,
  unit_manager_name, unit_manager_email, unit_manager_phone,
  preschool_manager_name, preschool_manager_email, preschool_manager_phone,
  ca.name AS care_area_name, ca.short_name AS care_area_short_name,
  daycare.operation_times,
  daycare.shift_care_operation_times,
  daycare.mealtime_breakfast,
  daycare.mealtime_lunch,
  daycare.mealtime_snack,
  daycare.mealtime_supper,
  daycare.mealtime_evening_snack
FROM daycare
LEFT JOIN employee finance_decision_handler ON finance_decision_handler.id = daycare.finance_decision_handler
JOIN care_area ca ON daycare.care_area_id = ca.id
WHERE ${predicate(predicate.forTable("daycare"))}
        """
    )
}

fun Database.Read.getDaycares(
    clock: EvakaClock,
    filter: AccessControlFilter<DaycareId>,
    includeClosed: Boolean = true,
): List<Daycare> {
    val predicate =
        if (includeClosed) Predicate.alwaysTrue()
        else
            Predicate {
                where("$it.closing_date IS NULL OR $it.closing_date >= ${bind(clock.today())}")
            }
    return createQuery { daycaresQuery(filter.toPredicate().and(predicate)) }.toList<Daycare>()
}

fun Database.Read.getDaycaresById(ids: Set<DaycareId>): Map<DaycareId, Daycare> {
    if (ids.isEmpty()) return emptyMap()
    return createQuery { daycaresQuery(Predicate { where("$it.id = ANY(${bind(ids)})") }) }
        .mapTo<Daycare>()
        .useSequence { rows -> rows.associateBy { it.id } }
}

data class UnitApplyPeriods(
    val id: DaycareId,
    val daycareApplyPeriod: DateRange?,
    val preschoolApplyPeriod: DateRange?,
    val clubApplyPeriod: DateRange?,
)

fun Database.Read.getUnitApplyPeriods(ids: Collection<DaycareId>): List<UnitApplyPeriods> =
    createQuery {
            sql(
                """
SELECT id, daycare_apply_period, preschool_apply_period, club_apply_period
FROM daycare
WHERE id = ANY(${bind(ids)})
"""
            )
        }
        .toList()

fun Database.Read.getDaycare(id: DaycareId): Daycare? =
    createQuery { daycaresQuery(Predicate { where("$it.id = ${bind(id)}") }) }
        .exactlyOneOrNull<Daycare>()

fun Database.Read.getLastPlacementDate(daycareId: DaycareId): LocalDate? =
    createQuery {
            sql(
                """
SELECT GREATEST(
    (SELECT MAX(end_date) FROM placement WHERE unit_id = ${bind(daycareId)}),
    (SELECT MAX(end_date) FROM backup_care WHERE unit_id = ${bind(daycareId)})
)
"""
            )
        }
        .exactlyOneOrNull()

fun Database.Read.isValidDaycareId(id: DaycareId): Boolean =
    createQuery { sql("SELECT EXISTS (SELECT 1 FROM daycare WHERE id = ${bind(id)}) AS valid") }
        .exactlyOne()

fun Database.Read.getDaycareStub(daycareId: DaycareId): UnitStub? =
    createQuery {
            sql(
                """
SELECT id, name, type as care_types
FROM daycare
WHERE id = ${bind(daycareId)}
"""
            )
        }
        .exactlyOneOrNull()

fun Database.Transaction.createDaycare(areaId: AreaId, name: String): DaycareId =
    createUpdate {
            sql(
                """
INSERT INTO daycare (name, care_area_id)
SELECT ${bind(name)}, ${bind(areaId)}
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne()

fun Database.Transaction.updateDaycare(id: DaycareId, fields: DaycareFields) = execute {
    sql(
        """
UPDATE daycare
SET
  name = ${bind(fields.name)},
  opening_date = ${bind(fields.openingDate)},
  closing_date = ${bind(fields.closingDate)},
  care_area_id = ${bind(fields.areaId)},
  type = ${bind(fields.type)}::care_types[],
  daily_preschool_time = ${bind(fields.dailyPreschoolTime)},
  daily_preparatory_time = ${bind(fields.dailyPreparatoryTime)},
  daycare_apply_period = ${bind(fields.daycareApplyPeriod)},
  preschool_apply_period = ${bind(fields.preschoolApplyPeriod)},
  club_apply_period = ${bind(fields.clubApplyPeriod)},
  provider_type = ${bind(fields.providerType)},
  capacity = ${bind(fields.capacity)},
  language = ${bind(fields.language)},
  with_school = ${bind(fields.withSchool)},
  ghost_unit = ${bind(fields.ghostUnit)},
  upload_to_varda = ${bind(fields.uploadToVarda)},
  upload_children_to_varda = ${bind(fields.uploadChildrenToVarda)},
  upload_to_koski = ${bind(fields.uploadToKoski)},
  invoiced_by_municipality = ${bind(fields.invoicedByMunicipality)},
  cost_center = ${bind(fields.costCenter)},
  dw_cost_center = ${bind(fields.dwCostCenter)},
  finance_decision_handler = ${bind(fields.financeDecisionHandlerId)},
  additional_info = ${bind(fields.additionalInfo)},
  phone = ${bind(fields.phone)},
  email = ${bind(fields.email)},
  url = ${bind(fields.url)},
  street_address = ${bind(fields.visitingAddress.streetAddress)},
  postal_code = ${bind(fields.visitingAddress.postalCode)},
  post_office = ${bind(fields.visitingAddress.postOffice)},
  mailing_street_address = ${bind(fields.mailingAddress.streetAddress)},
  mailing_po_box = ${bind(fields.mailingAddress.poBox)},
  mailing_post_office = ${bind(fields.mailingAddress.postOffice)},
  mailing_postal_code = ${bind(fields.mailingAddress.postalCode)},
  location = ${bind(fields.location)},
  decision_daycare_name = ${bind(fields.decisionCustomization.daycareName)},
  decision_handler = ${bind(fields.decisionCustomization.handler)},
  decision_handler_address = ${bind(fields.decisionCustomization.handlerAddress)},
  decision_preschool_name = ${bind(fields.decisionCustomization.preschoolName)},
  oph_unit_oid = ${bind(fields.ophUnitOid)},
  oph_organizer_oid = ${bind(fields.ophOrganizerOid)},
  business_id = ${bind(fields.businessId)},
  iban = ${bind(fields.iban)},
  provider_id = ${bind(fields.providerId)},
  partner_code = ${bind(fields.partnerCode)},
  operation_times = ${bind(fields.operationTimes)},
  unit_manager_name = ${bind(fields.unitManager.name)},
  unit_manager_phone = ${bind(fields.unitManager.phone)},
  unit_manager_email = ${bind(fields.unitManager.email)},
  preschool_manager_name = ${bind(fields.preschoolManager.name)},
  preschool_manager_phone = ${bind(fields.preschoolManager.phone)},
  preschool_manager_email = ${bind(fields.preschoolManager.email)},
  shift_care_operation_times = ${bind(fields.shiftCareOperationTimes)},
  shift_care_open_on_holidays = ${bind(fields.shiftCareOpenOnHolidays)},
  mealtime_breakfast = ${bind(fields.mealtimes.breakfast)},
  mealtime_lunch = ${bind(fields.mealtimes.lunch)},
  mealtime_snack = ${bind(fields.mealtimes.snack)},
  mealtime_supper = ${bind(fields.mealtimes.supper)},
  mealtime_evening_snack = ${bind(fields.mealtimes.eveningSnack)}
WHERE id = ${bind(id)}
"""
    )
}

fun Database.Transaction.updateUnitClosingDate(unitId: DaycareId, closingDate: LocalDate) =
    createUpdate {
            sql("UPDATE daycare SET closing_date = ${bind(closingDate)} WHERE id = ${bind(unitId)}")
        }
        .updateExactlyOne()

fun Database.Read.getApplicationUnits(
    type: ApplicationUnitType,
    date: LocalDate,
    shiftCare: Boolean?,
    onlyApplicable: Boolean,
): List<PublicUnit> =
    createQuery {
            sql(
                """
SELECT
    id,
    name,
    type,
    provider_type,
    language,
    street_address,
    postal_code,
    post_office,
    phone,
    email,
    url,
    location,
    opening_date,
    closing_date,
    ghost_unit,
    provides_shift_care
FROM daycare
WHERE ${bind(date)} <= COALESCE(closing_date, 'infinity'::date)
    AND (NOT ${bind(shiftCare ?: false)} OR provides_shift_care)
    AND (
        (${bind(type == ApplicationUnitType.CLUB)} AND type && '{CLUB}'::care_types[] AND (NOT ${bind(onlyApplicable)} OR (club_apply_period @> ${bind(date)})))
        OR (${bind(type == ApplicationUnitType.DAYCARE)} AND type && '{CENTRE, FAMILY, GROUP_FAMILY}'::care_types[] AND (NOT ${bind(onlyApplicable)} OR (daycare_apply_period @> ${bind(date)})))
        OR (${bind(type == ApplicationUnitType.PRESCHOOL)} AND type && '{PRESCHOOL}'::care_types[] AND (NOT ${bind(onlyApplicable)} OR (preschool_apply_period @> ${bind(date)})))
        OR (${bind(type == ApplicationUnitType.PREPARATORY)} AND type && '{PREPARATORY_EDUCATION}'::care_types[] AND (NOT ${bind(onlyApplicable)} OR (preschool_apply_period @> ${bind(date)})))
    )
ORDER BY name
"""
            )
        }
        .toList()

fun Database.Read.getAllApplicableUnits(applicationType: ApplicationType): List<PublicUnit> {
    val applyPeriod =
        when (applicationType) {
            ApplicationType.CLUB -> "club_apply_period"
            ApplicationType.DAYCARE -> "daycare_apply_period"
            ApplicationType.PRESCHOOL -> "preschool_apply_period"
        }

    val today = HelsinkiDateTime.now().toLocalDate()
    return createQuery {
            sql(
                """
SELECT
    id,
    name,
    type,
    provider_type,
    language,
    street_address,
    postal_code,
    post_office,
    phone,
    email,
    url,
    location,
    opening_date,
    closing_date,
    ghost_unit,
    provides_shift_care,
    club_apply_period,
    daycare_apply_period,
    preschool_apply_period
FROM daycare
WHERE daterange(null, closing_date, '[]') @> ${bind(today)} AND
    ($applyPeriod && daterange(${bind(today)}, null, '[]') OR provider_type = 'PRIVATE')
ORDER BY name
    """
            )
        }
        .toList()
}

fun Database.Read.getDaycareGroupSummaries(daycareId: DaycareId): List<DaycareGroupSummary> =
    createQuery {
            sql(
                """
SELECT id, name, end_date
FROM daycare_group
WHERE daycare_id = ${bind(daycareId)}
"""
            )
        }
        .toList()

fun Database.Read.getUnitFeatures(today: LocalDate): List<UnitFeatures> =
    createQuery {
            sql(
                """
SELECT id, name, enabled_pilot_features AS features, provider_type, type
FROM daycare
WHERE closing_date IS NULL OR closing_date >= ${bind(today)}
ORDER BY name
"""
            )
        }
        .toList()

fun Database.Transaction.addUnitFeatures(
    daycareIds: List<DaycareId>,
    features: List<PilotFeature>,
) {
    execute {
        sql(
            """
UPDATE daycare
SET enabled_pilot_features = enabled_pilot_features || (${bind(features)})::pilot_feature[]
WHERE id = ANY(${bind(daycareIds)})
"""
        )
    }
}

fun Database.Transaction.removeUnitFeatures(
    daycareIds: List<DaycareId>,
    features: List<PilotFeature>,
) {
    execute {
        sql(
            """
UPDATE daycare
SET enabled_pilot_features = array(
    SELECT unnest(enabled_pilot_features)
    EXCEPT
    SELECT unnest((${bind(features)})::pilot_feature[])
)::pilot_feature[]
WHERE id = ANY(${bind(daycareIds)})
"""
        )
    }
}

fun Database.Read.getUnitFeatures(id: DaycareId): UnitFeatures? =
    createQuery {
            sql(
                """
SELECT id, name, enabled_pilot_features AS features, provider_type, type
FROM daycare
WHERE id = ${bind(id)}
"""
            )
        }
        .exactlyOneOrNull()

fun Database.Read.anyUnitHasFeature(ids: Collection<DaycareId>, feature: PilotFeature): Boolean =
    createQuery {
            sql(
                """
SELECT EXISTS(
    SELECT 1
    FROM daycare
    WHERE id = ANY(${bind(ids)})
    AND ${bind(feature)} = ANY(enabled_pilot_features)
)
"""
            )
        }
        .exactlyOne<Boolean>()

fun Database.Read.getUnitOperationPeriods(
    unitIds: List<DaycareId>?
): Map<DaycareId, UnitOperationPeriod> =
    createQuery {
            sql(
                """
SELECT id, opening_date, closing_date
FROM daycare unit
WHERE id = ANY(${bind(unitIds)})
"""
            )
        }
        .toMap {
            Pair(column("id"), UnitOperationPeriod(column("opening_date"), column("closing_date")))
        }

fun Database.Read.isDaycareOpenForPeriod(id: DaycareId, period: FiniteDateRange): Boolean =
    createQuery {
            sql(
                """
SELECT EXISTS (
    SELECT 1
    FROM daycare
    WHERE id = ${bind(id)}
    AND daterange(opening_date, closing_date, '[]') @> daterange(${bind(period.start)}, ${bind(period.end)}, '[]')
)
"""
            )
        }
        .exactlyOne<Boolean>()
