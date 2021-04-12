// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.dev

import com.fasterxml.jackson.databind.ObjectMapper
import fi.espoo.evaka.application.ApplicationStatus
import fi.espoo.evaka.application.persistence.club.ClubFormV0
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.application.persistence.objectMapper
import fi.espoo.evaka.daycare.service.AbsenceType
import fi.espoo.evaka.daycare.service.CareType
import fi.espoo.evaka.decision.DecisionStatus
import fi.espoo.evaka.decision.DecisionType
import fi.espoo.evaka.identity.ExternalId
import fi.espoo.evaka.invoicing.domain.FeeAlteration
import fi.espoo.evaka.invoicing.domain.IncomeEffect
import fi.espoo.evaka.invoicing.domain.IncomeType
import fi.espoo.evaka.invoicing.domain.IncomeValue
import fi.espoo.evaka.invoicing.domain.VoucherValue
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import org.intellij.lang.annotations.Language
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.kotlin.bindKotlin
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.core.qualifier.QualifiedType
import org.jdbi.v3.json.Json
import org.postgresql.util.PGobject
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * Insert one row of data to the database.
 *
 * * all fields from `row` are bound separately as parameters
 * * SQL must return "id" column of type UUID
 */
private fun Handle.insertTestDataRow(row: Any, @Language("sql") sql: String): UUID = createUpdate(sql)
    .bindKotlin(row)
    .executeAndReturnGeneratedKeys()
    .mapTo<UUID>()
    .asSequence()
    .single()

fun Handle.insertTestCareArea(area: DevCareArea): UUID = insertTestDataRow(
    area,
    """
INSERT INTO care_area (id, name, short_name, area_code, sub_cost_center)
VALUES (:id, :name, :shortName, :areaCode, :subCostCenter)
RETURNING id
"""
)

fun Handle.insertTestDaycare(daycare: DevDaycare): UUID = insertTestDataRow(
    daycare,
    """
WITH insert_unit_manager AS (
  INSERT INTO unit_manager (name) VALUES (:unitManager.name)
  RETURNING id
)
INSERT INTO daycare (
  id, name, opening_date, closing_date, care_area_id, type, daycare_apply_period, preschool_apply_period, club_apply_period, provider_type,
  capacity, language, ghost_unit, upload_to_varda, upload_to_koski, invoiced_by_municipality, cost_center,
  additional_info, phone, email, url,
  street_address, postal_code, post_office,
  location, mailing_street_address, mailing_po_box, mailing_postal_code, mailing_post_office,
  unit_manager_id,
  decision_daycare_name, decision_preschool_name, decision_handler, decision_handler_address,
  oph_unit_oid, oph_organizer_oid, oph_organization_oid, round_the_clock, operation_days
) VALUES (
  :id, :name, :openingDate, :closingDate, :areaId, :type::care_types[], :daycareApplyPeriod, :preschoolApplyPeriod, :clubApplyPeriod, :providerType,
  :capacity, :language, :ghostUnit, :uploadToVarda, :uploadToKoski, :invoicedByMunicipality, :costCenter,
  :additionalInfo, :phone, :email, :url,
  :visitingAddress.streetAddress, :visitingAddress.postalCode, :visitingAddress.postOffice,
  :location, :mailingAddress.streetAddress, :mailingAddress.poBox, :mailingAddress.postalCode, :mailingAddress.postOffice,
  (SELECT id FROM insert_unit_manager),
  :decisionCustomization.daycareName, :decisionCustomization.preschoolName, :decisionCustomization.handler, :decisionCustomization.handlerAddress,
  :ophUnitOid, :ophOrganizerOid, :ophOrganizationOid, :roundTheClock, :operationDays
)
RETURNING id
"""
)

fun updateDaycareAcl(h: Handle, daycareId: UUID, externalId: ExternalId, role: UserRole) {
    h
        .createUpdate("INSERT INTO daycare_acl (employee_id, daycare_id, role) VALUES ((SELECT id from employee where external_id = :external_id), :daycare_id, :role)")
        .bind("daycare_id", daycareId)
        .bind("external_id", externalId)
        .bind("role", role)
        .execute()
}

