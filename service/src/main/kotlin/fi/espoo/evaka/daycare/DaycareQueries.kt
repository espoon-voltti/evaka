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
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.auth.AclAuthorization
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.bindNullable
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Coordinate
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.utils.europeHelsinki
import org.jdbi.v3.core.kotlin.bindKotlin
import org.jdbi.v3.core.kotlin.mapTo
import java.time.LocalDate
import java.util.UUID

data class DaycareFields(
    val name: String,
    val openingDate: LocalDate?,
    val closingDate: LocalDate?,
    val areaId: UUID,
    val type: Set<CareType>,
    val daycareApplyPeriod: DateRange?,
    val preschoolApplyPeriod: DateRange?,
    val clubApplyPeriod: DateRange?,
    val providerType: ProviderType,
    val capacity: Int,
    val language: Language,
    val ghostUnit: Boolean,
    val uploadToVarda: Boolean,
    val uploadToKoski: Boolean,
    val invoicedByMunicipality: Boolean,
    val costCenter: String?,
    val financeDecisionHandlerId: UUID?,
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
    val ophOrganizationOid: String?,
    val operationDays: Set<Int>?,
    val roundTheClock: Boolean
) {
    fun validate() {
        if (name.isBlank()) {
            throw BadRequest("Name cannot be blank")
        }
    }
}

data class DaycareGroupSummary(val id: GroupId, val name: String)

private fun Database.Read.getDaycaresQuery() = createQuery(
    // language=SQL
    """
SELECT
  daycare.*,
  finance_decision_handler.id AS finance_decision_handler_id,
  finance_decision_handler.first_name AS finance_decision_handler_first_name,
  finance_decision_handler.last_name AS finance_decision_handler_last_name,
  finance_decision_handler.created AS finance_decision_handler_created,
  um.name AS unit_manager_name, um.email AS unit_manager_email, um.phone AS unit_manager_phone,
  ca.name AS care_area_name, ca.short_name AS care_area_short_name
FROM daycare
LEFT JOIN unit_manager um ON unit_manager_id = um.id
LEFT JOIN employee finance_decision_handler ON finance_decision_handler.id = finance_decision_handler
JOIN care_area ca ON care_area_id = ca.id
WHERE :idFilter::uuid[] IS NULL OR daycare.id = ANY(:idFilter)
"""
)

fun Database.Read.getDaycares(authorizedUnits: AclAuthorization): List<Daycare> = getDaycaresQuery()
    .bindNullable("idFilter", authorizedUnits.ids)
    .mapTo<Daycare>()
    .toList()

data class UnitApplyPeriods(
    val id: DaycareId,
    val daycareApplyPeriod: DateRange?,
    val preschoolApplyPeriod: DateRange?,
    val clubApplyPeriod: DateRange?
)

fun Database.Read.getUnitApplyPeriods(ids: Collection<DaycareId>): List<UnitApplyPeriods> = createQuery(
    // language=SQL
    """
SELECT id, daycare_apply_period, preschool_apply_period, club_apply_period
FROM daycare
WHERE id = ANY(:ids)
    """.trimIndent()
).bind("ids", ids.toTypedArray())
    .mapTo<UnitApplyPeriods>()
    .toList()

fun Database.Read.getDaycare(id: DaycareId): Daycare? = getDaycaresQuery()
    .bindNullable("idFilter", listOf(id))
    .mapTo<Daycare>()
    .asSequence()
    .firstOrNull()

fun Database.Read.isValidDaycareId(id: DaycareId): Boolean = createQuery(
    // language=SQL
    """
SELECT EXISTS (SELECT 1 FROM daycare WHERE id = :id) AS valid
    """.trimIndent()
).bind("id", id).mapTo<Boolean>().asSequence().single()

fun Database.Read.getDaycareStub(daycareId: DaycareId): UnitStub? = createQuery(
    // language=SQL
    """
SELECT id, name
FROM daycare
WHERE id = :daycareId
"""
)
    .bind("daycareId", daycareId)
    .mapTo<UnitStub>()
    .asSequence()
    .firstOrNull()

