// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare

import fi.espoo.evaka.application.ApplicationType
import fi.espoo.evaka.daycare.controllers.ApplicationUnitType
import fi.espoo.evaka.daycare.controllers.PublicUnit
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.daycare.service.DaycareManager
import fi.espoo.evaka.shared.AreaId
import fi.espoo.evaka.shared.DatabaseTable
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.Predicate
import fi.espoo.evaka.shared.db.QuerySql
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Coordinate
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.TimeRange
import fi.espoo.evaka.shared.security.PilotFeature
import fi.espoo.evaka.shared.security.actionrule.AccessControlFilter
import fi.espoo.evaka.shared.security.actionrule.toPredicate
import java.time.DayOfWeek
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
    val decisionCustomization: DaycareDecisionCustomization,
    val ophUnitOid: String?,
    val ophOrganizerOid: String?,
    val operationTimes: List<TimeRange?>,
    val roundTheClock: Boolean,
    val businessId: String,
    val iban: String,
    val providerId: String
) {
    fun validate() {
        if (name.isBlank()) {
            throw BadRequest("Name cannot be blank")
        }
    }
}

data class DaycareGroupSummary(val id: GroupId, val name: String, val endDate: LocalDate?)

fun daycaresQuery(predicate: Predicate<DatabaseTable.Daycare>) =
    QuerySql.of<DatabaseTable.Daycare> {
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
  daycare.round_the_clock,
  daycare.enabled_pilot_features,
  daycare.business_id,
  daycare.iban,
  daycare.provider_id,
  daycare.care_area_id, 
  finance_decision_handler.id AS finance_decision_handler_id,
  coalesce(finance_decision_handler.preferred_first_name, finance_decision_handler.first_name) AS finance_decision_handler_first_name,
  finance_decision_handler.last_name AS finance_decision_handler_last_name,
  finance_decision_handler.created AS finance_decision_handler_created,
  unit_manager_name, unit_manager_email, unit_manager_phone,
  ca.name AS care_area_name, ca.short_name AS care_area_short_name,
  daycare.operation_times
FROM daycare
LEFT JOIN employee finance_decision_handler ON finance_decision_handler.id = daycare.finance_decision_handler
JOIN care_area ca ON daycare.care_area_id = ca.id
WHERE ${predicate(predicate.forTable("daycare"))}
        """
                .trimIndent()
        )
    }

fun Database.Read.getDaycares(filter: AccessControlFilter<DaycareId>): List<Daycare> =
    @Suppress("DEPRECATION") createQuery(daycaresQuery(filter.toPredicate())).toList<Daycare>()

data class UnitApplyPeriods(
    val id: DaycareId,
    val daycareApplyPeriod: DateRange?,
    val preschoolApplyPeriod: DateRange?,
    val clubApplyPeriod: DateRange?
)

fun Database.Read.getUnitApplyPeriods(ids: Collection<DaycareId>): List<UnitApplyPeriods> =
    @Suppress("DEPRECATION")
    createQuery(
            // language=SQL
            """
SELECT id, daycare_apply_period, preschool_apply_period, club_apply_period
FROM daycare
WHERE id = ANY(:ids)
    """
                .trimIndent()
        )
        .bind("ids", ids)
        .toList<UnitApplyPeriods>()

fun Database.Read.getDaycare(id: DaycareId): Daycare? =
    @Suppress("DEPRECATION")
    createQuery(daycaresQuery(Predicate { where("$it.id = ${bind(id)}") }))
        .exactlyOneOrNull<Daycare>()

fun Database.Read.isValidDaycareId(id: DaycareId): Boolean =
    @Suppress("DEPRECATION")
    createQuery(
            // language=SQL
            """
SELECT EXISTS (SELECT 1 FROM daycare WHERE id = :id) AS valid
    """
                .trimIndent()
        )
        .bind("id", id)
        .exactlyOne<Boolean>()

fun Database.Read.getDaycareStub(daycareId: DaycareId): UnitStub? =
    @Suppress("DEPRECATION")
    createQuery(
            // language=SQL
            """
SELECT id, name, type as care_types
FROM daycare
WHERE id = :daycareId
"""
        )
        .bind("daycareId", daycareId)
        .exactlyOneOrNull<UnitStub>()

fun Database.Transaction.createDaycare(areaId: AreaId, name: String): DaycareId =
    @Suppress("DEPRECATION")
    createUpdate(
            // language=SQL
            """
INSERT INTO daycare (name, care_area_id)
SELECT :name, :areaId
"""
        )
        .bind("name", name)
        .bind("areaId", areaId)
        .executeAndReturnGeneratedKeys()
        .exactlyOne<DaycareId>()

fun Database.Transaction.updateDaycareManager(daycareId: DaycareId, manager: UnitManager) =
    @Suppress("DEPRECATION")
    createUpdate(
            """
UPDATE daycare
SET
  unit_manager_name = :name,
  unit_manager_email = :email,
  unit_manager_phone = :phone
WHERE id = :daycareId
"""
        )
        .bind("daycareId", daycareId)
        .bindKotlin(manager)
        .execute()

fun Database.Transaction.updateDaycare(id: DaycareId, fields: DaycareFields) =
    @Suppress("DEPRECATION")
    createUpdate(
            // language=SQL
            """
UPDATE daycare
SET
  name = :name,
  opening_date = :openingDate,
  closing_date = :closingDate,
  care_area_id = :areaId,
  type = :type::care_types[],
  daily_preschool_time = :dailyPreschoolTime,
  daily_preparatory_time = :dailyPreparatoryTime,
  daycare_apply_period = :daycareApplyPeriod,
  preschool_apply_period = :preschoolApplyPeriod,
  club_apply_period = :clubApplyPeriod,
  provider_type = :providerType,
  capacity = :capacity,
  language = :language,
  ghost_unit = :ghostUnit,
  upload_to_varda = :uploadToVarda,
  upload_children_to_varda = :uploadChildrenToVarda,
  upload_to_koski = :uploadToKoski,
  invoiced_by_municipality = :invoicedByMunicipality,
  cost_center = :costCenter,
  dw_cost_center = :dwCostCenter,
  finance_decision_handler = :financeDecisionHandlerId,
  additional_info = :additionalInfo,
  phone = :phone,
  email = :email,
  url = :url,
  street_address = :visitingAddress.streetAddress,
  postal_code = :visitingAddress.postalCode,
  post_office = :visitingAddress.postOffice,
  mailing_street_address = :mailingAddress.streetAddress,
  mailing_po_box = :mailingAddress.poBox,
  mailing_post_office = :mailingAddress.postOffice,
  mailing_postal_code = :mailingAddress.postalCode,
  location = :location,
  decision_daycare_name = :decisionCustomization.daycareName,
  decision_handler = :decisionCustomization.handler,
  decision_handler_address = :decisionCustomization.handlerAddress,
  decision_preschool_name = :decisionCustomization.preschoolName,
  oph_unit_oid = :ophUnitOid,
  oph_organizer_oid = :ophOrganizerOid,
  round_the_clock = :roundTheClock,
  business_id = :businessId,
  iban = :iban,
  provider_id = :providerId,
  operation_times = :operationTimes
WHERE id = :id
"""
        )
        .bind("id", id)
        .bindKotlin(fields)
        .execute()

fun Database.Read.getApplicationUnits(
    type: ApplicationUnitType,
    date: LocalDate,
    shiftCare: Boolean?,
    onlyApplicable: Boolean
): List<PublicUnit> {
    // language=sql
    val sql =
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
    round_the_clock
FROM daycare
WHERE :date <= COALESCE(closing_date, 'infinity'::date)
    AND (NOT :shiftCare OR round_the_clock)
    AND (
        (:club AND type && '{CLUB}'::care_types[] AND (NOT :onlyApplicable OR (club_apply_period @> :date)))
        OR (:daycare AND type && '{CENTRE, FAMILY, GROUP_FAMILY}'::care_types[] AND (NOT :onlyApplicable OR (daycare_apply_period @> :date)))
        OR (:preschool AND type && '{PRESCHOOL}'::care_types[] AND (NOT :onlyApplicable OR (preschool_apply_period @> :date)))
        OR (:preparatory AND type && '{PREPARATORY_EDUCATION}'::care_types[] AND (NOT :onlyApplicable OR (preschool_apply_period @> :date)))
    )
ORDER BY name ASC
    """
            .trimIndent()
    @Suppress("DEPRECATION")
    return createQuery(sql)
        .bind("date", date)
        .bind("onlyApplicable", onlyApplicable)
        .bind("shiftCare", shiftCare ?: false)
        .bind("club", type == ApplicationUnitType.CLUB)
        .bind("daycare", type == ApplicationUnitType.DAYCARE)
        .bind("preschool", type == ApplicationUnitType.PRESCHOOL)
        .bind("preparatory", type == ApplicationUnitType.PREPARATORY)
        .toList<PublicUnit>()
}