fun updateDaycareAclWithEmployee(h: Handle, daycareId: UUID, employeeId: UUID, role: UserRole) {
    h
        .createUpdate("INSERT INTO daycare_acl (employee_id, daycare_id, role) VALUES (:employeeId, :daycare_id, :role)")
        .bind("daycare_id", daycareId)
        .bind("employeeId", employeeId)
        .bind("role", role)
        .execute()
}

fun Database.Transaction.createMobileDeviceToUnit(id: UUID, unitId: UUID, name: String = "Nimeämätön laite") {
    // language=sql
    val sql =
        """
        INSERT INTO employee (id, first_name, last_name, email, external_id)
        VALUES (:id, :name, 'Yksikkö', null, null);
        
        INSERT INTO mobile_device (id, unit_id, name) VALUES (:id, :unitId, :name);
        """.trimIndent()

    createUpdate(sql)
        .bind("id", id)
        .bind("unitId", unitId)
        .bind("name", name)
        .execute()
}

fun removeDaycareAcl(h: Handle, daycareId: UUID, externalId: ExternalId) {
    h
        .createUpdate("DELETE FROM daycare_acl WHERE daycare_id=:daycare_id AND employee_id=(SELECT id from employee where external_id = :external_id)")
        .bind("daycare_id", daycareId)
        .bind("external_id", externalId)
        .execute()
}

fun Handle.insertTestEmployee(employee: DevEmployee) = insertTestDataRow(
    employee,
    """
INSERT INTO employee (id, first_name, last_name, email, external_id, roles, pin)
VALUES (:id, :firstName, :lastName, :email, :externalId, :roles::user_role[], :pin)
RETURNING id
"""
)

fun Database.Transaction.insertTestMobileDevice(device: DevMobileDevice) = this.handle.insertTestDataRow(
    device,
    """
INSERT INTO mobile_device (id, unit_id, name, deleted, long_term_token)
VALUES (:id, :unitId, :name, :deleted, :longTermToken)
RETURNING id
    """
)

fun Handle.insertTestPerson(person: DevPerson) = insertTestDataRow(
    person,
    """
INSERT INTO person (
    id, date_of_birth, date_of_death, first_name, last_name, social_security_number, email, phone, language,
    street_address, postal_code, post_office, residence_code, nationalities, restricted_details_enabled, restricted_details_end_date,
    invoicing_street_address, invoicing_postal_code, invoicing_post_office
) VALUES (
    :id, :dateOfBirth, :dateOfDeath, :firstName, :lastName, :ssn, :email, :phone, :language,
    :streetAddress, :postalCode, :postOffice, :residenceCode, :nationalities, :restrictedDetailsEnabled, :restrictedDetailsEndDate,
    :invoicingStreetAddress, :invoicingPostalCode, :invoicingPostOffice
)
RETURNING id
"""
)

fun insertTestParentship(
    h: Handle,
    headOfChild: UUID,
    childId: UUID,
    id: UUID = UUID.randomUUID(),
    startDate: LocalDate = LocalDate.of(2019, 1, 1),
    endDate: LocalDate = LocalDate.of(2019, 12, 31)
): UUID {
    h
        .createUpdate(
            """
            INSERT INTO fridge_child (id, head_of_child, child_id, start_date, end_date)
            VALUES (:id, :headOfChild, :childId, :startDate, :endDate)
            """
        )
        .bindMap(
            mapOf(
                "id" to id,
                "headOfChild" to headOfChild,
                "childId" to childId,
                "startDate" to startDate,
                "endDate" to endDate
            )
        )
        .execute()
    return id
}

fun Handle.insertTestParentship(parentship: DevParentship): DevParentship {
    val withId = if (parentship.id == null) parentship.copy(id = UUID.randomUUID()) else parentship
    // language=sql
    val sql =
        """
        INSERT INTO fridge_child (id, head_of_child, child_id, start_date, end_date)
        VALUES (:id, :headOfChildId, :childId, :startDate, :endDate)
        """.trimIndent()
    createUpdate(sql).bindKotlin(withId).execute()
    return withId
}