fun Database.Transaction.createDaycare(areaId: UUID, name: String): DaycareId = createUpdate(
    // language=SQL
    """
WITH insert_manager AS (
  INSERT INTO unit_manager DEFAULT VALUES
  RETURNING id
)
INSERT INTO daycare (name, care_area_id, unit_manager_id)
SELECT :name, :areaId, insert_manager.id
FROM insert_manager
"""
)
    .bind("name", name)
    .bind("areaId", areaId)
    .executeAndReturnGeneratedKeys()
    .mapTo<DaycareId>()
    .one()

fun Database.Transaction.updateDaycareManager(daycareId: DaycareId, manager: UnitManager) = createUpdate(
    // language=SQL
    """
UPDATE unit_manager
SET
  name = :name,
  email = :email,
  phone = :phone
WHERE id = (SELECT unit_manager_id FROM daycare WHERE id = :daycareId)
"""
)
    .bind("daycareId", daycareId)
    .bindKotlin(manager)
    .execute()

fun Database.Transaction.updateDaycare(id: DaycareId, fields: DaycareFields) = createUpdate(
    // language=SQL
    """
UPDATE daycare
SET
  name = :name,
  opening_date = :openingDate,
  closing_date = :closingDate,
  care_area_id = :areaId,
  type = :type::care_types[],
  daycare_apply_period = :daycareApplyPeriod,
  preschool_apply_period = :preschoolApplyPeriod,
  club_apply_period = :clubApplyPeriod,
  provider_type = :providerType,
  capacity = :capacity,
  language = :language,
  ghost_unit = :ghostUnit,
  upload_to_varda = :uploadToVarda,
  upload_to_koski = :uploadToKoski,
  invoiced_by_municipality = :invoicedByMunicipality,
  cost_center = :costCenter,
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
  oph_organization_oid = :ophOrganizationOid,
  operation_days = :operationDays,
  round_the_clock = :roundTheClock
WHERE id = :id
"""
).bind("id", id)
    .bindKotlin(fields)
    .execute()

fun Database.Read.getApplicationUnits(type: ApplicationUnitType, date: LocalDate, shiftCare: Boolean?, onlyApplicable: Boolean): List<PublicUnit> {
    // language=sql
    val sql = """
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
    """.trimIndent()
    return createQuery(sql)
        .bind("date", date)
        .bind("onlyApplicable", onlyApplicable)
        .bind("shiftCare", shiftCare ?: false)
        .bind("club", type == ApplicationUnitType.CLUB)
        .bind("daycare", type == ApplicationUnitType.DAYCARE)
        .bind("preschool", type == ApplicationUnitType.PRESCHOOL)
        .bind("preparatory", type == ApplicationUnitType.PREPARATORY)
        .mapTo<PublicUnit>()
        .toList()
}

fun Database.Read.getAllApplicableUnits(applicationType: ApplicationType): List<PublicUnit> {
    val applyPeriod = when (applicationType) {
        ApplicationType.CLUB -> "club_apply_period"
        ApplicationType.DAYCARE -> "daycare_apply_period"
        ApplicationType.PRESCHOOL -> "preschool_apply_period"
    }

    // language=sql
    val sql = """
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
WHERE $applyPeriod && daterange(:date, null, '[]') OR provider_type = 'PRIVATE'
ORDER BY name ASC
    """.trimIndent()

    return createQuery(sql)
        .bind("date", LocalDate.now(europeHelsinki))
        .mapTo<PublicUnit>()
        .toList()
}

fun Database.Read.getUnitManager(unitId: DaycareId): DaycareManager? = createQuery(
    // language=SQL
    """
    SELECT coalesce(m.name, '') AS name, coalesce(m.email, '') AS email, coalesce(m.phone, '') AS phone
    FROM unit_manager m
    JOIN daycare u ON m.id = u.unit_manager_id
    WHERE u.id = :unitId
    """.trimIndent()
)
    .bind("unitId", unitId)
    .mapTo<DaycareManager>()
    .firstOrNull()

fun Database.Read.getDaycareGroupSummaries(daycareId: DaycareId): List<DaycareGroupSummary> = createQuery(
    """
SELECT id, name
FROM daycare_group
WHERE daycare_id = :daycareId
    """
).bind("daycareId", daycareId)
    .mapTo<DaycareGroupSummary>()
    .list()
