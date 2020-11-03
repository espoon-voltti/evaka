// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare

import fi.espoo.evaka.daycare.controllers.ApplicationUnitType
import fi.espoo.evaka.daycare.controllers.PublicUnit
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.daycare.service.DaycareManager
import fi.espoo.evaka.shared.auth.AclAuthorization
import fi.espoo.evaka.shared.db.bindNullable
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Coordinate
import org.jdbi.v3.core.Handle
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
    val canApplyDaycare: Boolean,
    val canApplyPreschool: Boolean,
    val canApplyClub: Boolean,
    val providerType: ProviderType,
    val roundTheClock: Boolean,
    val capacity: Int,
    val language: Language,
    val uploadToVarda: Boolean,
    val uploadToKoski: Boolean,
    val invoicedByMunicipality: Boolean,
    val costCenter: String?,
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
    val operationDays: Set<Int>?
) {
    fun validate() {
        if (name.isBlank()) {
            throw BadRequest("Name cannot be blank")
        }
    }
}

private fun Handle.getDaycaresQuery() = createQuery(
    // language=SQL
    """
SELECT
  daycare.*,
  um.name AS unit_manager_name, um.email AS unit_manager_email, um.phone AS unit_manager_phone,
  ca.name AS care_area_name, ca.short_name AS care_area_short_name
FROM daycare
LEFT JOIN unit_manager um ON unit_manager_id = um.id
JOIN care_area ca ON care_area_id = ca.id
WHERE :idFilter::uuid[] IS NULL OR daycare.id = ANY(:idFilter)
"""
)

fun Handle.getDaycares(authorizedUnits: AclAuthorization): List<Daycare> = getDaycaresQuery()
    .bindNullable("idFilter", authorizedUnits.ids)
    .mapTo<Daycare>()
    .toList()

data class DaycareApplyFlags(val id: UUID, val canApplyDaycare: Boolean, val canApplyPreschool: Boolean)

fun Handle.getDaycareApplyFlags(ids: Collection<UUID>): List<DaycareApplyFlags> = createQuery(
    // language=SQL
    """
SELECT id, can_apply_daycare, can_apply_preschool
FROM daycare
WHERE id = ANY(:ids)
    """.trimIndent()
).bind("ids", ids.toTypedArray())
    .mapTo<DaycareApplyFlags>()
    .toList()

fun Handle.getDaycare(id: UUID): Daycare? = getDaycaresQuery()
    .bindNullable("idFilter", listOf(id))
    .mapTo<Daycare>()
    .asSequence()
    .firstOrNull()

fun Handle.isValidDaycareId(id: UUID): Boolean = createQuery(
    // language=SQL
    """
SELECT EXISTS (SELECT 1 FROM daycare WHERE id = :id) AS valid
    """.trimIndent()
).bind("id", id).mapTo<Boolean>().asSequence().single()

fun Handle.getDaycareStub(daycareId: UUID): UnitStub? = createQuery(
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

fun Handle.createDaycare(areaId: UUID, name: String): UUID = createUpdate(
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
    .mapTo<UUID>()
    .one()

fun Handle.updateDaycareManager(daycareId: UUID, manager: UnitManager) = createUpdate(
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

fun Handle.updateDaycare(id: UUID, fields: DaycareFields) = createUpdate(
    // language=SQL
    """
UPDATE daycare
SET
  name = :name,
  opening_date = :openingDate,
  closing_date = :closingDate,
  care_area_id = :areaId,
  type = :type::care_types[],
  can_apply_daycare = :canApplyDaycare,
  can_apply_preschool = :canApplyPreschool,
  can_apply_club = :canApplyClub,
  provider_type = :providerType,
  round_the_clock = :roundTheClock,
  capacity = :capacity,
  language = :language,
  upload_to_varda = :uploadToVarda,
  upload_to_koski = :uploadToKoski,
  invoiced_by_municipality = :invoicedByMunicipality,
  cost_center = :costCenter,
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
  operation_days = :operationDays
WHERE id = :id
"""
).bind("id", id)
    .bindKotlin(fields)
    .execute()

fun Handle.getApplicationUnits(type: ApplicationUnitType, date: LocalDate, onlyApplicable: Boolean): List<PublicUnit> =
    createQuery(
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
    can_apply_daycare,
    can_apply_preschool,
    opening_date,
    closing_date
FROM daycare
WHERE daterange(opening_date, closing_date, '[]') @> :date AND (
    (:club AND type && '{CLUB}'::care_types[] AND (NOT :onlyApplicable OR can_apply_club))
    OR (:daycare AND type && '{CENTRE, FAMILY, GROUP_FAMILY}'::care_types[] AND (NOT :onlyApplicable OR can_apply_daycare))
    OR (:preschool AND type && '{PRESCHOOL}'::care_types[] AND (NOT :onlyApplicable OR can_apply_preschool))
    OR (:preparatory AND type && '{PREPARATORY_EDUCATION}'::care_types[] AND (NOT :onlyApplicable OR can_apply_preschool))
)
ORDER BY name ASC
        """.trimIndent()
    )
        .bind("date", date)
        .bind("onlyApplicable", onlyApplicable)
        .bind("club", type == ApplicationUnitType.CLUB)
        .bind("daycare", type == ApplicationUnitType.DAYCARE)
        .bind("preschool", type == ApplicationUnitType.PRESCHOOL)
        .bind("preparatory", type == ApplicationUnitType.PREPARATORY)
        .mapTo<PublicUnit>()
        .toList()

fun Handle.getUnitManager(unitId: UUID): DaycareManager? = createQuery(
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