fun insertTestPartnership(
    h: Handle,
    adult1: UUID,
    adult2: UUID,
    id: UUID = UUID.randomUUID(),
    startDate: LocalDate = LocalDate.of(2019, 1, 1),
    endDate: LocalDate = LocalDate.of(2019, 12, 31)
): UUID {
    h
        .createUpdate(
            """
            INSERT INTO fridge_partner (partnership_id, indx, person_id, start_date, end_date)
            VALUES (:id, :index, :personId, :startDate, :endDate)
            """
        )
        .bindMap(
            mapOf(
                "id" to id,
                "index" to 1,
                "personId" to adult1,
                "startDate" to startDate,
                "endDate" to endDate
            )
        )
        .execute()
    h
        .createUpdate(
            """
            INSERT INTO fridge_partner (partnership_id, indx, person_id, start_date, end_date)
            VALUES (:id, :index, :personId, :startDate, :endDate)
            """
        )
        .bindMap(
            mapOf(
                "id" to id,
                "index" to 2,
                "personId" to adult2,
                "startDate" to startDate,
                "endDate" to endDate
            )
        )
        .execute()
    return id
}

fun insertTestApplication(
    h: Handle,
    id: UUID = UUID.randomUUID(),
    sentDate: LocalDate? = LocalDate.of(2019, 1, 1),
    dueDate: LocalDate? = LocalDate.of(2019, 5, 1),
    status: ApplicationStatus = ApplicationStatus.SENT,
    guardianId: UUID,
    childId: UUID,
    otherGuardianId: UUID? = null,
    hideFromGuardian: Boolean = false,
    transferApplication: Boolean = false
): UUID {
    h
        .createUpdate(
            """
            INSERT INTO application (id, sentdate, duedate, status, guardian_id, child_id, other_guardian_id, origin, hidefromguardian, transferApplication)
            VALUES (:id, :sentDate, :dueDate, :status::application_status_type, :guardianId, :childId, :otherGuardianId, 'ELECTRONIC'::application_origin_type, :hideFromGuardian, :transferApplication)
            """
        )
        .bindMap(
            mapOf(
                "id" to id,
                "sentDate" to sentDate,
                "dueDate" to dueDate,
                "status" to status,
                "guardianId" to guardianId,
                "childId" to childId,
                "otherGuardianId" to otherGuardianId,
                "hideFromGuardian" to hideFromGuardian,
                "transferApplication" to transferApplication
            )
        )
        .execute()
    return id
}

fun insertTestApplicationForm(h: Handle, applicationId: UUID, document: DaycareFormV0, revision: Int = 1) {
    h.createUpdate(
        """
UPDATE application_form SET latest = FALSE
WHERE application_id = :applicationId AND revision < :revision
"""
    ).bind("applicationId", applicationId)
        .bind("revision", revision)
        .execute()
    h.createUpdate(
        // language=SQL
        """
INSERT INTO application_form (application_id, revision, document, latest)
VALUES (:applicationId, :revision, :document, TRUE)
"""
    )
        .bind("applicationId", applicationId)
        .bind("revision", revision)
        .bindByType("document", document, QualifiedType.of(document.javaClass).with(Json::class.java))
        .execute()
}

fun insertTestClubApplicationForm(h: Handle, applicationId: UUID, document: ClubFormV0, revision: Int = 1) {
    h.createUpdate(
        """
UPDATE application_form SET latest = FALSE
WHERE application_id = :applicationId AND revision < :revision
"""
    ).bind("applicationId", applicationId)
        .bind("revision", revision)
        .execute()
    h.createUpdate(
        // language=SQL
        """
INSERT INTO application_form (application_id, revision, document, latest)
VALUES (:applicationId, :revision, :document, TRUE)
"""
    )
        .bind("applicationId", applicationId)
        .bind("revision", revision)
        .bindByType("document", document, QualifiedType.of(document.javaClass).with(Json::class.java))
        .execute()
}

fun Handle.insertTestChild(child: DevChild) = insertTestDataRow(
    child,
    """
INSERT INTO child (id, allergies, diet, medication, additionalinfo, preferred_Name)
VALUES (:id, :allergies, :diet, :medication, :additionalInfo, :preferredName)
ON CONFLICT(id) DO UPDATE
SET id = :id, allergies = :allergies, diet = :diet, medication = :medication, additionalInfo = :additionalInfo, preferred_name = :preferredName
RETURNING id
    """
)

fun Handle.insertTestPlacement(placement: DevPlacement) = insertTestDataRow(
    placement,
    """
INSERT INTO placement (id, type, child_id, unit_id, start_date, end_date)
VALUES (:id, :type, :childId, :unitId, :startDate, :endDate)
RETURNING id
"""
)