fun Database.Read.getAllApplicableUnits(applicationType: ApplicationType): List<PublicUnit> {
    val applyPeriod =
        when (applicationType) {
            ApplicationType.CLUB -> "club_apply_period"
            ApplicationType.DAYCARE -> "daycare_apply_period"
            ApplicationType.PRESCHOOL -> "preschool_apply_period"
        }

    // language=sql
    val sql =
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
    round_the_clock,
    club_apply_period,
    daycare_apply_period,
    preschool_apply_period
FROM daycare
WHERE daterange(null, closing_date) @> :today AND
    ($applyPeriod && daterange(:today, null, '[]') OR provider_type = 'PRIVATE')
ORDER BY name ASC
    """
            .trimIndent()

    @Suppress("DEPRECATION")
    return createQuery(sql).bind("today", HelsinkiDateTime.now().toLocalDate()).toList<PublicUnit>()
}

fun Database.Read.getUnitManager(unitId: DaycareId): DaycareManager? =
    @Suppress("DEPRECATION")
    createQuery(
            // language=SQL
            """
    SELECT unit_manager_name AS name, unit_manager_phone AS phone, unit_manager_email AS email
    FROM daycare
    WHERE id = :unitId
    """
                .trimIndent()
        )
        .bind("unitId", unitId)
        .exactlyOneOrNull<DaycareManager>()

fun Database.Read.getDaycareGroupSummaries(daycareId: DaycareId): List<DaycareGroupSummary> =
    @Suppress("DEPRECATION")
    createQuery(
            """
SELECT id, name, end_date
FROM daycare_group
WHERE daycare_id = :daycareId
    """
        )
        .bind("daycareId", daycareId)
        .toList<DaycareGroupSummary>()

fun Database.Read.getUnitFeatures(): List<UnitFeatures> =
    @Suppress("DEPRECATION")
    createQuery(
            """
    SELECT id, name, enabled_pilot_features AS features, provider_type, type
    FROM daycare
    ORDER BY name
    """
                .trimIndent()
        )
        .toList<UnitFeatures>()

fun Database.Transaction.addUnitFeatures(
    daycareIds: List<DaycareId>,
    features: List<PilotFeature>
) {
    @Suppress("DEPRECATION")
    createUpdate(
            """
        UPDATE daycare
        SET enabled_pilot_features = enabled_pilot_features || (:features)::pilot_feature[]
        WHERE id = ANY(:ids)
        """
                .trimIndent()
        )
        .bind("ids", daycareIds)
        .bind("features", features)
        .execute()
}

fun Database.Transaction.removeUnitFeatures(
    daycareIds: List<DaycareId>,
    features: List<PilotFeature>
) {
    @Suppress("DEPRECATION")
    createUpdate(
            """
        UPDATE daycare
        SET enabled_pilot_features = array(
            SELECT unnest(enabled_pilot_features)
            EXCEPT
            SELECT unnest((:features)::pilot_feature[])
        )::pilot_feature[]
        WHERE id = ANY(:ids)
        """
                .trimIndent()
        )
        .bind("ids", daycareIds)
        .bind("features", features)
        .execute()
}

fun Database.Read.getUnitFeatures(id: DaycareId): UnitFeatures? =
    @Suppress("DEPRECATION")
    createQuery(
            """
    SELECT id, name, enabled_pilot_features AS features, provider_type, type
    FROM daycare
    WHERE id = :id
    """
                .trimIndent()
        )
        .bind("id", id)
        .exactlyOneOrNull<UnitFeatures>()

fun Database.Read.anyUnitHasFeature(ids: Collection<DaycareId>, feature: PilotFeature): Boolean =
    createQuery<Any> {
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

private data class UnitOperationDays(val id: DaycareId, val operationDays: List<Int>)

fun Database.Read.getUnitOperationDays(): Map<DaycareId, Set<DayOfWeek>> =
    @Suppress("DEPRECATION")
    createQuery("""
    SELECT id, operation_days
    FROM daycare
    """)
        .mapTo<UnitOperationDays>()
        .useIterable {
            it.fold(mutableMapOf()) { acc, row ->
                acc[row.id] = row.operationDays.map { DayOfWeek.of(it) }.toSet()
                acc
            }
        }