fun insertTestPlacement(
    h: Handle,
    id: UUID = UUID.randomUUID(),
    childId: UUID = UUID.randomUUID(),
    unitId: UUID = UUID.randomUUID(),
    type: PlacementType = PlacementType.DAYCARE,
    startDate: LocalDate = LocalDate.of(2019, 1, 1),
    endDate: LocalDate = LocalDate.of(2019, 12, 31)
): UUID {
    h
        .createUpdate(
            """
            INSERT INTO placement (id, child_id, unit_id, type, start_date, end_date)
            VALUES (:id, :childId, :unitId, :type::placement_type, :startDate, :endDate)
            """
        )
        .bindMap(
            mapOf(
                "id" to id,
                "childId" to childId,
                "unitId" to unitId,
                "type" to type.toString(),
                "startDate" to startDate,
                "endDate" to endDate
            )
        )
        .execute()
    return id
}

fun insertTestServiceNeed(
    h: Handle,
    childId: UUID,
    updatedBy: UUID,
    id: UUID = UUID.randomUUID(),
    startDate: LocalDate = LocalDate.of(2019, 1, 1),
    endDate: LocalDate? = LocalDate.of(2019, 12, 31),
    hoursPerWeek: Double = 40.0,
    partDay: Boolean = false,
    partWeek: Boolean = false,
    shiftCare: Boolean = false
): UUID {
    h
        .createUpdate(
            """
            INSERT INTO service_need (id, child_id, start_date, end_date, hours_per_week, part_day, part_week, shift_care, updated_by)
            VALUES (:id, :childId, :startDate, :endDate, :hoursPerWeek, :partDay, :partWeek, :shiftCare, :updatedBy)
            """
        )
        .bindMap(
            mapOf(
                "id" to id,
                "childId" to childId,
                "startDate" to startDate,
                "endDate" to endDate,
                "hoursPerWeek" to hoursPerWeek,
                "partDay" to partDay,
                "partWeek" to partWeek,
                "shiftCare" to shiftCare,
                "updatedBy" to updatedBy
            )
        )
        .execute()
    return id
}

fun insertTestIncome(
    h: Handle,
    objectMapper: ObjectMapper,
    personId: UUID,
    id: UUID = UUID.randomUUID(),
    validFrom: LocalDate = LocalDate.of(2019, 1, 1),
    validTo: LocalDate? = null,
    data: Map<IncomeType, IncomeValue> = mapOf(),
    effect: IncomeEffect = IncomeEffect.MAX_FEE_ACCEPTED,
    updatedAt: Instant = Instant.now(),
    updatedBy: UUID = UUID.randomUUID()
): UUID {
    h
        .createUpdate(
            """
            INSERT INTO income (id, person_id, valid_from, valid_to, data, effect, updated_at, updated_by)
            VALUES (:id, :personId, :validFrom, :validTo, :data, :effect::income_effect, :updatedAt, :updatedBy)
            """
        )
        .bindMap(
            mapOf(
                "id" to id,
                "personId" to personId,
                "validFrom" to validFrom,
                "validTo" to validTo,
                "data" to PGobject().apply {
                    type = "jsonb"
                    value = objectMapper.writeValueAsString(data)
                },
                "effect" to effect.toString(),
                "updatedAt" to Timestamp(updatedAt.toEpochMilli()),
                "updatedBy" to updatedBy
            )
        )
        .execute()
    return id
}

fun insertTestFeeAlteration(
    h: Handle,
    childId: UUID,
    id: UUID = UUID.randomUUID(),
    type: FeeAlteration.Type = FeeAlteration.Type.DISCOUNT,
    amount: Double = 50.0,
    isAbsolute: Boolean = false,
    validFrom: LocalDate = LocalDate.of(2019, 1, 1),
    validTo: LocalDate? = null,
    notes: String = "",
    updatedAt: Instant = Instant.now(),
    updatedBy: UUID
): UUID {
    h
        .createUpdate(
            """
            INSERT INTO fee_alteration (id, person_id, type, amount, is_absolute, valid_from, valid_to, notes, updated_at, updated_by)
            VALUES (:id, :childId, :type::fee_alteration_type, :amount, :isAbsolute, :validFrom, :validTo, :notes, :updatedAt, :updatedBy)
            """.trimIndent()
        )
        .bindMap(
            mapOf(
                "id" to id,
                "childId" to childId,
                "type" to type.toString(),
                "amount" to amount,
                "isAbsolute" to isAbsolute,
                "validFrom" to validFrom,
                "validTo" to validTo,
                "notes" to notes,
                "updatedAt" to updatedAt,
                "updatedBy" to updatedBy
            )
        )
        .execute()
    return id
}

fun Handle.insertTestPricing(pricing: DevPricing) = insertTestDataRow(
    pricing,
    """
INSERT INTO pricing (id, valid_from, valid_to, multiplier, max_threshold_difference, min_threshold_2, min_threshold_3, min_threshold_4, min_threshold_5, min_threshold_6, threshold_increase_6_plus)
VALUES (:id, :validFrom, :validTo, :multiplier, :maxThresholdDifference, :minThreshold2, :minThreshold3, :minThreshold4, :minThreshold5, :minThreshold6, :thresholdIncrease6Plus)
RETURNING id
    """.trimIndent()
)

fun Handle.insertTestVoucherValue(voucherValue: VoucherValue) = insertTestDataRow(
    voucherValue,
    "INSERT INTO voucher_value (id, validity, voucher_value) VALUES (:id, :validity, :voucherValue)"
)

fun Handle.insertTestDaycareGroup(group: DevDaycareGroup) = insertTestDataRow(
    group,
    """
INSERT INTO daycare_group (id, daycare_id, name, start_date)
VALUES (:id, :daycareId, :name, :startDate)
"""
)

fun insertTestDaycareGroupPlacement(
    h: Handle,
    daycarePlacementId: UUID = UUID.randomUUID(),
    groupId: UUID = UUID.randomUUID(),
    id: UUID = UUID.randomUUID(),
    startDate: LocalDate = LocalDate.of(2019, 1, 1),
    endDate: LocalDate = LocalDate.of(2019, 12, 31)
): UUID {
    h
        .createUpdate(
            """
                INSERT INTO daycare_group_placement (id, daycare_placement_id, daycare_group_id, start_date, end_date)
                VALUES (:id, :placementId, :groupId, :startDate, :endDate)
            """
        )
        .bindMap(
            mapOf(
                "id" to id,
                "placementId" to daycarePlacementId,
                "groupId" to groupId,
                "startDate" to startDate,
                "endDate" to endDate
            )
        )
        .execute()
    return id
}

fun insertTestPlacementPlan(
    h: Handle,
    applicationId: UUID,
    unitId: UUID,
    id: UUID = UUID.randomUUID(),
    type: PlacementType = PlacementType.DAYCARE,
    startDate: LocalDate = LocalDate.of(2019, 1, 1),
    endDate: LocalDate = LocalDate.of(2019, 12, 31),
    preschoolDaycareStartDate: LocalDate? = null,
    preschoolDaycareEndDate: LocalDate? = null,
    updated: Instant = Instant.now(),
    deleted: Boolean? = false
): UUID {
    h
        .createUpdate(
            """
            INSERT INTO placement_plan (id, unit_id, application_id, type, start_date, end_date, preschool_daycare_start_date, preschool_daycare_end_date, updated, deleted)
            VALUES (:id, :unitId, :applicationId, :type::placement_type, :startDate, :endDate, :preschoolDaycareStartDate, :preschoolDaycareEndDate, :updated, :deleted)
            """
        )
        .bindMap(
            mapOf(
                "id" to id,
                "applicationId" to applicationId,
                "unitId" to unitId,
                "type" to type.toString(),
                "startDate" to startDate,
                "endDate" to endDate,
                "preschoolDaycareStartDate" to preschoolDaycareStartDate,
                "preschoolDaycareEndDate" to preschoolDaycareEndDate,
                "updated" to updated,
                "deleted" to deleted
            )
        )
        .execute()
    return id
}

data class TestDecision(
    val createdBy: UUID,
    val sentDate: LocalDate = LocalDate.now(),
    val unitId: UUID,
    val applicationId: UUID,
    val type: DecisionType,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val status: DecisionStatus = DecisionStatus.PENDING,
    val requestedStartDate: LocalDate? = null,
    val resolvedBy: UUID? = null,
    val resolved: Instant? = resolvedBy?.let { Instant.ofEpochSecond(1546300800) }, // 2019-01-01 midnight
    val pendingDecisionEmailsSentCount: Int? = 0,
    val pendingDecisionEmailSent: Instant? = null
)

fun Handle.insertTestDecision(decision: TestDecision) = insertTestDataRow(
    decision,
    """
INSERT INTO decision (created_by, sent_date, unit_id, application_id, type, start_date, end_date, status, requested_start_date, resolved, resolved_by, pending_decision_emails_sent_count, pending_decision_email_sent)
VALUES (:createdBy, :sentDate, :unitId, :applicationId, :type, :startDate, :endDate, :status, :requestedStartDate, :resolved, :resolvedBy, :pendingDecisionEmailsSentCount, :pendingDecisionEmailSent)
RETURNING id
"""
)

fun Handle.insertTestAssistanceNeed(assistanceNeed: DevAssistanceNeed) = insertTestDataRow(
    assistanceNeed,
    """
INSERT INTO assistance_need (id, updated_by, child_id, start_date, end_date, capacity_factor, description, bases, other_basis)
VALUES (:id, :updatedBy, :childId, :startDate, :endDate, :capacityFactor, :description, :bases::assistance_basis[], :otherBasis)
RETURNING id
"""
)

fun Handle.insertTestAssistanceAction(assistanceAction: DevAssistanceAction) = insertTestDataRow(
    assistanceAction,
    """
INSERT INTO assistance_action (id, updated_by, child_id, start_date, end_date, actions, other_action, measures)
VALUES (:id, :updatedBy, :childId, :startDate, :endDate, :actions::assistance_action_type[], :otherAction, :measures::assistance_measure[])
RETURNING id
"""
)

fun insertTestCaretakers(
    h: Handle,
    groupId: UUID,
    id: UUID = UUID.randomUUID(),
    amount: Double = 3.0,
    startDate: LocalDate = LocalDate.of(2019, 1, 1),
    endDate: LocalDate? = null
) {
    h
        .createUpdate(
            """
            INSERT INTO daycare_caretaker (id, group_id, amount, start_date, end_date)
            VALUES (:id, :groupId, :amount, :startDate, :endDate)
            """.trimIndent()
        )
        .bindMap(
            mapOf(
                "id" to id,
                "groupId" to groupId,
                "amount" to amount,
                "startDate" to startDate,
                "endDate" to (endDate ?: LocalDate.of(9999, 1, 1))
            )
        )
        .execute()
}

fun insertTestStaffAttendance(
    h: Handle,
    id: UUID = UUID.randomUUID(),
    groupId: UUID,
    date: LocalDate,
    count: Double
) {
    //language=sql
    val sql =
        """
        INSERT INTO staff_attendance (id, group_id, date, count)
        VALUES (:id, :groupId, :date, :count)
        """.trimIndent()
    h.createUpdate(sql)
        .bindMap(
            mapOf(
                "id" to id,
                "groupId" to groupId,
                "date" to date,
                "count" to count
            )
        )
        .execute()
}

fun insertTestAbsence(
    h: Handle,
    id: UUID = UUID.randomUUID(),
    childId: UUID,
    date: LocalDate,
    careType: CareType,
    absenceType: AbsenceType = AbsenceType.SICKLEAVE,
    modifiedBy: String = "Someone"
) {
    //language=sql
    val sql =
        """
        INSERT INTO absence (id, child_id, date, care_type, absence_type, modified_by)
        VALUES (:id, :childId, :date, :careType, :absenceType, :modifiedBy)
        """.trimIndent()
    h.createUpdate(sql)
        .bindMap(
            mapOf(
                "id" to id,
                "childId" to childId,
                "date" to date,
                "careType" to careType,
                "absenceType" to absenceType,
                "modifiedBy" to modifiedBy
            )
        )
        .execute()
}

fun insertTestChildAttendance(
    h: Handle,
    id: UUID = UUID.randomUUID(),
    childId: UUID,
    unitId: UUID,
    arrived: Instant,
    departed: Instant?
) {
    //language=sql
    val sql =
        """
        INSERT INTO child_attendance (id, child_id, unit_id, arrived, departed)
        VALUES (:id, :childId, :unitId, :arrived, :departed)
        """.trimIndent()
    h.createUpdate(sql)
        .bindMap(
            mapOf(
                "id" to id,
                "childId" to childId,
                "unitId" to unitId,
                "arrived" to arrived,
                "departed" to departed
            )
        )
        .execute()
}

fun insertTestBackUpCare(
    h: Handle,
    childId: UUID,
    unitId: UUID,
    startDate: LocalDate,
    endDate: LocalDate,
    groupId: UUID? = null,
    id: UUID = UUID.randomUUID()
) {
    //language=sql
    val sql =
        """
        INSERT INTO backup_care (id, child_id, unit_id, start_date, end_date, group_id)
        VALUES (:id, :childId, :unitId, :startDate, :endDate, :groupId)
        """.trimIndent()
    h.createUpdate(sql)
        .bindMap(
            mapOf(
                "id" to id,
                "childId" to childId,
                "unitId" to unitId,
                "startDate" to startDate,
                "endDate" to endDate,
                "groupId" to groupId
            )
        )
        .execute()
}

fun insertTestBackupCare(
    h: Handle,
    backupCare: DevBackupCare
) = h.createUpdate(
    // language=SQL
    """
INSERT INTO backup_care (id, child_id, unit_id, group_id, start_date, end_date)
VALUES (:id, :childId, :unitId, :groupId, :startDate, :endDate)
"""
)
    .bind("id", backupCare.id ?: UUID.randomUUID())
    .bind("childId", backupCare.childId)
    .bind("unitId", backupCare.unitId)
    .bind("groupId", backupCare.groupId)
    .bind("startDate", backupCare.period.start)
    .bind("endDate", backupCare.period.end)
    .execute()

fun insertApplication(h: Handle, application: ApplicationWithForm): UUID {
    val id = application.id ?: UUID.randomUUID()

    //language=sql
    val sql =
        """
        INSERT INTO application(id, sentdate, duedate, status, guardian_id, child_id, origin, checkedbyadmin, hidefromguardian, transferapplication, other_guardian_id)
        VALUES(:id, :sentDate, :dueDate, :applicationStatus::application_status_type, :guardianId, :childId, :origin::application_origin_type, :checkedByAdmin, :hideFromGuardian, :transferApplication, :otherGuardianId)
        """.trimIndent()

    h.createUpdate(sql)
        .bind("id", id)
        .bind("sentDate", application.sentDate)
        .bind("dueDate", application.dueDate)
        .bind("applicationStatus", application.status)
        .bind("guardianId", application.guardianId)
        .bind("childId", application.childId)
        .bind("origin", application.origin)
        .bind("checkedByAdmin", application.checkedByAdmin)
        .bind("hideFromGuardian", application.hideFromGuardian)
        .bind("transferApplication", application.transferApplication)
        .bind("otherGuardianId", application.otherGuardianId)
        .execute()

    return id
}

fun insertApplicationForm(h: Handle, applicationForm: ApplicationForm): UUID {
    h.createUpdate(
        """
UPDATE application_form SET latest = FALSE
WHERE application_id = :applicationId AND revision < :revision
"""
    ).bind("applicationId", applicationForm.applicationId)
        .bind("revision", applicationForm.revision)
        .execute()
    val id = applicationForm.id ?: UUID.randomUUID()

    //language=sql
    val sql =
        """
        INSERT INTO application_form(id, application_id, created, revision, updated, document, latest)
        VALUES(:id, :applicationId, :created, :revision, :updated, :document::JSON, TRUE)
        """.trimIndent()

    h.createUpdate(sql)
        .bind("id", id)
        .bind("applicationId", applicationForm.applicationId)
        .bind("created", applicationForm.createdDate)
        .bind("revision", applicationForm.revision)
        .bind("updated", applicationForm.updated)
        .bind("document", objectMapper().writeValueAsString(applicationForm.document))
        .execute()

    return id
}

data class DevFamilyContact(
    val id: UUID,
    val childId: UUID,
    val contactPersonId: UUID,
    val priority: Int
)

fun Handle.insertFamilyContact(contact: DevFamilyContact) = insertTestDataRow(
    contact,
    """
INSERT INTO family_contact (id, child_id, contact_person_id, priority)
VALUES (:id, :childId, :contactPersonId, :priority)
RETURNING id
"""
)

fun Handle.deleteFamilyContact(id: UUID) = createUpdate("DELETE FROM family_contact WHERE id = :id").bind("id", id).execute()
